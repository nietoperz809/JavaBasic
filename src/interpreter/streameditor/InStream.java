/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interpreter.streameditor;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author Administrator
 */
class InStream extends java.io.InputStream implements Serializable
{
    public static final long serialVersionUID = 1L;
    public ArrayBlockingQueue<Character> buffer = new ArrayBlockingQueue<>(128);

    /**
     *
     * @param
     */
    public InStream ()
    {
    }

    @Override
    public int available()
    {
        return buffer.remainingCapacity();
    }
    
    @Override
    public int read()
    {
        try
        {
            return buffer.take();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            return -1;
        }
    }
}
