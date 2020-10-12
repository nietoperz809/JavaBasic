package interpreter.statement;

import applications.DrawingGUI;
import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by Administrator on 10/22/2016.
 */
public class PSQUAREStatement extends Statement
{
    private Expression _x;
    private Expression _y;
    private Expression _width;

    public PSQUAREStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.PSQUARE);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    private static void parse (PSQUAREStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s._x = s.getNumericArg(lt);
        s.checkComma(lt);
        s._y = s.getNumericArg(lt);
        s.checkComma(lt);
        s._width = s.getNumericArg(lt);
    }

    @Override
    public String unparse ()
    {
        return keyword.name() + " " + _x + "," + _y + "," + _width;
    }

    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        DrawingGUI pw = PLOTStatement.makePlotWindow();
        if (pw != null)
        {
            int rad = (int) _width.value(pgm);
            int x = (int)_x.value(pgm) - rad/2;
            int y = (int) _y.value(pgm) - rad/2;

            pw.canvas.square(x, y, rad);
        }
        return pgm.nextStatement(this);
    }
}
