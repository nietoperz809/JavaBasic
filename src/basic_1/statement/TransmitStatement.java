package basic_1.statement;

import basic_1.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

public class TransmitStatement extends Statement
{
    private Expression socketVariable;
    private Expression textVariable;


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
        s.socketVariable = s.getStringArg(lt);
        s.checkComma (lt);
        s.textVariable = s.getStringArg(lt);
        Token t5 = lt.nextToken(); // bracket close
    }


    @Override
    protected Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        Program.ExtendedSocket ext = pgm.sockMap.get (socketVariable.stringValue(pgm));
        String txt = textVariable.stringValue(pgm);
        try {
            if (ext.textMode) {
                PrintWriter pw = new PrintWriter(ext.sock.getOutputStream(), true);
                pw.println(txt);
            } else {
                byte[] dat = txt.getBytes();
                ext.sock.getOutputStream().write(dat);
            }
        } catch (Exception e) {
            System.out.println("socket tx fail: "+ e);
        }
        return pgm.nextStatement(this);
    }
}
