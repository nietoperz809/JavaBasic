package interpreter.statement;

import interpreter.KeyWords;
import interpreter.LexicalTokenizer;
import interpreter.Program;
import interpreter.Statement;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by Administrator on 6/10/2016.
 */
public class SpeechCloseStatement extends Statement
{
    /**
     * CLC command
     * @param lt
     */
    public SpeechCloseStatement (LexicalTokenizer lt) {
        super(KeyWords.SPCLOSE);
    }

    @Override
    public Statement doit (Program pgm, InputStream in, PrintStream out) {
        pgm.unsetVoiceFilename();
        return pgm.nextStatement(this);
    }
}
