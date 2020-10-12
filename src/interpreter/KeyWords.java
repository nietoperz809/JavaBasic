/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interpreter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
public enum KeyWords
{
    GOTO ("goto", "jump to another line"),
    KILL ("kill", "kill other BASIC Window"),
    GOSUB ("gosub", "jump to subroutine and save return"),
    RETURN ("return", "return from subroutine"),
    PRINT ("print", "output text and variables"),
    IF ("if", "test condition and possibly do a branch"),
    THEN ("then", "alternative path for if statement"),
    END ("end", "end program execution"),
    DATA ("data", "define list of values"),
    RESTORE ("restore", "reset internal data read pointer"),
    READ ("read", "read one value from data section and increments read pointer"),
    ON ("on", "precede goto or gosub statement"),
    ON_GOSUB ("gosub", "on ... jump to subroutine on condition"),
    ON_GOTO ("goto", "on ... conditional jump"),
    REM ("rem", "line comment"),
    FOR ("for", "begins for loop"),
    TO ("to", "before last value in for loop"),
    NEXT ("next", "execute next looping"),
    STEP ("step", "step with of for loop"),
    NAME ("name", "give this thread a name"),
    LET ("let", "assign values to variables"),
    INPUT ("input", "input values into variables"),
    STOP ("stop", "stop the program"),
    DIM ("dim", "define arrays"),
    RANDOMIZE ("randomize", "initialize random generator"),
    TRON ("tron", "start trace mode"),
    TROFF ("troff", "end trace mode"),
    CLS ("cls", "clear BASIC window"),
    SLEEP ("sleep", "sleep for n milliseconds or * (infinite)"),
    WAKEUP ("wakeup", "wake a thread up"),
    TWEET ("tweet", "send message over twitter"),
    SEQ ("seq", "define MIDI sequence"),
    SCLR ("sclr", "delete all MIDI tracks"),
    SPLAY ("splay", "run MIDI sequencer"),
    SSPEED ("sspeed", "set MIDI sequencer speed"),
    NOTES ("notes", "prints out midi notes"),
    SPEAK ("say", "speak out a text"),
    SAM ("sam", "speak with S.A.M. voice"),
    SPFILE ("sayopen", "set file name for speech output"),
    SPCLOSE ("sayclose", "close speech-to-file output"),
    PITCH ("pitch", "set the speech pitch"),
    PDRAWTO ("pdrawto", "draw line from old plot pos to new one"),
    COLOR ("color", "set background and text colors"),
    PLOT ("plot", "plot point in graphics window"),
    PCLS ("pcls", "clear plotter window"),
    PCOLOR ("pcolor", "set plot color"),
    PCIRCLE ("pcircle", "draw circle"),
    PSQUARE ("psquare", "draw square"),
    SEND ("send", "send msg to another thread"),
    PLINE ("pline", "draw line"),
    PPRINT ("pprint", "print into plot window"),
    RATE ("rate", "set the speech rate"),   // Must be last statement

    CMD_NEW ("new", "erase program in memory"),
    CMD_RUN ("run", "run program"),
    CMD_LIST ("list", "list program"),
    CMD_CAT ("cat", "show file content in BASIC window"),
    CMD_DEL ("del", "delete a file"),
    CMD_RESUME ("resume", "resume stopped program"),
    CMD_BYE ("bye", "close this BASIC window"),
    CMD_SAVE ("save", "save program to disk"),
    CMD_LOAD ("load", "load program from disk"),
    CMD_DUMP ("dump", "dump variables"),
    CMD_CONT ("cont"),
    CMD_INSTRLIST ("instrlist", "lists MIDI instruments"),
    CMD_CMDS ("cmds", "generate sorted list of commands"),
    CMD_DIR ("dir", "show current directory"),

    RND ("rnd", "get a random number"),
    INT ("int", "strip fraction from number"),
    SIN ("sin", "sinus"),
    COS ("cos", "cosinus"),
    TAN ("tan", "tangens"),
    ATN ("atn", "arctan"),
    SQR ("sqr", "square root"),
    MAX ("max", "greater of 2 values"),
    MIN ("min", "smaller of 2 values"),
    ABS ("abs", "strips off the sign of a negative value"),
    GETNAME ("name$", "get the thread name"),
    TID ("tid", "get the thread id"),
    LEFT ("left$", "get left part of string"),
    RIGHT ("right$", "get right part of string"),
    MID ("mid$", "get middle part of string"),
    CHR ("chr$", "make character from integer"),
    LEN ("len", "get string length"),
    VAL ("val", "read number from string"),
    SPC ("spc$", "create a string of n spaces"),
    LOG ("log", "natural logarithm of n"),
    FRE ("fre", "get free java heap space"), // doesn't really do anything here.
    SPAWN ("spawn", "start a new basic instance and return thread id"),
    SGN ("sgn", "get sign of number, 1 or -1"),
    TAB ("tab", "create string of tabs"),
    STR ("str$", "convert number to string"),
    INKEYS ("inkey$", "get the last key the user typed"),
    TIME ("time", "get free running timer value"),

    OP_ADD ("+"),   // Addition '+'
    OP_SUB ("-"),   // Subtraction '-'
    OP_MUL ("*"),   // Multiplication '*'
    OP_DIV ("/"),   // Division '/'
    OP_EXP ("**"),   // Exponentiation '**'
    OP_AND ("&"),   // Bitwise AND '&'
    OP_IOR ("|"),   // Bitwise inclusive OR '|'
    OP_XOR ("^"),   // Bitwise exclusive OR '^'
    OP_NOT ("!"),   // Unary negation '!'
    OP_EQ ("="),  // Equality '='
    OP_NE ("<>"),  // Inequality '<>'
    OP_LT ("<"),  // Less than '<'
    OP_LE ("<="),  // Less than or equal '<='
    OP_GT (">"),  // Greater than '>'
    OP_GE (">="),  // Greater than or equal '>='
    OP_BAND ("AND"),  // Boolean AND '.AND.'
    OP_BIOR ("OR"),  // Boolean inclusive or '.OR.'
    OP_BXOR ("XOR"),  // Boolean exclusive or '.XOR.'
    OP_BNOT ("NOT"),  // Boolean negation '.NOT.'
    OP_NEG ("-"),  // Unary minus

    SYMBOL ("symbol"),
    COMMAND ("command"),
    CONSTANT ("constant"),
    FUNCTION ("function"),
    KEYWORD ("keyword"),
    EOL ("eol"),
    STRING ("string"),
    ERROR ("error"),
    STRING_VARIABLE ("string variable"),
    BOOLEAN_OPERATOR ("numeric variable"),
    OPERATOR ("boolean operator"),
    VARIABLE ("operator"),

    ENDLIST ("");

    //private static List<KeyWords> _list;

    public static List<KeyWords> getFiltered (String search)
    {
        EnumSet<KeyWords> set = EnumSet.copyOf (keywords);
        set.addAll (commands);
        set.addAll (functions);
        return set.stream ()
                .sorted (Comparator.comparing (KeyWords::toString))
                .filter (u ->
                {
                    String s = u.toString ().toUpperCase ();
                    return s.contains (search.toUpperCase ()) || search.isEmpty ();
                })
                .collect (Collectors.toList ());
    }

    final public static EnumSet<KeyWords> keywords = EnumSet.range (GOTO, RATE);
    final public static EnumSet<KeyWords> commands = EnumSet.range (CMD_NEW, CMD_DIR);
    final public static EnumSet<KeyWords> functions = EnumSet.range (RND, TIME);
    final public static EnumSet<KeyWords> operators = EnumSet.range (OP_ADD, OP_NEG);
    final public static EnumSet<KeyWords> tokentype = EnumSet.range (SYMBOL, VARIABLE);

    private final String text;
    private final String desc;

    /**
     * @param text
     */
    KeyWords (final String text)
    {
        this.text = text;
        this.desc = null;
    }

    KeyWords (final String text, final String desc)
    {
        this.text = text;
        this.desc = desc;
    }

    public String getDesc ()
    {
        return desc;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString ()
    {
        return text;
    }
}
