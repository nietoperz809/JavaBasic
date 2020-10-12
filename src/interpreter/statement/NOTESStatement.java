/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interpreter.statement;

import java.io.InputStream;
import java.io.PrintStream;

import interpreter.KeyWords;
import interpreter.LexicalTokenizer;
import interpreter.Program;
import interpreter.Statement;
import midisystem.Notes;

/**
 *
 * @author Administrator
 */
public class NOTESStatement extends Statement
{
    /**
     * CLC command
     * @param lt
     */
    public NOTESStatement (LexicalTokenizer lt) {
        super(KeyWords.NOTES);
    }

    @Override
    public Statement doit(Program pgm, InputStream in, PrintStream out) {
        out.println (new Notes().toString());
        return pgm.nextStatement(this);
    }
}
