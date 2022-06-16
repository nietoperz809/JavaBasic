package basic_1.statement;

import basic_1.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

/**
 * Created by Administrator on 4/23/2016.
 */
public class NAMEStatement extends Statement
{
    // This is the line number to transfer control too.
    private Vector<PrintItem> args;

    public NAMEStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.NAME);
        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        String sss = StringExParser.printItemsToString (pgm, args);
        Thread.currentThread().setName(sss);
        //pgm.
        System.out.println(sss);
        return pgm.nextStatement(this);
    }


    private static void parse(NAMEStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s.args = StringExParser.parseStringExpression(lt);
    }

}
