/*
 * PRINTStatement.java - Implement the PRINT Statement.
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
import java.util.Vector;

/**
 * The PRINT statement.
 *
 * The PRINT statement writes values out to the output stream. It can print both
 * numeric and string exressions.
 *
 * The syntax of the PRINT statement is : PRINT Expression [, Expression] | [;
 * Expression]
 *
 * Items separated by a semicolon will have no space between them, items
 * separated by a comma will have a tab inserted between them.
 *
 * Syntax Errors: Unexpected symbol in input.
 */
public class PRINTStatement extends Statement
{
    // This is the line number to transfer control too.
    private Vector<PrintItem> args;

    public PRINTStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.PRINT);
        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        PrintItem pi = null;
        int col = 0;
        for (int i = 0; i < args.size(); i++)
        {
            String z;
            pi = (PrintItem) (args.elementAt(i));
            z = pi.value(pgm, col);
            out.print(z);
            col += z.length();
        }
        if ((pi == null) || pi.needCR())
        {
            out.print("\n");
        }
        return pgm.nextStatement(this);
    }

    @Override
    public String unparse()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("PRINT ");
        for (int i = 0; i < args.size(); i++)
        {
            PrintItem pi = (PrintItem) (args.elementAt(i));
            sb.append(pi.unparse());
        }
        return sb.toString();
    }


    private static void parse(PRINTStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s.args = StringExParser.parseStringExpression(lt);
    }

}
