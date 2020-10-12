package interpreter.statement;

import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

/**
 * Created by Administrator on 4/23/2016.
 */
public class NAMEStatement extends Statement
{
    // This is the line number to transfer control too.
    private Vector args;

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

    @Override
    public String unparse()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(keyword.name()).append(" ");
        for (int i = 0; i < args.size(); i++)
        {
            PrintItem pi = (PrintItem) (args.elementAt(i));
            sb.append(pi.unparse());
        }
        return sb.toString();
    }

    private static void parse(NAMEStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s.args = StringExParser.parseStringExpression(lt);
    }

}
