package basic_1;


import java.util.Vector;

/**
 * Created by Administrator on 4/23/2016.
 */
public interface StringExParser
{
    static Vector<PrintItem> parseStringExpression(LexicalTokenizer lt) throws BASICSyntaxError
    {
        Vector<PrintItem> result = new Vector<>();
        Token t;

        while (true)
        {
            t = lt.nextToken();
            switch (t.typeNum())
            {
                case CONSTANT:
                case FUNCTION:
                case VARIABLE:
                case STRING:
                case OPERATOR:
                    lt.unGetToken();
                    result.addElement(new PrintItem(PrintItem.EXPRESSION, ParseExpression.expression(lt)));
                    break;
                case SYMBOL:
                    switch ((int) t.numValue())
                    {
                        case '(':
                            lt.unGetToken();
                            result.addElement(new PrintItem(PrintItem.EXPRESSION, ParseExpression.expression(lt)));
                            break;
                        case ';':
                            result.addElement(new PrintItem(PrintItem.SEMI, null));
                            break;
                        case ',':
                            result.addElement(new PrintItem(PrintItem.TAB, null));
                            break;
                        default:
                            lt.unGetToken();
                            return result;
                    }
                    break;
                case EOL:
                    return result;
                default:
                    lt.unGetToken();
                    return result;
            }
        }
    }

    static String printItemsToString (Program pgm, Vector<PrintItem> args) throws BASICRuntimeError
    {
        StringBuilder buff = new StringBuilder();
        PrintItem pi = null;
        int col = 0;
        for (int i = 0; i < args.size(); i++)
        {
            String z;
            pi = args.elementAt(i);
            z = pi.value(pgm, col);
            buff.append(z);
            col += z.length();
        }
        if ((pi == null) || pi.needCR())
        {
            buff.append("\n");
        }
        return buff.toString();
    }
}
