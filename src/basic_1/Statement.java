package basic_1;

import basic_1.util.RedBlackTree;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Enumeration;

/**
 * This class defines BASIC statements. As with expressions, there is a subclass
 * of Statement that does parsing called ParseStatement. This separation allows
 * the statement object to be half as large as it might otherwise be.
 * <p>
 * The <i>execute</i> interface defines what the BASIC statements *do*. These
 * are all called by the containing <b>Program</b>.
 */
public abstract class Statement {
    public KeyWords keyword; // type of statement
    // type of statement
    public int line;
    private String orig; // original string that was parsed into this statement.
    // original string that was parsed into this statement.

    public Statement nxt;  // if there are chained statements
    private RedBlackTree vars; // variables used by this statement.
    // variables used by this statement.

    /**
     * Construct a new statement object with a valid key.
     *
     * @param key
     */
    protected Statement(KeyWords key) {
        keyword = key;
    }

    protected Statement() {
    }

    /**
     * This method does the actual statement execution. It works by calling the
     * abstract function 'doit' which is defined in each statement subclass. The
     * runtime error (if any) is caught so that the line number and statement
     * can be attached to the result and then it is re-thrown.
     */
    public Statement execute(Program pgm, InputStream in, PrintStream out) throws BASICRuntimeError {
        Statement nxt;
        try {
            nxt = doit(pgm, in, out);
            //System.out.println(nxt);
        } catch (BASICRuntimeError e) {
            throw new BASICRuntimeError(this, e.getMsg());
        } catch (Exception ex) {
            throw new BASICRuntimeError(this, "Java Error: " + ex);
        }
        return nxt;
    }

    /**
     * Return a string representation of this statement. If the original text
     * was set then use that, otherwise reconstruct the string from the parse
     * tree.
     */

    /**
     * Put a reference to the original string from which this statement was
     * parsed. this can be used for listing out the program in the native case
     * style of the user.
     */
    public void addText(String t) {
        orig = t;
    }

    /**
     * Return the statement as a string.
     */
    public String asString() {
        return orig;
    }

    /**
     * Update line number information in this statement. Used to determine the
     * next line to execute.
     */
    public void addLine(int l) {
        line = l;
        if (nxt != null) {
            nxt.addLine(l);
        }
    }

    /**
     * Return this statements line number.
     */
    public int lineNo() {
        return line;
    }

    /**
     * reconstruct the statement from the parse tree, this is most useful for
     * diagnosing parsing problems.
     */
    public String unparse() {
        return keyword.name();
    }


    /**
     * This method "runs" this statement and returns a reference on the next
     * statement to run or null if there is no next statement.
     *
     * @throws BASICRuntimeError if there is a problem during statement
     *                           execution such as divide by zero etc.
     */
    protected abstract Statement doit(Program pgm, InputStream in, PrintStream out)
            throws BASICRuntimeError;

    /**
     * The trace method can be used during execution to print out what the
     * program is doing.
     */
//    public void trace(Program pgm, PrintStream ps) {
//        StringBuilder sb = new StringBuilder();
//        String n;
//        sb.append("**:");
//
//        if (vars == null) {
//            vars = getVars();
//        }
//
//        /*
//         * Print the line we're executing on the output stream.
//         */
//        n = line + "";
//        for (int zz = 0; zz < 5 - n.length(); zz++) {
//            sb.append(' ');
//        }
//        sb.append(n);
//        sb.append(':');
//        sb.append(unparse());
//        ps.println(sb);
//        if (vars != null) {
//            for (Enumeration e = vars.elements(); e.hasMoreElements(); ) {
//                VariableExpression vi = (VariableExpression) e.nextElement();
//                String t;
//                try {
//                    t = vi.stringValue(pgm);
//                } catch (BASICRuntimeError bse) {
//                    t = "Not yet defined.";
//                }
//            }
//        }
//    }

    /**
     * Can be overridden by statements that use variables in their execution.
     */
//    protected RedBlackTree getVars() {
//        return null;
//    }

    /**
     * Read one argument expression
     *
     * @param lt program lt
     * @return expression
     * @throws BASICSyntaxError
     */
    public Expression getNumericArg(LexicalTokenizer lt) throws BASICSyntaxError {
        Token t = lt.nextToken();
        switch (t.typeNum()) {
            case CONSTANT:
            case VARIABLE:
            case FUNCTION:
                lt.unGetToken();
                return ParseExpression.expression(lt);
            default:
                throw new BASICSyntaxError("param must be constant or variable");
        }
    }

    public Expression getStringArg(LexicalTokenizer lt) throws BASICSyntaxError {
        Token t = lt.nextToken();
        switch (t.typeNum()) {
            case STRING:
            case VARIABLE:
            case FUNCTION:
                lt.unGetToken();
                return ParseExpression.expression(lt);
            default:
                throw new BASICSyntaxError("param must be constant or variable");
        }
    }

    public void checkComma(LexicalTokenizer lt) throws BASICSyntaxError {
        Token t = lt.nextToken();
        if (!t.isSymbol(',')) {
            lt.unGetToken();
            throw new BASICSyntaxError("missing comma separator");
        }
    }
}
