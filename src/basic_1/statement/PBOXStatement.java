package basic_1.statement;

import applications.DrawingGUI;
import basic_1.*;

import java.io.InputStream;
import java.io.PrintStream;

public class PBOXStatement extends Statement
{
    Expression _x;
    Expression _y;
    Expression _width;
    Expression _height;

    protected void init (KeyWords key, LexicalTokenizer lt) throws BASICSyntaxError
    {
        keyword = key;
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    public PBOXStatement ()
    {
    }

    public PBOXStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        init (KeyWords.PBOX, lt);
    }

    private static void parse (PBOXStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s._x = s.getNumericArg(lt);
        s.checkComma(lt);
        s._y = s.getNumericArg(lt);
        s.checkComma(lt);
        s._width = s.getNumericArg(lt);
        s.checkComma(lt);
        s._height = s.getNumericArg(lt);
    }

    @Override
    public String unparse ()
    {
        return keyword.name() + " " + _x + "," + _y + "," + _width + "," + _height;
    }

    void doIt2 (int x, int y, int rad1, int rad2)
    {
        DrawingGUI pw = PLOTStatement.makePlotWindow();
        pw.canvas.box(x, y, rad1, rad2);
    }

    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        int rad1 = (int) _width.value(pgm);
        int rad2 = (int) _height.value(pgm);
        int x = (int)_x.value(pgm) - rad1/2;
        int y = (int) _y.value(pgm) - rad2/2;

        doIt2(x, y, rad1, rad2);
        return pgm.nextStatement(this);
    }
}
