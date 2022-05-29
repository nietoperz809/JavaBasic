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
package basic_1;

import applications.BasicGUI;
import basic_1.streameditor.StreamingTextArea;
import midisystem.MidiSynthSystem;
import misc.Transmitter;

import javax.sound.midi.Instrument;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.concurrent.FutureTask;

import static basic_1.ParseStatement.statement;
import static misc.Misc.formatBasicLine;

/**
 * This class is an "interactive" BASIC environment. You can think of it as
 * BASIC debug mode. Using the streams you passed in to create the object, it
 * hosts an interactive session allowing the user to enter BASIC programs, run
 * them, save them, and load them.
 */
public class CommandInterpreter {
    public LexicalTokenizer tokenizer;
    public Program basicProgram;

    private StreamingTextArea streamingTextArea;
    transient private DataInputStream inStream;
    transient private PrintStream outStream;

    final transient private BasicGUI m_bg;

    /**
     * Create a new command interpreter attached to the passed in streams.
     */
    public CommandInterpreter(BasicGUI bg) {
        m_bg = bg;
    }

    // This method basically dispatches the commands of the command interpreter.
    private Program processCommand(Program pgm, LexicalTokenizer lt, Token x) throws Exception {
        Token t;

        switch (x.kwValue) {
            case CMD_RESUME:
                try {
                    pgm.resume(inStream, outStream);
                } catch (BASICRuntimeError e) {
                    outStream.println(e.getMsg());
                }
                return pgm;

            case CMD_CONT:
                try {
                    pgm.cont(inStream, outStream);
                } catch (BASICRuntimeError e) {
                    outStream.println(e.getMsg());
                }
                return pgm;

            case CMD_RUN:
                t = lt.nextToken();
                int startline = 0;
                if (t.typeNum() == KeyWords.CONSTANT) {
                    startline = (int) t.numValue();
                }
                //System.out.println("before run"); // +++++++++++++++++++++++++
                streamingTextArea.startRunMode();
                try {
                    pgm.run(inStream, outStream, startline);
                } catch (BASICRuntimeError e2) {
                    outStream.println(e2.getMsg());
                }
                //System.out.println("after run");  // +++++++++++++++++++++++++
                streamingTextArea.stopRunMode();
                return pgm;

            case CMD_CMDS:
                t = lt.nextToken();
                String search = "";
                if (t.type == KeyWords.KEYWORD) {
                    search = t.sValue;
                } else if (t instanceof Variable) {
                    Variable v1 = (Variable) t;
                    search = v1.name;
                }
                List<KeyWords> list = KeyWords.getFiltered(search);
                for (KeyWords k : list) {
                    String description = k.getDesc();
                    String cmd = k.toString().toUpperCase();
                    outStream.print(cmd);
                    outStream.println(" - " + description);
                }
                return pgm;

            case CMD_INSTRLIST:
                Instrument[] instr = MidiSynthSystem.get().getInstruments();
                StringBuilder sb = new StringBuilder();
                for (int n = 0; n < instr.length; n++) {
                    sb.append(n);
                    sb.append(" -- ");
                    sb.append(instr[n].toString());
                    sb.append('\n');
                }
                outStream.println(sb);
                return pgm;

            case CMD_CAT:
                t = lt.nextToken();
                if (t.typeNum() != KeyWords.STRING) {
                    outStream.println("File name expected for CAT Command.");
                    return pgm;
                }
                File f = new File(t.stringValue());
                InputStream in = new FileInputStream(f);
                Transmitter tr = new Transmitter(in, outStream);
                tr.doTransmission(null);
                return pgm;

            case CMD_DEL:
                t = lt.nextToken();
                if (t.typeNum() != KeyWords.STRING) {
                    outStream.println("File name expected for DEL Command.");
                    return pgm;
                }
                new File(t.stringValue()).delete();
                return pgm;

            case CMD_SAVE:
                t = lt.nextToken();
                if (t.typeNum() != KeyWords.STRING) {
                    outStream.println("File name expected for SAVE Command.");
                    return pgm;
                }
                String fname = t.stringValue();
                if (!fname.endsWith(".bas"))
                    fname = fname + ".bas";
                FileOutputStream fos;
                File ff = new File (fname);
                fos = new FileOutputStream(ff);
                outStream.println("Saving file... "+ff.getAbsolutePath());
                PrintStream pp = new PrintStream(fos);
                pgm.list(pp);
                pp.flush();
                fos.close();
                return pgm;

            case CMD_LOAD:
                t = lt.nextToken();
                if (t.typeNum() != KeyWords.STRING) {
                    outStream.println("File name expected for LOAD command.");
                    return pgm;
                }
                try {
                    pgm = Program.load(t.stringValue(), pgm.area);
                    outStream.println("File loaded.");
                } catch (IOException e) {
                    outStream.println("File " + t.stringValue() + " not found.");
                    return pgm;
                } catch (BASICSyntaxError bse) {
                    outStream.println("Syntax error reading file.");
                    outStream.println(bse.getMsg());
                    return pgm;
                }
                return pgm;

            case CMD_DIR:
                t = lt.nextToken();
                String path;
                if (t.typeNum() != KeyWords.STRING) {
                     path = ".";
                }
                else {
                    path = t.stringValue();
                }
                File[] filesInFolder = new File(path).listFiles();
                for (final File fileEntry : filesInFolder) {
                    if (fileEntry.isFile()) {
                        outStream.println(fileEntry.getName() + " -- " + fileEntry.length());
                    }
                }
                return pgm;

            case CMD_RENUMBER:
                Point pt = get2Val(lt, new Point(10,10));
                return pgm.renumber(pt.x, pt.y);

            case CMD_LIST:
                Point pt2 = get2Val(lt, new Point(0,Integer.MAX_VALUE));
                pgm.list (pt2.x, pt2.y, outStream);
                return pgm;
        }
        outStream.println("Command not implemented.");
        return pgm;
    }

    private final char[] data = new char[256];

    public void dispose() {
        streamingTextArea.destroy();
    }

    /**
     * Read 2 values sparated by comma
     * @param lt used tokenizer
     * @param pt default values
     * @return a point as carrier for both values
     */
    private Point get2Val (LexicalTokenizer lt, Point pt) {
        Token t = lt.nextToken();
        if (t.typeNum() == KeyWords.EOL) {
            return pt;
        }
        if (t.typeNum() == KeyWords.CONSTANT) {
            pt.x = (int) t.numValue();
            t = lt.nextToken();
            if (t.typeNum() == KeyWords.EOL) {
                return pt;
            }
            if (t.isSymbol(',')) {
                t = lt.nextToken();
                if (t.typeNum() == KeyWords.CONSTANT) {
                    pt.y = (int) t.numValue();
                    return pt;
                }
            }
        }
        return pt;
    }

    /**
     * Starts the interactive session. When running the user should see the
     * "Ready." prompt. The session ends when the user types the
     * <code>bye</code> command.
     *
     * @return EndReason 1 == BYE detected
     * @throws java.lang.Exception
     */
    public void prestart(StreamingTextArea area) {
        System.err.println("start BASIC system");
        streamingTextArea = area;
        inStream = new DataInputStream(area.getInputStream());
        outStream = new PrintStream(area.getOutputStream());

        if (tokenizer == null) {
            System.out.println("create Tokenizer");
            tokenizer = new LexicalTokenizer(data);
        }
        if (basicProgram == null) {
            System.out.println("create BASIC prog");
            basicProgram = new Program(area);
        }

        outStream.println("*JavaBasic*\nType CMDS or CMDS n to see commands (beginning with n)\n");
    }

    public void runCLI() throws Exception {
        while (true) {
            String lineData = formatBasicLine (streamingTextArea.getBufferedLine());

            m_bg.setLineInList(lineData);

            tokenizer.reset(lineData);

            if (!tokenizer.hasMoreTokens()) {
                //System.out.println("no more tokens");
                continue;
            }

            Token t = tokenizer.nextToken();
            boolean red = true;
            switch (t.typeNum()) {
                /*
                 * Process one of the command interpreter's commands.
                 */
                case COMMAND:
                    if (t.kwValue == KeyWords.CMD_BYE) {
                        FutureTask<?> ft = m_bg.basicTask;
                        if (ft != null) {
                            ft.cancel(true);
                            m_bg.dispose();
                        }
                        return;
                    } else if (t.kwValue == KeyWords.CMD_NEW) {
                        basicProgram = new Program(streamingTextArea);
                        System.gc();
                    } else {
                        basicProgram = processCommand(basicProgram, tokenizer, t);
                    }
                    break;

                /*
                 * Process an initial number, it can be a new statement line
                 * or it may be an implicit delete command.
                 */
                case CONSTANT:
                    Token peek = tokenizer.nextToken();
                    if (peek.typeNum() == KeyWords.EOL) {
                        basicProgram.del((int) t.numValue());
                        break;
                    } else {
                        tokenizer.unGetToken();
                    }
                    try {
                        Statement s = statement(tokenizer);
                        s.addText(lineData);
                        s.addLine((int) t.numValue());
                        basicProgram.add((int) t.numValue(), s);
                    } catch (BASICSyntaxError e) {
                        outStream.println("Syntax Error : " + e.getMsg());
                        outStream.println(tokenizer.showError());
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
                    tokenizer.unGetToken();
                    try {
                        Program prg = new Program(streamingTextArea); // empty prog
                        Statement s = statement(tokenizer);
                        if (s.keyword == KeyWords.NULL)
                            red = false;
                        do {
                            s = s.execute(prg, inStream, outStream);
                        }
                        while (s != null);
                    } catch (BASICSyntaxError e) {
                        outStream.println("Syntax Error : " + e.getMsg());
                        outStream.println(tokenizer.showError());
                    } catch (BASICRuntimeError er) {
                        outStream.println("RUNTIME ERROR.");
                        outStream.println(er.getMsg());
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
                    outStream.println("Error, Token not recognized.");
                    break;
            }
            if (red)
                outStream.println("Ready.");
        }
    }
}
