/*
 * Tools of all kind
 */
package misc;

import org.apache.commons.lang.StringUtils;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
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

    public static DecimalFormat df = new DecimalFormat("#.########");

    private static final ExecutorService globalExecutor = Executors.newCachedThreadPool(); //Executors.newFixedThreadPool(20);

    public static FutureTask<?> execute (Runnable r)
    {
//        if (getExecutorFreeSlots() <= 0)
//        {
//            System.out.println("Thread pool exhausted");
//        }
        return (FutureTask<?>) globalExecutor.submit(r);
    }

//    private static int getExecutorFreeSlots ()
//    {
//        int tc = ((ThreadPoolExecutor) globalExecutor).getActiveCount();
//        int tm = ((ThreadPoolExecutor) globalExecutor).getCorePoolSize();
//        //System.out.println(tc + "/" + tm);
//        return tm - tc;
//    }

    public static String humanReadableByteCount (long bytes)
    {
        DecimalFormat myFormatter = new DecimalFormat("000,000,000");
        return "  " + myFormatter.format(bytes);
    }

    private static String formatBlHelper(String in) {
        in = in.replaceAll("\\s{2,}"," ").trim();
        in = in.replace("\t", " ");
        String in2;
        do {
            in2 = in;
            in = in.replaceAll (", | ,",",");
        } while (!in.equals(in2));
        return in;
    }

    /**
     * remove multple banks exxcept between "
     * @param in raw input line
     * @return trimmed line
     */
    public static String formatBasicLine (String in)
    {
        String[] str = StringUtils.substringsBetween(in, "\"", "\"");
        if (str == null)
            return formatBlHelper(in);
        for (int s = 0; s < str.length; s++)
            in = in.replace(str[s], "#" + s);
        in = formatBlHelper(in);
        for (int s = 0; s < str.length; s++)
            in = in.replace("#" + s, str[s]);
        return in;
    }

    /**
     * @return string on the clipboard
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
