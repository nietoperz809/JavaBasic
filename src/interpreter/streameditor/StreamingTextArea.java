package interpreter.streameditor;

import misc.BlockCaret;
import misc.Misc;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author Administrator
 */
public class StreamingTextArea extends JTextArea implements Runnable
{
    private static final long serialVersionUID = 1L;
    private final InStream in;

    private final OutStream out;

    private transient Thread thread;

    public char lastKey = 0xffff;

    private int linenum = 0;

    /**
     *
     */
    public StreamingTextArea ()
    {
        super();
        setCaret(new BlockCaret());
        in = new InStream();
        out = new OutStream();
        listenCaret();
        startThread();
    }

    public void setColors (int fore, int back)
    {
        setBackground(new java.awt.Color(back));
        setForeground(new java.awt.Color(fore));
    }

    private void listenCaret ()
    {
        // Add a caretListener to the editor. This is an anonymous class because it is inline and has no specific name.
        this.addCaretListener((CaretEvent e) ->
        {
            JTextArea editArea = (JTextArea) e.getSource();

            // Lets start with some default values for the line and column.
            // We create a try catch to catch any exceptions. We will simply ignore such an error for our demonstration.
            try
            {
                // First we find the position of the caret. This is the number of where the caret is in relation to the start of the JTextArea
                // in the upper left corner. We use this position to find offset values (eg what line we are on for the given position as well as
                // what position that line starts on.
                int caretpos = editArea.getCaretPosition();
                linenum = editArea.getLineOfOffset(caretpos);

                // We subtract the offset of where our line starts from the overall caret position.
                // So lets say that we are on line 5 and that line starts at caret position 100, if our caret position is currently 106
                // we know that we must be on column 6 of line 5.
                //columnnum = caretpos - editArea.getLineStartOffset(linenum);
            }
            catch (Exception ex)
            {
            }
            // Once we know the position of the line and the column, pass it to a helper function for updating the status bar.
        });
        this.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped (KeyEvent e)
            {
                char c = e.getKeyChar();
                lastKey = c;
                if (c == '\n')
                {
                    try
                    {
                        StreamingTextArea editArea = (StreamingTextArea) e.getSource();
                        String[] lines = editArea.getText().split("\\n");
                        int idx = (linenum > 0) ? linenum - 1 : linenum;
                        if (lines.length > idx)
                        {
                            String t = lines[idx];
                            char[] cta = t.toCharArray();
                            for (Character ct : cta)
                            {
                                in.buffer.put(ct);
                            }
                        }
                        in.buffer.put('\n');
                    }
                    catch (InterruptedException ex)
                    {
                    }
                }
            }

            @Override
            public void keyPressed (KeyEvent e)
            {
            }

            @Override
            public void keyReleased (KeyEvent e)
            {
            }
        });
    }

    public final void startThread ()
    {
        thread = null;
        Misc.execute(this);
    }

    /**
     * @return
     */
    public InputStream getInputStream ()
    {
        return in;
    }

    public PrintStream getPrintStream ()
    {
        return new PrintStream(out);
    }

    public DataInputStream getDataInputStream ()
    {
        return new DataInputStream(in);
    }

    /**
     * @return
     */
    public OutputStream getOutputStream ()
    {
        return out;
    }

    @Override
    public void paste ()
    {
        super.paste();
        String s = Misc.getClipBoardString();
        fakeIn(s);
    }

    public void fakeIn (String s)
    {
        for (int n = 0; n < s.length(); n++)
        {
            try {
                in.buffer.put(s.charAt(n));
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     *
     */
    public synchronized void destroy ()
    {
        thread.interrupt();
    }

    @Override
    public void run ()
    {
        thread = Thread.currentThread();
        while (!thread.isInterrupted())
        {
            Character c;
            try
            {
                c = out.buffer.take();
            }
            catch (InterruptedException ex)
            {
                System.out.println("stream thread ended");
                break;
            }
            try
            {
                synchronized (this)
                {
                    int cp = getCaretPosition();
                    insert(""+c, cp);
                    setCaretPosition(cp+1);
                }
            }
            catch (Exception ex)
            {
                System.out.println(ex + " -- " + c);
            }
        }
    }
}
