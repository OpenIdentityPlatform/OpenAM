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
 * $Id: CheckBoxJPanel.java,v 1.2 2009/07/24 22:03:32 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.panels;

import java.util.ResourceBundle;
import javax.swing.event.ListSelectionListener;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;

import com.sun.identity.diagnostic.base.core.ui.gui.list.CheckBoxListCellRenderer;
import com.sun.identity.diagnostic.base.core.ui.gui.list.CheckBoxListEntry;


public class CheckBoxJPanel extends javax.swing.JPanel {
    
    /** Creates new form CheckBoxJPanel */
    public CheckBoxJPanel(ResourceBundle rb) {
        initComponents(rb);
        checkBoxjList.setCellRenderer(new CheckBoxListCellRenderer());
    }
    
    /** 
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents(ResourceBundle rb) {
        jScrollPane1 = new javax.swing.JScrollPane();
        checkBoxjList = new javax.swing.JList();

        setLayout(new java.awt.BorderLayout());

        setBorder(javax.swing.BorderFactory.createTitledBorder(
            rb.getString("lbl_select_test")));
        checkBoxjList.setFixedCellWidth(180);
        jScrollPane1.setViewportView(checkBoxjList);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);
    }
    
    
    private javax.swing.JList checkBoxjList;
    private javax.swing.JScrollPane jScrollPane1;
    
    public void addTest(CheckBoxListEntry entry) {
        ListModel model = checkBoxjList.getModel();
        if (model instanceof DefaultListModel) {
            ((DefaultListModel)model).addElement(entry);
        } else {
            DefaultListModel newModel = new DefaultListModel();
            newModel.addElement(entry);
            checkBoxjList.setModel(newModel);
        }
    }
    
    public void addCheckBoxListSelectionListener(
        ListSelectionListener listener
    ) {
        checkBoxjList.addListSelectionListener(listener);
    }
    
    public void removeAllTests() {
        ListModel model = checkBoxjList.getModel();
        if (model instanceof DefaultListModel) {
            ((DefaultListModel)model).removeAllElements();
        }
    }
    
    public Object[] getSelectedTests() {
        return checkBoxjList.getSelectedValues();
    }
    
    public void enableComponentsAfterRunning() {
        checkBoxjList.setEnabled(true);
    }
    
    public void disableComponentsWhileRunning() {
        checkBoxjList.setEnabled(false);
    }
}
