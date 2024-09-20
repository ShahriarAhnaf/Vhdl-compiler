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

package ece351.w.rdescent;

import java.util.LinkedList;

import org.parboiled.common.ImmutableList;

import ece351.util.Lexer;
import ece351.w.ast.WProgram;
import ece351.w.ast.Waveform;

public final class WRecursiveDescentParser {
    private final Lexer lexer;
    // member WPgraom.... .

    public WRecursiveDescentParser(final Lexer lexer) {
        this.lexer = lexer;
    }

    public static WProgram parse(final String input) {
    	final WRecursiveDescentParser p = new WRecursiveDescentParser(new Lexer(input));
        return p.parse();
    }

    public Waveform waveform() {
        Waveform wave = new Waveform();
        // must start with keyword 
        if(lexer.inspectID()){
            wave = wave.rename(lexer.consumeID());
            // needs drip seperator
            if(lexer.inspect(":")){
                lexer.consume(":");

                // consumes all bits
                while(!lexer.inspect(";")){
                    if(lexer.inspect("0")){
                        wave = wave.append(lexer.consume("0"));
                    } else if(lexer.inspect("1")){
                        wave = wave.append(lexer.consume("1"));
                    }
                    else throw new IllegalArgumentException("invalid bit character found during parsing");
                }
                // consume end of waveform
                lexer.consume(";");
            }else throw new IllegalArgumentException("invalid after name ");
        } 
        else { // rejection.
            throw new IllegalArgumentException("invalid pin name or start of waveform");
        }
        return wave;
    }    
    public WProgram parse() {
        WProgram wavyP = new WProgram();
        wavyP = wavyP.append(waveform()); // Waveform+ : one or more waveforms cant be null
        while (!lexer.inspectEOF()) {
            wavyP = wavyP.append(waveform());
        }
        lexer.consumeEOF();
        return wavyP;
    }
}
