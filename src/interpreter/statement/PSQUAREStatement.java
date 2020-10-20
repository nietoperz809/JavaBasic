package interpreter.statement;

import applications.DrawingGUI;
import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;

public class PSQUAREStatement extends PBOXStatement
{
    public PSQUAREStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        init (KeyWords.PSQUARE, lt);
    }

    void doIt2 (int x, int y, int rad1, int rad2)
    {
        DrawingGUI pw = PLOTStatement.makePlotWindow();
        pw.canvas.square(x, y, rad1, rad2);
    }
}
