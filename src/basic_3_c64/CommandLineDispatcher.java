package basic_3_c64;

import applications.CommodoreGui;
import misc.Misc;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by Administrator on 1/18/2017.
 */
public class CommandLineDispatcher {
    public final ProgramStore progStore = new ProgramStore();
    private final CommodoreGui cbmGui;
    public BasicRunner basicRunner;

    private int speed = 990;

    public CommandLineDispatcher(CommodoreGui screen) {
        cbmGui = screen;
        new Thread(() ->
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            while (!Thread.interrupted()) {
                try {
                    char[] in = cbmGui.area.getBufferedLine().toCharArray();
                    handleInput(in);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        // Dummy runner
        basicRunner = new BasicRunner(new String[0], speed, cbmGui);
    }

    private void dir(String filter) {
        if (filter != null) {
            if (filter.endsWith("\"") && filter.startsWith("\""))
                filter = filter.substring(1, filter.length()-1);
        }
        ByteArrayOutputStream res = Misc.dir(filter);
        if (res != null) {
            cbmGui.area.getPrintStream().print(res);
        }
    }

    private void run() {
        basicRunner = new BasicRunner(progStore.toArray(), speed, cbmGui);
        basicRunner.start(true);
    }

//    private void renumber(String[] split) {
//        try {
//            Prettifier pf = new Prettifier(progStore);
//            switch (split.length) {
//                case 1:
//                    pf.doRenumber();
//                    break;
//                case 2:
//                    int v1 = Integer.parseInt(split[1]);
//                    pf.doRenumber(v1, v1);
//                    break;
//                case 3:
//                    int va = Integer.parseInt(split[1]);
//                    int vb = Integer.parseInt(split[2]);
//                    pf.doRenumber(va, vb);
//                    break;
//            }
//            m_screen.matrix.putString(ProgramStore.OK);
//        } catch (Exception ex) {
//            m_screen.matrix.putString(ProgramStore.ERROR);
//        }
//    }

    private void list(String[] split) {
        if (split.length == 2) {
            try {
                int i1 = Integer.parseInt(split[1]);  // single number
                if (i1 >= 0) // positive
                {
                    cbmGui.area.getPrintStream().println(progStore.list(i1, i1));
                } else // negative
                {
                    cbmGui.area.getPrintStream().println(progStore.list(0, -i1));
                }
            } catch (NumberFormatException ex) {
                String[] args = split[1].split("-");
                int i1 = Integer.parseInt(args[0]);
                int i2;
                try {
                    i2 = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex2) {
                    i2 = Integer.MAX_VALUE;
                }
                cbmGui.area.getPrintStream().println(progStore.list(i1, i2));
            }
        } else  // no args
        {
            cbmGui.area.getPrintStream().println(progStore);
        }
        cbmGui.area.getPrintStream().println(ProgramStore.OK);
    }

    /**
     * Main function. Runs in a separate thread
     */
    private void handleInput(char[] in) {
        System.gc();
        //System.runFinalization();

        String s = new String(in).trim();
        String[] split = s.split(" ");
        s = s.toLowerCase();
        if (split[0].equalsIgnoreCase("list")) {
            list(split);
        } else if (s.equals("new")) {
            progStore.clear();
            cbmGui.area.getPrintStream().println(ProgramStore.OK);
        } else if (s.equals("cls")) {
            cbmGui.area.setText("");
        } else if (s.equals("run")) {
            run();
        } else if (split[0].equalsIgnoreCase("dir")) {
            dir(split.length == 2 ? split[1] : null);
            cbmGui.area.getPrintStream().println(ProgramStore.OK);
        } else if (split[0].equalsIgnoreCase("speed")) {
            try {
                speed = Integer.parseInt(split[1]);
                cbmGui.area.getPrintStream().println(ProgramStore.OK);
            } catch (NumberFormatException ex) {
                cbmGui.area.getPrintStream().println(ProgramStore.ERROR);
            }
        } else if (split[0].equalsIgnoreCase("save")) {
            String msg = progStore.save(split[1]);
            cbmGui.area.getPrintStream().println(msg);
        } else if (split[0].equalsIgnoreCase("load")) {
            String msg = progStore.load(split[1]);
            cbmGui.area.getPrintStream().println(msg);
        } else {
            try {
                progStore.insert(s);
            } catch (Exception unused) {
                cbmGui.area.getPrintStream().println(BasicRunner.runSingleLine(s, cbmGui));
            }
        }
    }
}
