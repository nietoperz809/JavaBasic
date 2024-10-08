/*
 * ENDStatement.java - Implement the END Statement.
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

import basic_1.KeyWords;
import basic_1.LexicalTokenizer;
import basic_1.Program;
import basic_1.Statement;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * The END Statement
 *
 * Like the STOP statement, the END statement halts program execution at the
 * current statement, however unlike STOP the program cannot be resumed from the
 * point at which it exited. The syntax for the END statement is: END
 */
public class ENDStatement extends Statement
{

    public ENDStatement (LexicalTokenizer ignoredLt) {
        super(KeyWords.END);
    }

    public Statement doit(Program pgm, InputStream in, PrintStream out) {
        return null;
    }

}
