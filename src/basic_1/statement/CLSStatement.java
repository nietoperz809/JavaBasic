/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package basic_1.statement;

import basic_1.KeyWords;
import basic_1.LexicalTokenizer;
import basic_1.Program;
import basic_1.Statement;

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
     * @param ignoredLt
     */
    public CLSStatement (LexicalTokenizer ignoredLt) {
        super(KeyWords.CLS);
    }

    @Override
    public Statement doit (Program pgm, InputStream in, PrintStream out) {
        pgm.area.setText("");
        return pgm.nextStatement(this);
    }
}
