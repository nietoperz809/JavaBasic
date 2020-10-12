package misc;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

public class ClipboardImage implements ClipboardOwner
{
    public ClipboardImage (Image i)
    {
        try
        {
            //Robot robot = new Robot();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screen = new Rectangle(screenSize);
            //BufferedImage i = robot.createScreenCapture(screen);
            TransferableImage trans = new TransferableImage(i);
            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
            c.setContents(trans, this);
        }
        catch (Exception x)
        {
            x.printStackTrace();
            //System.exit(1);
        }
    }

//    public static void main(String[] arg)
//    {
//        ClipboardImage ci = new ClipboardImage();
//    }

    @Override
    public void lostOwnership(Clipboard clip, Transferable trans)
    {
        System.out.println("Lost Clipboard Ownership");
    }

    private class TransferableImage implements Transferable
    {

        private final Image i;

        public TransferableImage(Image i)
        {
            this.i = i;
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException
        {
            if (flavor.equals(DataFlavor.imageFlavor) && i != null)
            {
                return i;
            }
            else
            {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            DataFlavor[] flavors = new DataFlavor[1];
            flavors[0] = DataFlavor.imageFlavor;
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            DataFlavor[] flavors = getTransferDataFlavors();
            for (DataFlavor flavor1 : flavors)
            {
                if (flavor.equals(flavor1))
                {
                    return true;
                }
            }

            return false;
        }
    }
}
