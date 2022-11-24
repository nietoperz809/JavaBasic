/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package misc;

import applications.MemoryMonitorGUI;

import javax.swing.*;
import java.beans.PropertyVetoException;
import java.io.Serializable;

/**
 * @author Administrator
 */
public abstract class MDIChild extends JInternalFrame implements Serializable {
    public static final long serialVersionUID = 1L;

    public MDIChild() {
        super();
        System.err.println("Starting " + this.getClass().getName());
    }

    @Override
    public void dispose() {
        super.dispose();
        System.err.println("Disposing " + this.getClass().getName());
        System.gc();
        System.runFinalization();
    }
}
