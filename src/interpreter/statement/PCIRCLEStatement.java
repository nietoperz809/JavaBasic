package interpreter.statement;

import applications.DrawingGUI;
import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by Administrator on 10/21/2016.
 */
public class PCIRCLEStatement extends Statement
{
    private Expression _x;
    private Expression _y;
    private Expression _radius;

    public PCIRCLEStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.PCIRCLE);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    private static void parse (PCIRCLEStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s._x = s.getNumericArg(lt);
        s.checkComma(lt);
        s._y = s.getNumericArg(lt);
        s.checkComma(lt);
        s._radius = s.getNumericArg(lt);
    }

    public String unparse ()
    {
        return "PCIRCLE " + _x + "," + _y + "," + _radius;
    }

    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        DrawingGUI pw = PLOTStatement.makePlotWindow();
        if (pw != null)
        {
            int rad = (int) _radius.value(pgm);
            int x = (int)_x.value(pgm) - rad/2;
            int y = (int) _y.value(pgm) - rad/2;

            pw.canvas.circle(x, y, rad);
        }
        return pgm.nextStatement(this);
    }
}
