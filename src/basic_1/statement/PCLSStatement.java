package basic_1.statement;

import applications.DrawingGUI;
import basic_1.KeyWords;
import basic_1.LexicalTokenizer;
import basic_1.Program;
import basic_1.Statement;

import java.io.InputStream;
import java.io.PrintStream;

public class PCLSStatement extends Statement
{
    /**
     * CLC command
     * @param lt
     */
    public PCLSStatement (LexicalTokenizer lt) {
        super(KeyWords.PCLS);
    }

    @Override
    public Statement doit (Program pgm, InputStream in, PrintStream out)
    {
        DrawingGUI pw = PLOTStatement.getPlotWindow();
        if (pw != null)
            pw.canvas.clear();
        return pgm.nextStatement(this);
    }
}
