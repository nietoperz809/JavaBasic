/*
 * ParseExpression.java - Parse an expression.
 *
 * Copyright (c) 1996 Chuck McManis, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * CHUCK MCMANIS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE
 * SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. CHUCK MCMANIS
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package interpreter;

/**
 * This class implements a recursive-descent parser for the expression grammar
 * we have designed for BASIC.
 *
 * This grammar is defined with some nonterminals that allow us to embed the
 * precedence relationship of operators into the grammar. The grammar is defined
 * as follows:
 *
 * ELEMENT ::= id | ( expression ) | function PRIMARY ::= - ELEMENT | ! ELEMENT
 * | .NOT. ELEMENT | ELEMENT FACTOR ::= PRIMARY ** FACTOR | PRIMARY TERM ::=
 * TERM * FACTOR | TERM / FACTOR | FACTOR SUM ::= SUM + TERM | SUM - TERM | TERM
 * LOGIC ::= LOGIC & SUM | LOGIC ^ SUM | LOGIC | SUM | SUM RELATION ::= LOGIC =
 * LOGIC | LOGIC < LOGIC  | LOGIC > LOGIC | LOGIC <> LOGIC | LOGIC <= LOGIC | LOGIC
 * >= LOGIC | LOGIC | EXPRESSION ::= RELATION .AND. RELATION | RELATION .OR.
 * RELATION | RELATION .XOR. RELATION | RELATION
 *
 * STRELEMENT ::= id | strfunction
 *
 * STREXP ::= STRELEMENT + STREXP | STRELEMENT
 *
 * Expressions that are parsed as either EXPRESSIONs or RELATIONs have the type
 * BooleanExpression when they are returned.
 *
 * Precidence rules from lowest to highest : 0. .AND., .OR., .XOR. 1. =, < , <=,
 * >, >= 2. &, |, ^ 3. +, - 4. *, / 5. ** 6. unary -, unary !, unary .NOT.
 *
 */
public class ParseExpression extends Expression
{

    private static Expression element (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Expression result = null;

        Token t = lt.nextToken();

        if (t.isSymbol('('))
        {
            result = expression(lt);
            t = lt.nextToken();
            if (!t.isSymbol(')'))
            {
                lt.unGetToken();
                throw new BASICSyntaxError("mismatched parenthesis in expression");
            }
        }
        else if (t.typeNum() == KeyWords.CONSTANT)
        {
            result = new ConstantExpression(t.numValue());
        }
        else if (t.typeNum() == KeyWords.STRING)
        {
            result = new ConstantExpression(t.stringValue());
        }
        else if (t.typeNum() == KeyWords.VARIABLE)
        {
            result = new VariableExpression((Variable) t);
        }
        else if (t.typeNum() == KeyWords.FUNCTION)
        {
            result = FunctionExpression.parse(KeyWords.values()[(int)t.numValue()], lt);
        }
        else
        {
            lt.unGetToken();
            throw new BASICSyntaxError("Unexpected symbol in expression.");
        }

        return result;
    }

    private static Expression primary (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Token t = lt.nextToken();

        if (t.isOp(KeyWords.OP_NOT))
        {
            return new Expression(KeyWords.OP_NOT, primary(lt));
        }
        else if (t.isOp(KeyWords.OP_SUB))
        {
            return new Expression(KeyWords.OP_NEG, primary(lt));
        }
        else if (t.isOp(KeyWords.OP_BNOT))
        {
            return new BooleanExpression(KeyWords.OP_BNOT, primary(lt));
        }
        lt.unGetToken();
        return element(lt);
    }

    private static Expression factor (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Expression result;
        Token t;

        result = primary(lt);
        if (result.isString())
        {
            return result;
        }

        t = lt.nextToken();
        if (t.isOp(KeyWords.OP_EXP))
        {
            return new Expression(KeyWords.OP_EXP, result, factor(lt));
        }
        lt.unGetToken();
        return result;
    }

    private static Expression term (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Expression result;
        Token t;

        result = factor(lt);
        if (result.isString())
        {
            return result;
        }

        while (true)
        {
            t = lt.nextToken();
            if (t.isOp(KeyWords.OP_MUL))
            {
                result = new Expression(KeyWords.OP_MUL, result, factor(lt));
            }
            else if (t.isOp(KeyWords.OP_DIV))
            {
                result = new Expression(KeyWords.OP_DIV, result, factor(lt));
            }
            else
            {
                break;
            }
        }
        lt.unGetToken();
        return result;
    }

    private static Expression sum (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Expression result;
        Token t;

        result = term(lt);
        if (result.isString())
        {
            return result;
        }

        while (true)
        {
            t = lt.nextToken();
            if (t.isOp(KeyWords.OP_ADD))
            {
                result = new Expression(KeyWords.OP_ADD, result, term(lt));
            }
            else if (t.isOp(KeyWords.OP_SUB))
            {
                result = new Expression(KeyWords.OP_SUB, result, term(lt));
            }
            else
            {
                break;
            }
        }
        lt.unGetToken();
        return result;
    }

    private static Expression logic (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Expression result;
        Token t;

        result = sum(lt);
        if (result.isString())
        {
            return result;
        }

        while (true)
        {
            t = lt.nextToken();
            if (t.isOp(KeyWords.OP_AND))
            {
                result = new Expression(KeyWords.OP_AND, result, sum(lt));
            }
            else if (t.isOp(KeyWords.OP_XOR))
            {
                result = new Expression(KeyWords.OP_XOR, result, sum(lt));
            }
            else if (t.isOp(KeyWords.OP_IOR))
            {
                result = new Expression(KeyWords.OP_IOR, result, sum(lt));
            }
            else
            {
                break;
            }
        }
        lt.unGetToken();
        return result;
    }

    private static Expression string (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Expression result;
        Token t;

        result = logic(lt);
        if (!result.isString())
        {
            return result;
        }
        while (true)
        {
            Expression arg2;
            t = lt.nextToken();
            if (!t.isOp(KeyWords.OP_ADD))
            {
                lt.unGetToken();
                return result;
            }
            arg2 = logic(lt);
            if (!arg2.isString())
            {
                throw new BASICSyntaxError("Only add is allowed in string expressions.");
            }
            result = new StringExpression(KeyWords.OP_ADD, result, arg2);
        }
    }

    private static Expression relation (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Expression result;
        Token t;

        result = string(lt);
        t = lt.nextToken();
        if (t.typeNum() != KeyWords.OPERATOR)
        {
            lt.unGetToken();
            return result;
        }
        switch (KeyWords.values()[(int)t.numValue()])
        {
            case OP_EQ:
                result = new BooleanExpression(KeyWords.OP_EQ, result, string(lt));
                break;
            case OP_NE:
                result = new BooleanExpression(KeyWords.OP_NE, result, string(lt));
                break;
            case OP_LT:
                result = new BooleanExpression(KeyWords.OP_LT, result, string(lt));
                break;
            case OP_LE:
                result = new BooleanExpression(KeyWords.OP_LE, result, string(lt));
                break;
            case OP_GT:
                result = new BooleanExpression(KeyWords.OP_GT, result, string(lt));
                break;
            case OP_GE:
                result = new BooleanExpression(KeyWords.OP_GE, result, string(lt));
                break;
        }
        lt.unGetToken();
        return result;
    }

    public static Expression expression (LexicalTokenizer lt) throws BASICSyntaxError
    {
        Expression result;
        Token t;

        result = relation(lt);
        while (true)
        {
            t = lt.nextToken();
            if (t.isOp(KeyWords.OP_BAND))
            {
                result = new BooleanExpression(KeyWords.OP_BAND, result, relation(lt));
            }
            else if (t.isOp(KeyWords.OP_BIOR))
            {
                result = new BooleanExpression(KeyWords.OP_BIOR, result, relation(lt));
            }
            else if (t.isOp(KeyWords.OP_BXOR))
            {
                result = new BooleanExpression(KeyWords.OP_BXOR, result, relation(lt));
            }
            else
            {
                break;
            }
        }
        lt.unGetToken();
        return result;
    }
}
