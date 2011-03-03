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
 * $Id: ListJPanel.java,v 1.2 2009/07/24 22:04:13 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.panels;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.JFileChooser;
import javax.swing.ListModel;

import com.sun.identity.diagnostic.base.core.ui.gui.list.ImageListCellRenderer;
import com.sun.identity.diagnostic.base.core.ui.gui.list.ImageListEntry;
import java.awt.event.ItemListener;

public class ListJPanel extends javax.swing.JPanel {
    
    private JFileChooser containerDirChooser = null;
    private File containerDirFile = null;
    private JFileChooser containerDomainDirChooser = null;
    private File containerDomainDirFile = null;
    private JFileChooser configDirChooser = null;
    private File configDirFile = null;
    private ResourceBundle rb;
    
    /** Creates new form ListJPanel */
    public ListJPanel(ResourceBundle rb) {
        this.rb = rb;
        initComponents();
        categoryjList.setCellRenderer(new ImageListCellRenderer());
        webContainerjComboBox.setRenderer(new ImageListCellRenderer());        
    }
    
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        categoryjList = new javax.swing.JList();
        jPanel2 = new javax.swing.JPanel();
        configDirjTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        containerBasejLabel = new javax.swing.JLabel();
        containerDirjTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        configDirBrowsejButton = new javax.swing.JButton();
        containerDirBrowsejButton = new javax.swing.JButton();
        webContainerjComboBox = new javax.swing.JComboBox();
        containerDomainjLabel = new javax.swing.JLabel();
        containerDomainDirjTextField = new javax.swing.JTextField();
        containerDomainDirBrowsejButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        runSelectedjButton = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        setBorder(javax.swing.BorderFactory.createTitledBorder(
            rb.getString("lbl_component")));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(
            rb.getString("lbl_category")));
        categoryjList.setSelectionMode(
            javax.swing.ListSelectionModel.SINGLE_SELECTION);
        categoryjList.setVisibleRowCount(5);
        jScrollPane1.setViewportView(categoryjList);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        add(jPanel1, java.awt.BorderLayout.WEST);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(
            rb.getString("lbl_web_container")));
        configDirjTextField.setColumns(10);
        configDirjTextField.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(configDirjTextField, gridBagConstraints);

        jLabel2.setText(rb.getString("txt_lbl_cfg_dir"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel2, gridBagConstraints);

        containerBasejLabel.setText(rb.getString("txt_lbl_container_bdir"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(containerBasejLabel, gridBagConstraints);

        containerDirjTextField.setColumns(10);
        containerDirjTextField.setHorizontalAlignment(
            javax.swing.JTextField.LEFT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(containerDirjTextField, gridBagConstraints);

        jLabel4.setText(rb.getString("txt_lbl_container_type"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(jLabel4, gridBagConstraints);

        configDirBrowsejButton.setText(rb.getString("btn_lbl_browse"));
        configDirBrowsejButton.addActionListener(
            new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configDirBrowsejButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(configDirBrowsejButton, gridBagConstraints);

        containerDirBrowsejButton.setText(rb.getString("btn_lbl_browse"));
        containerDirBrowsejButton.addActionListener(
            new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                containerDirBrowsejButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(containerDirBrowsejButton, gridBagConstraints);

        webContainerjComboBox.addItemListener(
            new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                webContainerjComboBoxItemStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(webContainerjComboBox, gridBagConstraints);

        containerDomainjLabel.setText(rb.getString("txt_lbl_container_ddir"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(containerDomainjLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(containerDomainDirjTextField, gridBagConstraints);

        containerDomainDirBrowsejButton.setText(rb.getString("btn_lbl_browse"));
        containerDomainDirBrowsejButton.addActionListener(
            new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                containerDomainDirBrowsejButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(containerDomainDirBrowsejButton, gridBagConstraints);

        add(jPanel2, java.awt.BorderLayout.CENTER);

        runSelectedjButton.setText(rb.getString("btn_lbl_run_selected"));
        runSelectedjButton.setEnabled(false);
        jPanel3.add(runSelectedjButton);

        add(jPanel3, java.awt.BorderLayout.EAST);

    }

    private void webContainerjComboBoxItemStateChanged(
        java.awt.event.ItemEvent evt
    ) {} 

    private void containerDomainDirBrowsejButtonActionPerformed(
        java.awt.event.ActionEvent evt
    ) {
        if (containerDomainDirChooser == null) {
            String initDir = System.getProperty("user.home");
            if (initDir != null) {
                containerDomainDirChooser = new JFileChooser(initDir);
            } else {
                containerDomainDirChooser = new JFileChooser();
            }
        } else {
            if (containerDomainDirFile != null) {
                containerDomainDirChooser.setCurrentDirectory(
                    containerDomainDirFile);
            }
        }
        containerDomainDirChooser.setFileSelectionMode(
            JFileChooser.DIRECTORIES_ONLY);
        containerDomainDirChooser.setMultiSelectionEnabled(false);
        containerDomainDirChooser.setDialogTitle(
            rb.getString("dlg_open_wc_domain_dir_title"));
        if (containerDomainDirChooser.showOpenDialog(this) == 
            JFileChooser.APPROVE_OPTION) {
            containerDomainDirFile = containerDomainDirChooser.getSelectedFile();
            if ((containerDomainDirFile != null) &&
                containerDomainDirFile.exists() &&
                containerDomainDirFile.isDirectory()){
                containerDomainDirjTextField.setText(
                    containerDomainDirFile.getAbsolutePath());
            }
        }
    }

    private void containerDirBrowsejButtonActionPerformed(
        java.awt.event.ActionEvent evt
    ) {
        if (containerDirChooser == null) {
            String initDir = System.getProperty("user.home");
            if (initDir != null) {
                containerDirChooser = new JFileChooser(initDir);
            } else {
                containerDirChooser = new JFileChooser();
            }
        } else {
            if (containerDirFile != null) {
                containerDirChooser.setCurrentDirectory(containerDirFile);
            }
        }
        containerDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        containerDirChooser.setMultiSelectionEnabled(false);
        containerDirChooser.setDialogTitle(
            rb.getString("dlg_open_wc_base_dir_title"));
        if (containerDirChooser.showOpenDialog(this) == 
            JFileChooser.APPROVE_OPTION) {
            containerDirFile = containerDirChooser.getSelectedFile();
            if ((containerDirFile != null) && containerDirFile.exists() &&
                containerDirFile.isDirectory()){
                containerDirjTextField.setText(
                    containerDirFile.getAbsolutePath());
            }
        }
    }

    private void configDirBrowsejButtonActionPerformed(
        java.awt.event.ActionEvent evt
    ) {
        if (configDirChooser == null) {
            String initDir = System.getProperty("user.home");
            if (initDir != null) {
                configDirChooser = new JFileChooser(initDir);
            } else {
                configDirChooser = new JFileChooser();
            }
        } else {
            if (configDirFile != null) {
                configDirChooser.setCurrentDirectory(configDirFile);
            }
        }
        configDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        configDirChooser.setMultiSelectionEnabled(false);
        configDirChooser.setDialogTitle(rb.getString("dlg_open_osso_cfg_dir_title"));
        if (configDirChooser.showOpenDialog(this) == 
            JFileChooser.APPROVE_OPTION) {
            configDirFile = configDirChooser.getSelectedFile();
            if ((configDirFile != null) && configDirFile.exists() &&
                configDirFile.isDirectory()){
                configDirjTextField.setText(configDirFile.getAbsolutePath());
            }
        }
    }
    
    
    private javax.swing.JList categoryjList;
    private javax.swing.JButton configDirBrowsejButton;
    private javax.swing.JTextField configDirjTextField;
    private javax.swing.JLabel containerBasejLabel;
    private javax.swing.JButton containerDirBrowsejButton;
    private javax.swing.JTextField containerDirjTextField;
    private javax.swing.JButton containerDomainDirBrowsejButton;
    private javax.swing.JTextField containerDomainDirjTextField;
    private javax.swing.JLabel containerDomainjLabel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton runSelectedjButton;
    private javax.swing.JComboBox webContainerjComboBox;
    
    public void addCategory(ImageListEntry entry) {
        ListModel model = categoryjList.getModel();
        if (model instanceof DefaultListModel) {
            ((DefaultListModel)model).addElement(entry);
        } else {
            DefaultListModel newModel = new DefaultListModel();
            newModel.addElement(entry);
            categoryjList.setModel(newModel);
        }        
    }
    
    public void addWebContainer(ImageListEntry entry) {
        ComboBoxModel model = webContainerjComboBox.getModel();
        if (model instanceof DefaultComboBoxModel) {
            ((DefaultComboBoxModel)model).addElement(entry);
        } else {
            DefaultComboBoxModel newModel = new DefaultComboBoxModel
                    ();
            newModel.addElement(entry);
            webContainerjComboBox.setModel(newModel);
        }
    }
    
    public void addCategoryListSelectionListener(
        ListSelectionListener listener
    ) {
        categoryjList.addListSelectionListener(listener);
    }
    
    public Object getSelectedCategory() {
        return categoryjList.getSelectedValue();
    }
    
    public Object getSelectedWebContainer() {
        return webContainerjComboBox.getSelectedItem();
    }
    
    public void addWebContainerItemListener(ItemListener listener) {
        webContainerjComboBox.addItemListener(listener);
    }    
    
    public void addRunSelectedButtonActionListener(ActionListener listener) {
        runSelectedjButton.addActionListener(listener);
    }
    
    public String getContainerDir() {
        return containerDirjTextField.getText();
    }
    
    public String getContainerDomainDir() {
        return containerDomainDirjTextField.getText();
    }
    
    public String getConfigDir() {
        return configDirjTextField.getText();
    }
    
    public void setContainerBaseLabel(String name) {
        containerBasejLabel.setText(name);
    }
    
    public void setContainerDomainLabel(String name) {
        containerDomainjLabel.setText(name);
    }
    
    public void disableComponentsWhileRunning() {
        categoryjList.setEnabled(false);
        configDirBrowsejButton.setEnabled(false);
        configDirjTextField.setEnabled(false);
        containerDirBrowsejButton.setEnabled(false);
        containerDirjTextField.setEnabled(false);
        runSelectedjButton.setEnabled(false);
        containerDomainDirjTextField.setEnabled(false);
        containerDomainDirBrowsejButton.setEnabled(false);
        webContainerjComboBox.setEnabled(false);
    }
    
    public void enableComponentsAfterRunning() {
        categoryjList.setEnabled(true);
        configDirBrowsejButton.setEnabled(true);
        configDirjTextField.setEnabled(true);
        containerDirBrowsejButton.setEnabled(true);
        containerDirjTextField.setEnabled(true);
        runSelectedjButton.setEnabled(true);
        containerDomainDirjTextField.setEnabled(true);
        containerDomainDirBrowsejButton.setEnabled(true);
        webContainerjComboBox.setEnabled(true);
    }
    
    public void enableRunSelected() {
        runSelectedjButton.setEnabled(true);
    }
    
    public void disableRunSelected() {
        runSelectedjButton.setEnabled(false);
    }
}
