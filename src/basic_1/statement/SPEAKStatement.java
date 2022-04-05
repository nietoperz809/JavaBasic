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
package basic_1.statement;

import basic_1.*;
import misc.MainWindow;

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
public class SPEAKStatement extends Statement
{
    // This is the line number to transfer control too.
    private Vector args;

    public SPEAKStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.SPEAK);
        if (lt.getBuffer() != null)
            parse(lt);
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        String sss = StringExParser.printItemsToString (pgm, args);
        MainWindow.voice.speak (sss);
        return pgm.nextStatement(this);
    }


    private void parse (LexicalTokenizer lt) throws BASICSyntaxError
    {
        args = StringExParser.parseStringExpression(lt);
    }

}
