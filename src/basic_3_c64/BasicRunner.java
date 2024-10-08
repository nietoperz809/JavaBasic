package basic_3_c64;

import applications.CBMGui;
import com.sixtyfour.Basic;
import com.sixtyfour.DelayTracer;
import com.sixtyfour.config.CompilerConfig;
import com.sixtyfour.plugins.MemoryListener;

/**
 * Proxy class to instantiate an run the BASIC system
 */
public class BasicRunner implements Runnable {
    private static volatile boolean running = false;
    private final Basic olsenBasic;
    private final CBMGui screen;
    private static final CompilerConfig config = new CompilerConfig();
    private MemoryListener memListener;

    public void pause(boolean b) {
        olsenBasic.setPause(b);
    }

    public BasicRunner(String[] program, int speed, CBMGui shellFrame) {
        screen = shellFrame;
        olsenBasic = new Basic(program);
        if (speed > 0) {
            DelayTracer t = new DelayTracer(speed);
            olsenBasic.setTracer(t);
        }
        olsenBasic.setOutputChannel(new ShellOutputChannel(shellFrame));
        olsenBasic.setInputProvider(new ShellInputProvider(shellFrame));
        //olsenBasic.getMachine().setMemoryListener(new PeekPokeHandler(shellFrame));
    }

    /**
     * Compile an run a single line
     *
     * @param in the BASIC line
     * @param sf reference to shell main window
     * @return textual representation of success/error
     */
    public static String runSingleLine(String in, CBMGui sf) {
        try {
            Basic b = new Basic("0 let pi=3.14159265:" + in.toUpperCase());
            //b.getMachine().setMemoryListener(new PeekPokeHandler(sf));
            b.compile(config, false);
            b.setOutputChannel(new ShellOutputChannel(sf));
            b.setInputProvider(new ShellInputProvider(sf));
            b.start(config);
            return "";
        } catch (Exception ex) {
            return ex.getMessage().toUpperCase() + "\n";
        }
    }

    /**
     * Start BASIC task
     *
     * @param synchronous if true the caller is blocked
     */
    public void start(boolean synchronous) {
        if (running) {
            System.out.println("already running ...");
            return;
        }
        Thread thread = new Thread(this);
        thread.start();
        //screen.matrix.setCursorOnOff(false);
        if (!synchronous) {
            return;
        }
        try {
            thread.join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //screen.matrix.setCursorOnOff(true);
    }


    public Basic getOlsenBasic() {
        return olsenBasic;
    }

    @Override
    public void run() {
        running = true;
        try {
            olsenBasic.run(config);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            running = false;
            System.out.println("basic thread finished");
        }
    }
}
