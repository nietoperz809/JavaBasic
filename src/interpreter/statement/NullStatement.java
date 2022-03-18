package interpreter.statement;

import interpreter.KeyWords;
import interpreter.LexicalTokenizer;
import interpreter.Program;
import interpreter.Statement;

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
