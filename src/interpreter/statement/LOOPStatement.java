package interpreter.statement;

import interpreter.*;
import interpreter.util.RedBlackTree;

import java.io.InputStream;
import java.io.PrintStream;

public class LOOPStatement extends Statement
{
    public LOOPStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.LOOP);

        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    private static void parse (LOOPStatement s, LexicalTokenizer lt) {
    }


    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError {
        Statement xs;
        DOStatement s;

        while (true) {
            xs = pgm.pop();
            if (xs == null) {
                throw new BASICRuntimeError("DO without LOOP");
            }

            if (!(xs instanceof DOStatement)) {
                throw new BASICRuntimeError("Bogus intervening statement: " + xs.asString());
            }
            s = (DOStatement) xs;

            pgm.push(s);
            return pgm.nextStatement(s);
        }
    }
}
