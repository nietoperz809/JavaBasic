package basic_1.statement;

import applications.BasicGUI;
import basic_1.*;
import basic_1.streameditor.StreamingTextArea;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

public class TransmitStatement extends Statement
{
    private String socket;
    private Vector _text;

    public TransmitStatement(LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.TRANSMIT);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }
    private static void parse (TransmitStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        Vector args = StringExParser.parseStringExpression(lt);
        System.out.println(args);

//        s.socket = s.
//        s.checkComma(lt);
//        s._text = StringExParser.parseStringExpression(lt);
    }

//    public String unparse ()
//    {
//        return keyword.name() + " " + _thread + "," + _text;
//    }


    @Override
    protected Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        Statement s;
        s = pgm.nextStatement(this);
//        StreamingTextArea cd = BasicGUI.streamMap.get((long) _thread.value(pgm));
//        if (cd != null)
//        {
//            String str = StringExParser.printItemsToString(pgm, _text);
//            cd.fakeIn(str+"\n");
//        }
        return s;
    }
}
