/*
 * RANDOMIZEStatement.java - Implement the RANDOMIZE Statement.
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

package interpreter.statement;

import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * The RANDOMIZE statement.
 * <p>
 * The RANDOMIZE statement seeds the random number generator. Syntax is:
 * RANDOMIZE
 */
public class RANDOMIZEStatement extends Statement
{
    private Expression nExpn;
//    private boolean useTimeOfDay = false;

    public RANDOMIZEStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.RANDOMIZE);
        if (lt.getBuffer() != null)
        {
            Token t = lt.nextToken();
            switch (t.typeNum())
            {
                case OPERATOR:
                case CONSTANT:
                case VARIABLE:
                case FUNCTION:
                    lt.unGetToken();
                    nExpn = ParseExpression.expression(lt);
//                case KEYWORD:
//                    if (t.kwValue != KeyWords.TIMER)
//                    {
//                        throw new BASICSyntaxError("Badly formed randomize statement.");
//                    }
//                    useTimeOfDay = true;
                default:
                    lt.unGetToken();
            }
        }
    }


    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        if (nExpn != null)
        {
            pgm.randomize(nExpn.value(pgm));
        }
        else
        {
            pgm.randomize((double) System.currentTimeMillis());
        }
        return pgm.nextStatement(this);
    }

}
