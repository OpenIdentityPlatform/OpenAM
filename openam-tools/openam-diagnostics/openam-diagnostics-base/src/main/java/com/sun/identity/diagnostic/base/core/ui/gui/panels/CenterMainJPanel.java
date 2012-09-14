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
 * $Id: CenterMainJPanel.java,v 1.2 2009/07/24 22:03:11 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.panels;

import java.awt.Point;
import java.util.ResourceBundle;
import javax.swing.event.ListSelectionListener;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import javax.swing.text.Document;

import com.sun.identity.diagnostic.base.core.ui.gui.list.CheckBoxListEntry;

public class CenterMainJPanel extends javax.swing.JPanel {
    
    private CheckBoxJPanel checkBoxPanel;
    private TableJPanel tablePanel;
    
    /** Creates new form CenterMainJPanel */
    public CenterMainJPanel(ResourceBundle rb) {
        initComponents(rb);
        checkBoxPanel = new CheckBoxJPanel(rb);
        tablePanel = new TableJPanel(rb);
        add(checkBoxPanel, java.awt.BorderLayout.WEST);
        add(tablePanel, java.awt.BorderLayout.CENTER);
    }
    
    /** 
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents(ResourceBundle rb) {
        setLayout(new java.awt.BorderLayout());
        setBorder(javax.swing.BorderFactory.createTitledBorder(
            rb.getString("lbl_test")));
    }
    
    public void addTest(CheckBoxListEntry entry) {
        checkBoxPanel.addTest(entry);
    }
    
    public void removeAllTests() {
        checkBoxPanel.removeAllTests();
    }
    
    public Object[] getSelectedTests() {
        return checkBoxPanel.getSelectedTests();
    }
    
    public void appendResultMessage(String message) {
        tablePanel.appendResultMessage(message);
    }
    
    public void appendInfoMessage(String message) {
        tablePanel.appendInfoMessage(message);
    }
    
    public void appendWarningMessage(String message) {
        tablePanel.appendWarningMessage(message);
    }
    
    public void appendErrorMessage(String message) {
        tablePanel.appendErrorMessage(message);
    }
    
    public void enableComponentsAfterRunning() {
        tablePanel.enableComponentsAfterRunning();
        checkBoxPanel.enableComponentsAfterRunning();
    }
    
    public void disableComponentsWhileRunning() {
        tablePanel.disableComponentsWhileRunning();
        checkBoxPanel.disableComponentsWhileRunning();
    }
    
    public void addCheckBoxListSelectionListener(
        ListSelectionListener listener) {
        checkBoxPanel.addCheckBoxListSelectionListener(listener);
    }
    
    public TableModel getTableModel() {
        return tablePanel.getTableModel();
    }
    
    public void clearAll() {
        tablePanel.clearAll();
    }
    
    public Document getDocument() {
        return tablePanel.getDocument();
    }
    
    public ListSelectionModel getTableSelectionModel() {
        return tablePanel.getTableSelectionModel();
    }
    
    public Point getCurrentViewPosition() {
        return tablePanel.getCurrentViewPosition();
    }
}
