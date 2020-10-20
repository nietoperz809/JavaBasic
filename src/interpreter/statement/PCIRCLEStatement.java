package interpreter.statement;

import applications.DrawingGUI;
import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by Administrator on 10/21/2016.
 */
public class PCIRCLEStatement extends PBOXStatement
{
    public PCIRCLEStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        init (KeyWords.PCIRCLE, lt);
    }

    void doIt2 (int x, int y, int rad1, int rad2)
    {
        DrawingGUI pw = PLOTStatement.makePlotWindow();
        pw.canvas.circle (x, y, rad1, rad2);
    }
}
