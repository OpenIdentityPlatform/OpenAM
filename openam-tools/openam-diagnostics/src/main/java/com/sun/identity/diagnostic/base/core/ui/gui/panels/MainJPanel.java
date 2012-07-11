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
 * $Id: MainJPanel.java,v 1.3 2009/11/13 21:53:58 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.panels;


import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLWriter;

import com.sun.identity.diagnostic.base.core.ToolContext;
import com.sun.identity.diagnostic.base.core.ToolManager;
import com.sun.identity.diagnostic.base.core.ToolLogWriter;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.base.core.service.ServiceRequest;
import com.sun.identity.diagnostic.base.core.service.ServiceResponse;
import com.sun.identity.diagnostic.base.core.service.ToolService;
import com.sun.identity.diagnostic.base.core.ui.gui.event.MessageEvent;
import com.sun.identity.diagnostic.base.core.ui.gui.event.MessageListener;
import com.sun.identity.diagnostic.base.core.ui.gui.list.CheckBoxListEntry;
import com.sun.identity.diagnostic.base.core.ui.gui.list.ImageListEntry;
import com.sun.identity.diagnostic.base.core.ui.gui.table.CounterTableCell;
import com.sun.identity.diagnostic.base.core.ui.gui.table.ResultTableCell;
import com.sun.identity.diagnostic.base.core.ui.gui.table.StartTimeTableCell;
import com.sun.identity.diagnostic.base.core.ui.gui.table.TestsTableModel;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;

public class MainJPanel extends javax.swing.JPanel implements ToolConstants {
    
    private NorthMainJPanel northPanel;
    private CenterMainJPanel centerPanel;
    private ButtonJPanel southPanel;
    private ToolContext tContext;
    private HashMap<String, HashMap> serviceCategories;
    private static int previousCategorySelectedIndex;
    private static CounterThread counterThread;
    
    public static final String SELECTED_ICON =
        "/com/sun/identity/diagnostic/base/core/ui/gui/images/BlubOnIcon.gif";
    public static final String NOT_SELECTED_ICON =
        "/com/sun/identity/diagnostic/base/core/ui/gui/images/BlubOffIcon.gif";
   
    private final ResourceBundle rb =
         ResourceBundle.getBundle(ToolConstants.RESOURCE_BUNDLE_NAME);
    
    public MainJPanel() {
        initComponents();
        counterThread = new CounterThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                counterThread.shutdown();
            }
        });
        counterThread.start();
        previousCategorySelectedIndex = -1;
        northPanel = new NorthMainJPanel(rb);
        centerPanel = new CenterMainJPanel(rb);
        southPanel = new ButtonJPanel(this, rb);
        add(northPanel, java.awt.BorderLayout.NORTH);
        add(centerPanel, java.awt.BorderLayout.CENTER);
        add(southPanel, java.awt.BorderLayout.SOUTH);
        tContext = ToolManager.getInstance().getToolContext();
        final IToolOutput outputWriter = tContext.getOutputWriter();
        outputWriter.addMessageListener(new MessageListener() {
            public void messagePublished(MessageEvent e) {
                switch (e.getMessageType()) {
                    case MessageEvent.RESULT_MESSAGE:
                        centerPanel.appendResultMessage(e.getMessage());
                        break;
                    case MessageEvent.INFO_MESSAGE:
                        centerPanel.appendInfoMessage(e.getMessage());
                        break;
                    case MessageEvent.WARNING_MESSAGE:
                        centerPanel.appendWarningMessage(e.getMessage());
                        break;
                    case MessageEvent.ERROR_MESSAGE:
                        centerPanel.appendErrorMessage(e.getMessage());
                }
            }
        });
        final TestsTableModel finalTableModel = 
            (TestsTableModel) centerPanel.getTableModel();
        finalTableModel.addTableModelListener(new TableModelListener() {
            
            public void tableChanged(TableModelEvent e) {
                int type = e.getType();
                switch (type) {
                    case TableModelEvent.INSERT:
                        southPanel.enableClearAllButton();
                        southPanel.enableSaveAllButton();
                        break;
                    case TableModelEvent.UPDATE:
                    case TableModelEvent.DELETE:
                        if (finalTableModel.getRowCount() == 0) {
                            southPanel.disableClearAllButton();
                            southPanel.disableSaveAllButton();
                        }
                }
            }
            
        });
        southPanel.addClearAllButtonActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(centerPanel,
                    rb.getString("dlg_clear_txt_msg"), 
                    rb.getString("dlg_clear_title_msg"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) ==
                    JOptionPane.YES_OPTION) {
                    centerPanel.clearAll();
                }
            }
            
        });
        southPanel.addSaveAllButtonActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                File file = null;
                JFileChooser chooser = new JFileChooser();
                while (chooser.showSaveDialog(centerPanel) == 
                    JFileChooser.APPROVE_OPTION) {
                    file = chooser.getSelectedFile();
                    if (!file.getName().endsWith(".html")) {
                        file = new File(file.getAbsolutePath() + ".html");
                    }
                    if (file != null) {
                        if (file.exists()) {
                            if (JOptionPane.showConfirmDialog(centerPanel, 
                                "Replace file " + file.getName() + " ?", 
                                "Replace File", JOptionPane.YES_NO_OPTION) != 
                                JOptionPane.YES_OPTION) {
                                continue;
                            }
                        }
                        FileWriter writer = null;
                        try {
                            writer = new FileWriter(file);
                            HTMLWriter htmlWriter = new HTMLWriter(writer, 
                                (HTMLDocument) centerPanel.getDocument());
                            htmlWriter.write();
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(centerPanel, 
                                ex.getMessage(), "IO Error", 
                                JOptionPane.ERROR_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(centerPanel, 
                                ex.getMessage(), "Error", 
                                JOptionPane.ERROR_MESSAGE);
                        } finally {
                            if (writer != null) {
                                try {
                                    writer.close();
                                } catch (IOException ignored) {}
                            }
                        }
                    }
                    break;
                }
            }
        });
        serviceCategories = tContext.getServiceCategories();
        Set<String> categories = serviceCategories.keySet();
        for (String name : categories) {
            northPanel.addCategory(new ImageListEntry(name, null));
        }
        for (int i = 0; i < WEB_CONTAINERS.length; i++) {
            northPanel.addWebContainer(new ImageListEntry(WEB_CONTAINERS[i], 
                WEB_CONTAINERS_ICON[i]));
        }
        northPanel.addWebContainerItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                ImageListEntry entry = (ImageListEntry) e.getItem();
                String name = entry.getName();
                if (name.equals(rb.getString("dd_sun_app_server"))) {
                    northPanel.setContainerBaseLabel(
                        rb.getString("txt_lbl_container_bdir"));
                    northPanel.setContainerDomainLabel(
                        rb.getString("txt_lbl_container_ddir"));
                } else {
                    if (name.equals(rb.getString("dd_sun_web_server"))) {
                        northPanel.setContainerBaseLabel(
                            rb.getString("txt_lbl_container_bdir"));
                        northPanel.setContainerDomainLabel(
                            rb.getString("txt_lbl_container_ddir"));
                    } else {
                        if (name.equals(rb.getString("dd_bea_weblogic"))) {
                            northPanel.setContainerBaseLabel(
                                rb.getString("txt_lbl_container_idir"));
                            northPanel.setContainerDomainLabel(
                                rb.getString("txt_lbl_container_ddir"));
                        } else {
                            if (name.equals(rb.getString("dd_ibm_websphere"))) {
                                northPanel.setContainerBaseLabel(
                                    rb.getString("txt_lbl_container_pdir"));
                                northPanel.setContainerDomainLabel(
                                    rb.getString("txt_lbl_container_sdir"));
                            }
                        }
                    }
                }
            }
        });
        northPanel.addCategoryListSelectionListener(
            new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                JList list = (JList) e.getSource();
                int index = list.getSelectedIndex();
                if (index != previousCategorySelectedIndex) {
                    String categoryName = 
                        ((ImageListEntry)list.getSelectedValue()).getName();
                    HashMap<String, HashMap> serviceNameMap = 
                        serviceCategories.get(categoryName);
                    Set<String> serviceName = serviceNameMap.keySet();
                    centerPanel.removeAllTests();
                    for (String name : serviceName) {
                        if (!name.equalsIgnoreCase(rb.getString("all").trim())) {
                            centerPanel.addTest(new CheckBoxListEntry(name, 
                                SELECTED_ICON, NOT_SELECTED_ICON));
                        }
                    }
                    previousCategorySelectedIndex = index;
                }
            }
        });
        centerPanel.addCheckBoxListSelectionListener(
            new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                JList list = (JList) e.getSource();
                if (list.getSelectedIndex() >= 0) {
                    northPanel.enableRunSelected();
                } else {
                    northPanel.disableRunSelected();
                }
            }
        });
        final MainJPanel mainProgram = this;
        northPanel.addRunSelectedButtonActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ImageListEntry entry = 
                    (ImageListEntry)northPanel.getSelectedCategory();
                if (entry != null) {
                    ToolService service = null;
                    String categoryName = entry.getName();
                    final HashMap<String, HashMap> opCodeMap = 
                        serviceCategories.get(categoryName);
                    Set<String> opNames = opCodeMap.keySet();
                    for (String opName : opNames) {
                        HashMap<String, String> opToService = 
                            opCodeMap.get(opName);
                        Set<String> funNames = opToService.keySet();
                        for (String funName : funNames) {
                            service = tContext.getService(
                                opToService.get(funName).toLowerCase());
                            break;
                        }
                        break;
                    }
                    final Object[] tests = centerPanel.getSelectedTests();
                    if ((tests != null) && (tests.length > 0)) {
                        mainProgram.disableComponentsWhileRunning();
                        TestsTableModel tableModel = 
                            (TestsTableModel) centerPanel.getTableModel();
                        ListSelectionModel selectionModel = 
                            centerPanel.getTableSelectionModel();
                        final int[] rowNum = new int[tests.length];
                        for (int i = 0; i < tests.length; i++) {
                            CheckBoxListEntry testEntry = 
                                (CheckBoxListEntry) tests[i];
                            Object[] values = 
                                new Object[tableModel.getColumnCount() + 1];
                            values[TestsTableModel.START_TIME_COLUMN] = 
                                new StartTimeTableCell(rb);
                            values[TestsTableModel.Test_COLUMN] = 
                                testEntry.getName();
                            values[TestsTableModel.TIME_ELAPSED_COLUMN] = 
                                new CounterTableCell();
                            values[TestsTableModel.RESULT_COLUMN] = 
                                new ResultTableCell(rb);
                            tableModel.addRow(values);
                            rowNum[i] = tableModel.getRowCount() - 1;
                        }
                        final ToolService finalService = service;
                        final TestsTableModel finalTableModel = tableModel;
                        final ListSelectionModel finalSelectionModel = 
                            selectionModel;
                        Runnable workRunnable = new Runnable() {
                            public void run() {
                                for (int i = 0; i < tests.length; i++) {
                                    finalSelectionModel.setSelectionInterval(
                                        rowNum[i], rowNum[i]);
                                    StartTimeTableCell startTimeCell = 
                                        (StartTimeTableCell) 
                                        finalTableModel.getValueAt(rowNum[i], 
                                        TestsTableModel.START_TIME_COLUMN);
                                    startTimeCell.start();
                                    finalTableModel.fireTableRowsUpdated(
                                        rowNum[i], rowNum[i]);
                                    CheckBoxListEntry testEntry = 
                                        (CheckBoxListEntry) tests[i];
                                    HashMap<String, String> opToService = 
                                        opCodeMap.get(testEntry.getName());
                                    Set<String> cmds = opToService.keySet();
                                    HashSet singleCmdSet = new HashSet();
                                    HashMap singleParamSet = new HashMap();
                                    for (String cmd : cmds) {
                                        singleCmdSet.add(cmd);
                                    }
                                    ServiceRequest sReq = new ServiceRequest();
                                    sReq.setCommand(singleCmdSet);
                                    singleParamSet.put(CONFIG_DIR, 
                                        northPanel.getConfigDir());
                                    singleParamSet.put(CONTAINER_DIR, 
                                        northPanel.getContainerDir());
                                    singleParamSet.put(CONTAINER_TYPE, 
                                        ((ImageListEntry)
                                        northPanel.getSelectedWebContainer()).getName());
                                    singleParamSet.put(CONTAINER_DOMAIN_DIR, 
                                        northPanel.getContainerDomainDir());
                                    sReq.setData(singleParamSet);
                                    ResultTableCell resultCell = 
                                        (ResultTableCell) 
                                        finalTableModel.getValueAt(rowNum[i], 
                                        TestsTableModel.RESULT_COLUMN);
                                    resultCell.setViewPosition(new Point(0, 
                                        centerPanel.getCurrentViewPosition().y));
                                    resultCell.start();
                                    finalTableModel.fireTableRowsUpdated(
                                        rowNum[i], rowNum[i]);
                                    final CounterTableCell counterCell = 
                                        (CounterTableCell) 
                                        finalTableModel.getValueAt(rowNum[i], 
                                        TestsTableModel.TIME_ELAPSED_COLUMN);
                                    final int finalRowNum = rowNum[i];
                                    CounterRunnable counter = 
                                        new CounterRunnable(new Runnable() {
                                        public void run() {
                                            counterCell.increase();
                                            finalTableModel.fireTableRowsUpdated(
                                                finalRowNum, finalRowNum);
                                        }
                                    });
                                    counterThread.runTask(counter);
                                    try {
                                        ServiceResponse sRes = 
                                            new ServiceResponse();
                                        finalService.processRequest(sReq, sRes);
                                        if (sRes.getStatus().equals(
                                            ServiceResponse.PASS)) {
                                            resultCell.setResult(true);
                                            finalTableModel.fireTableRowsUpdated(
                                                rowNum[i], rowNum[i]);
                                        } else {
                                            resultCell.setResult(false);
                                            finalTableModel.fireTableRowsUpdated(
                                                rowNum[i], rowNum[i]);
                                        }
                                        ToolLogWriter.log(sRes.getMessage());
                                        ToolLogWriter.log(sRes.getError());
                                        ToolLogWriter.log(sRes.getWarning());
                                        ToolLogWriter.log(sRes.getResult(1));
                                        ToolLogWriter.log(sRes.getResult(2));
                                    } catch (NullPointerException npe) {
                                        /**/
                                        outputWriter.printError(npe.toString());
                                        resultCell.setResult(false);
                                        finalTableModel.fireTableRowsUpdated(
                                            rowNum[i], rowNum[i]);
                                    } catch (Exception ex) {
                                        /**/
                                        resultCell.setResult(false);
                                        outputWriter.printError(ex.toString());
                                        finalTableModel.fireTableRowsUpdated(
                                            rowNum[i], rowNum[i]);
                                    }
                                    counter.finished();
                                }
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        mainProgram.enableComponentsAfterRunning();
                                    }
                                });
                            }
                        };
                        Thread workerThread = new Thread(workRunnable);
                        workerThread.start();
                    }
                }
            }
            
        });
    }
    
    private void enableComponentsAfterRunning() {
        northPanel.enableComponentsAfterRunning();
        centerPanel.enableComponentsAfterRunning();
        southPanel.enableComponentsAfterRunning();
    }
    
    private void disableComponentsWhileRunning() {
        northPanel.disableComponentsWhileRunning();
        centerPanel.disableComponentsWhileRunning();
        southPanel.disableComponentsWhileRunning();
    }
    
    class CounterThread extends Thread {
        private Runnable task;
        private volatile boolean shutdown;
        
        public CounterThread() {
            super("CounterThread");
            shutdown = false;
        }
        
        public synchronized void runTask(Runnable task) {
            this.task = task;
            notify();
        }
        
        public void run() {
            Runnable runTask = null;
            while (true) {
                synchronized (this) {
                    if (!shutdown) {
                        if (task != null){
                            runTask = task;
                            task = null;
                        } else {
                            try {
                                wait();
                            } catch (InterruptedException ignored) {}
                            continue;
                        }
                    }
                }
                if (shutdown) {
                    break;
                }
                runTask.run();
                if (shutdown) {
                    break;
                }
            }
        }
        
        public synchronized void shutdown() {
            shutdown = true;
            notify();
        }
    }
    
    class CounterRunnable implements Runnable {
        private volatile boolean finished;
        private Runnable task;
        
        public CounterRunnable(Runnable task) {
            finished = false;
            this.task = task;
        }
        
        public void run() {
            while (true) {
                try {
                    synchronized (this) {
                        Thread.sleep(1000);
                    }
                    task.run();
                    synchronized (this) {
                        Thread.yield();
                        if (finished) {
                            break;
                        }
                    }
                } catch (Exception ex) {}
            }
            synchronized (this) {
                notify();
            }
        }
        
        public synchronized void finished() {
            finished = true;
            try {
                wait();
            } catch (InterruptedException ignored) {}
        }
    }
    
    private void initComponents() {
        setLayout(new java.awt.BorderLayout());
    }
}
