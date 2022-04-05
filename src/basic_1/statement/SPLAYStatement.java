/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package basic_1.statement;

import java.io.InputStream;
import java.io.PrintStream;

import basic_1.*;
import midisystem.MidiSynthSystem;

/**
 *
 * @author Administrator
 */
public class SPLAYStatement extends Statement
{
    private int repeats = 0;
    /**
     * CLC command
     * @param lt
     */
    public SPLAYStatement (LexicalTokenizer lt) {
        super(KeyWords.SPLAY);
        if (lt.getBuffer() != null)
        {
            Token t = lt.nextToken();
            if (t.type == KeyWords.EOL)
                return;
            if (t.type == KeyWords.CONSTANT)
                repeats = (int) t.nValue - 1;
            System.out.println(t);
        }
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        if (repeats > 0)
            MidiSynthSystem.get().setLoops(repeats);
        MidiSynthSystem.get().start();
        try
        {
            MidiSynthSystem.get().waitUntilEnd();
        }
        catch (Exception ex)
        {
            throw new BASICRuntimeError ("WaitUntilEnd failed");
        }
        return pgm.nextStatement(this);
    }
}
