package basic_1.statement;

import applications.DrawingGUI;
import basic_1.*;
import misc.MDIChild;
import misc.MainWindow;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by Administrator on 10/18/2016.
 */
public class PLOTStatement extends Statement
{
    private static final ThreadLocal<DrawingGUI> plotter =
            ThreadLocal.withInitial(() -> null);

    public static DrawingGUI getPlotWindow()
    {
        return plotter.get();
    }

    private Expression xval;
    private Expression yval;

    protected PLOTStatement(KeyWords key)
    {
        super(key);
    }

    public PLOTStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.PLOT);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    static void parse (PLOTStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s.xval = s.getNumericArg(lt);
        s.checkComma(lt);
        s.yval = s.getNumericArg(lt);
    }

    public String unparse ()
    {
        return keyword.name() + " " + xval + "," + yval;
    }

    public void doPlot (DrawingGUI d, int x, int y)
    {
        d.canvas.plot(x, y);
    }

    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        DrawingGUI w = makePlotWindow();

        int x = (int) xval.value(pgm);
        int y = (int) yval.value(pgm);

        doPlot (w,x,y);

        return pgm.nextStatement(this);
    }

    public static DrawingGUI makePlotWindow()
    {
        DrawingGUI w = plotter.get();
        if (w == null)
        {
            w = createPlotWindow();
        }
        else if (!w.isVisible())
        {
            w = createPlotWindow();
        }
        return w;
    }

    private static DrawingGUI createPlotWindow ()
    {
        plotter.remove();
        MDIChild ji = MainWindow.getInstance().createMDIChild(DrawingGUI.class, null);
        ji.setTitle("Basic plot window: " + Thread.currentThread().getName());
        plotter.set((DrawingGUI) ji);
        return (DrawingGUI) ji;
    }
}
