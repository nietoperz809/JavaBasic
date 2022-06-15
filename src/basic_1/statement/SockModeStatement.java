package basic_1.statement;

import basic_1.*;

import java.io.InputStream;
import java.io.PrintStream;

public class SockModeStatement extends Statement
{
    private Expression socketVariable;
    private Expression modeVariable;


    public SockModeStatement(LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.SOCKMODE);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    private static void parse (SockModeStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        Token t1 = lt.nextToken(); //numeric 40 bracket open
        s.socketVariable = s.getStringArg(lt);
        s.checkComma (lt);
        s.modeVariable = s.getNumericArg(lt);
        Token t5 = lt.nextToken(); // bracket close
    }


    @Override
    protected Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        Program.ExtendedSocket ext = pgm.sockMap.get (socketVariable.stringValue(pgm));
        int n = (int) modeVariable.value(pgm);
        if (n == 0 || n == 1)
            ext.textMode = (n==1);
        else
            throw new BASICRuntimeError("only 0 or 1 allowed");
        return pgm.nextStatement(this);
    }
}
