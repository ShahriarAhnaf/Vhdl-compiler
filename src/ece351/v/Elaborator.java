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

package ece351.v;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.parboiled.common.ImmutableList;

import ece351.common.ast.AndExpr;
import ece351.common.ast.AssignmentStatement;
import ece351.common.ast.ConstantExpr;
import ece351.common.ast.EqualExpr;
import ece351.common.ast.Expr;
import ece351.common.ast.NAndExpr;
import ece351.common.ast.NOrExpr;
import ece351.common.ast.NaryAndExpr;
import ece351.common.ast.NaryOrExpr;
import ece351.common.ast.NotExpr;
import ece351.common.ast.OrExpr;
import ece351.common.ast.Statement;
import ece351.common.ast.VarExpr;
import ece351.common.ast.XNOrExpr;
import ece351.common.ast.XOrExpr;
import ece351.common.visitor.PostOrderExprVisitor;
import ece351.util.CommandLine;
import ece351.v.ast.Architecture;
import ece351.v.ast.Component;
import ece351.v.ast.DesignUnit;
import ece351.v.ast.IfElseStatement;
import ece351.v.ast.Process;
import ece351.v.ast.VProgram;

/**
 * Inlines logic in components to architecture body.
 */
public final class Elaborator extends PostOrderExprVisitor {

	private final Map<String, String> current_map = new LinkedHashMap<String, String>();
	private final Map<String, DesignUnit> design_lookup = new LinkedHashMap<String, DesignUnit>();
	
	public static void main(String[] args) {
		System.out.println(elaborate(args));
	}
	
	public static VProgram elaborate(final String[] args) {
		return elaborate(new CommandLine(args));
	}
	
	public static VProgram elaborate(final CommandLine c) {
        final VProgram program = DeSugarer.desugar(c);
        return elaborate(program);
	}
	
	public static VProgram elaborate(final VProgram program) {
		final Elaborator e = new Elaborator();
		return e.elaborateit(program);
	}

	private VProgram elaborateit(final VProgram root) {

		// our ASTs are immutable. so we cannot mutate root.
		// we need to construct a new AST that will be the return value.
		// it will be like the input (root), but different.
		VProgram result = new VProgram();
		int compCount = 0;

		for(DesignUnit du: root.designUnits){
			// iterate over all of the designUnits in root.
			// for each one, construct a new architecture.
			// design_lookup.put(du.arch.architectureName, du);
			design_lookup.put(du.entity.identifier, du); // only need entity
			Architecture a = du.arch.varyComponents(ImmutableList.<Component>of());
			// this gives us a copy of the architecture with an empty list of components.
			// now we can build up this Architecture with new components.
			// In the elaborator, an architectures list of signals, and set of statements may change (grow)
			//populate dictionary/map
			for(String bruh : du.entity.input){
				current_map.put(bruh, bruh);
			}
			for(String bruh : du.entity.output){
				current_map.put(bruh, bruh);
			}

			for(Component c: du.arch.components) { // for every instance elaborate it.
				compCount++;
				
				// find DU
				DesignUnit comp_design = design_lookup.get(c.entityName);
				if(comp_design == null) continue;
				//add input signals, map to ports
				// always inputs first 
				for(int i=0; i < comp_design.entity.input.size(); i++) {
					// port -> inputs
					current_map.put(comp_design.entity.input.get(i) ,c.signalList.get(i));
				}
				for(int i=comp_design.entity.input.size(); i < comp_design.entity.output.size()+comp_design.entity.input.size(); i++){
				// add output signal map to port
					current_map.put(comp_design.entity.output.get(i-comp_design.entity.input.size()) ,c.signalList.get(i));
				}	
				//add local signals, add to signal list of current designUnit
				for(String local_sig : comp_design.arch.signals){
					a = a.appendSignal( "comp_" + compCount + "_" + local_sig);
					current_map.put(local_sig, "comp_" + compCount + "_" + local_sig);
				}						
				//loop through the statements in the architecture body		
				// assumption that all entitys being substituted are only statements by induction
				for(Statement s: comp_design.arch.statements){
					if(s instanceof AssignmentStatement){ 
					// make the appropriate variable substitutions for signal assignment statements
					// i.e., call changeStatementVars\
						s = changeStatementVars((AssignmentStatement)s);
					}
					else if (s instanceof Process){ // should be a process
						s  = expandProcessComponent((Process)s);
					}

					a = a.appendStatement(s);
				}
				
			}
			// simply append the old statements since they should just work.
			for(Statement s: du.arch.statements){
				if(s instanceof AssignmentStatement){ 
				// make the appropriate variable substitutions for signal assignment statements
				// i.e., call changeStatementVars\
					s = changeStatementVars((AssignmentStatement)s);
				}
				else if (s instanceof Process){ // should be a process
					s  = expandProcessComponent((Process)s);
				}
				a = a.appendStatement(s);
			}
			
			 // append this new architecture to result
			 DesignUnit new_du = new DesignUnit(a,du.entity);
			 result = result.append(new_du);
		}
		assert result.repOk();
		return result;
	}
	
	// you do not have to use these helper methods; we found them useful though
	private Process expandProcessComponent(final Process process) {
		Process p = new Process(ImmutableList.of(), ImmutableList.of());
		for(String s : process.sensitivityList){
			p.appendSensitivity(current_map.get(s)); // replacements
		}
		for(Statement s: process.sequentialStatements){
			if(s instanceof AssignmentStatement){
				s = changeStatementVars((AssignmentStatement)s);
			} else if (s instanceof IfElseStatement){
				s = changeIfVars((IfElseStatement)s);
			}
			p.appendStatement(s);
		}
		return p;
	}
	
	// you do not have to use these helper methods; we found them useful though
	private  IfElseStatement changeIfVars(final IfElseStatement s) {
		IfElseStatement result = new IfElseStatement(traverseExpr(s.condition));
			for(AssignmentStatement a : s.ifBody){
			result.appendToTrueBlock(changeStatementVars(a));
		}
		for(AssignmentStatement a : s.elseBody){
			result.appendToElseBlock(changeStatementVars(a));
		}
		return result;
	}

	// you do not have to use these helper methods; we found them useful though
	private AssignmentStatement changeStatementVars(final AssignmentStatement s){
		// final boss of nested statements
		return traverseAssignmentStatement(s.varyOutputVar((VarExpr) visitVar(s.outputVar)));
	}
	
	
	@Override
	public Expr visitVar(VarExpr e) {
		// TODO replace/substitute the variable found in the map
		// will make lots of duplicates this way
		assert current_map.get(e.identifier) != null;
		return new VarExpr(current_map.get(e.identifier));
	}
	
	// do not rewrite these parts of the AST
	@Override public Expr visitConstant(ConstantExpr e) { return e; }
	@Override public Expr visitNot(NotExpr e) { return e; }
	@Override public Expr visitAnd(AndExpr e) { return e; }
	@Override public Expr visitOr(OrExpr e) { return e; }
	@Override public Expr visitXOr(XOrExpr e) { return e; }
	@Override public Expr visitEqual(EqualExpr e) { return e; }
	@Override public Expr visitNAnd(NAndExpr e) { return e; }
	@Override public Expr visitNOr(NOrExpr e) { return e; }
	@Override public Expr visitXNOr(XNOrExpr e) { return e; }
	@Override public Expr visitNaryAnd(NaryAndExpr e) { return e; }
	@Override public Expr visitNaryOr(NaryOrExpr e) { return e; }
}
