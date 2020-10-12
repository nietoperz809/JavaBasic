/*
 * TRONStatement.java - Implement the TRON Statement.
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
 * The TRON statement.
 *
 * The TRON statement turns TRacing ON. Syntax is:
 *      TRON ["string expression"]
 *
 * If filename is supplied the trace output is directed there.
 */
public class TRONStatement extends Statement
{

    // This is the line number to transfer control too.
    private Expression traceFile;

    public TRONStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.TRON);

        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError {
        if (traceFile != null)
            pgm.trace(true, traceFile.stringValue(pgm));
        pgm.trace(true);
        return pgm.nextStatement(this);
    }

    public String unparse() {
        return "TRON "+((traceFile != null) ? "\""+traceFile.unparse()+"\"" : "");
    }

    /**
     * Parse TRON Statement.
     */
    private static void parse(TRONStatement s, LexicalTokenizer lt) throws BASICSyntaxError {
        Token t = lt.nextToken();
        if ((t.typeNum() == KeyWords.EOL) || (t.isSymbol(':'))) {
            lt.unGetToken();
            return;
        }
        lt.unGetToken();
        s.traceFile = ParseExpression.expression(lt);
        if (! s.traceFile.isString()) {
            throw new BASICSyntaxError("String expression expected.");
        }
    }
}
