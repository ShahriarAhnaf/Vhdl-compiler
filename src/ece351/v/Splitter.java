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

import java.util.LinkedHashSet;
import java.util.Set;

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
import ece351.v.ast.DesignUnit;
import ece351.v.ast.IfElseStatement;
import ece351.v.ast.Process;
import ece351.v.ast.VProgram;

/**
 * Process splitter.
 */
public final class Splitter extends PostOrderExprVisitor {
	private final Set<String> usedVarsInExpr = new LinkedHashSet<String>();

	public static void main(String[] args) {
		System.out.println(split(args));
	}
	
	public static VProgram split(final String[] args) {
		return split(new CommandLine(args));
	}
	
	public static VProgram split(final CommandLine c) {
		final VProgram program = DeSugarer.desugar(c);
        return split(program);
	}
	
	public static VProgram split(final VProgram program) {
		VProgram p = Elaborator.elaborate(program);
		final Splitter s = new Splitter();
		return s.splitit(p);
	}

	private VProgram splitit(final VProgram program) {
					// Determine if the process needs to be split into multiple processes
						// Split the process if there are if/else statements so that the if/else statements only assign values to one pin
		VProgram split_program = new VProgram();
		for(DesignUnit du : program.designUnits){
	// ALready Elaborated thus no components, just statements 
			Architecture a = du.arch.varyStatements(ImmutableList.of()); // get empty statements

			// append them by changing or NOT!
			for(Statement s: du.arch.statements){
				if(s instanceof Process){
					// loop through process statements
					Process p = ((Process)s).setStatements(ImmutableList.of()); // empty the new process 
					for(Statement process_s: ((Process)s).sequentialStatements) {
						if(process_s instanceof IfElseStatement) {
							// if ( ((IfElseStatement)process_s).ifBody.size() > 1) // symetrical case no need to check other body {
							// {
								// call split 0_0
								// RETURNS LIST OF NEW PROCESSES
								ImmutableList<Statement> split_if = splitIfElseStatement((IfElseStatement)process_s);
								
								// add them each into the arch
								for(Statement new_p: split_if){
									a = a.appendStatement(new_p); // new processes goes into the process statements
								}
							// }else{
							// 	p = p.appendStatement(process_s); // single if statements, just append the unalterned process back
							// }
						}
						else { // will have to test more..
							// non if statements inside the process preserved
							p = p.appendStatement(process_s); 
						}
					}
					if(p.sequentialStatements.size() != 0) a = a.appendStatement(p);
				} else {
					a = a.appendStatement(s); // just normal statement 
				}

			}
			
			DesignUnit new_d = new DesignUnit(a, du.entity);
			split_program = split_program.append(new_d);
		}

		return split_program;
	}
	
	// s

	// You do not have to use this helper method, but we found it useful
	// RETURNS PROCESSES
	private ImmutableList<Statement> splitIfElseStatement(final IfElseStatement ifStmt) {
		
		// havea  set of output vars simply need to create a bunch of new ifstatements/output var.
		ImmutableList<Statement> result = ImmutableList.of(); // bunch of processes
		// loop over each statement in the ifBody
		for(Statement s: ifStmt.ifBody){
			VarExpr ifvar = ((AssignmentStatement)s).outputVar;
			ImmutableList<AssignmentStatement> split_if = ImmutableList.of();
			ImmutableList<AssignmentStatement> split_else = ImmutableList.of();
			
			traverseAssignmentStatement((AssignmentStatement)s); // if body.
			traverseExpr(ifStmt.condition); // get the necessary vars
			split_if = split_if.append( (AssignmentStatement)s );
			// looking for
			Process p = new Process();
			 // loop over each statement in the elseBody
			for(Statement bruh: ifStmt.elseBody){
				if( ((AssignmentStatement)bruh).outputVar.equals(ifvar)){
					// proceed to making da sensitivity list.
					traverseAssignmentStatement((AssignmentStatement)bruh);
					split_else = split_else.append( (AssignmentStatement)bruh ) ;
				}	
			}
			for(String var: usedVarsInExpr){
				p = p.appendSensitivity(var);
			}
			// new if statement
			IfElseStatement new_if = new IfElseStatement(split_else, split_if, ifStmt.condition);
			p = p.appendStatement(new_if);
			result = result.append(p);
			usedVarsInExpr.clear(); // onto the next!
		}
		return result;
		
	}

	@Override
	public Expr visitVar(final VarExpr e) {
		this.usedVarsInExpr.add(e.identifier);
		return e;
	}

	// no-ops
	@Override public Expr visitConstant(ConstantExpr e) { return e; }
	@Override public Expr visitNot(NotExpr e) { return e; }
	@Override public Expr visitAnd(AndExpr e) { return e; }
	@Override public Expr visitOr(OrExpr e) { return e; }
	@Override public Expr visitXOr(XOrExpr e) { return e; }
	@Override public Expr visitNAnd(NAndExpr e) { return e; }
	@Override public Expr visitNOr(NOrExpr e) { return e; }
	@Override public Expr visitXNOr(XNOrExpr e) { return e; }
	@Override public Expr visitEqual(EqualExpr e) { return e; }
	@Override public Expr visitNaryAnd(NaryAndExpr e) { return e; }
	@Override public Expr visitNaryOr(NaryOrExpr e) { return e; }

}
