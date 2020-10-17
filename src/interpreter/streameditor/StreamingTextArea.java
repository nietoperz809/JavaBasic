package interpreter.streameditor;

import misc.BlockCaret;
import misc.Misc;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Administrator
 */
public class StreamingTextArea extends JTextArea implements Runnable
{
    public ArrayBlockingQueue<String> lineBuffer = new ArrayBlockingQueue<>(128,true);

    private final InStream in;
    private final OutStream out;

    private transient Thread thread;
    public char lastKey = 0xffff;
    private int previousLinenum = 0;
    private boolean basicIsRunning = false;

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

    public String getPreviousLine()
    {
        try
        {
            int start = getLineStartOffset (previousLinenum);
            int end = getLineEndOffset (previousLinenum);
            return getText (start,end-start).trim();
        }
        catch (BadLocationException e)
        {
            return "";
        }
    }

    private void listenCaret ()
    {
        // Add a caretListener to the editor. This is an anonymous class because it is inline and has no specific name.
        this.addCaretListener((CaretEvent e) ->
        {
            JTextArea editArea = (JTextArea) e.getSource();

            try
            {
                int caretpos = editArea.getCaretPosition();
                previousLinenum = editArea.getLineOfOffset(caretpos)-1;
                //columnnum = caretpos - editArea.getLineStartOffset(linenum);
            }
            catch (Exception ex)
            {
            }
        });

        this.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped (KeyEvent e)
            {
                char c = e.getKeyChar();
                lastKey = c;

                try
                {
                    //if (c == '\n' || !Character.isISOControl(c))
                    if (basicIsRunning)
                        in.buffer.put(c);
                }
                catch (InterruptedException interruptedException)
                {
                    interruptedException.printStackTrace();
                }

            }

            @Override
            public void keyPressed (KeyEvent e)
            {
            }

            @Override
            public void keyReleased (KeyEvent e)
            {
                if (e.getKeyChar() == '\n'&& !basicIsRunning)
                {
                    String s = getPreviousLine();
                    try
                    {
                        lineBuffer.put(s);
                    }
                    catch (InterruptedException interruptedException)
                    {
                        interruptedException.printStackTrace();
                    }
                }
            }
        });
    }

    public String getBufferedLine()
    {
        try
        {
            return lineBuffer.take();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return "";
        }
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
            try
            {
                in.buffer.put(s.charAt(n));
            }
            catch (InterruptedException e)
            {
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

        try
        {
            out.buffer.put('*');
        }
        catch (InterruptedException e)
        {
            return;
        }
    }

    @Override
    public void run ()
    {
        thread = Thread.currentThread();
        System.out.println("stream thread start");
        while (true) //!thread.isInterrupted())
        {
            Character c;
            try
            {
                c = out.buffer.take();
            }
            catch (InterruptedException ex)
            {
                System.out.println("stream thread ended");
                return;
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

    public void startRunMode()
    {
        basicIsRunning = true;
        lineBuffer.clear();
    }

    public void stopRunMode()
    {
        basicIsRunning = false;
        in.buffer.clear();
    }
}
