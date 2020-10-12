package interpreter.statement;

import applications.DrawingGUI;
import interpreter.BASICSyntaxError;
import interpreter.KeyWords;
import interpreter.LexicalTokenizer;

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
