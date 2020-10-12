/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interpreter.statement;

import interpreter.KeyWords;
import interpreter.LexicalTokenizer;
import interpreter.Program;
import interpreter.Statement;

import java.io.InputStream;
import java.io.PrintStream;

/**

 *
 * @author Administrator
 */
public class CLSStatement extends Statement
{
    /**
     * CLC command
     * @param lt
     */
    public CLSStatement (LexicalTokenizer lt) {
        super(KeyWords.CLS);
    }

    @Override
    public Statement doit (Program pgm, InputStream in, PrintStream out) {
        pgm.area.setText("");
        return pgm.nextStatement(this);
    }
}
