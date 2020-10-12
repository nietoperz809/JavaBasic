package applications;

import misc.MDIChild;
import monitor.MemoryMonitor;

import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.util.concurrent.CompletableFuture;

/**
 * New Class.
 * User: Administrator
 * Date: 05.01.2009
 * Time: 22:46:57
 */
public class MemoryMonitorGUI extends MDIChild
{
    private class LocaWindowListener implements InternalFrameListener
    {
        @Override
        public void internalFrameOpened (InternalFrameEvent e)
        {

        }

        @Override
        public void internalFrameClosing (InternalFrameEvent e)
        {
            mon.surf.stop();
        }

        @Override
        public void internalFrameClosed (InternalFrameEvent e)
        {
        }

        @Override
        public void internalFrameIconified (InternalFrameEvent e)
        {

        }

        @Override
        public void internalFrameDeiconified (InternalFrameEvent e)
        {

        }

        @Override
        public void internalFrameActivated (InternalFrameEvent e)
        {

        }

        @Override
        public void internalFrameDeactivated (InternalFrameEvent e)
        {

        }
    }

    private  MemoryMonitor mon;
    private static MemoryMonitorGUI gui;

    @Override
    public void dispose ()
    {
        super.dispose ();
        gui = null;
    }

    public MemoryMonitorGUI (CompletableFuture<Long> dummy) throws Exception
    {
        super();

        if (gui == null)
        {
            gui = this;
        }
        else
        {
            throw new Exception ("mem mon already there");
        }

        setClosable(true);
        setResizable(true);
        setMaximizable(true);
        setIconifiable(true);
        
        this.addInternalFrameListener(new LocaWindowListener());
        mon = new MemoryMonitor();
        add (mon);
//        setSize (600,200);
//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        Dimension frameSize = getSize();
//        int x = (screenSize.width - frameSize.width) / 2;
//        int y = (screenSize.height - frameSize.height) / 2;
//        setTitle ("Memory Monitor");
//        setLocation(x, y);
        setVisible (true);
        mon.surf.start();
    }
}
