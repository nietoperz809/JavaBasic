package basic_1.statement;

import applications.DrawingGUI;
import basic_1.*;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by Administrator on 10/20/2016.
 */
public class PCOLORStatement extends Statement
{
    private Expression _r;
    private Expression _g;
    private Expression _b;

    public PCOLORStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.PCOLOR);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    private static void parse (PCOLORStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s._r = s.getNumericArg(lt);
        s.checkComma(lt);
        s._g = s.getNumericArg(lt);
        s.checkComma(lt);
        s._b = s.getNumericArg(lt);
    }

    public String unparse ()
    {
        return keyword.name() + " " + _r + "," + _g + "," + _b;
    }

    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        DrawingGUI pw = PLOTStatement.getPlotWindow();
        if (pw != null)
        {
            pw.canvas.setColor((int) _r.value(pgm),
                    (int) _g.value(pgm),
                    (int) _b.value(pgm));
        }
        return pgm.nextStatement(this);
    }
}
