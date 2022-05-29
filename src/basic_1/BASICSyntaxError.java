/*
 * BASICSyntaxError.java
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
 * Thrown by the parser if it can't parse an input line.
 */
public class BASICSyntaxError extends BASICError {
    /**
     * A Syntax error with message <i>errorMessage</i>
     * @param errorMessage
     */
    public BASICSyntaxError(String errorMessage) {
        super(errorMessage);
    }

// --Commented out by Inspection START (5/29/2022 7:58 PM):
//    BASICSyntaxError(Statement thisStatement, String errorMessage) {
//        super(thisStatement, errorMessage);
//    }
// --Commented out by Inspection STOP (5/29/2022 7:58 PM)

    /**
     * Return the syntax error message.
     * @return 
     */
    @Override
    public String getMsg() { return " "+super.getMsg(); }
}