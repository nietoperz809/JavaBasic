package basic_1.statement;

import applications.BasicGUI;
import basic_1.*;
import basic_1.streameditor.StreamingTextArea;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

public class SENDStatement extends Statement
{
    private Expression _thread;
    private Vector _text;

    public SENDStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.PLINE);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }
    private static void parse (SENDStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s._thread = s.getNumericArg(lt);
        s.checkComma(lt);
        s._text = StringExParser.parseStringExpression(lt);
    }

    public String unparse ()
    {
        return keyword.name() + " " + _thread + "," + _text;
    }


    @Override
    protected Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        Statement s;
        s = pgm.nextStatement(this);
        StreamingTextArea cd = BasicGUI.streamMap.get((long) _thread.value(pgm));
        if (cd != null)
        {
            String str = StringExParser.printItemsToString(pgm, _text);
            cd.fakeIn(str+"\n");
        }
        return s;
    }
}
