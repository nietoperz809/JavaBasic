/*
 * REMStatement.java - A very simple statement type.
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

import interpreter.KeyWords;
import interpreter.LexicalTokenizer;
import interpreter.Program;
import interpreter.Statement;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * The REM Statement
 *
 * The REMark statement is used to insert comments into the source code. All
 * text after the REM keyword, up to the end of line, is ignored by the
 * interpreter. The syntax for the statement is: REM comment text
 */
public class REMStatement extends Statement
{
    private String comment;

    public REMStatement (LexicalTokenizer lt) {
        super(KeyWords.REM);

        if (lt.getBuffer() != null)
            comment = lt.asString();
    }

    public Statement doit(Program pgm, InputStream in, PrintStream out) {
        return pgm.nextStatement(this);
    }

    public String unparse()
    {
        return " REM " + comment;
    }
}
