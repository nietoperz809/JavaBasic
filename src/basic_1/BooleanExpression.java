/*
 * BooleanExpression.java
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
package basic_1;

/**
 * This class implements Boolean expression parsing and evaluation. unlike
 * normal arithmetic expressions, boolean expressions are only true or false.
 */
public class BooleanExpression extends Expression
{
    BooleanExpression(KeyWords t, Expression a, Expression b) {
        super(t, a, b);
    }

    BooleanExpression(KeyWords t, Expression a) {
        super(t, a);
    }

    @Override
    public double value (Program pgm) throws BASICRuntimeError
    {
        if (arg1 != null && arg2 != null)
        {
            if (!(arg1.isString() || arg2.isString()))
            {
                return super.value(pgm);
            }
        }
        
        switch (oper)
        {
            case OP_EQ:
                return (arg1.stringValue(pgm).compareTo(arg2.stringValue(pgm)) == 0) ? 1 : 0;
            case OP_NE:
                return (arg1.stringValue(pgm).compareTo(arg2.stringValue(pgm)) != 0) ? 1 : 0;
            case OP_LT:
                return (arg1.stringValue(pgm).compareTo(arg2.stringValue(pgm)) < 0) ? 1 : 0;
            case OP_LE:
                return (arg1.stringValue(pgm).compareTo(arg2.stringValue(pgm)) <= 0) ? 1 : 0;
            case OP_GT:
                return (arg1.stringValue(pgm).compareTo(arg2.stringValue(pgm)) > 0) ? 1 : 0;
            case OP_GE:
                return (arg1.stringValue(pgm).compareTo(arg2.stringValue(pgm)) >= 0) ? 1 : 0;
            default:
                return super.value(pgm);
        }
    }

}
