package basic_1.statement;

import applications.BasicGUI;
import basic_1.*;
import basic_1.streameditor.StreamingTextArea;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Vector;

public class TransmitStatement extends Statement
{
    private Expression socket;
    private Expression text;


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
        Token t1 = lt.nextToken(); //numeric 40 bracket open
        s.socket = s.getStringArg(lt);
        Token t3 = lt.nextToken(); // numeric 44 comma
        s.text = s.getStringArg(lt);
        Token t5 = lt.nextToken(); // bracket close
    }


    @Override
    protected Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        Socket sock = pgm.sockMap.get (socket.stringValue(pgm));
        byte[] dat = text.stringValue(pgm).getBytes();
        try {
            sock.getOutputStream().write(dat);
        } catch (IOException e) {
            System.out.println("socket tx fail");
        }
        return pgm.nextStatement(this);
    }
}
