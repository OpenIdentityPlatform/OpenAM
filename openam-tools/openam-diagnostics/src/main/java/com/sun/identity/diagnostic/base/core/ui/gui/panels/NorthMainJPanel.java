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
 * $Id: NorthMainJPanel.java,v 1.2 2009/07/24 22:05:14 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.panels;

import com.sun.identity.diagnostic.base.core.ui.gui.list.ImageListEntry;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import javax.swing.event.ListSelectionListener;

public class NorthMainJPanel extends javax.swing.JPanel {
    
    private LogoJPanel logoPanel;
    private TextFieldJPanel textFieldPanel;
    private ListJPanel listPanel;
    private javax.swing.JPanel jPanel1;
    
    /** Creates new form NorthMainJPanel */
    public NorthMainJPanel(ResourceBundle rb) {
         initComponents();
         logoPanel = new LogoJPanel();
         textFieldPanel = new TextFieldJPanel(rb);
         listPanel = new ListJPanel(rb);
         jPanel1.add(logoPanel, java.awt.BorderLayout.NORTH);
         jPanel1.add(textFieldPanel, java.awt.BorderLayout.CENTER);
         add(listPanel, java.awt.BorderLayout.CENTER);
    }
    
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        setLayout(new java.awt.BorderLayout());
        jPanel1.setLayout(new java.awt.BorderLayout());
        add(jPanel1, java.awt.BorderLayout.NORTH);
    }
    
    public void addCategory(ImageListEntry entry) {
        listPanel.addCategory(entry);
    }
    
    public Object getSelectedCategory() {
        return listPanel.getSelectedCategory();
    }
    
    public void addWebContainer(ImageListEntry entry) {
        listPanel.addWebContainer(entry);
    }
    
    public Object getSelectedWebContainer() {
        return listPanel.getSelectedWebContainer();
    }
    
    public void addCategoryListSelectionListener(
        ListSelectionListener listener
    ) {
        listPanel.addCategoryListSelectionListener(listener);
    }
    
    public void addWebContainerItemListener(ItemListener listener) {
        listPanel.addWebContainerItemListener(listener);
    }
    
    public void addRunSelectedButtonActionListener(ActionListener listener) {
        listPanel.addRunSelectedButtonActionListener(listener);
    }
    
    public void setContainerBaseLabel(String name) {
        listPanel.setContainerBaseLabel(name);
    }
    
    public void setContainerDomainLabel(String name) {
        listPanel.setContainerDomainLabel(name);
    }
    
    public String getContainerDir() {
        return listPanel.getContainerDir();
    }
    
    public String getContainerDomainDir() {
        return listPanel.getContainerDomainDir();
    }
    
    public String getConfigDir() {
        return listPanel.getConfigDir();
    }
    
    public void enableComponentsAfterRunning() {
        listPanel.enableComponentsAfterRunning();
    }
    
    public void disableComponentsWhileRunning() {
        listPanel.disableComponentsWhileRunning();
    }
    
    public void enableRunSelected() {
        listPanel.enableRunSelected();
    }
    
    public void disableRunSelected() {
        listPanel.disableRunSelected();
    }
}
