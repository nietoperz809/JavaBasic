/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package midisystem;

import java.util.Arrays;

/**
 *
 * @author Administrator
 */
public class Notes
{
    static private final String[] src = new String[]
    {
        "C", "C#", "D", "D#", "E", "F",
        "F#", "G", "G#", "A", "A#", "B",
    };


    @Override
    public String toString()
    {
        return Arrays.toString(src);
    }
            
    static private int find(String s) throws IllegalArgumentException
    {
        for (int n = 0; n < src.length; n++)
        {
            if (src[n].equals(s))
            {
                return n;
            }
        }
        throw new IllegalArgumentException("Not a note:" + s);
    }

    private static String getNote (int n) throws IndexOutOfBoundsException
    {
        if (n < 0 || n > 127)
        {
            throw new IndexOutOfBoundsException("" + n + " must be between 0...127");
        }
        int m = n % 12;
        int offs = n / 12 - 2;
        return src[m] + offs;
    }

    static public int getNumber(String note)
    {
        try
        {
            char c = note.charAt(1);
            String n1 = "" + note.charAt(0);
            String n2;
            if (c == '#')
            {
                n1 += '#';
                n2 = note.substring(2);
            }
            else
            {
                n2 = note.substring(1);
            }
            int res = Integer.parseInt(n2);
            if (res < -2 || res > 8)
            {
                throw new IllegalArgumentException("illegal note:" + note);
            }
            res = (res + 2) * 12 + find(n1);
            return res;
        }
        catch (Exception ex)
        {
            throw new IllegalArgumentException("illegal note:" + note);
        }
    }
    
    /**
     * Testing main func
     * @param args
     * @throws Exception 
     */
    static public void main(String... args) throws Exception
    {
        for (int n = 0; n < 128; n++)
        {
            System.out.println(getNote(n));
        }
        System.out.println(getNumber("C2"));
    }
}
