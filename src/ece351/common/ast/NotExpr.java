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

package ece351.common.ast;

import ece351.common.visitor.ExprVisitor;
import kodkod.engine.bool.Operator.Nary;

public final class NotExpr extends UnaryExpr{
	public NotExpr(Expr argument) {
		super(argument);
	}

	public NotExpr(Object pop) {
		this( (Expr)pop );
	}

	public NotExpr() { this(null); }
	
	@Override
    protected final Expr simplifyOnce() {		
    	// simplify our child first
    			// !true = false
    			// !false = true
    		// !!x = x
    		// nothing changed
    		// something changed
		Expr e_true = ConstantExpr.make(false);
		Expr e_false = ConstantExpr.make(true);

		UnaryExpr result = newUnaryExpr(this.expr.simplify());
		if(result.expr.equals(e_true)){
			return e_false; // flip
		}
		else if (result.expr.equals(e_false)) {
			return e_true; // flip
		}
		else if(result.expr.getClass() == NotExpr.class){ // ??? better way
			NotExpr not = (NotExpr) expr; // get access
			return not.expr;
		} 
    	return result; // return 
    }
	
    public Expr accept(final ExprVisitor v){
    	return v.visitNot(this);
    }
	
	@Override
	public String operator() {
		return Constants.NOT;
	}
	@Override
	public UnaryExpr newUnaryExpr(final Expr expr) {
		return new NotExpr(expr);
	}

}
