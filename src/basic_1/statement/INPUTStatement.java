/*
 * INPUTStatement.java - Implement the INPUT Statement.
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

import java.io.*;
import java.util.Vector;

/**
 * This is the INPUT statement.
 *
 * The syntax of the INPUT statement is : INPUT [ "prompt"; ] var1, var2, ...
 * varN
 *
 * The variables can be string variables or numeric variables but they cannot be
 * expressions. When reading into a string variable all characters up to the
 * first comma are stored into the string variable, unless the string is quoted
 * with " characters.
 *
 * If insufficient input is provided, the prompt is re-iterated for more data.
 * Syntax errors: Semi-colon expected after the prompt string. Malformed INPUT
 * statement. Runtime errors: Type mismatch.
 *
 */
public class INPUTStatement extends Statement
{

    /**
     * The prompt is displayed prior to requesting input.
     */
    private String prompt;

    /**
     * This vector holds a list of variables to fill
     */
    private Vector args;

    /**
     * Construct a new INPUT statement object.
     */
    public INPUTStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.INPUT);
        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    /**
     * Execute the INPUT statement. Most of the work is done in fillArgs.
     */
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        DataInputStream dis = new DataInputStream(in);
        getMoreData(dis, out, prompt);
        fillArgs(in, out, prompt, pgm, args);
        return (pgm.nextStatement(this));
    }

    /**
     * Reconstruct this statement from its parsed data.
     */

    /**
     * This is our buffer for processing INPUT statement requests.
     */
    private int currentPos = 500;
    private final char[] buffer = new char[256];
    String sbuff = "";

    private void getMoreData (DataInputStream inx, PrintStream out, String prompt) throws BASICRuntimeError
    {
        BufferedReader brx = new BufferedReader(new InputStreamReader(inx));
        if (prompt != null)
        {
            out.print(prompt);
        }
        else
        {
            out.print("?");
        }
        out.print(" ");

        try
        {
            sbuff = inx.readLine();
        }
        catch (IOException ioe)
        {
            throw new BASICRuntimeError(this, "I/O error on input.");
        }
        if (sbuff == null)
        {
            throw new BASICRuntimeError(this, "Out of data for INPUT.");
        }
        for (int i = 0; i < buffer.length; i++)
        {
            if (i == sbuff.length())
                break;
            buffer[i] = sbuff.charAt(i);
        }
        buffer[sbuff.length()] = '\n';
        currentPos = 0;
    }

    /*
     * Read a floating point number from the character buffer array.
     */
    private double getNumber (DataInputStream in, PrintStream out, String prompt) {
        currentPos = sbuff.length();
        return Double.parseDouble(sbuff);
    }

    private String getString (DataInputStream in, PrintStream out, String prompt) throws BASICRuntimeError
    {
        StringBuilder sb = new StringBuilder();

        if (currentPos >= buffer.length)
        {
            getMoreData(in, out, prompt);
        }

        while (Character.isWhitespace(buffer[currentPos]))
        {
            if (buffer[currentPos] == '\n')
            {
                getMoreData(in, out, prompt);
            }
            currentPos++;
            if (currentPos >= buffer.length)
            {
                getMoreData(in, out, prompt);
            }
        }

        boolean inQuote = false;
        while (true)
        {
            switch ((int) buffer[currentPos])
            {
                case '\n':
                    return (sb.toString()).trim();
                case '"':
                    if (buffer[currentPos + 1] == '"')
                    {
                        currentPos++;
                        sb.append('"');
                    }
                    else if (inQuote)
                    {
                        currentPos++;
                        return sb.toString();
                    }
                    else
                    {
                        inQuote = true;
                    }
                    break;
                case ',':
                    if (inQuote)
                    {
                        sb.append(',');
                    }
                    else
                    {
                        return (sb.toString()).trim();
                    }
                    break;
                default:
                    sb.append(buffer[currentPos]);
            }
            currentPos++;
            if (currentPos >= buffer.length)
            {
                sb.toString();
            }
        }
    }

    private void fillArgs (InputStream in, PrintStream out, String prompt, Program pgm, Vector v) throws BASICRuntimeError
    {
        DataInputStream d;

        if (in instanceof DataInputStream)
        {
            d = (DataInputStream) in;
        }
        else
        {
            d = new DataInputStream(in);
        }

        currentPos = 0; // skip "? "

        for (int i = 0; i < v.size(); i++)
        {
            Variable vi = (Variable) v.elementAt(i);
            if (buffer[currentPos] == '\n')
            {
                getMoreData(d, out, "(more)" + ((prompt == null) ? "?" : prompt));
            }
            if (!vi.isString())
            {
                pgm.setVariable(vi, getNumber(d, out, prompt));
            }
            else
            {
                pgm.setVariable(vi, getString(d, out, prompt));
            }
            while (true)
            {
                if (buffer[currentPos] == ',')
                {
                    currentPos++;
                    break;
                }

                if (buffer[currentPos] == '\n')
                {
                    break;
                }

                if (Character.isWhitespace(buffer[currentPos]))
                {
                    currentPos++;
                    continue;
                }
                throw new BASICRuntimeError(this, "Comma expected, got '" + buffer[currentPos] + "'.");
            }
        }
    }

    /**
     * Parse INPUT Statement.
     */
    private static void parse(INPUTStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        Token t;
        boolean needComma = false;
        s.args = new Vector();

        // get optional prompt string.
        t = lt.nextToken();
        if (t.typeNum() == KeyWords.STRING)
        {
            s.prompt = t.stringValue();
            t = lt.nextToken();
            if (!t.isSymbol(';') && !t.isSymbol(','))
            {
                throw new BASICSyntaxError("semi-colon or comma expected after prompt string.");
            }
        }
        else
        {
            lt.unGetToken();
        }
        while (true)
        {
            t = lt.nextToken();
            if (t.typeNum() == KeyWords.EOL)
            {
                return;
            }

            if (needComma)
            {
                if (!t.isSymbol(','))
                {
                    lt.unGetToken();
                    return;
                }
                needComma = false;
                continue;
            }
            if (t.typeNum() == KeyWords.VARIABLE)
            {
                s.args.addElement(t);
            }
            else
            {
                throw new BASICSyntaxError("malformed INPUT statement.");
            }
            needComma = true;
        }
    }
}
