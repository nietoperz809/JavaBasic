package interpreter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Renumberer {
    private final HashMap<String, String> jumpMap = new HashMap<>();
    private final ArrayList<String> lines = new ArrayList<>();
    private final Program prg;

    public Renumberer (Program prg) {
        this.prg = prg;
    }

    public void add (String s) {
        lines.add(s);
    }

    private boolean isLineNum (String in) {
        try {
            int i = Integer.parseInt(in);
            return i >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int findJump (String[] arr, String what)
    {
        for (int s=0; s<arr.length; s++)
        {
            if (arr[s].equalsIgnoreCase(what))
            {
                if (what.equalsIgnoreCase("then"))
                {
                    if (isLineNum(arr[s+1]))
                        return s;
                    return -1;
                }
                return s;
            }
        }
        return -1;
    }

    private void doForPass2 (String keyword, String[] line) {
        int g = findJump (line, keyword);
        if (g >= 0) {
            line[g+1] = jumpMap.get (line[g+1]);
        }
    }

    public void doIt() {
        int num = 100;
        // pass 1
        for (int s = 0; s<lines.size(); s++) {
            String[] ss = lines.get(s).split(" ");
            jumpMap.put(ss[0], ""+num);
            ss[0] = jumpMap.get(ss[0]);
            lines.set(s, String.join(" ", ss));
            num += 100;
        }
        // pass 2
        for (int s = 0; s<lines.size(); s++) {
            String[] ss = lines.get(s).split(" ");
            doForPass2 ("goto", ss);
            doForPass2 ("gosub", ss);
            doForPass2 ("then", ss);
            lines.set(s, String.join(" ", ss));
        }
    }

    public Program finish() {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            sb.append(s).append("\r\n");
        }
        InputStream targetStream = new ByteArrayInputStream (sb.toString().getBytes());
        try {
            return Program.load(targetStream, prg.area);
        } catch (Exception e) {
            System.out.println("ren fail "+e.toString());
        }
        return null;
    }
}