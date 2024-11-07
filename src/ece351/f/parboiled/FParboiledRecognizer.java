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

import java.lang.foreign.Linker.Option;
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

//Parboiled requires that this class not be final
public /*final*/ class FParboiledRecognizer extends FBase implements Constants {

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
	
	public static void main(final String... args) {
		final CommandLine c = new CommandLine(args);
    	process(FParboiledRecognizer.class, c.readInputSpec());
    }

	@Override
	public Rule Program() {
		// STUB: return NOTHING; // TODO: replace this stub
		// For the grammar production Id, ensure that the Id does not match any of the keywords specified
		// in the rule, 'Keyword'
    	return Sequence(
            OneOrMore(
                formula()
            ),
            EOI
        );
    }

    Rule formula() {
        return Sequence(
                var(),
                Optional(W1()),
                "<=",
                Optional(W1()),
                expr(),
                ";",
                ZeroOrMore(W1())
        );
     }
    
    Rule expr() { 
        // call term on every or seperated object
        return Sequence(
                term(),
                ZeroOrMore(
                    W1(),OR(), W1() ,term() // term comes back with Expr on top.
                    )
        );
     } 

    Rule term() { 
        return Sequence(
                factor(),
                ZeroOrMore(
                    W1(),AND(), W1(), factor() // term comes back with Expr on top.
                    )
        );
    }
	Rule factor() { 
        return  FirstOf(
                    Sequence(NOT(), W1(),factor()),
                    Sequence("(", Optional(W1()), expr(), Optional(W1()),")"),
                    var(),
                    constant()
                );
    }
	Rule var() { 
        // checks if its an id 
        return Sequence(
                        TestNot(Keyword()),
                        OneOrMore(Char()) 
                    );
     }
	Rule constant() { 
        // 
        return Sequence(
                "'",
                FirstOf("0","1"),// construct via string
                "'"
        );
    } 


}
