package streameditor;

import misc.Misc;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Administrator
 */
public class StreamingTextArea extends JTextArea implements Runnable {
    public final ArrayBlockingQueue<String> lineBuffer = new ArrayBlockingQueue<>(128, true);

    private final InStream in;
    private final OutStream out;

    private transient Thread thread;
    public char lastKey = 0xffff;
    private int previousLinenum = 0;
    volatile private boolean basicIsRunning = false;
    private boolean manual_interrupt;

    public StreamingTextArea() {
        super();
        setCaret(new BlockCaret());
        in = new InStream();
        out = new OutStream();
        listenCaret();
        startThread();
    }

    public void setColors(int fore, int back) {
        setBackground(new java.awt.Color(back));
        setForeground(new java.awt.Color(fore));
    }

    public String getPreviousLine() {
        try {
            int start = getLineStartOffset(previousLinenum);
            int end = getLineEndOffset(previousLinenum);
            return getText(start, end - start).trim();
        } catch (BadLocationException e) {
            return "";
        }
    }

    private void listenCaret() {
        // Add a caretListener to the editor. This is an anonymous class because it is inline and has no specific name.
        this.addCaretListener((CaretEvent e) ->
        {
            JTextArea editArea = (JTextArea) e.getSource();

            try {
                int caretpos = editArea.getCaretPosition();
                previousLinenum = editArea.getLineOfOffset(caretpos) - 1;
                //columnnum = caretpos - editArea.getLineStartOffset(linenum);
            } catch (Exception ignored) {
            }
        });

        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                lastKey = c;
                if (basicIsRunning)
                    in.write(c);
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == '\n' && !basicIsRunning) {
                    String s = getPreviousLine();
                    try {
                        lineBuffer.put(s);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
            }
        });
    }

    public String getBufferedLine() {
        try {
            return lineBuffer.take();
        } catch (Exception e) {
            //e.printStackTrace();
            return "";
        }
    }

    public final void startThread() {
        // thread = null;
        Misc.execute(this);
    }

    public InputStream getInputStream() {
        return in;
    }

    public PrintStream getPrintStream() {
        return new PrintStream(out);
    }

    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public void paste() {
        super.paste();
        String clip = Misc.getClipBoardString();
        String[] split = Objects.requireNonNull(clip).split("\\n");
        for (String s : split) {
            try {
                lineBuffer.put(s);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void fakeIn(String s) {
        System.out.println("fake " + s);
        try {
            if (basicIsRunning) {
                for (int n = 0; n < s.length(); n++) {
                    in.write(s.charAt(n));
                }
            } else {
                lineBuffer.put(s);
            }
        } catch (InterruptedException e) {
            System.out.println("fakeIn interrupted");
        }
    }

    /**
     *
     */
    public synchronized void destroy() {
        thread.interrupt();
        fakeIn("bye\n");
    }

    public void interrupt() {
        manual_interrupt = true;
        thread.interrupt();
    }


    @Override
    public void run() {
        thread = Thread.currentThread();
        System.out.println("stream thread start");
        while (true) //!thread.isInterrupted())
        {
            char c;
            try {
                c = out.buffer.take();
            } catch (InterruptedException ex) {
                if (manual_interrupt) {
                    c = 0;
                } else {
                    System.out.println("stream thread ended");
                    return;
                }
            }
            try {
                synchronized (this) {
                    int cp = getCaretPosition();
                    insert("" + c, cp);
                    setCaretPosition(cp + 1);
                }
            } catch (Exception ex) {
                System.out.println(ex + " -- " + c);
            }
        }
    }

    public synchronized void startRunMode() {
        basicIsRunning = true;
        lineBuffer.clear();
    }

    public synchronized void stopRunMode() {
        basicIsRunning = false;
        in.reset();
    }
}
