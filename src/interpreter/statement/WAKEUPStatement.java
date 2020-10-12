package interpreter.statement;

import applications.BasicGUI;
import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Administrator on 4/24/2016.
 */
public class WAKEUPStatement extends Statement
{
    private int lineTarget;

    public WAKEUPStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.WAKEUP);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    /**
     * Parse GOTO Statement.
     */
    private static void parse (WAKEUPStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        Token t = lt.nextToken();
        if (t.typeNum() != KeyWords.CONSTANT)
        {
            throw new BASICSyntaxError("thread ID required");
        }
        s.lineTarget = (int) t.numValue();
    }

    public String unparse ()
    {
        return keyword.name() +" " + lineTarget;
    }

    @Override
    public Statement doit (Program pgm, InputStream in, PrintStream out) {
        Statement s;
        s = pgm.nextStatement(this);
        CountDownLatch cd = BasicGUI.latchMap.get((long) lineTarget);
        if (cd != null)
        {
            cd.countDown();
        }
        return s;
    }

}
