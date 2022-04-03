/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interpreter.statement;

import interpreter.*;
import misc.MainWindow;

import java.io.InputStream;
import java.io.PrintStream;

/**
 *
 * @author Administrator
 */
public class RATEStatement extends Statement
{
    private Expression nExpn;

    public RATEStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.RATE);
        if (lt.getBuffer() != null)
        {
            Token t = lt.nextToken();
            switch (t.typeNum())
            {
                case OPERATOR:
                case CONSTANT:
                case VARIABLE:
                    lt.unGetToken();
                    nExpn = ParseExpression.expression(lt);
                default:
                    lt.unGetToken();
            }
        }
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        if (nExpn != null)
        {
            MainWindow.voice.setRate((float)nExpn.value(pgm));
        }
        return pgm.nextStatement(this);
    }

}
