/*
 * FunctionExpression.java - this is a built in function call.
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

import misc.MainWindow;
import misc.Misc;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static java.lang.Runtime.getRuntime;

/**
 * This class implements the mathematical functions for BASIC. The tokenizer
 * will scan the input for one of the strings in <i>functions[]</i> and if it
 * finds it, it returns a token of FUNCTION with its value being that of the
 * index of the string into the array. (which are convientiently mapped into
 * named constants).
 * <p>
 * Parsing of the arguments to to the function are left up to the parse method
 * in this class.
 */
public class FunctionExpression extends Expression
{
    private Expression sVar; // STRING function variable.

    private Random r;

    @Override
    public void print (PrintStream p)
    {
        p.print (oper.toString ().toUpperCase ());
        p.print ("(");
        if (arg1 != null)
        {
            arg1.print (p);
        }
        if (arg2 != null)
        {
            arg2.print (p);
        }
    }

    @Override
    public String toString ()
    {
        return "FunctionExpression:: '";
    }


    private FunctionExpression (KeyWords t, Expression e)
    {
        super ();
        oper = t;
        arg2 = e;
    }

    private FunctionExpression (KeyWords t, Expression a1, Expression a2)
    {
        super ();
        arg1 = a1;
        arg2 = a2;
        oper = t;
    }

    public FunctionExpression(KeyWords t, LexicalTokenizer lt, Expression e) {
        super ();
        oper = t;
        arg2 = e;
    }

    @Override
    public double value (Program p) throws BASICRuntimeError
    {
        try
        {
            switch (oper)
            {
                case TID:
                    return Thread.currentThread ().getId ();
                case RND:
                    if (r == null)
                    {
                        r = p.getRandom ();
                    }
                    return (r.nextDouble () * arg2.value (p));
                case INT:
                    return Math.floor (arg2.value (p));
                case SIN:
                    return Math.sin (arg2.value (p));
                case FN:
                    return 0.0; //arg2.value (p);
                case COS:
                    return Math.cos (arg2.value (p));
                case TAN:
                    return Math.tan (arg2.value (p));
                case ATN:
                    return Math.atan (arg2.value (p));
                case SQR:
                    return Math.sqrt (arg2.value (p));
                case MAX:
                    return Math.max (arg1.value (p), arg2.value (p));
                case MIN:
                    return Math.min (arg1.value (p), arg2.value (p));
                case ABS:
                    return Math.abs (arg2.value (p));
                case LEN:
                    String s = arg2.stringValue (p);
                    return s.length ();
                case LOG:
                    return Math.log (arg2.value (p));
                case FRE:
                    return getRuntime().totalMemory();

                case TIME:
                    return System.currentTimeMillis () - p.basetime;

                case SPAWN:
                {
                    CompletableFuture<Long> future = new CompletableFuture<> ();
                    MainWindow.getInstance ().createMDIChild (applications.BasicGUI.class, future);
                    return future.join ();
                }

                case SGN:
                    double v = arg2.value (p);
                    if (v < 0)
                    {
                        return -1.0;
                    }
                    else if (v > 0)
                    {
                        return 1.0;
                    }
                    return 0.0;

                case VAL:
                    double dd;
                    String zz = (arg2.stringValue (p)).trim ();
                    try
                    {
                        dd = Double.parseDouble(zz);
                    } catch (NumberFormatException nfe)
                    {
                        throw new BASICRuntimeError ("Invalid string for VAL function.");
                    }
                    return dd;
                default:
                    throw new BASICRuntimeError ("Unknown or non-numeric function.");
            }
        } catch (Exception e)
        {
            if (e instanceof BASICRuntimeError)
            {
                throw (BASICRuntimeError) e;
            }
            else
            {
                throw new BASICRuntimeError ("Arithmetic Exception.");
            }
        }
    }

    @Override
    public boolean isString ()
    {
        switch (oper)
        {
            case GETNAME:
            case LEFT:
            case CONNECT:
            case RIGHT:
            case MID:
            case CHR:
            case STR:
            case IP:
            case SPC:
            case TAB:
            case INKEYS:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String stringValue (Program pgm) throws BASICRuntimeError
    {
        return stringValue (pgm, 0);
    }

    @Override
    String stringValue (Program pgm, int column) throws BASICRuntimeError
    {
        String ss = null;
        int len = 0;
        StringBuilder sb;
        int a;

        if (sVar != null)
        {
            ss = sVar.stringValue (pgm);
            len = ss.length ();
        }

        switch (oper)
        {
            case CONNECT:
            {
                int port = (int) arg2.value (pgm);
                try {
                    InetAddress ip = InetAddress.getByName(ss);
                    Socket sock = new Socket(ip, port);
                    pgm.sockMap.put(sock.toString(), sock);
                    return sock.toString();
                } catch (IOException e) {
                    return "failed";
                }
            }
            case LEFT:
                assert ss != null;
                return ss.substring (0, (int) arg2.value (pgm));
            case RIGHT:
                assert ss != null;
                return ss.substring (len - (int) arg2.value (pgm));
            case MID:
                int t = (int) arg1.value (pgm);
                assert ss != null;
                return ss.substring (t - 1, (t - 1) + (int) arg2.value (pgm));
            case CHR:
                return "" + (char) arg2.value (pgm);
            case GETNAME:
                return Thread.currentThread ().getName ();
            case INKEYS:
                if (pgm.area.lastKey == 0xffff)
                {
                    return "";
                }
                //pgm.area.inBuffer.reset();
                char c = pgm.area.lastKey;
                pgm.area.lastKey = 0xffff;
                return "" + c;

            case STR:
                return Misc.df.format (arg2.value (pgm));

            case IP:
                try {
                    InetAddress in = InetAddress.getByName(arg2.stringValue(pgm));
                    return in.getHostAddress();
                } catch (UnknownHostException e) {
                    return "unknown";
                }

            case SPC:
                sb = new StringBuilder();
                a = (int) arg2.value (pgm);
                for (int i = 0; i < a; i++)
                {
                    sb.append (' ');
                }
                return sb.toString ();

            case TAB:
                a = (int) arg2.value (pgm);
                sb = new StringBuilder();
                for (int i = column; i < a; i++)
                {
                    sb.append (' ');
                }
                return sb.toString ();

            default:
                return "Function not implemented yet.";
        }
    }

    /**
     * Parse a function argument. This code pulls off the '(' and ')' around the
     * arguments passed to the function and parses them.
     */
    public static FunctionExpression parse (KeyWords ty, LexicalTokenizer lt) throws BASICSyntaxError
    {
        FunctionExpression result;
        Expression a;
        Expression b;
        Expression se;
        Token t;

        if (ty == KeyWords.INKEYS ||
                ty == KeyWords.TIME ||
                ty == KeyWords.SPAWN ||
                ty == KeyWords.GETNAME)
        {
            return new FunctionExpression (ty, new ConstantExpression (0));
        }

        t = lt.nextToken ();

        if (!t.isSymbol ('('))
        {
            if (ty == KeyWords.TID)
            {
                lt.unGetToken ();
                return new FunctionExpression (ty, new ConstantExpression (1));
            }
            else if (ty == KeyWords.RND)
            {
                lt.unGetToken ();
                return new FunctionExpression (ty, new ConstantExpression (1));
            }
            else if (ty == KeyWords.FRE)
            {
                lt.unGetToken ();
                return new FunctionExpression (ty, new ConstantExpression (0));
            }
            throw new BASICSyntaxError ("Missing argument for function.");
        }
        switch (ty)
        {
            case TID:
            case RND:
            case INT:
            case SIN:
            case COS:
            case TAN:
            case ATN:
            case SQR:
            case ABS:
            case CHR:
            case VAL:
            case STR:
            case IP:
            case SPC:
            case TAB:
            case LOG:
                a = ParseExpression.expression (lt);
                if (a instanceof BooleanExpression)
                {
                    throw new BASICSyntaxError (ty.toString ().toUpperCase () + " function cannot accept boolean expression.");
                }
                if ((ty == KeyWords.VAL) && (!a.isString ()))
                {
                    throw new BASICSyntaxError (ty.toString ().toUpperCase () + " requires a string valued argument.");
                }
                result = new FunctionExpression (ty, a);
                break;

            case FN:
                a = ParseExpression.expression (lt);
                if (!a.isString ())
                {
                    throw new BASICSyntaxError ("function name missing.");
                }
                result = new FunctionExpression (ty, a);
                break;

                case MAX:
            case MIN:
                a = ParseExpression.expression (lt);
                if (a instanceof BooleanExpression)
                {
                    throw new BASICSyntaxError (ty + " function cannot accept boolean expression.");
                }
                t = lt.nextToken ();
                if (!t.isSymbol (','))
                {
                    throw new BASICSyntaxError (ty + " function expects two arguments.");
                }
                b = ParseExpression.expression (lt);
                if (b instanceof BooleanExpression)
                {
                    throw new BASICSyntaxError (ty + " function cannot accept boolean expression.");
                }
                result = new FunctionExpression (ty, a, b);
                break;

            case LEN:
                a = ParseExpression.expression (lt);
                if (!a.isString ())
                {
                    throw new BASICSyntaxError (ty
                            + " function expects a string argumnet.");
                }
                result = new FunctionExpression (ty, a);
                break;

            case CONNECT:
                case LEFT:
            case RIGHT:
                se = ParseExpression.expression (lt);
                if (!se.isString ())
                {
                    throw new BASICSyntaxError (
                            "Function expects a string expression.");
                }
                t = lt.nextToken ();
                if (!t.isSymbol (','))
                {
                    throw new BASICSyntaxError (ty
                            + " function requires two arguments.");
                }
                a = ParseExpression.expression (lt);
                result = new FunctionExpression (ty, a);
                result.sVar = se;
                break;

            case MID:
                se = ParseExpression.expression (lt);
                if (!se.isString ())
                {
                    throw new BASICSyntaxError (
                            "Function expects a string expression.");
                }
                t = lt.nextToken ();
                if (!t.isSymbol (','))
                {
                    throw new BASICSyntaxError (ty
                            + " function requires at least two arguments.");
                }
                a = ParseExpression.expression (lt);
                t = lt.nextToken ();
                if (t.isSymbol (')'))
                {
                    b = new ConstantExpression (1.0);
                    lt.unGetToken ();
                }
                else if (t.isSymbol (','))
                {
                    b = ParseExpression.expression (lt);
                }
                else
                {
                    throw new BASICSyntaxError (ty
                            + " unexpected symbol in expression.");
                }
                result = new FunctionExpression (ty, a, b);
                result.sVar = se;
                break;
            default:
                throw new BASICSyntaxError ("Unknown function on input.");

        }
        t = lt.nextToken ();
        if (!t.isSymbol (')'))
        {
            throw new BASICSyntaxError ("Missing closing parenthesis for function.");
        }
        return result;
    }
}
