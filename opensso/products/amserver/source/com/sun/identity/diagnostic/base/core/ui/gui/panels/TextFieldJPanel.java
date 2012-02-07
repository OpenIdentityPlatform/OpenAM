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
 * $Id: TextFieldJPanel.java,v 1.2 2009/07/24 22:06:32 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.panels;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.ResourceBundle;

public class TextFieldJPanel extends javax.swing.JPanel {
    
    /** Creates new form TextFieldJPanel */
    public TextFieldJPanel(ResourceBundle rb) {
        initComponents(rb);
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch(UnknownHostException ex) {
            //ex.printStackTrace();
            System.out.println("Exception in TextFieldJPanel :" + ex.getMessage());
        }
        if (hostName != null) {
            hostNamejTextField.setText(hostName);
        } else {
            hostNamejTextField.setText("");
        }
        
        datejTextField.setText(new Date().toString());
        OSjTextField.setText(System.getProperty("os.name"));
        userNamejTextField.setText(System.getProperty("user.name"));
    }
    
    private void initComponents(ResourceBundle rb) {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel10 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        hostNamejTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        datejTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        OSjTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        userNamejTextField = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

        setBorder(javax.swing.BorderFactory.createTitledBorder(
            rb.getString("lbl_host_server_info")));
        jPanel10.setLayout(new java.awt.GridBagLayout());

        add(jPanel10, new java.awt.GridBagConstraints());

        jLabel1.setText(rb.getString("txt_lbl_hname"));
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(jLabel1, gridBagConstraints);

        hostNamejTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(hostNamejTextField, gridBagConstraints);

        jLabel2.setText(rb.getString("txt_lbl_date"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(jLabel2, gridBagConstraints);

        datejTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(datejTextField, gridBagConstraints);

        jLabel3.setText(rb.getString("txt_lbl_os"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(jLabel3, gridBagConstraints);

        OSjTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(OSjTextField, gridBagConstraints);

        jLabel4.setText(rb.getString("txt_lbl_uname"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(jLabel4, gridBagConstraints);

        userNamejTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(userNamejTextField, gridBagConstraints);
    }
    
    private javax.swing.JTextField OSjTextField;
    private javax.swing.JTextField datejTextField;
    private javax.swing.JTextField hostNamejTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JTextField userNamejTextField;
}
