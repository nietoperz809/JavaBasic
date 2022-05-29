package basic_1.statement;

import basic_1.*;
import org.mathIT.util.FunctionParser;

import java.io.InputStream;
import java.io.PrintStream;

public class DEFStatement extends Statement
{

    String func;
    String name;
    public DEFStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.DEF);
        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    public Statement doit(Program pgm, InputStream in, PrintStream out) {
        pgm.defFuncs.put (name, new FunctionParser(func.toLowerCase()));
        return pgm.nextStatement(this);
    }

    // 10 DEF FN FTEST1(X) = X*3
    private static void parse(DEFStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        String line = lt.asString().trim();
        if (!Character.isWhitespace(line.charAt(2))) {
            line = line.substring(0,2)+' '+line.substring(2);
        }
        String[] toks = line.split("\\s+");
        if (toks.length != 4)
            throw new BASICSyntaxError("Malformed DEF FN");
        if (!toks[0].equalsIgnoreCase("FN"))
            throw new BASICSyntaxError("missing FN");
        if (!toks[2].equals("="))
            throw new BASICSyntaxError("missing =");
        String[] fparts = toks[1].split("\\(");
        s.name = fparts[0];
        s.func = toks[3];
    }

}
