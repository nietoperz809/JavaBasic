/*
 * DIMStatement.java - The DIMENSION statement.
 *
 * Copyright (c) 1996 Chuck McManis, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * CHUCK MCMANIS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. CHUCK MCMANIS
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package basic_1.statement;

import basic_1.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

/**
 * The DIMENSION statement.
 * <p>
 * The DIMENSION statement is used to declare arrays in the BASIC language.
 * Unlike scalar variables arrays must be declared before they are used. Three
 * policy decisions are in force: 1) Array and scalars share the same variable
 * name space so DIM A(1,1) and LET A = 20 don't work together. 2) Non-declared
 * arrays have no default declaration. Some BASICs will default an array
 * reference to a 10 element array. 3) Arrays are limited to four dimensions.
 * <p>
 * Statement syntax is : DIM var1(i1, ...), var2(i1, ...), ...
 * <p>
 * Errors: No arrays declared. Non-array declared.
 */
public class DIMStatement extends Statement {

    private Vector args;

    public DIMStatement(LexicalTokenizer lt) throws BASICSyntaxError {
        super(KeyWords.DIM);
        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    /**
     * Actually execute the dimension statement. What occurs is that the
     * declareArray() method gets called to define this variable as an array.
     */
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError {
        for (int i = 0; i < args.size(); i++) {
            Variable vi = (Variable) (args.elementAt(i));
            if (vi.name.startsWith("fn")) {
                throw new BASICRuntimeError("FN... not allowed as array name");
            }
            pgm.declareArray(vi);
        }
        return pgm.nextStatement(this);
    }

//    public String unparse()
//    {
//        StringBuilder sb = new StringBuilder();
//
//        sb.append(keyword.toString()).append(" ");
//        for (int i = 0; i < args.size(); i++)
//        {
//            Variable va = (Variable) args.elementAt(i);
//            sb.append(va.unparse());
//            if (i < args.size() - 1)
//            {
//                sb.append(", ");
//            }
//        }
//        return sb.toString();
//    }

    /**
     * Parse the DIMENSION statement.
     */
    private static void parse(DIMStatement s, LexicalTokenizer lt) throws BASICSyntaxError {
        Token t;
        Variable va;
        s.args = new Vector();

        while (true) {
            /* Get the variable name */
            t = lt.nextToken();
            if (t.typeNum() != KeyWords.VARIABLE) {
                if (s.args.size() == 0) {
                    throw new BASICSyntaxError("No arrays declared!");
                }
                lt.unGetToken();
                return;
            }
            va = (Variable) t;
            if (!va.isArray()) {
                throw new BASICSyntaxError("Non-array declaration.");
            }
            s.args.addElement(t);

            /* this could be a comma or the end of the statement. */
            t = lt.nextToken();
            if (!t.isSymbol(',')) {
                lt.unGetToken();
                return;
            }
        }
    }

}
