/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Fall 2021 (1219)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.f.techmapper;

import java.io.PrintWriter;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.parboiled.common.ImmutableList;

import kodkod.engine.bool.Operator.Nary;
import kodkod.util.collections.IdentityHashSet;
import ece351.common.ast.AndExpr;
import ece351.common.ast.AssignmentStatement;
import ece351.common.ast.ConstantExpr;
import ece351.common.ast.EqualExpr;
import ece351.common.ast.Expr;
import ece351.common.ast.NAndExpr;
import ece351.common.ast.NOrExpr;
import ece351.common.ast.NaryAndExpr;
import ece351.common.ast.NaryExpr;
import ece351.common.ast.NaryOrExpr;
import ece351.common.ast.NotExpr;
import ece351.common.ast.OrExpr;
import ece351.common.ast.VarExpr;
import ece351.common.ast.XNOrExpr;
import ece351.common.ast.XOrExpr;
import ece351.common.visitor.PostOrderExprVisitor;
import ece351.f.FParser;
import ece351.f.analysis.ExtractAllExprs;
import ece351.f.ast.FProgram;
import ece351.util.Examiner;

public final class TechnologyMapper extends PostOrderExprVisitor {

	/** Where we will write the output to. */
	private final PrintWriter out;
	
	/**
	 * Table of substitutions for common subexpression elimination. Note that
	 * this IdentityHashMap has non-deterministic iteration ordering. The staff
	 * solution iterates over substitutions.values(), but does not print those
	 * results directly: instead it stores those results in the nodes TreeSet
	 * (below) which sorts them before they are printed. When computing edges
	 * the staff solution uses this table only for lookups and does not iterate
	 * over the contents.
	 */
	private final IdentityHashMap<Expr,Expr> substitutions = new IdentityHashMap<Expr,Expr>();

	/**
	 * The set of nodes in our circuit diagram. (We'll produce a node for each
	 * .) We could just print the nodes directly to the output stream instead of
	 * building up this set, but then we might output the same node twice, and
	 * we might get a nonsensical order. The set uniqueness property ensure that
	 * we will ultimately print each node exactly once. TreeSet gives us
	 * deterministic iteration order: alphabetical.
	 */
	private final SortedSet<String> nodes = new TreeSet<String>();
	
	/**
	 * The set of edges in our circuit diagram. We could just print the edges
	 * directly to the output stream instead of building up this set, but then
	 * we might output the same edge twice. The set uniqueness property ensure
	 * that we will ultimately print each edge exactly once. LinkedHashSet gives
	 * us deterministic iteration order: insertion order. We need insertion
	 * order here because the elements will be inserted into this set by the
	 * post order traversal of the AST.
	 */
	private final Set<String> edges = new LinkedHashSet<String>();
	
	public TechnologyMapper(final PrintWriter out) {
		this.out = out;
	}
	
	public TechnologyMapper() {
		 this(new PrintWriter(System.out));
	}
	
	public static void main(final String arg) {
		main(new String[]{arg});
	}
	public static void main(final String[] args) {
		render(FParser.parse(args), new PrintWriter(System.out));
	}
	
	/**
	 * Translate an FProgram to Graphviz format.
	 */
	public static void render(final FProgram program, final PrintWriter out) {
		final TechnologyMapper tm = new TechnologyMapper(out);
		tm.render(program);
	}

	public void render(final FProgram program) {
		render(program, Examiner.Isomorphic);
	}
	
	private boolean de_morgan_viable(NaryExpr n){
		int expression_count = 0;
				int not_count = 0;
				// count not expresisons in children.
				for(Expr child : n.children){
					expression_count++;
					if(child instanceof NotExpr) not_count++;
				}
				// not a or not b or c == not (a and b and not c) SHOULD NOT HAPPEN
				if(not_count > Math.ceil(expression_count/2)) {
					// add the demorgan as the substitute for the expression
					return true;
				}
				return false;
	}
	/** Where the real work happens. */
	public void render(final FProgram program, final Examiner examiner) {
		header(out);
		
		// build a set of all of the exprs in the program
		 // Extract all equal expressions in the FProgram using ExtractAllExprs
		 IdentityHashSet<Expr> fake = ExtractAllExprs.allExprs(program);
		 for (Expr expr : fake) {
			for (Expr other_e : fake) {
				if(expr.isomorphic(other_e)) { 
					substitutions.put(expr,other_e);
				}
			}
		}
		FProgram visited = traverseFProgram(program); // traverse AST and build edges and nodes
		nodes.clear();
		edges.clear();
		substitutions.clear(); // new drip adding soon
		IdentityHashSet<Expr> allexprs = ExtractAllExprs.allExprs(visited);
		 for (Expr expr : allexprs) {
			for (Expr other_e : allexprs) {
				if(expr.isomorphic(other_e)) { 
					substitutions.put(expr,other_e);
				}
			}
		}
		// each assignment statement in the program to create output nodes and edges
		 for (AssignmentStatement stmt : visited.formulas) {
			 // Visit the expression to create nodes and edges for the expression's structure
			 edge(substitutions.get(visitExpr(stmt.expr)), visitExpr(stmt.outputVar));
		}
	 
		 //Print all unique nodes
		 for (String node : nodes) {
			 out.println(node);
		 }
	 
		 // Print all unique edges in the order they were added
		 for (String edge : edges) {
			 out.println(edge);
		 }

		// print footer
		footer(out);
		out.flush();
		
		// release memory
		substitutions.clear();
		nodes.clear();
		edges.clear();
	}

	
	private static void header(final PrintWriter out) {
		out.println("digraph g {");
		out.println("    // header");
		out.println("    rankdir=LR;");
		out.println("    margin=0.01;");
		out.println("    node [shape=\"plaintext\"];");
		out.println("    edge [arrowhead=\"diamond\"];");
		out.println("    // circuit ");
	}

	private static void footer(final PrintWriter out) {
		out.println("}");
	}

    /**
     * ConstantExpr follows the Singleton pattern, so we don't need
     * to look in the substitution table: we already know there are
     * only ConstantExpr objects in existence, one for True and one
     * for False.
     */
	@Override
	public Expr visitConstant(final ConstantExpr e) {
		node(e.serialNumber(), e.toString());
		return e;
	}

	@Override
	public Expr visitVar(final VarExpr e) {
		final Expr e2 = substitutions.get(e); // from already made table
		assert e2 != null : "no substitution for " + e + " " + e.serialNumber();
		node(e2.serialNumber(), e2.toString());
		return e;
	}

	@Override
	public Expr visitNot(final NotExpr e) {
			if(substitutions.get(e) == null){
				substitutions.put(e, e); // again a demorgan ahh problem
			}
			edge(visitExpr(e.expr), e);
			node(e.serialNumber(),e.serialNumber(), "../../gates/not_noleads.png");	
			return substitutions.get(e);
	}

	// these should not exist since Fprogram parses them into Nary..?
	@Override
	public Expr visitAnd(final AndExpr e) {
		edge(e.left, e);
		edge(e.right, e);
		node(substitutions.get(e).serialNumber(), substitutions.get(e).serialNumber(), "../../gates/and_noleads.png");
		return e;
	}

	@Override
	public Expr visitOr(final OrExpr e) {
		edge(e.left, e);
		edge(e.right, e);
		node(substitutions.get(e).serialNumber(),substitutions.get(e).serialNumber(), "../../gates/or_noleads.png");
		return e;
	}

	@Override public Expr visitNaryAnd(final NaryAndExpr e) {
		// and expr
			if(de_morgan_viable( (NaryExpr)e) ) {
				// change the ast...? by flipping
				NaryExpr n = (NaryExpr) e;
					ImmutableList<Expr> new_list = ImmutableList.of();
					for(Expr child : n.children){
						if(child instanceof NotExpr){
							new_list = new_list.append( ((NotExpr)child).expr );
						}else {
							NotExpr lol = new NotExpr(child);
							new_list = new_list.append(lol);
						}
					}
					NaryOrExpr nor = new NaryOrExpr(new_list);
					NotExpr le_not = new NotExpr(nor);
					substitutions.put(le_not, le_not);
					substitutions.put(nor, nor);
					return le_not; // new demorganed expression
			}
			else 
			{
				// node(substitutions.get(e).serialNumber(),substitutions.get(e).serialNumber(), "../../gates/or_noleads.png");
				// for (Expr child : e.children) {
				// 	visitExpr(child); // visit lower level first
				// }
				// for (Expr child : e.children) {
				// 	edge(child, substitutions.get(e));
				// }
				// return substitutions.get(e); // account for subexpression elimination
				// nary expressions might get changed mid traversal due to de morgans....
				// THUS! 
				if(substitutions.get(e) == null){
					substitutions.put(e,e);
				} 
				node(e.serialNumber(),e.serialNumber(), "../../gates/or_noleads.png");
				for (Expr child : e.children) {
					visitExpr(child); // visit lower level first
				}
				for (Expr child : e.children) {
					edge(child, e);
				}
				return e;
			}
	}
	@Override public Expr visitNaryOr(final NaryOrExpr e) { 
		// or expr
		if(de_morgan_viable((NaryExpr)e) ) {
			// change the ast...? by flipping
			NaryExpr n = (NaryExpr) e;
				ImmutableList<Expr> new_list = ImmutableList.of();
				for(Expr child : n.children){
					if(child instanceof NotExpr){
						new_list = new_list.append( ((NotExpr)child).expr );
					}else {
						NotExpr lol = new NotExpr(child);
						new_list = new_list.append(lol);
					}
				}
				return new NotExpr(new NaryAndExpr(new_list));
		}
		else 
		{
			// node(substitutions.get(e).serialNumber(),substitutions.get(e).serialNumber(), "../../gates/or_noleads.png");
			// for (Expr child : e.children) {
			// 	visitExpr(child); // visit lower level first
			// }
			// for (Expr child : e.children) {
			// 	edge(child, substitutions.get(e));
			// }
			// return substitutions.get(e); // account for subexpression elimination
			// THIS LOW KEY SHOULDNT WORK WTF
			// THUS! DUE TO SUBSTITUION NARY CHANGES....
			if(substitutions.get(e) == null){
				substitutions.put(e,e);
			} 
			node(e.serialNumber(),e.serialNumber(), "../../gates/and_noleads.png");
				for (Expr child : e.children) {
					visitExpr(child); // visit lower level first
				}
				for (Expr child : e.children) {
					edge(child, e);
				}
				return e ;
		}
	}

	private void node(final String name, final String label) {
		nodes.add("    " + name + "[label=\"" + label + "\"];");
	}

	private void node(final String name, final String label, final String image) {
		nodes.add(String.format("    %s [label=\"%s\", image=\"%s\"];", name, label, image));
	}

	private void edge(final Expr source, final Expr target) {
		assert substitutions.get(source) != null : "edge source no substitutions";
		assert substitutions.get(target) != null : "edge target no substitutions";
		edge(substitutions.get(source).serialNumber(), substitutions.get(target).serialNumber());
	}
	
	private void edge(final String source, final String target) {
		edges.add("    " + source + " -> " + target + " ;");
	}
	
	public Expr visitExpr(final Expr e) {
		// Check the type of expression and call the corresponding visit method
		if (e instanceof ConstantExpr) {
			return visitConstant((ConstantExpr) e);
		} else if (e instanceof VarExpr) {
			return visitVar((VarExpr) e);
		} else if (e instanceof NotExpr) {
			return visitNot((NotExpr) e);
		// } else if (e instanceof AndExpr) {
		// 	return visitAnd((AndExpr) e);
		// } else if (e instanceof OrExpr) {
		// 	return visitOr((OrExpr) e);
		} else if (e instanceof NaryAndExpr) {
			return visitNaryAnd((NaryAndExpr) e);
		} else if (e instanceof NaryOrExpr) {
			return visitNaryOr((NaryOrExpr) e);
		} else {
			throw new IllegalArgumentException("non supported expression type: " + e.getClass());
		}
	}
	@Override public Expr visitXOr(final XOrExpr e) { throw new IllegalStateException("TechnologyMapper does not support " + e.getClass()); }
	@Override public Expr visitNAnd(final NAndExpr e) { throw new IllegalStateException("TechnologyMapper does not support " + e.getClass()); }
	@Override public Expr visitNOr(final NOrExpr e) { throw new IllegalStateException("TechnologyMapper does not support " + e.getClass()); }
	@Override public Expr visitXNOr(final XNOrExpr e) { throw new IllegalStateException("TechnologyMapper does not support " + e.getClass()); }
	@Override public Expr visitEqual(final EqualExpr e) { throw new IllegalStateException("TechnologyMapper does not support " + e.getClass()); }

}
