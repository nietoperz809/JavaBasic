/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interpreter.statement;

import applications.BasicGUI;
import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

public class SLEEPStatement extends Statement
{
    // This is the sleep time
    private int lineTarget;
    private final static int FOREVER = -1;

    public SLEEPStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.SLEEP);
        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        Statement s;
        s = pgm.nextStatement(this);
        try
        {
            if (lineTarget == FOREVER)
            {
                long id = Thread.currentThread().getId();
                CountDownLatch cd = new CountDownLatch(1);
                BasicGUI.latchMap.put(id, cd);
                try
                {
                    cd.await();
                }
                catch (InterruptedException ex)
                {
                    throw new BASICRuntimeError("cd wait interrupted");
                }
                BasicGUI.latchMap.remove(id);
            }
            else
                Thread.sleep(lineTarget);
        }
        catch (InterruptedException ex)
        {
            throw new BASICRuntimeError("SLEEP " + ex.toString());
        }
        return s;
    }

    @Override
    public String unparse()
    {
        return keyword.name() + " " + lineTarget;
    }

    /**
     * Parse GOTO Statement.
     */
    private static void parse(SLEEPStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        Token t = lt.nextToken();
        if (t.stringValue() != null && t.stringValue().equals("*"))
        {
            s.lineTarget = FOREVER;  // magic value
            return;
        }
        if (t.typeNum() != KeyWords.CONSTANT)
        {
            throw new BASICSyntaxError("positive number or -1 required");
        }
        s.lineTarget = (int) t.numValue();
    }

}
