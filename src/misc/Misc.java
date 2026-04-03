/*
 * Tools of all kind
 */
package misc;

import org.apache.commons.lang.StringUtils;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.datatransfer.DataFlavor.stringFlavor;

/**
 * @author Administrator
 */
public final class Misc {
    private static final String BUILD_NUMBER = "357";
    private static final String BUILD_DATE = "06/23/2025 12:02:16 AM";

    public static final String buildInfo = "JavaBasic, Build: " + BUILD_NUMBER + ", " + BUILD_DATE
            + " -- " + System.getProperty("java.version");

    public static DecimalFormat df = new DecimalFormat("#.########");

    private static final ExecutorService globalExecutor = Executors.newCachedThreadPool(); //Executors.newFixedThreadPool(20);

    public static FutureTask<?> execute(Runnable r) {
//        if (getExecutorFreeSlots() <= 0)
//        {
//            System.out.println("Thread pool exhausted");
//        }
        return (FutureTask<?>) globalExecutor.submit(r);
    }

    public static String getTextBetweenQuotes(String input) {
        if (input == null) {
            return null; // Handle null input
        }

        // Regex: \"(.*?)\" matches text between double quotes (non-greedy)
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1); // Group 1 contains the text inside quotes
        }
        return null; // No match found
    }

    public static ByteArrayOutputStream dir(String path) {
        if (path == null) {
            path = System.getProperty("user.dir");
        }
        File[] filesInFolder = new File(path).listFiles();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
            ps.println("List of Dir: " + path);
            if (filesInFolder != null) {
                for (final File fileEntry : filesInFolder) {
                    if (fileEntry.isFile()) {
                        ps.println(fileEntry.getName() + " -- " + fileEntry.length());
                    }
                }
                return baos;
            }
        } catch (Exception e) {
            System.err.println("Error writing to PrintStream: " + e.getMessage());
        }
        return null;
    }

//    private static int getExecutorFreeSlots ()
//    {
//        int tc = ((ThreadPoolExecutor) globalExecutor).getActiveCount();
//        int tm = ((ThreadPoolExecutor) globalExecutor).getCorePoolSize();
//        //System.out.println(tc + "/" + tm);
//        return tm - tc;
//    }

    public static String humanReadableByteCount(long bytes) {
        DecimalFormat myFormatter = new DecimalFormat("000,000,000");
        return "  " + myFormatter.format(bytes);
    }

    private static String formatBlHelper(String in) {
        in = in.replaceAll("\\s{2,}", " ").trim();
        in = in.replace("\t", " ");
        String in2;
        do {
            in2 = in;
            in = in.replaceAll(", | ,", ",");
        } while (!in.equals(in2));
        return in.toUpperCase();
    }

    /**
     * remove multiple blanks, exxcept between "
     *
     * @param in raw input line
     * @return trimmed line
     */
    public static String formatBasicLine(String in) {
        String[] quoted = StringUtils.substringsBetween(in, "\"", "\"");
        if (quoted == null)
            return formatBlHelper(in);
        for (int s = 0; s < quoted.length; s++)
            in = in.replace(quoted[s], "#" + s);
        in = formatBlHelper(in);
        for (int s = 0; s < quoted.length; s++)
            in = in.replace("#" + s, quoted[s]);
        return in;
    }

    /**
     * @return string on the clipboard
     */
    public static String getClipBoardString() {
        Clipboard clipboard = getDefaultToolkit().getSystemClipboard();
        Transferable clipData = clipboard.getContents(clipboard);
        if (clipData != null) {
            try {
                if (clipData.isDataFlavorSupported(stringFlavor)) {
                    return (String) (clipData.getTransferData(stringFlavor));
                }
            } catch (UnsupportedFlavorException | IOException ufe) {
                System.err.println("getClipoardString fail");
            }
        }
        return null;
    }

    /**
     * Get byte array from resource bundle
     *
     * @param name what resource
     * @return the resource as byte array
     * @throws Exception if smth. went wrong
     */
    static public byte[] extractResource(String name) throws Exception {
        InputStream is = ClassLoader.getSystemResourceAsStream(name);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int r = is.read(buffer);
            if (r == -1) {
                break;
            }
            out.write(buffer, 0, r);
        }

        return out.toByteArray();
    }

    /**
     * Play WAV
     *
     * @param data the WAV file as byte array
     * @throws Exception If smth. went wrong
     */
    public static Clip playWave(byte[] data) throws Exception {
        final Clip clip = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));
        InputStream inp = new BufferedInputStream(new ByteArrayInputStream(data));
        clip.open(AudioSystem.getAudioInputStream(inp));
        clip.start();
        return clip;
    }

    public static void playWaveAndWait(byte[] data, int timeoutSeconds) throws Exception {
        timeoutSeconds *= 10;
        final AtomicBoolean ab = new AtomicBoolean(false);
        Clip c = playWave(data);
        c.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP)
                ab.set(true);
        });
        while (!ab.get()) {
            Thread.sleep(100);
            if (0 == timeoutSeconds--)
                break;
        }
        c.close();
    }
}
