/*
 * NEXTStatement.java - Implement the NEXT Statement.
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

/**
 * The NEXT statement
 * <p>
 * The NEXT statement causes transfer to return to the line following it
 * corresponding FOR statement.
 * <p>
 * Syntax is :
 * NEXT [var]
 * <p>
 * Policy:
 * If the variable name is omitted, then this next matches any FOR
 * statement once. It is still illegal to enter a FOR statement in
 * the middle.
 */
public class NEXTStatement extends Statement
{

    // This is the line number to transfer control too.
    private Variable myVar;

    public NEXTStatement (LexicalTokenizer lt) {
        super(KeyWords.NEXT);

        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    /**
     * Parse NEXT Statement.
     */
    private static void parse (NEXTStatement s, LexicalTokenizer lt) {
        Token t = lt.nextToken();
        /*
         * Do NEXT statements *require* a variable? In many BASIC implementations
         * the variable is optional.
         */
        if (t.typeNum() != KeyWords.VARIABLE)
        {
            lt.unGetToken();
            return;
        }
        s.myVar = (Variable) t;
    }

    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        Statement xs;
        FORStatement s;

        /*
         *
         */
        do {
            xs = pgm.pop();
            if (xs == null) {
                throw new BASICRuntimeError("NEXT without FOR");
            }

            if (!(xs instanceof FORStatement)) {
                throw new BASICRuntimeError("Bogus intervening statement: " + xs.asString());
            }
            s = (FORStatement) xs;
            /*
             * Since we have the policy set to be "optional next variable"
             * We use a little trick here to 'bond' the next at run time.
             * When we get here, if the next statement has no variable, we
             * give it the variable of the first FOR statement we pop off
             * the stack.
             */
            if (myVar == null) {
                myVar = s.myVar;
            }
        } while (!s.myVar.name.equalsIgnoreCase(myVar.name));

        double stepValue = s.sExp.value(pgm);
        if (stepValue == 0)
        {
            throw new BASICRuntimeError("step value of 0.0 in for loop.");
        }
        pgm.setVariable(myVar, pgm.getVariable(myVar) + s.sExp.value(pgm));

        double endValue = s.eExp.value(pgm);
        double currentValue = pgm.getVariable(myVar);
        double startValue = s.nExp.value(pgm);

        if (startValue >= endValue)
        {
            if ((currentValue < endValue) || (currentValue > startValue))
            {
                return pgm.nextStatement(this);
            }
            else
            {
                pgm.push(s);
                return pgm.nextStatement(s);
            }
        }
        else
        {
            if ((currentValue > endValue) || (currentValue < startValue))
            {
                return pgm.nextStatement(this);
            }
            else
            {
                pgm.push(s);
                return pgm.nextStatement(s);
            }
        }

    }

}
