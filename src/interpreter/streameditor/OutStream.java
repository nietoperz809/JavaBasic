/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interpreter.streameditor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author Administrator
 */
class OutStream extends OutputStream implements Serializable
{
    public ArrayBlockingQueue<Character> buffer = new ArrayBlockingQueue<>(128, true);


    public OutStream ()
    {
    }


    @Override
    public void write (int bt)
    {
        try
        {
            buffer.put((char)bt);
        }
        catch (InterruptedException e)
        {
        }
    }
}
