/*
 * Tools of all kind
 */
package misc;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.text.DecimalFormat;
import java.util.concurrent.*;

import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.datatransfer.DataFlavor.stringFlavor;

/**
 * @author Administrator
 */
public class Misc
{
    private static final String BUILD_NUMBER = "51";
    private static final String BUILD_DATE = "10/18/2020 02:41:41 AM";

    public static final String buildInfo = "JavaBasic, Build: " + BUILD_NUMBER + ", " + BUILD_DATE
            + " -- " + System.getProperty ("java.version");

    private static final ExecutorService globalExecutor = Executors.newCachedThreadPool(); //Executors.newFixedThreadPool(20);

    public static DecimalFormat df = new DecimalFormat("#.########");

    public static FutureTask<?> execute (Runnable r)
    {
        if (getExecutorFreeSlots() <= 0)
        {
            System.out.println("Thread pool exhausted");
        }
        return (FutureTask<?>) globalExecutor.submit(r);
    }

    private static int getExecutorFreeSlots ()
    {
        int tc = ((ThreadPoolExecutor) globalExecutor).getActiveCount();
        int tm = ((ThreadPoolExecutor) globalExecutor).getCorePoolSize();
        //System.out.println(tc + "/" + tm);
        return tm - tc;
    }

    public static String humanReadableByteCount (long bytes)
    {
        DecimalFormat myFormatter = new DecimalFormat("000,000,000");
        return "  " + myFormatter.format(bytes);
    }

    /**
     * @return
     */
    public static String getClipBoardString ()
    {
        Clipboard clipboard = getDefaultToolkit().getSystemClipboard();
        Transferable clipData = clipboard.getContents(clipboard);
        if (clipData != null)
        {
            try
            {
                if (clipData.isDataFlavorSupported(stringFlavor))
                {
                    return (String) (clipData.getTransferData(stringFlavor));
                }
            }
            catch (UnsupportedFlavorException | IOException ufe)
            {
                System.err.println("getClipoardString fail");
            }
        }
        return null;
    }

// --Commented out by Inspection START (10/19/2020 2:23 AM):
//    /**
//     * Centers Component
//     *
//     * @param a Component to center
//     * @param b Parent component
//     */
//    public static void centerComponent (Component a, Component b)
//    {
//        Dimension db = b.getSize();
//        Dimension da = a.getSize();
//        Point pt = new Point((db.width - da.width) / 2, (db.height - da.height) / 2);
//        if (pt.x < 0)
//        {
//            pt.x = 0;
//        }
//        if (pt.y < 0)
//        {
//            pt.y = 0;
//        }
//        a.setLocation(pt);
//    }
// --Commented out by Inspection STOP (10/19/2020 2:23 AM)


    /**
     * Get byte array from resource bundle
     * @param name what resource
     * @return the resource as byte array
     * @throws Exception if smth. went wrong
     */
    static public byte[] extractResource (String name) throws Exception
    {
        InputStream is = ClassLoader.getSystemResourceAsStream (name);

        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        byte[] buffer = new byte[1024];
        while (true)
        {
            int r = is.read (buffer);
            if (r == -1)
            {
                break;
            }
            out.write (buffer, 0, r);
        }

        return out.toByteArray ();
    }

    /**
     * Play WAV
     * @param data the WAV file as byte array
     * @throws Exception If smth. went wrong
     */
    public static void playWave (byte[] data) throws Exception
    {
        final Clip clip = (Clip) AudioSystem.getLine (new Line.Info (Clip.class));
        InputStream inp  = new BufferedInputStream(new ByteArrayInputStream(data));
        clip.open (AudioSystem.getAudioInputStream (inp));
        clip.start ();
    }
}
