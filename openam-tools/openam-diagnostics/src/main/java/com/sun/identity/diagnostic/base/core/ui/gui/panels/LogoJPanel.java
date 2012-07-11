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
 * $Id: LogoJPanel.java,v 1.1 2008/11/22 02:19:57 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.panels;

public class LogoJPanel extends javax.swing.JPanel {
    
    /** Creates new form LogoJPanel */
    public LogoJPanel() {
        initComponents();
    }
    
    private void initComponents() {
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource(
           "/com/sun/identity/diagnostic/base/core/ui/gui/images/sunLogo.jpg")));
        add(jLabel2, java.awt.BorderLayout.EAST);

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 24));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("OpenSSO Diagnostic Tool");
        add(jLabel1, java.awt.BorderLayout.CENTER);
    }
    
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
}
