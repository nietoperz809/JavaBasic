/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misc;


import applications.BasicGUI;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;

/**
 * $id:$
 *
 * @author Administrator
 */
public class MainWindow extends javax.swing.JFrame
{
    private static MainWindow instance;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDesktopPane desktopPane;

    public static Voice voice;

    /**
     * Creates new form MDIApplication
     */
    private MainWindow ()
    {
        instance = this;
        initComponents();
        setSize (new Dimension (800, 600));
        this.setTitle (Misc.buildInfo);
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        KevinVoiceDirectory dir = new KevinVoiceDirectory();
        voice = dir.getVoices()[0];
        voice.allocate();

//        // Set Focus to BasicWindow
//        addFocusListener (new FocusAdapter()
//        {
//            @Override
//            public void focusGained (FocusEvent e)
//            {
//                JInternalFrame[] list = desktopPane.getAllFrames();
//                if (list.length > 0)
//                {
//                    JInternalFrame fr = list[list.length-1];
//                    if (fr instanceof BasicGUI)
//                    {
//                        ((BasicGUI)fr).area.requestFocus();
//                    }
//                }
//            }
//        });
    }

    /**
     * @return the instance
     */
    public static MainWindow getInstance ()
    {
        return instance;
    }

    public BasicGUI getBasicGUIFromThreadID (long tid)
    {
        JInternalFrame[] frames = desktopPane.getAllFrames();
        for (JInternalFrame f : frames)
        {
            if (f instanceof BasicGUI)
            {
                if (((BasicGUI)f).threadID == tid)
                    return (BasicGUI)f;
            }
        }
        return null;
    }

    /**
     * m0xyzptlkxy0
     *
     * @param args the command line arguments
     */
    public static void main (String[] args) throws Exception
    {

        /* Set the look and feel */
        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() ->
                new MainWindow().setVisible(true));
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // menuBar.add(Box.createGlue()) added manually !!!!!!!!!!!!!!!
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents

    private void initComponents ()
    {
        desktopPane = new JDesktopPane();
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu();
        JMenu jMenu1 = new JMenu();
        JMenuItem jMenuItem7 = new JMenuItem();
        JMenuItem jMenuItem11 = new JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        desktopPane.setDoubleBuffered(true);
        desktopPane.setPreferredSize(new java.awt.Dimension(400, 400));

        menuBar.setBackground(new java.awt.Color(255, 255, 255));
        menuBar.setPreferredSize(new java.awt.Dimension(100, 21));

        fileMenu.setMnemonic('f');
        fileMenu.setText("Start ...");

        JMenuItem jMenuItemBasic = new JMenuItem();
        jMenuItemBasic.setText("New 1st.BASIC");
        jMenuItemBasic.addActionListener(evt -> createMDIChild(applications.BasicGUI.class, null));
        fileMenu.add(jMenuItemBasic);

        JMenuItem jMenuItemTC = new JMenuItem();
        jMenuItemTC.setText("New TinyCat BASIC");
        jMenuItemTC.addActionListener(evt -> createMDIChild(applications.TinyCatGUI.class, null));
        fileMenu.add(jMenuItemTC);

        JMenuItem jMenuItemCBM = new JMenuItem();
        jMenuItemCBM.setText("New CBM BASIC");
        jMenuItemCBM.addActionListener(evt -> createMDIChild(applications.CBMGui.class, null));
        fileMenu.add(jMenuItemCBM);

        JMenuItem jMenuItemMon = new JMenuItem();
        jMenuItemMon.setText("Mem Monitor");
        jMenuItemMon.addActionListener(evt -> createMDIChild(applications.MemoryMonitorGUI.class, null));
        fileMenu.add(jMenuItemMon);

        menuBar.add(fileMenu);

        jMenu1.setText("Window ...");
        jMenu1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jMenuItem7.setText("Arrange");
        jMenuItem7.addActionListener(evt -> MDIActions.arrange(desktopPane));
        jMenu1.add(jMenuItem7);

        jMenuItem11.setText("Cascade");
        jMenuItem11.addActionListener(evt -> MDIActions.cascade(desktopPane));
        jMenu1.add(jMenuItem11);

        //menuBar.add(Box.createGlue());

        menuBar.add(jMenu1);

        setJMenuBar(menuBar);

        getContentPane().setLayout(new GridLayout ());
        getContentPane ().add (desktopPane);
    }// </editor-fold>//GEN-END:initComponents

    private void addChild (MDIChild c)
    {
        desktopPane.add(c);
        c.moveToFront();
        MDIActions.arrange(desktopPane);
    }

    /**
     * Creates new MDI child from class name
     *
     * @param c Runtime class Must be of type JInternalFrame
     * @return New JInternalFrame or null on Error
     */
    public MDIChild createMDIChild (Class<?> c, CompletableFuture<Long> thread_id)
    {
        try
        {
            Constructor<?> cons = c.getDeclaredConstructor(CompletableFuture.class);
            MDIChild q = (MDIChild) cons.newInstance(new Object[] {thread_id});
            addChild(q);
            return q;
        }
        catch (Exception ex)
        {
            System.out.println(ex);
        }
        return null;
    }
}
