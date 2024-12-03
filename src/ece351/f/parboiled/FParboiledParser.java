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

package ece351.f.parboiled;

import java.lang.invoke.MethodHandles;

import org.parboiled.Rule;

import ece351.common.ast.AndExpr;
import ece351.common.ast.AssignmentStatement;
import ece351.common.ast.ConstantExpr;
import ece351.common.ast.Constants;
import ece351.common.ast.Expr;
import ece351.common.ast.NotExpr;
import ece351.common.ast.OrExpr;
import ece351.common.ast.VarExpr;
import ece351.f.ast.FProgram;
import ece351.util.CommandLine;

// Parboiled requires that this class not be final
public /*final*/ class FParboiledParser extends FBase implements Constants {

	public static Class<?> findLoadedClass(String className) throws IllegalAccessException {
        try {
            return MethodHandles.lookup().findClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> loadClass(byte[] code) throws IllegalAccessException {
        return MethodHandles.lookup().defineClass(code);
    }
	public static void main(final String[] args) {
    	final CommandLine c = new CommandLine(args);
    	final String input = c.readInputSpec();
    	final FProgram fprogram = parse(input);
    	assert fprogram.repOk();
    	final String output = fprogram.toString();
    	
    	// if we strip spaces and parens input and output should be the same
    	if (strip(input).equals(strip(output))) {
    		// success: return quietly
    		return;
    	} else {
    		// failure: make a noise
    		System.err.println("parsed value not equal to input:");
    		System.err.println("    " + strip(input));
    		System.err.println("    " + strip(output));
    		System.exit(1);
    	}
    }
	
	private static String strip(final String s) {
		return s.replaceAll("\\s", "").replaceAll("\\(", "").replaceAll("\\)", "");
	}
	
	public static FProgram parse(final String inputText) {
		final FProgram result = (FProgram) process(FParboiledParser.class, inputText).resultValue;
		assert result.repOk();
		return result;
	}

	
	@Override
	public Rule Program() {
		// For the grammar production Id, ensure that the Id does not match any of the keywords specified
		// in the rule, 'Keyword'
    	return Sequence(
            push(new FProgram()),
            OneOrMore(
                formula(),
                push( ((FProgram)pop(1)).append( (AssignmentStatement)pop() ) ) // 
            ),
            EOI
        );
        // lexer.consumeEOF();
        // assert fp.repOk();
        // return fp;
    }

    Rule formula() {
        return Sequence(
                var(),
                Optional(W1()),
                "<=",
                Optional(W1()),
                expr(),
                push(new AssignmentStatement(((VarExpr)pop(1)), (Expr)pop())),
                ZeroOrMore(W1()),
				";",
                ZeroOrMore(W1())
        );
     }
    
    Rule expr() { 
        // call term on every or seperated object
        return Sequence(
                term(),
                // push(match()), // expr
                ZeroOrMore(
					W1(),OR(), W1() ,term(),
                    push(new OrExpr((Expr)pop(1), (Expr)pop())) // term comes back with Expr on top.
                    )
        );
        // Expr e = term();
        // while(lexer.inspect("or")){
        //     if(lexer.inspect("or")){
        //         lexer.consume("or");
        //     }
        //     e = new OrExpr(e, term());
        // } // find all terms 
        // return e;
     } 

    Rule term() { 
        return Sequence(
                factor(), // will push either Constant / var 
                // push(match()), // expr
                ZeroOrMore(
                    		W1(),AND(), W1(), factor(),
                   			push(new AndExpr((Expr)pop(1), (Expr)pop())) // term comes back with Expr on top.
                    	)
        );
        // Expr e = factor();
        // while(lexer.inspect("and")){
        //     if(lexer.inspect("and")){
        //         lexer.consume("and");
        //     }
        //     e = new AndExpr(e, factor()); // not an and, must be a factor itself
        // }
        // return e;
    }
	Rule factor() { 
		return  FirstOf(
			Sequence(NOT(), W1(),factor(), push(new NotExpr((Expr)pop()))),
			Sequence("(", Optional(W1()), expr(), Optional(W1()),")"),
			var(),
			constant()
		);


            // if(lexer.inspect("not")){
            //     lexer.consume("not");
            //     return new NotExpr(factor()); // 
            // }
            // else if (lexer.inspect("(")){
            //     lexer.consume("(");
            //     Expr e = expr(); 
            //     if(lexer.inspect(")")){
            //         lexer.consume(")");
            //         return e;
            //     }
            //     else { // fails if braket is not closed.
            //         throw new IllegalArgumentException("Close parenthesis not found after expression");
            //     }
            // }
            // else if (peekConstant()){
            //     return constant();
            // }
            // else {
            //     return var();
            // }
    }
	Rule var() { 
        // checks if its an id 
        return Sequence(
                        TestNot(Keyword()),
                        Sequence(Char(), ZeroOrMore(FirstOf(Char(), Digit(), "_")) ), // need to accept _ and digits after too.... FORGOT UNTIL LAB 12 LOL
						push(new VarExpr(match()))
                    );
     }
	Rule constant() { 
        // 
        return Sequence(
                "'",
                FirstOf("0","1"),
                push(ConstantExpr.make((String)match())), // construct via string
                "'"
        );
        // lexer.consume("'");
    } 

}
