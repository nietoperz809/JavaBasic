package interpreter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Renumberer {
    private final HashMap<String, String> jumpMap = new HashMap<>();
    private final ArrayList<String> lines = new ArrayList<>();
    private final Program prg;
    private final int start;
    private final int step;

    public Renumberer(Program prg, int start, int step) {
        this.prg = prg;
        this.start = start;
        this.step = step;
    }

    public void add(String s) {
        lines.add(s);
    }

    private boolean isLineNum(String in) {
        try {
            int i = Integer.parseInt(in);
            return i >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int findJump(String[] arr, String what) {
        for (int s = 0; s < arr.length; s++) {
            if (arr[s].equalsIgnoreCase(what)) {
                if (what.equalsIgnoreCase("then")) {
                    if (isLineNum(arr[s + 1]))
                        return s;
                    return -1;
                }
                return s;
            }
        }
        return -1;
    }

    private void doForPass2(String keyword, String[] line) {
        int g = findJump(line, keyword);
        if (g >= 0) {
            String targ = line[g+1];
            if (targ.contains(",")) { // list
                String[] ss = targ.split(",");
                for (int n = 0; n<ss.length; n++) {
                    ss[n] = jumpMap.get(ss[n]);
                }
                line[g+1] = String.join(",", ss);
            } else { // single number
                line[g + 1] = jumpMap.get(targ);
            }
        }
    }

    public void doIt() {
        int num = this.start;
        // pass 1
        for (int s = 0; s < lines.size(); s++) {
            String[] ss = lines.get(s).split(" ");
            jumpMap.put(ss[0], "" + num);
            ss[0] = jumpMap.get(ss[0]);
            lines.set(s, String.join(" ", ss));
            num += this.step;
        }
        // pass 2
        for (int s = 0; s < lines.size(); s++) {
            String[] ss = lines.get(s).split(" ");
            doForPass2("goto", ss);
            doForPass2("gosub", ss);
            doForPass2("then", ss);
            lines.set(s, String.join(" ", ss));
        }
    }

    public Program finish() {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            sb.append(s).append("\r\n");
        }
        InputStream targetStream = new ByteArrayInputStream(sb.toString().getBytes());
        try {
            return Program.load(targetStream, prg.area);
        } catch (Exception e) {
            System.out.println("ren fail " + e.toString());
        }
        return null;
    }
}
