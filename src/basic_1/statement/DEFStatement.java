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

    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        pgm.defFuncs.put (name, new FunctionParser(func.toLowerCase()));
        return pgm.nextStatement(this);
    }

    // 10 DEF FN FTEST1(X) = X*3
    // FN FTEST1(X) -> FN ("FTEST","X")
    private static void parse(DEFStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        String[] toks = lt.asString().trim().split("\\s+");
        if (toks.length != 4)
            throw new BASICSyntaxError("Malformed DEF FN");
        if (!toks[0].toUpperCase().equals("FN"))
            throw new BASICSyntaxError("missing FN");
        if (!toks[2].equals("="))
            throw new BASICSyntaxError("missing =");
        String[] fparts = toks[1].split("\\(");
        s.name = fparts[0];
        s.func = toks[3]; //new DefFunc(fparts[0], fparts[1].substring(0, fparts[1].length() - 1), toks[3]);
    }

}
