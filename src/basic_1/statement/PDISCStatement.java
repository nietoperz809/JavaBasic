package basic_1.statement;

import applications.DrawingGUI;
import basic_1.BASICSyntaxError;
import basic_1.KeyWords;
import basic_1.LexicalTokenizer;

public class PDISCStatement extends PBOXStatement
{
    public PDISCStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        init (KeyWords.PDISC, lt);
    }

    void doIt2 (int x, int y, int rad1, int rad2)
    {
        DrawingGUI pw = PLOTStatement.makePlotWindow();
        pw.canvas.disc (x, y, rad1, rad2);
    }

}
