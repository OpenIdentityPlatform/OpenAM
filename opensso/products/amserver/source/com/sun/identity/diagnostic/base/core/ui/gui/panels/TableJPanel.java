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
 * $Id: TableJPanel.java,v 1.2 2009/07/24 22:05:57 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.panels;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ResourceBundle;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.text.Document;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.sun.identity.diagnostic.base.core.ui.gui.table.LabelTableCell;
import com.sun.identity.diagnostic.base.core.ui.gui.table.LabelTableCellRenderer;
import com.sun.identity.diagnostic.base.core.ui.gui.table.ResultTableCell;
import com.sun.identity.diagnostic.base.core.ui.gui.table.TestsTableModel;


public class TableJPanel extends javax.swing.JPanel {
    
    private TestsTableModel tableModel;
    
    public static final String RED = "#FF0000";
    public static final String ORANGE = "#FF9900";
    public static final String BLUE = "#0000FF";
    public static final String BLACK = "#000000";
    
    /** Creates new form TableJPanel */
    public TableJPanel(ResourceBundle rb) {
        initComponents(rb);
        tableModel = new TestsTableModel();
        testsjTable.setModel(tableModel);
        testsjTable.setDefaultRenderer(LabelTableCell.class, 
            new LabelTableCellRenderer());
        testsjTable.getSelectionModel().setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        testsjTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (testsjTable.isEnabled()) {
                    messagejScrollPane.getViewport().setViewPosition(
                        ((ResultTableCell) testsjTable.getValueAt(
                        testsjTable.getSelectedRow(), 
                        TestsTableModel.RESULT_COLUMN)).getViewPosition());
                }
            }
        });
    }
    
    private void initComponents(ResourceBundle rb) {
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        testsjTable = new javax.swing.JTable();
        messagejScrollPane = new javax.swing.JScrollPane();
        messagejEditorPane = new javax.swing.JEditorPane();

        setLayout(new java.awt.BorderLayout());

        setBorder(javax.swing.BorderFactory.createTitledBorder(
            rb.getString("lbl_results")));
        jSplitPane1.setDividerLocation(150);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setAutoscrolls(true);
        jSplitPane1.setOneTouchExpandable(true);
        testsjTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        testsjTable.setName("Tests Table");
        jScrollPane1.setViewportView(testsjTable);
        jSplitPane1.setTopComponent(jScrollPane1);
        messagejEditorPane.setEditable(false);
        messagejEditorPane.setContentType("text/html");
        messagejScrollPane.setViewportView(messagejEditorPane);
        jSplitPane1.setRightComponent(messagejScrollPane);
        add(jSplitPane1, java.awt.BorderLayout.CENTER);
    }
    
    
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JEditorPane messagejEditorPane;
    private javax.swing.JScrollPane messagejScrollPane;
    private javax.swing.JTable testsjTable;
    
    public void appendResultMessage(String message) {
        appendMessage(BLACK, message);
    }
    
    public void appendInfoMessage(String message) {
        appendMessage(BLUE, message);
    }
    
    public void appendWarningMessage(String message) {
        appendMessage(ORANGE, message);
    }
    
    public void appendErrorMessage(String message) {
        appendMessage(RED, message);
    }
    
    private void appendMessage(String colorString, String s) {
        HTMLEditorKit kit = (HTMLEditorKit) messagejEditorPane.getEditorKit();
        HTMLDocument doc = (HTMLDocument) messagejEditorPane.getDocument();
        try {
            String newS = s.replaceAll("<", "&lt;");
            newS = newS.replaceAll(">", "&gt;");
            newS = newS.replaceAll("\n", "<br>");
            kit.insertHTML(doc, messagejEditorPane.getCaretPosition(), 
                "<font color=" + colorString + ">" + newS, 0, 0, HTML.Tag.FONT);
            messagejEditorPane.setCaretPosition(doc.getLength() - 1);
        } catch(Exception ex) {
            //ex.printStackTrace();
            System.out.println("Exception in TableJPanel :" + ex.getMessage());
        }
    }
    
    public void enableComponentsAfterRunning() {
        testsjTable.setEnabled(true);
    }
    
    public void disableComponentsWhileRunning() {
        testsjTable.setEnabled(false);
    }
    
    public TableModel getTableModel() {
        return testsjTable.getModel();
    }
    
    public void clearAll() {
        testsjTable.setEnabled(false);
        TestsTableModel model = (TestsTableModel) testsjTable.getModel();
        model.removeAll();
        Document doc = messagejEditorPane.getDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch(Exception ex) {
            //ex.printStackTrace();
            System.out.println("Exception in TableJPanel :" + ex.getMessage());

        }
        testsjTable.setEnabled(true);
    }
    
    public Document getDocument() {
        return messagejEditorPane.getDocument();
    }
    
    public ListSelectionModel getTableSelectionModel() {
        return testsjTable.getSelectionModel();
    }
    
    public Point getCurrentViewPosition() {
        try {
            Rectangle viewRect = messagejEditorPane.modelToView(
                messagejEditorPane.getCaretPosition());
            return new Point(viewRect.x, viewRect.y);
        } catch (Exception ignored) {}
        return new Point(0, 0);
    }
}
