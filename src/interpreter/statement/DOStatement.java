package interpreter.statement;

import interpreter.*;
import interpreter.util.RedBlackTree;

import java.io.InputStream;
import java.io.PrintStream;

public class DOStatement extends Statement
{

    // This is the line number to transfer control too.
    //int lineTarget;
//    Expression nExp;
//    Expression eExp;
//    Expression sExp;
//    Variable myVar;

    public DOStatement (LexicalTokenizer lt) throws BASICSyntaxError
    {
        super(KeyWords.DO);
//        if (lt.getBuffer() != null)
//            parse(this, lt);
    }

    public Statement doit(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError
    {
        //pgm.setVariable(myVar, nExp.value(pgm));
        pgm.push(this);
        return pgm.nextStatement(this);
    }

    public String unparse()
    {
        return "";
    }

//    /**
//     * Collect the variables associated with the execution of this statement.
//     */
//    @Override
//    public RedBlackTree getVars ()
//    {
//        RedBlackTree vv = new RedBlackTree();
//        nExp.trace(vv);
//        eExp.trace(vv);
//        if (sExp != null)
//        {
//            sExp.trace(vv);
//        }
//        return vv;
//    }

//    private static boolean noBool(Expression e)
//    {
//        return (!(e instanceof BooleanExpression));
//    }

    /**
     * Parse FOR Statement.
     */
    private static void parse(DOStatement s, LexicalTokenizer lt) throws BASICSyntaxError
    {
//        Token t = lt.nextToken();
//
//        if (t.typeNum() != KeyWords.VARIABLE)
//        {
//            throw new BASICSyntaxError("Missing variable in FOR statement");
//        }
//
//        s.myVar = (Variable) t;
//        if (s.myVar.isString())
//        {
//            throw new BASICSyntaxError("Numeric variable required for FOR statement.");
//        }
//
//        t = lt.nextToken();
//        if (!t.isOp(KeyWords.OP_EQ))
//        {
//            throw new BASICSyntaxError("Missing = in FOR statement");
//        }
//        s.nExp = ParseExpression.expression(lt);
//        noBool(s.nExp);
//        t = lt.nextToken();
//        if ((t.typeNum() != KeyWords.KEYWORD) || (t.kwValue != KeyWords.TO))
//        {
//            throw new BASICSyntaxError("Missing TO in FOR statement.");
//        }
//        s.eExp = ParseExpression.expression(lt);
//        noBool(s.eExp);
//        t = lt.nextToken();
//        if ((t.typeNum() != KeyWords.KEYWORD) || (t.kwValue != KeyWords.STEP))
//        {
//            lt.unGetToken();
//            s.sExp = new ConstantExpression(1.0);
//            return;
//        }
//        s.sExp = ParseExpression.expression(lt);
//        noBool(s.sExp);
    }

}
