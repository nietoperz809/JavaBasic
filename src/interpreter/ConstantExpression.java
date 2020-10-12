/*
 * ConstantExpression.java - An expression which is simply a constant.
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
package interpreter;

import java.io.PrintStream;

/**
 * This class implements the simplest possible expression, a constant.
 */
public class ConstantExpression extends Expression
{
    private double v;
    private String strCons;

    public ConstantExpression (double a)
    {
        super();
        v = a;
    }

    ConstantExpression(String a)
    {
        super();
        strCons = a;
    }

    public void print (PrintStream p)
    {
        p.print((strCons == null) ? v + " " : strCons);
    }

    public double value (Program pgm) throws BASICRuntimeError
    {
        if (strCons != null)
        {
            return 0;
        }
        return v;
    }

    public String unparse ()
    {
        if (strCons != null)
        {
            return ("\"" + strCons + "\"");
        }
        return "" + v;
    }

    String stringValue(Program pgm, int c) throws BASICRuntimeError
    {
        if (strCons != null)
        {
            return strCons;
        }
        return "" + v;
    }

    public String stringValue (Program pgm) throws BASICRuntimeError
    {
        if (strCons != null)
        {
            return strCons;
        }
        return "" + v;
    }

    public boolean isString ()
    {
        return (strCons != null);
    }

    public String toString()
    {
        if (strCons != null)
        {
            return strCons;
        }
        return v + " ";
    }
}
