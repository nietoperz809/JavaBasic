/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misc;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyVetoException;

/**
 *
 * @author Administrator
 */
public class MDIActions
{
//    public static void closeAll (JDesktopPane desk)
//    {
//        JInternalFrame[] allframes = desk.getAllFrames();
//        for (JInternalFrame f : allframes)
//        {
//            InternalFrameListener[] listeners = f.getInternalFrameListeners();
//            for (InternalFrameListener l : listeners)
//            {
//                l.internalFrameClosing(null);
//            }
//            desk.getDesktopManager().closeFrame(f);
//            for (InternalFrameListener l : listeners)
//            {
//                l.internalFrameClosed(null);
//            }
//        }
//    }

    public static void cascade (JDesktopPane desk)
    {
        JInternalFrame[] allframes = desk.getAllFrames();
        if (allframes.length == 0)
        {
            return;
        }
        // Rev array
        for(int i=0; i<allframes.length/2; i++)
        {
            JInternalFrame temp = allframes[i];
            allframes[i] = allframes[allframes.length -i -1];
            allframes[allframes.length -i -1] = temp;
        }

        int start = 0;
        Dimension size = desk.getSize();

        for (JInternalFrame f : allframes)
        {
            desk.getDesktopManager().resizeFrame(f, start, start,
                    size.width, size.height);
            start += 20;
            size.width -= 20;
            size.height -= 20;
        }
    }

    public static void arrange (JDesktopPane desk)
    {
        // How many frames do we have?
        JInternalFrame[] allframes = desk.getAllFrames();
        int count = allframes.length;
        if (count == 0)
        {
            return;
        }

        // Determine the necessary grid size
        int sqrt = (int) Math.sqrt(count);
        int rows = sqrt;
        int cols = sqrt;
        if (rows * cols < count)
        {
            cols++;
            if (rows * cols < count)
            {
                rows++;
            }
        }

        // Define some initial values for size & location.
        Dimension size = desk.getSize();

        int w = size.width / cols;
        int h = size.height / rows;
        int x = 0;
        int y = 0;

        // Iterate over the frames, deiconifying any iconified frames and then
        // relocating & resizing each.
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols && ((i * cols) + j < count); j++)
            {
                JInternalFrame f = allframes[(i * cols) + j];

                if (!f.isClosed() && f.isIcon())
                {
                    try
                    {
                        f.setIcon(false);
                    }
                    catch (PropertyVetoException ignored)
                    {
                    }
                }

                desk.getDesktopManager().resizeFrame(f, x, y, w, h);
                x += w;
            }
            y += h; // start the next row
            x = 0;
        }
    }
}
