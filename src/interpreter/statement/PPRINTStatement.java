package interpreter.statement;

import applications.DrawingGUI;
import interpreter.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

public class PPRINTStatement extends Statement
{
    Expression _x;
    Expression _y;
    private Vector<PrintItem> args;

    public PPRINTStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.PPRINT);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    private static void parse (PPRINTStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s._x = s.getNumericArg(lt);
        s.checkComma(lt);
        s._y = s.getNumericArg(lt);
        s.checkComma(lt);
        s.args = StringExParser.parseStringExpression(lt);
    }

    public String unparse ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("PPRINT "+_x+","+_y+", ");
        for (int i = 0; i < args.size(); i++)
        {
            PrintItem pi = (PrintItem) (args.elementAt(i));
            sb.append(pi.unparse());
        }
        return sb.toString();
    }

    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        DrawingGUI pw = PLOTStatement.makePlotWindow();

        PrintItem pi = null;
        int col = 0;
        String text = "";
        for (int i = 0; i < args.size(); i++)
        {
            String z;
            pi = (PrintItem) (args.elementAt(i));
            z = pi.value(pgm, col);
            text = text + z;
            col += z.length();
        }
        if ((pi == null) || pi.needCR())
        {
            text = text + "\n";
        }

        pw.canvas.print((int)_x.value(pgm), (int)_y.value(pgm), text);
        return pgm.nextStatement(this);
    }
}
