/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: GUIManager.java,v 1.2 2009/07/24 22:01:59 ak138937 Exp $
 *
 */


package com.sun.identity.diagnostic.base.core.ui.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;
import com.sun.identity.diagnostic.base.core.ui.UIManager;
import com.sun.identity.diagnostic.base.core.ui.gui.panels.MainJPanel;

/**
 * This class is responsible for taking care of UI workflow in GUI mode. 
 */

public class GUIManager extends UIManager {
    
    MainJPanel mainPanel;
    private static ResourceBundle rb;

    static {
        rb = ResourceBundle.getBundle(ToolConstants.RESOURCE_BUNDLE_NAME);
    }

    
    /** Creates a new instance of GUIManager */
    public GUIManager() {
    }
    
    /** 
     * This method is called once during application start up 
     * for GUI mode.
     */
    public void init() {
        System.out.println(rb.getString("dt_init_msg"));
        try {
            mainPanel = new MainJPanel();
        } catch (Exception e) {
            System.out.println(rb.getString("dt_init_exception_msg") + 
                e.getMessage());  
        }
    }
    
    /**
     * This method is responsible to bring up the tool
     * application in GUI mode.
     */
    public void startApplication() {
        try {
            System.out.println(rb.getString("dt_init_gui_msg_begin"));
            JFrame frame = new JFrame(rb.getString("dt_tool_title_msg"));
            frame.setSize(800, 700);
            frame.getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);
            frame.validate();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = frame.getSize();
            if (frameSize.height > screenSize.height) {
                frameSize.height = screenSize.height;
            }
            if (frameSize.width > screenSize.width) {
                frameSize.width = screenSize.width;
            }
            frame.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            System.out.println(rb.getString("dt_init_gui_msg_end"));
        } catch (Exception e) {
            System.out.println(rb.getString("dt_app_exception_msg") + 
                e.getMessage());  
        }
    }
    
    /**
     * This method is called during application shutdown in GUI mode.
     */
    public void stopApplication() {
    }
}
