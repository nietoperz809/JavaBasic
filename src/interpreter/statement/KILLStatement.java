package interpreter.statement;

import applications.BasicGUI;
import interpreter.*;
import misc.MainWindow;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.FutureTask;

public class KILLStatement extends Statement
{
    // This is the line number to transfer control too.
    Expression killTarget;

    public KILLStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.KILL);
        if (lt.getBuffer() != null)
            parse(this, lt);
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        BasicGUI bg = MainWindow.getInstance().getBasicGUIFromThreadID ((long)killTarget.value(pgm));
        if (bg != null)
        {
            FutureTask<?> ft = bg.basicTask;
            if (ft != null)
            {
                ft.cancel(true);
                bg.dispose();
            }
        }
        return pgm.nextStatement(this);
    }

    @Override
    public String unparse()
    {
        return keyword.toString()+" " + killTarget;
    }

    /**
     * Parse GOTO Statement.
     */
    private static void parse (KILLStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s.killTarget = s.getNumericArg(lt);
    }
}
