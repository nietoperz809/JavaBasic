/*
 * GOSUBStatement.java - Implement the GOSUB Statement.
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
 * The GOSUB statement.
 *
 * Like the GOTO statement, the GOSUB statement unconditionally tranfers control
 * to a non-linear sequence in the program. However, unlike GOTO the position is
 * remembered on the stack so that executing a RETURN statement will return
 * execution to the statement following the GOSUB. The destination is indicated
 * by a line number.
 *
 * Syntax : GOSUB line
 *
 * Syntax Errors: Line number required.
 *
 * Runtime Errors: Non-existent line number.
 */
public class GOSUBStatement extends Statement
{

    // This is the line number to transfer control too.
    private Expression lineTarget;

    public GOSUBStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.GOSUB);
        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        pgm.push(this);
        Statement s;
        int line = (int) lineTarget.value(pgm);
        s = pgm.getStatement (line);
        if (s != null)
        {
            return s;
        }
        throw new BASICRuntimeError("GOSUB non-existent line " + line + ".");
    }

    public String unparse()
    {
        return keyword.toString()+" " + lineTarget;
    }

    /**
     * Parse GOSUB Statement.
     */
    private static void parse(GOSUBStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s.lineTarget = s.getNumericArg(lt);
    }
}
