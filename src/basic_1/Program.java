/*
 * Program.java - One BASIC program, ready to roll.
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
package basic_1;

import streameditor.StreamingTextArea;
import basic_1.util.RedBlackTree;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;
import midisystem.MidiSynthSystem;
import misc.MainWindow;
import org.mathIT.util.FunctionParser;

import javax.sound.sampled.AudioFileFormat;
import java.io.*;
import java.net.Socket;
import java.util.*;

import static basic_1.ParseStatement.statement;
import static misc.Misc.formatBasicLine;

/**
 * This class instantiates a BASIC program. A valid program is one that is
 * parsed and ready to run. You can run it by invoking the run() method. The
 * standard input and output of the basic_prg_running basic program can either
 * be passed into the <b>run</b> method, or they can be presumed to be the in
 * and out streams referenced by the <b>System</b> class.
 * <p>
 * This class uses Red-Black trees to hold the parsed statements and the symbol
 * table.
 *
 * @author Chuck McManis
 * @version 1.1
 * @see CommandInterpreter
 */
public class Program //implements Runnable, Serializable
{
    public final StreamingTextArea area;
    // this tree holds all of the statements.
    private final RedBlackTree<Integer, Statement> stmts = new RedBlackTree<>();
    public boolean basic_prg_running = true;  // Program basic_prg_running
    public boolean thread_running = true; // Thread basic_prg_running 
    public final long basetime = System.currentTimeMillis();
    public final HashMap<String, FunctionParser> defFuncs = new HashMap<>();

    public static class ExtendedSocket {
        private final Closeable sock;
        public boolean textMode = false;
        public ExtendedSocket(Closeable s) {
            this.sock = s;
        }
        public Socket getSocket() {
            try {
                return (Socket)sock;
            } catch (ClassCastException e) {
                throw new BASICRuntimeError("not a Socket");
            }
        }
    }

    public final HashMap<String, ExtendedSocket> sockMap = new HashMap<>();
    private AudioPlayer audioPlayer;
    // this tree holds all of the variables.
    private RedBlackTree<String, Variable> vars = new RedBlackTree<>();
    private Stack<Statement> stmtStack = new Stack<>();
    private Vector<Token> dataStore = new Vector<>();
    private int dataPtr = 0;
    private Random r = new Random(0);

    public Program(StreamingTextArea ta) {
        area = ta;
    }

    /**
     * There are two ways to create a new program object, you can load one from
     * an already open stream or you can pass in a file name and load one from
     * the file system.
     */
    public static Program load(InputStream source, StreamingTextArea ar) throws IOException, BASICSyntaxError {
        BufferedReader dis
                = new BufferedReader(new InputStreamReader(source));

        LexicalTokenizer lt = new LexicalTokenizer();
        String lineData;
        Statement s;
        Token t;
        Program prog = new Program(ar);

        while (true) {
            // read a line of our BASIC program.
            lineData = dis.readLine();

            // if EOF simply return.
            if (lineData == null) {
                return prog;
            }

            // if the line was blank, ignore it.
            if (lineData.length() == 0) {
                continue;
            }

            lineData = formatBasicLine(lineData);
            lt.feedNewLine(lineData);
            t = lt.nextToken();
            if (t.typeNum() != KeyWords.CONSTANT) {
                throw new BASICSyntaxError("Line failed to start with a line number.");
            }

            try {
                s = statement(lt);
            } catch (BASICSyntaxError bse) {
                ar.getPrintStream().println("Syntax error: " + bse.getMsg());
                ar.getPrintStream().println(lt.showError());
                throw bse;
            }
            s.addText(lineData);
            s.addLine((int) t.numValue());
            prog.add((int) t.numValue(), s);
        }
    }

    public void RemoveAllSockets() throws IOException {
        for (ExtendedSocket s : sockMap.values()) {
            s.sock.close();
        }
        sockMap.clear();
    }

    /**
     * Load the specified file and parse the basic statements it contains.
     *
     * @throws IOException      when the filename cannot be located or opened.
     * @throws BASICSyntaxError when the file does not contain a properly formed
     *                          BASIC program.
     */
    public static Program load(String source, StreamingTextArea ar) throws IOException, BASICSyntaxError {
        // XXX this needs to use the SourceManager class //
        FileInputStream fis = new FileInputStream(source);
        Program r;
        try {
            r = load(fis, ar);
        } catch (BASICSyntaxError e) {
            fis.close();
            throw e;
        }
        fis.close();
        return r;
    }

    public Random getRandom() {
        return r;
    }

    public void randomize(double seed) {
        r = new Random((long) seed);
    }

    /**
     * Add a statement to the current program. Statements are indexed by line
     * number. If the add fails for some reason this method returns false.
     */
    void add(int line, Statement s) {
        stmts.put(line, s);
    }

    /**
     * Delete a statement from the current program. Statements are indexed by
     * line numbers. If the statement specified didn't exist, then this method
     * returns false.
     */
    void del(int line) {
        stmts.remove(line);
    }

    /**
     * Compute the indices based on the expressions in the variable object.
     */
    private double[] getIndices(Variable v) throws BASICRuntimeError {
        double[] result = new double[v.numExpn()];

        for (int i = 0; i < result.length; i++) {
            result[i] = v.expn(i).value(this);
        }
        return result;
    }

    /**
     * Return the numeric value of a variable in the symbol table.
     *
     * @throws BASICRuntimeError if the variable isn't defined.
     */
    public double getVariable(Variable v) throws BASICRuntimeError {
        double[] ii = getIndices(v);
        if (v.name.startsWith("fn")) { // TODO: function?
            String fname = v.name.substring(2).toUpperCase();
            FunctionParser fp = defFuncs.get (fname);
            if (fp == null) {
                throw new BASICRuntimeError("Func not defined: " + fname);
            }
            if (ii.length == 1)
                return fp.evaluate(0,ii[0]);
            return fp.evaluate(0, ii[0], ii[1]);
        }
        Variable vi = vars.get(v.name);
        if (vi == null) {
            throw new BASICRuntimeError("Undefined variable '" + v.name + "'");
        }
        if (!vi.isArray()) {
            return vi.numValue();
        }
        return vi.numValue(ii);
    }

    /**
     * Return the contents of the string variable named <i>name</i>. If the
     * variable has not yet been declared (ie used) this method throws a
     * BASICRuntime error.
     */
    String getString(Variable v) throws BASICRuntimeError {
        Variable vi = vars.get(v.name);
        if (vi == null) {
            throw new BASICRuntimeError("Variable " + v.name + " has not been initialized.");
        }
        if (!v.isArray()) {
            return vi.stringValue();
        }
        double[] ii = getIndices(v);
        return vi.stringValue(ii);
    }

    /**
     * Set the numeric variable <i>name</i> to have value <i>value</i>. If this
     * is the first time we have seen the variable, create a place for it in the
     * symbol table.
     */
    public <T> void setVariable(Variable v, T value) throws BASICRuntimeError {
        Variable vi = vars.get(v.name);
        if (vi == null) {
            if (v.isArray()) {
                throw new BASICRuntimeError("Array must be declared in a DIM statement");
            }
            vi = new Variable(v.name);
            vars.put(v.name, vi);
        }
        if (!vi.isArray()) {
            vi.setValue(value);
            return;
        }
        double[] ii = getIndices(v);
        vi.setValue(value, ii);
    }

    /**
     * This method is used by the DIM statement to DECLARE arrays. Given the
     * nature of arrays we force them to be declared before they can be used.
     * This is common to most BASIC implementations.
     */
    public void declareArray(Variable v) throws BASICRuntimeError {
        Variable vi;
        double[] ii = getIndices(v);
        vi = new Variable(v.name, ii);
        vars.put(v.name, vi);
    }

    /**
     * Compute and return the next program statement to be executed. The policy
     * is, if the current statement has another statement hanging off its
     * <i>nxt</i> pointer use that one, otherwise use the next one in the
     * program numerically.
     */
    public Statement nextStatement(Statement s) {
        if (s == null) {
            return null;
        } else if (s.nxt != null) {
            return s.nxt;
        }
        return stmts.next(s.line);
    }

    /**
     * Return the statment whose line number is <i>line</i>
     */
    public Statement getStatement(int line) {
        return stmts.get(line);
    }

    /**
     * List program lines from <i>start</i> to <i>end</i> out to the PrintStream
     * <i>p</i>. Note that due to a bug in the Windows implementation of
     * PrintStream this method is forced to append a <CR> to the file.
     */
    void list(int start, int end, PrintStream p) {
        for (Enumeration<Map.Entry<Integer, Statement>> e = stmts.elements(); e.hasMoreElements(); ) {
            Map.Entry<Integer, Statement> entry = e.nextElement();
            Statement s = entry.getValue();
            if ((s.lineNo() >= start) && (s.lineNo() <= end)) {
                p.print(s.asString());
                p.println(); // for Windows clients
            }
        }
    }

    Program renumber(int start, int step) throws NullPointerException {
        Renumberer ren = new Renumberer(this, start, step);
        Set<Integer> set = stmts.keySet();
        for (Integer i : set) {
            Statement stat = stmts.get(i);
            ren.add(stat.asString());
        }
        ren.doIt();
        return ren.finish();
    }


    /**
     * This is the first variation on list, it simply list from the starting
     * line to the the end of the program.
     */
    void list(int start, PrintStream p) {
        list(start, Integer.MAX_VALUE, p);
    }

    /**
     * This second variation on list will list the entire program to the passed
     * PrintStream object.
     */
    void list(PrintStream p) {
        list(0, p);
    }

    /**
     * Run the program and use the passed in streams as its input and output
     * streams.
     * <p>
     * Prior to basic_prg_running the program the statement stack is cleared,
     * and the data fifo is also cleared. Thus re-basic_prg_running a stopped
     * program will always work correctly.
     *
     * @throws BASICRuntimeError if an error occurs while basic_prg_running.
     */
    public void run(InputStream in, OutputStream out, int firstline) throws BASICError, IOException {
        PrintStream pout;
        Enumeration<Map.Entry<Integer, Statement>> e = stmts.elements();
        stmtStack = new Stack<>();    // assume no stacked statements ...
        dataStore = new Vector<>();   // ...  and no data to be read.
        dataPtr = 0;
        Statement s;

        vars = new RedBlackTree<>();

        // if the program isn't yet valid.
        if (!e.hasMoreElements()) {
            return;
        }

        if (out instanceof PrintStream) {
            pout = (PrintStream) out;
        } else {
            pout = new PrintStream(out);
        }

        /* First we load all of the data statements */
        while (e.hasMoreElements()) {
            s = (e.nextElement()).getValue();
            //System.out.println(s + " -- " + s.unparse());  // TODO test
            if (s.keyword == KeyWords.DATA) {
                s.execute(this, in, pout);
            }
        }

        e = stmts.elements();
        s = e.nextElement().getValue();
        if (firstline != 0)  // Skip lines if desired
        {
            do {
                if (s.line >= firstline) {
                    break;
                }
                s = e.nextElement().getValue();
            } while (e.hasMoreElements());
        }
        do {
            //Thread.yield();   // Let others run
            if (!basic_prg_running) {
                basic_prg_running = true;
                pout.println("Stopped at :" + s);
                push(s);
                break;
            }
            if (!thread_running) {
                thread_running = true;
                throw new BASICError ("Basic Thread forced to stop");
            }
            if (s.keyword != KeyWords.DATA) {

                s = s.execute(this, in, pout);
            } else {
                s = nextStatement(s);
            }
        }
        while (s != null);
        RemoveAllSockets();
        if (MidiSynthSystem.wasUsed())
            Objects.requireNonNull(MidiSynthSystem.get()).shutdown();
    }

    /**
     * This method resumes a program that has been stopped. If the program
     * wasn't really stopped it throws a BASICRuntimeError.
     *
     * @throws BASICRuntimeError - Program wasn't in a stopped state.
     */
    void resume(InputStream in, PrintStream pout) throws BASICRuntimeError {
        Statement s;

        s = pop();
        if ((s == null) || (s.keyword != KeyWords.STOP)) {
            throw new BASICRuntimeError("This program was not previously stopped.");
        }
        s = nextStatement(s);
        do {
            s = s.execute(this, in, pout);
        }
        while (s != null);
    }

    void cont(InputStream in, PrintStream pout) throws BASICRuntimeError {
        Statement s;

        s = pop();
        do {
            if (!basic_prg_running) {
                basic_prg_running = true;
                pout.println("Stopped at :" + s);
                push(s);
                break;
            }
            if (s.keyword != KeyWords.DATA) {

                s = s.execute(this, in, pout);
            } else {
                s = nextStatement(s);
            }
        }
        while (s != null);
    }


    /*
     * These methods deal with pushing and popping statements from the statement
     * stack, and data items from the data stack.
     */

    /**
     * Push this statement on the stack (one of FOR, GOSUB, or STOP)
     */
    public void push(Statement s) {
        stmtStack.push(s);
    }

    /**
     * Pop the next statement off the stack, return NULL if the stack is empty.
     */
    public Statement pop() {
        if (stmtStack.isEmpty()) {
            return null;
        }
        return stmtStack.pop();
    }

    /**
     * Add a token to the data FIFO.
     */
    public void pushData(Token t) {
        dataStore.addElement(t);
    }

    /**
     * Get the next token in the FIFO, return null if the FIFO is empty.
     */
    public Token popData() {
        if (dataPtr > (dataStore.size() - 1)) {
            return null;
        }
        return dataStore.elementAt(dataPtr++);
    }

    /**
     * Reset the data FIFO back to the beginning.
     */
    public void resetData() {
        dataPtr = 0;
    }

    /**
     * opem voice file
     */
    public void setVoiceFilename(String sss) {
        audioPlayer = new SingleFileAudioPlayer(sss, AudioFileFormat.Type.WAVE);
        MainWindow.voice.setAudioPlayer(audioPlayer);
    }

    /**
     * close voice file
     */
    public void unsetVoiceFilename() {
        try {
            audioPlayer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        VoiceManager voiceManager = VoiceManager.getInstance();
        MainWindow.voice = voiceManager.getVoice("kevin16");
        MainWindow.voice.allocate();
    }
}
