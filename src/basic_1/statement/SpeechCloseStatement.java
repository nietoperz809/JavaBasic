package basic_1.statement;

import basic_1.KeyWords;
import basic_1.LexicalTokenizer;
import basic_1.Program;
import basic_1.Statement;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by Administrator on 6/10/2016.
 */
public class SpeechCloseStatement extends Statement
{
    /**
     * CLC command
     * @param ignoredLt
     */
    public SpeechCloseStatement (LexicalTokenizer ignoredLt) {
        super(KeyWords.SPCLOSE);
    }

    @Override
    public Statement doit (Program pgm, InputStream in, PrintStream out) {
        pgm.unsetVoiceFilename();
        return pgm.nextStatement(this);
    }
}
