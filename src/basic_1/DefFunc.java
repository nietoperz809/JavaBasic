package basic_1;

import java.util.ArrayList;

public class DefFunc {
    public String name;
    public String args;
    public String expr;

    public DefFunc(String n, String a, String e) {
        name = n;
        args = a;
        expr = e;
    }

    public static DefFunc find(String n, ArrayList<DefFunc> al) {
        for (DefFunc d : al) {
            if (d.name.equals(n))
                return d;
        }
        return null;
    }

    public String toString() {
        return name + " -- " + args + " -- " + expr;
    }
}
