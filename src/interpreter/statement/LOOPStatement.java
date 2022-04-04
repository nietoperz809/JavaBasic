package interpreter.statement;

import interpreter.*;
import interpreter.util.RedBlackTree;

import java.io.InputStream;
import java.io.PrintStream;

public class LOOPStatement extends Statement
{
    private Expression nExp;

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
                //System.out.println(exp);
            }
            else {
                throw new BASICSyntaxError("must be WHILE statement");
            }
        }
    }


    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError {
        Statement xs;
        DOStatement s;

        while (true) {
            double v;
            if (nExp == null)
                v = 1.0;
            else
                v = nExp.value(pgm);

            xs = pgm.pop();
            if (xs == null) {
                throw new BASICRuntimeError("DO without LOOP");
            }

            if (!(xs instanceof DOStatement)) {
                throw new BASICRuntimeError("Bogus intervening statement: " + xs.asString());
            }
            s = (DOStatement) xs;

            if (v == 0.0)
                return pgm.nextStatement(this);

            pgm.push(s);
            return pgm.nextStatement(s);
        }
    }
}
