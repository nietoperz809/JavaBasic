package basic_1.statement;

import applications.DrawingGUI;
import basic_1.BASICSyntaxError;
import basic_1.KeyWords;
import basic_1.LexicalTokenizer;

public class DRAWTOStatement extends PLOTStatement
{
    public DRAWTOStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.PDRAWTO);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    @Override
    public void doPlot (DrawingGUI d, int x, int y)
    {
        d.canvas.drawto(x, y);
    }
}
