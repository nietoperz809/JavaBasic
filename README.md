<div align="center">

# JavaBasic
BASIC in an MDI environment<br>
Feature-rich (e.g. 2 speech synthesizers) old-school BASIC.


![Alt text](javabasic.jpg?raw=true "Title")

</div>

**New: Renumber command**<br>
&emsp;Renumber your code by using one of<br>
&emsp;renumber, renumber a, or renumber a,b<br>
&emsp;Where a is the start value and b is the step width.<br>
&emsp;10,10 are the default values
<br><br>
**New: Defining and calling functions**
<pre>
10 DEF FN FTEST1(X) = X*3
20 INPUT "Type in a number:"; A
29 REM Line 30 and 31 wil do the same ...
30 PRINT FNFTEST1(A)
31 PRINT FN FTEST1(A)
40 IF A<>64 THEN 20
</pre>
**New: TCP/IP Connections**
<pre>
5 REM WAIT FOR CONNECTION ON PORT 1234
10 A$ = LISTEN (1234)
20 PRINT "connected: " + A$
25 REM RECEIVE A STRING
30 S$ = RECV(A$)
40 IF S$ = "" THEN 30
50 PRINT "received: " +S$
55 REM TRANSMIT THE SAME STRING BACK
60 TRANSMIT (A$,S$)
65 REM REPEAT IF NOT "stop"
70 IF S$ <> "stop" THEN 30
75 REM CLOSE THE CONNECTION
80 LET B$ = CLOSE (A$)
90 PRINT "closed: "+B$
</pre>


Keywords:
--------
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
    STEP ("step", "step width of for loop"),
    NAME ("name", "give this thread a name"),
    LET ("let", "assign values to variables"),
    INPUT ("input", "input values into variables"),
    STOP ("stop", "stop the program"),
    DIM ("dim", "define arrays"),
    RANDOMIZE ("randomize", "initialize random generator"),
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

Commands:
---------
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
    CMD_DIR ("dir", "show current directory content"),

Functions:
----------
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

Operators
---------
    OP_ADD ("+"),   // Addition '+'
    OP_SUB ("-"),   // Subtraction '-'
    OP_MUL ("*"),   // Multiplication '*'
    OP_DIV ("/"),   // Division '/'
    OP_EXP ("**"),   // Exponentiation '**'
    OP_AND ("&"),   // Bitwise AND '&'
    OP_IOR ("|"),   // Bitwise inclusive OR '|'
    OP_XOR ("^"),   // Bitwise exclusive OR '^'
    OP_NOT ("!"),   // Unary negation '!'
    OP_EQ ("="),  // Equality and Assignment'='
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

