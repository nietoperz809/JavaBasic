/*
 * LexicalTokenizer.java -  Parse the input stream into tokens.
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
package interpreter;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Vector;

/**
 * This class parses the keywords and symbols out of a line of BASIC code (or
 * command line) and returns them as tokens. Each tokenizer maintains state on
 * where it is in the process.
 */
public class LexicalTokenizer implements Serializable
{
    public static final long serialVersionUID = 1L;
    // multiple expressions can be chained with these operators

    private final static EnumSet<KeyWords> boolTokens
            = EnumSet.of(KeyWords.OP_BAND, KeyWords.OP_BIOR, KeyWords.OP_BXOR, KeyWords.OP_BNOT);
    private int currentPos = 0;
    private int previousPos = 0;
    private int markPos = 0;
    private char[] buffer = null;
    // we just keep this around 'cuz we return it a lot.
    private final Token EOLToken = new Token(KeyWords.EOL, 0);

    public char[] getBuffer()
    {
        return buffer;
    }

    public LexicalTokenizer (char data[])
    {
        buffer = data;
        currentPos = 0;
    }

    public String getFirstTokenInBuffer() {
        StringBuilder sb = new StringBuilder();
        for (char b : buffer) {
            if (b == 0 || b == '\n')
                break;
            sb.append(b);
        }
        return sb.toString();
    }

    /**
     * Returns true if there are more tokens to be returned.
     */
    boolean hasMoreTokens ()
    {
        return currentPos < buffer.length;
    }

    /**
     * Set's the current "mark" so that the line can be re-parsed from this
     * point.
     */
    void mark ()
    {
        markPos = currentPos;
    }

    /**
     * Reset the line pointer to the mark for reparsing.
     */
    void resetToMark ()
    {
        currentPos = markPos;
    }

    /**
     * Reset the tokenizer with a new data buffer.
     */
    void reset (char buf[])
    {
        buffer = buf;
        currentPos = 0;
    }

    /**
     * Reset the current buffer to zero.
     */
    void reset ()
    {
        currentPos = 0;
    }

    /**
     * Reset the tokenizer by first filling its data buffer with the passed in
     * string, then reset the mark to zero for parsing.
     */
    public void reset (String x)
    {
        int l = x.length();
        for (int i = 0; i < l; i++)
        {
            buffer[i] = x.charAt(i);
        }
        buffer[l] = '\n'; // mark the end of the line.
        currentPos = 0;
    }

    /**
     * Given that there has been an error, return the string in the buffer and a
     * line of dashes (-) followed by a caret (^) at the current position. This
     * indicates where the tokenizer was when the error occured.
     */
    String showError ()
    {
        int errorPos = previousPos;
        currentPos = 0;
        String txt = asString();
        StringBuilder sb = new StringBuilder();
        sb.append(txt).append("\n");
        for (int i = 0; i < errorPos; i++)
        {
            sb.append('-');
        }
        sb.append('^');
        return sb.toString();
    }

    /*
     * Return the buffer from the current position to the end as a string.
     */
    public String asString ()
    {
        int ndx = currentPos;

        while ((buffer[ndx] != '\n') && (buffer[ndx] != '\r'))
        {
            ndx++;
        }
        String result = new String(buffer, currentPos, ndx - currentPos);
        previousPos = currentPos;
        currentPos = ndx;
        return (result);
    }

    /**
     * Give back the last token, basically a reset to this token's start. This
     * function is used extensively by the parser to "peek" ahead in the input
     * stream.
     */
    public void unGetToken ()
    {
        if (currentPos != previousPos)
        {
            currentPos = previousPos;
        }
    }

    /**
     * This is the meat of this class, return the "next" token from the current
     * tokenizer buffer. If the token isn't recognized an ERROR token will be
     * returned.
     */
    public Token nextToken ()
    {
        Token r;
        // if we recurse then we need to know what the position was
        int savePos = currentPos;
        /*
         * Always return a token, even if it is just EOL
         */
        if (currentPos >= buffer.length)
        {
            return EOLToken;
        }
        /*
         * Save our previous position for unGetToken() to work.
         */
        previousPos = currentPos;
        /*
         * eat white space.
         */
        while (isSpace(buffer[currentPos]))
        {
            currentPos++;
        }
        /*
         * Start by checking all of the special characters.
         */
        switch (buffer[currentPos])
        {
            // Various lexical symbols that have meaning.
            case '+':
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_ADD);
            case '-':
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_SUB);
            case '*':
                if (buffer[currentPos + 1] == '*')
                {
                    currentPos += 2;
                    return new Token(KeyWords.OPERATOR, KeyWords.OP_EXP);
                }
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_MUL);
            case '/':
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_DIV);
            case '^':
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_XOR);
            case '&':
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_AND);
            case '|':
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_IOR);
            case '!':
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_NOT);
            case '=':
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_EQ);
            case '<':
                if (buffer[currentPos + 1] == '=')
                {
                    currentPos += 2;
                    return new Token(KeyWords.OPERATOR, KeyWords.OP_LE);
                }
                else if (buffer[currentPos + 1] == '>')
                {
                    currentPos += 2;
                    return new Token(KeyWords.OPERATOR, KeyWords.OP_NE);
                }
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_LT);
            case '>':
                if (buffer[currentPos + 1] == '=')
                {
                    currentPos += 2;
                    return new Token(KeyWords.OPERATOR, KeyWords.OP_GE);
                }
                else if (buffer[currentPos + 1] == '<')
                {
                    currentPos += 2;
                    return new Token(KeyWords.OPERATOR, KeyWords.OP_NE);
                }
                currentPos++;
                return new Token(KeyWords.OPERATOR, KeyWords.OP_GT);
            case '(':
            case '\'':
            case '?':
            case ')':
            case ':':
            case ';':
            case ',':
                return new Token(KeyWords.SYMBOL, buffer[currentPos++]);
            /* Else we fall through to the next CASE (numeric constant) */
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                r = parseNumericConstant();
                if (r != null)
                {
                    return r;
                }
                return new Token(KeyWords.SYMBOL, buffer[currentPos++]);
            // process EOL characters. (dump <CR><LF> as just EOL)
            case '\r':
            case '\n':
                while (currentPos < buffer.length)
                {
                    currentPos++;
                }
                return EOLToken;
            // text enclosed in "quotes" is a string constant.
            case '"':
                StringBuilder sb = new StringBuilder();
                currentPos++;
                while (true)
                {
                    switch (buffer[currentPos])
                    {
                        case '\n':
                            return new Token(KeyWords.ERROR, "Missing end quote.");
                        case '"':
                            if (buffer[currentPos + 1] == '"')
                            {
                                currentPos++;
                                sb.append('"');
                            }
                            else
                            {
                                currentPos++;
                                return new Token(KeyWords.STRING, sb.toString());
                            }
                            break;
                        default:
                            sb.append(buffer[currentPos]);
                    }
                    currentPos++;
                    if (currentPos >= buffer.length)
                    {
                        return new Token(KeyWords.ERROR, "Missing end quote.");
                    }
                }
            default:
                r = parseBooleanOp();
                if (r != null)
                {
                    return r;
                }
                break;
        }
        if (!isLetter(buffer[currentPos]))
        {
            return new Token(KeyWords.ERROR, "Unrecognized input.");
        }
        /* compose an identifier */
        StringBuilder q = new StringBuilder();
        while (isLetter(buffer[currentPos]) || isDigit(buffer[currentPos]))
        {
            q.append(Character.toLowerCase(buffer[currentPos]));
            currentPos++;
        }
        if (buffer[currentPos] == '$' || buffer[currentPos] == '%')
        {
            q.append(buffer[currentPos++]);
        }
        String t = q.toString();
        //
        for (KeyWords k : KeyWords.functions)
        {
            if (t.compareTo(k.toString()) == 0)
            {
                return new Token(KeyWords.FUNCTION, k);
            }
        }

        /* Is it a BASIC keyword ? */
        for (KeyWords k : KeyWords.keywords)
        {
            if (t.compareTo(k.toString()) == 0)
            {
                return new Token(KeyWords.KEYWORD, k);
            }
        }

        /* Is it a command ? */
        for (KeyWords k : KeyWords.commands)
        {
            if (t.compareTo(k.toString()) == 0)
            {
                return new Token(KeyWords.COMMAND, k);
            }
        }
        /*
         * It must be a variable.
         *
         * If this is an array reference, the variable name
         * will be followed by '(' index ',' index ',' index ')'
         * (one to four indices)
         */
        if (buffer[currentPos] == '(')
        {
            currentPos++;
            Vector<Expression> expVec = new Vector<>();
            Expression expn[];

            // This line sets the maximum number of indices.
            for (int i = 0; i < 4; i++)
            {
                Expression thisE;
                try
                {
                    thisE = ParseExpression.expression(this);
                }
                catch (BASICSyntaxError bse)
                {
                    return new Token(KeyWords.ERROR, "Error parsing array index.");
                }
                expVec.addElement(thisE);
                if (buffer[currentPos] == ')')
                {
                    currentPos++; // skip past the paren
                    expn = new Expression[expVec.size()]; // this recurses to us
                    for (int k = 0; k < expVec.size(); k++)
                    {
                        expn[k] = (Expression) (expVec.elementAt(k));
                    }
                    previousPos = savePos; // this is so we can "unget"
                    return new Variable(t, expn);
                }
                if (buffer[currentPos] != ',')
                {
                    return new Token(KeyWords.ERROR, "Missing comma in array index.");
                }
                currentPos++;
            }
        }
        return new Variable(t);
    }

    /**
     * Return true if char is whitespace.
     */
    private static boolean isSpace (char c)
    {
        return ((c == ' ') || (c == '\t'));
    }

    /**
     * This method will attempt to parse out a numeric constant. A numeric
     * constant satisfies the form: 999.888e777 where '999' is the optional
     * integral part. where '888' is the optional fractional part. and '777' is
     * the optional exponential part. The '.' and 'E' are required if the
     * fractional or exponential part are present, there can be no internal
     * spaces in the number. Note that unary minuses are always stripped as a
     * symbol.
     * <p>
     * Also note that until the second character is read .5 and .and. appear to
     * start similarly.
     */
    private Token parseNumericConstant ()
    {
        double m = 0;   // Mantissa
        double f = 0;   // Fractional component
        int oldPos = currentPos; // save our place.
        boolean wasNeg;
        boolean isConstant = false;
        //Token r = null;

        // Look for the integral part.
        while (isDigit(buffer[currentPos]))
        {
            isConstant = true;
            m = (m * 10.0) + (buffer[currentPos++] - '0');
        }

        // Now look for the fractional part.
        if (buffer[currentPos] == '.')
        {
            currentPos++;
            double t = .1;
            while (isDigit(buffer[currentPos]))
            {
                isConstant = true;
                f += (t * (buffer[currentPos++] - '0'));
                t /= 10.0;
            }
        }

        m = (m + f);
        /*
         * If we parse no mantissa and no fractional digits, it can't be a
         * numeric constant now can it?
         */
        if (!isConstant)
        {
            currentPos = oldPos;
            return null;
        }

        // so it was a number, perhaps we are done with it.
        if ((buffer[currentPos] != 'E') && (buffer[currentPos] != 'e'))
        {
            return new Token(KeyWords.CONSTANT, m); // no exponent return value.
        }
        currentPos++; // skip over the 'e'

        int p = 0;
        double e;
        wasNeg = false;

        // check for negative exponent.
        if (buffer[currentPos] == '-')
        {
            wasNeg = true;
            currentPos++;
        }
        else if (buffer[currentPos] == '+')
        {
            currentPos++;
        }

        while (isDigit(buffer[currentPos]))
        {
            p = (p * 10) + (buffer[currentPos++] - '0');
        }

        try
        {
            e = Math.pow(10, p);
        }
        catch (ArithmeticException zzz)
        {
            return new Token(KeyWords.ERROR, "Illegal numeric constant.");
        }

        if (wasNeg)
        {
            e = 1 / e;
        }
        return new Token(KeyWords.CONSTANT, (m + f) * e);
    }

    /**
     * Check the input stream to see if it is one of the boolean operations.
     */
//    Token parseBooleanOp()
//    {
//        int oldPos = currentPos;
//        StringBuffer sb = new StringBuffer();
//        int len = 0;
//        Token r = null;
//
//        if (buffer[currentPos] != '.')
//        {
//            return null;
//        }
//        sb.append('.');
//        currentPos++;
//        do
//        {
//            sb.append(buffer[currentPos + len]);
//            len++;
//        }
//        while ((len < 7) && isLetter(buffer[currentPos + len]));
//        if (buffer[currentPos + len] == '.')
//        {
//            sb.append('.');
//            len++;
//            String x = sb.toString();
//            for (int i = 0; i < boolOps.length; i++)
//            {
//                if (x.equalsIgnoreCase(boolOps[i]))
//                {
//                    r = new Token(Token.OPERATOR, boolOps[i], boolTokens[i]);
//                    break;
//                }
//            }
//            if (r != null)
//            {
//                currentPos += len;
//                return r;
//            }
//        }
//        currentPos = oldPos;
//        return null;
//    }
    private Token parseBooleanOp ()
    {
        int oldPos = currentPos;
        StringBuilder sb = new StringBuilder();
        int len = 0;
        Token r = null;

        do
        {
            sb.append(buffer[currentPos + len]);
            len++;
        }
        while (isLetter(buffer[currentPos + len]));
        String x = sb.toString();
        for (KeyWords k : boolTokens)
        {
            if (x.equalsIgnoreCase(k.toString()))
            {
                r = new Token(KeyWords.OPERATOR, k);
                break;
            }
        }
        if (r != null)
        {
            currentPos += len;
            return r;
        }
        currentPos = oldPos;
        return null;
    }

    /**
     * return true if char is between a-z or A=Z
     */
    private static boolean isLetter (char c)
    {
        return (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')));
    }

    /**
     * Return true if char is between 0 and 9
     */
    private static boolean isDigit (char c)
    {
        return ((c >= '0') && (c <= '9'));
    }

}
