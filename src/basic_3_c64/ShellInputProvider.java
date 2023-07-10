package basic_3_c64;

import applications.CBMGui;
import com.sixtyfour.plugins.InputProvider;

/**
 * Created by Administrator on 1/4/2017.
 */
class ShellInputProvider implements InputProvider {
    private final CBMGui shellFrame;

    public ShellInputProvider(CBMGui shellFrame) {
        this.shellFrame = shellFrame;
    }

    @Override
    public Character readKey() {
        return shellFrame.area.lastKey;
    }

//    private String ringBuffToString() {
//        StringBuilder sb = new StringBuilder();
//        Character c;
//        while ((c = shellFrame.ringBuff.remove()) != null) {
//            sb.append(c);
//        }
//        return sb.toString().trim();
//    }

    @Override
    public String readString() {
        String s = shellFrame.area.getBufferedLine();
        int a = s.indexOf('?')+2;
            s = s.substring(a);
        return s;
    }
}
