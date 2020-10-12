package interpreter.statement;

import interpreter.*;
import misc.C64Colors;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Created by Administrator on 10/18/2016.
 */
public class COLORStatement extends Statement
{
    private Expression xval;
    private Expression yval;

    public COLORStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.COLOR);
        if (lt.getBuffer() != null)
        {
            parse(this, lt);
        }
    }

    static void parse (COLORStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
        s.xval = s.getNumericArg(lt);
        s.checkComma(lt);
        s.yval = s.getNumericArg(lt);
    }

    public String unparse ()
    {
        return keyword.name() + " " + xval + "," + yval;
    }


    public Statement doit (Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        int x = (int) xval.value(pgm);
        int y = (int) yval.value(pgm);

        //System.out.println ("Color: "+x+"/"+y);

        pgm.area.setColors (C64Colors.getColor(x), C64Colors.getColor(y));

        return pgm.nextStatement(this);
    }
}
