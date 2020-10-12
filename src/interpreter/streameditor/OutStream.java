/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interpreter.streameditor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 *
 * @author Administrator
 */
class OutStream extends OutputStream implements Serializable
{

    /**
     *
     */
    public static final long serialVersionUID = 1L;
    private final RingBuffer<Character> buffer;

    /**
     *
     * @param b
     */
    public OutStream (RingBuffer b)
    {
        buffer = b;
    }    
    
    @Override
    public void write(int b) throws IOException
    {
        try
        {
            buffer.add((char)b);
        }
        catch (InterruptedException ex)
        {
        }
    }
}
