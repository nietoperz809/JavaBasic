package basic_1.statement;

import basic_1.*;

import java.io.InputStream;
import java.io.PrintStream;

public class DOStatement extends Statement
{

    // This is the line number to transfer control too.

    public DOStatement (LexicalTokenizer lt) {
        super(KeyWords.DO);
    }

    public Statement doit(Program pgm, InputStream in, PrintStream out) {
        //pgm.setVariable(myVar, nExp.value(pgm));
        pgm.push(this);
        return pgm.nextStatement(this);
    }

    public String unparse()
    {
        return "";
    }

}
