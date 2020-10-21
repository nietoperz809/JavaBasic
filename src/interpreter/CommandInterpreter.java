/*
 * CommandInterpreter.java -  Provide the basic command line interface.
 *
 * ^
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

import applications.BasicGUI;
import interpreter.streameditor.StreamingTextArea;
import midisystem.MidiSynthSystem;
import misc.Transmitter;

import javax.sound.midi.Instrument;
import java.io.*;
import java.util.List;
import java.util.concurrent.FutureTask;

import static interpreter.ParseStatement.statement;

/**
 * This class is an "interactive" BASIC environment. You can think of it as
 * BASIC debug mode. Using the streams you passed in to create the object, it
 * hosts an interactive session allowing the user to enter BASIC programs, run
 * them, save them, and load them.
 */
public class CommandInterpreter
{
    public LexicalTokenizer tokenizer;
    public Program basicProgram;

    private StreamingTextArea streamingTextArea;
    transient private DataInputStream inStream;
    transient private PrintStream outStream;

    final transient private BasicGUI m_bg;

    /**
     * Create a new command interpreter attached to the passed in streams.
     */
    public CommandInterpreter (BasicGUI bg)
    {
        m_bg = bg;
    }

    /**
     * This method basically dispatches the commands of the command interpreter.
     */
    private Program processCommand (Program pgm, LexicalTokenizer lt, Token x) throws Exception
    {
        Token t;

        switch (x.kwValue)
        {
            case CMD_RESUME:
                try
                {
                    pgm.resume (inStream, outStream);
                }
                catch (BASICRuntimeError e)
                {
                    outStream.println (e.getMsg ());
                }
                return pgm;

            case CMD_CONT:
                try
                {
                    pgm.cont (inStream, outStream);
                }
                catch (BASICRuntimeError e)
                {
                    outStream.println (e.getMsg ());
                }
                return pgm;

            case CMD_RUN:
                t = lt.nextToken ();
                int startline = 0;
                if (t.typeNum () == KeyWords.CONSTANT)
                {
                    startline = (int) t.numValue ();
                }
                //System.out.println("before run"); // +++++++++++++++++++++++++
                streamingTextArea.startRunMode();
                try
                {
                    pgm.run (inStream, outStream, startline);
                }
                catch (BASICRuntimeError e2)
                {
                    outStream.println (e2.getMsg ());
                }
                //System.out.println("after run");  // +++++++++++++++++++++++++
                streamingTextArea.stopRunMode();
                return pgm;

            case CMD_CMDS:
                t = lt.nextToken ();
                String search = "";
                if (t.type == KeyWords.KEYWORD)
                {
                    search = t.sValue;
                }
                else if (t instanceof Variable)
                {
                    Variable v1 = (Variable) t;
                    search = v1.name;
                }
                List<KeyWords> list = KeyWords.getFiltered (search);
                for (KeyWords k : list)
                {
                    String description = k.getDesc ();
                    String cmd = k.toString ().toUpperCase ();
                    outStream.print (cmd);
                    outStream.println (" - " + description);
                }
                return pgm;

            case CMD_INSTRLIST:
                Instrument[] instr = MidiSynthSystem.get ().getInstruments ();
                StringBuilder sb = new StringBuilder ();
                for (int n = 0; n < instr.length; n++)
                {
                    sb.append (n);
                    sb.append (" -- ");
                    sb.append (instr[n].toString ());
                    sb.append ('\n');
                }
                outStream.println (sb.toString ());
                return pgm;

            case CMD_CAT:
                t = lt.nextToken ();
                if (t.typeNum () != KeyWords.STRING)
                {
                    outStream.println ("File name expected for CAT Command.");
                    return pgm;
                }
                File f = new File (t.stringValue ());
                InputStream in = new FileInputStream (f);
                Transmitter tr = new Transmitter (in, outStream);
                tr.doTransmission (null);
                return pgm;

            case CMD_DEL:
                t = lt.nextToken ();
                if (t.typeNum () != KeyWords.STRING)
                {
                    outStream.println ("File name expected for DEL Command.");
                    return pgm;
                }
                new File (t.stringValue ()).delete ();
                return pgm;

            case CMD_SAVE:
                t = lt.nextToken ();
                if (t.typeNum () != KeyWords.STRING)
                {
                    outStream.println ("File name expected for SAVE Command.");
                    return pgm;
                }
                String fname = t.stringValue();
                if (!fname.endsWith(".bas"))
                    fname = fname+".bas";
                outStream.println ("Saving file...");
                FileOutputStream fos;
                fos = new FileOutputStream (fname);
                PrintStream pp = new PrintStream (fos);
                pgm.list (pp);
                pp.flush ();
                fos.close ();
                return pgm;

            case CMD_LOAD:
                t = lt.nextToken ();
                if (t.typeNum () != KeyWords.STRING)
                {
                    outStream.println ("File name expected for LOAD command.");
                    return pgm;
                }
                try
                {
                    pgm = Program.load (t.stringValue (), outStream, pgm.area);
                    outStream.println ("File loaded.");
                } catch (IOException e)
                {
                    outStream.println ("File " + t.stringValue () + " not found.");
                    return pgm;
                } catch (BASICSyntaxError bse)
                {
                    outStream.println ("Syntax error reading file.");
                    outStream.println (bse.getMsg ());
                    return pgm;
                }
                return pgm;

            case CMD_DIR:
                File[] filesInFolder = new File (".").listFiles ();
                for (final File fileEntry : filesInFolder)
                {
                    if (fileEntry.isFile ())
                    {
                        outStream.println (fileEntry.getName () + " -- " + fileEntry.length ());
                    }
                }
                return pgm;

            case CMD_DUMP:
                PrintStream zzz = outStream;
                t = lt.nextToken ();
                if (t.typeNum () == KeyWords.STRING)
                {
                    try
                    {
                        zzz = new PrintStream (new FileOutputStream (t.stringValue ()));
                    } catch (IOException ii)
                    {
                    }
                }
                pgm.dump (zzz);
                if (zzz != outStream)
                {
                    zzz.close ();
                }
                return pgm;

            case CMD_LIST:
                t = lt.nextToken ();
                if (t.typeNum () == KeyWords.EOL)
                {
                    pgm.list (outStream);
                }
                else if (t.typeNum () == KeyWords.CONSTANT)
                {
                    int strt = (int) t.numValue ();
                    t = lt.nextToken ();
                    if (t.typeNum () == KeyWords.EOL)
                    {
                        pgm.list (strt, outStream);
                    }
                    else if (t.isSymbol (','))
                    {
                        t = lt.nextToken ();
                        if (t.typeNum () != KeyWords.CONSTANT)
                        {
                            outStream.println ("Illegal parameter to LIST command.");
                            outStream.println (lt.showError ());
                            return pgm;
                        }
                        int e = (int) t.numValue ();
                        pgm.list (strt, e, outStream);
                    }
                    else
                    {
                        outStream.println ("Syntax error in LIST command.");
                        outStream.println (lt.showError ());
                    }
                }
                else
                {
                    outStream.println ("Syntax error in LIST command.");
                    outStream.println (lt.showError ());
                }
                return pgm;
        }
        outStream.println ("Command not implemented.");
        return pgm;
    }

    private final char[] data = new char[256];

    public void dispose ()
    {
        streamingTextArea.destroy ();
    }

    /**
     * Starts the interactive session. When running the user should see the
     * "Ready." prompt. The session ends when the user types the
     * <code>bye</code> command.
     *
     * @return EndReason 1 == BYE detected
     * @throws java.lang.Exception
     */

    public void prestart (StreamingTextArea area)
    {
        System.err.println ("start BASIC system");
        streamingTextArea = area;
        inStream = new DataInputStream (area.getInputStream ());
        outStream = new PrintStream (area.getOutputStream ());

        if (tokenizer == null)
        {
            System.out.println ("create Tokenizer");
            tokenizer = new LexicalTokenizer (data);
        }
        if (basicProgram == null)
        {
            System.out.println ("create BASIC prog");
            basicProgram = new Program (area);
        }

        outStream.println ("*JavaBasic*\nType CMDS or CMDS n to see commands (beginning with n)\n");
    }

    public void runCLI () throws Exception
    {
        while (true)
        {
            String lineData = streamingTextArea.getBufferedLine();
            m_bg.setLineInList (lineData);

            tokenizer.reset (lineData);

            if (!tokenizer.hasMoreTokens ())
            {
                System.out.println ("no more tokens");
                continue;
            }

            Token t = tokenizer.nextToken ();
            switch (t.typeNum ())
            {
                /*
                 * Process one of the command interpreter's commands.
                 */
                case COMMAND:
                    if (t.kwValue == KeyWords.CMD_BYE)
                    {
                        FutureTask<?> ft = m_bg.basicTask;
                        if (ft != null)
                        {
                            ft.cancel (true);
                            m_bg.dispose ();
                        }
                        return;
                    }
                    else if (t.kwValue == KeyWords.CMD_NEW)
                    {
                        basicProgram = new Program (streamingTextArea);
                        System.gc ();
                    }
                    else
                    {
                        basicProgram = processCommand (basicProgram, tokenizer, t);
                    }
                    break;

                /*
                 * Process an initial number, it can be a new statement line
                 * or it may be an implicit delete command.
                 */
                case CONSTANT:
                    Token peek = tokenizer.nextToken ();
                    if (peek.typeNum () == KeyWords.EOL)
                    {
                        basicProgram.del ((int) t.numValue ());
                        break;
                    }
                    else
                    {
                        tokenizer.unGetToken ();
                    }
                    try
                    {
                        Statement s = statement (tokenizer);
                        s.addText (lineData);
                        s.addLine ((int) t.numValue ());
                        basicProgram.add ((int) t.numValue (), s);
                    } catch (BASICSyntaxError e)
                    {
                        outStream.println ("Syntax Error : " + e.getMsg ());
                        outStream.println (tokenizer.showError ());
                        continue;
                    }
                    break;

                /*
                 * If initially it is a variable or a statement keyword then it
                 * must be an 'immediate' line.
                 */
                case VARIABLE:
                case KEYWORD: // immediate mode
                case SYMBOL:
                    tokenizer.unGetToken ();
                    try
                    {
                        Program prg = new Program(streamingTextArea); // empty prog
                        Statement s = statement (tokenizer);
                        do
                        {
                            s = s.execute (prg, inStream, outStream);
                        }
                        while (s != null);
                    }
                    catch (BASICSyntaxError e)
                    {
                        outStream.println ("Syntax Error : " + e.getMsg ());
                        outStream.println (tokenizer.showError ());
                    }
                    catch (BASICRuntimeError er)
                    {
                        outStream.println ("RUNTIME ERROR.");
                        outStream.println (er.getMsg ());
                    }
                    break;

                /*
                 * Blank lines are ignored.
                 */
                case EOL:
                    break;

                /*
                 * Anything else is an error.
                 */
                default:
                    outStream.println ("Error, Token not recognized.");
                    break;
            }
            outStream.println ("Ready.");
        }
    }
}
