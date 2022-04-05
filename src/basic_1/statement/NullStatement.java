package basic_1.statement;

import basic_1.KeyWords;
import basic_1.LexicalTokenizer;
import basic_1.Program;
import basic_1.Statement;

import java.io.InputStream;
import java.io.PrintStream;

public class NullStatement extends Statement
{

    public NullStatement (LexicalTokenizer lt) {
        super(KeyWords.NULL);
    }

    public Statement doit(Program pgm, InputStream in, PrintStream out) {
        return null;
    }

}
