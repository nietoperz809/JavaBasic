

/*
 */
package basic_1.statement;

import java.io.InputStream;
import java.io.PrintStream;
import javax.sound.midi.InvalidMidiDataException;

import basic_1.*;
import midisystem.MidiSynthSystem;

/**
 */
public class SSPEEDStatement extends Statement
{
    // This is the line number to transfer control too.
    private int speed;

    public SSPEEDStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.SSPEED);

        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    @Override
    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        try
        {
            MidiSynthSystem.get().setSpeed(speed);
        }
        catch (InvalidMidiDataException ex)
        {
           throw new BASICRuntimeError ("Command execution failed");
        }
        return pgm.nextStatement(this);
    }

    @Override
    public String unparse ()
    {
        return keyword.name() + " " + speed;
    }

    /**
     * Parse GOTO Statement.
     */
    private static void parse(SSPEEDStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        Token t = lt.nextToken();
        if (t.typeNum() != KeyWords.CONSTANT)
        {
            throw new BASICSyntaxError("number required after SSPEED.");
        }
        s.speed = (int) t.numValue();
    }

}
