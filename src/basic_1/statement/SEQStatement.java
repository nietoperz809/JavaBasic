package basic_1.statement;

import java.io.InputStream;
import java.io.PrintStream;

import basic_1.*;
import midisystem.MidiSynthSystem;
import midisystem.TrackMaker;

/**
 * SEQ based on DATA Statement
 */
public class SEQStatement extends Statement
{
    private String arg;
    private int channel;
    private VariableExpression ve;
    
    public SEQStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.SEQ);

        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        if (ve != null)
            arg = ve.stringValue(pgm);
        TrackMaker tm = new TrackMaker (MidiSynthSystem.get(), channel);
        try
        {
            tm.fromString(arg);
        }
        catch (Exception ex)
        {
           throw new BASICRuntimeError ("Bogus Sequence string");
        }
        return pgm.nextStatement(this);
    }

    @Override
    public String unparse()
    {
        return "SEQ "+arg;
    }

    /**
     * Parse SEQ Statement.
     */
    private static void parse(SEQStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        Token t = lt.nextToken();
        if (t.typeNum() != KeyWords.CONSTANT)
        {
            throw new BASICSyntaxError("Channel number missing.");
        }
        s.channel = (int) t.numValue();
        t = lt.nextToken();
        if (t.typeNum() != KeyWords.SYMBOL || t.nValue != ',')
        {
            throw new BASICSyntaxError("Comma expected.");
        }
        t = lt.nextToken();
        if (t.typeNum() == KeyWords.STRING)
        {
            s.arg = t.stringValue();
            return;
        }
        else if (t.typeNum() == KeyWords.VARIABLE)
        {
            s.ve = new VariableExpression((Variable)t);
            return;
        }
        throw new BASICSyntaxError("Only Sound String allowed.");
    }
}
