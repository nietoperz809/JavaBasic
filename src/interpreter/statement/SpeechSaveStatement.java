package interpreter.statement;

import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

/**
 * Created by Administrator on 6/10/2016.
 */
public class SpeechSaveStatement extends Statement
{
    // This is the line number to transfer control too.
    private Vector args;

    /**
     * Construct a new statement object with a valid key.
     *
     * @param lt
     */
    public SpeechSaveStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.SPFILE);
        if (lt.getBuffer() != null)
            parse(lt);
    }

    @Override
    public String unparse ()
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

    @Override
    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        String sss = StringExParser.printItemsToString (pgm, args);
        sss = sss.trim();
        pgm.setVoiceFilename (sss);
        return pgm.nextStatement(this);
    }

    private void parse (LexicalTokenizer lt) throws BASICSyntaxError
    {
        args = StringExParser.parseStringExpression(lt);
    }
}
