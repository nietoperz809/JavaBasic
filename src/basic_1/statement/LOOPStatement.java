package basic_1.statement;

import basic_1.*;

import java.io.InputStream;
import java.io.PrintStream;

public class LOOPStatement extends Statement
{
    private Expression nExp;
    private boolean neg;

    public LOOPStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.LOOP);

        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    private static void parse (LOOPStatement s, LexicalTokenizer lt) throws BASICSyntaxError {
        Token t = lt.nextToken();
        if (t.typeNum() == KeyWords.EOL)
        {
            lt.unGetToken();
        }
        else {
            if (t.stringValue().equals("while"))
            {
                s.nExp = ParseExpression.expression(lt);
                s.neg = false;
            }
            else if (t.stringValue().equals("until"))
            {
                s.nExp = ParseExpression.expression(lt);
                s.neg = true;
            }
            else {
                throw new BASICSyntaxError("must be WHILE statement");
            }
        }
    }


    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError {
        Statement xs;
        DOStatement s;

        //while (true) {
            boolean v;
            if (nExp == null)
                v = true;
            else
                v = (nExp.value(pgm) != 0.0) ^ neg;

            xs = pgm.pop();
            if (xs == null) {
                throw new BASICRuntimeError("DO without LOOP");
            }

            if (!(xs instanceof DOStatement)) {
                throw new BASICRuntimeError("Bogus intervening statement: " + xs.asString());
            }
            s = (DOStatement) xs;

            if (!v)
                return pgm.nextStatement(this);

            pgm.push(s);
            return pgm.nextStatement(s);
        //}
    }
}
