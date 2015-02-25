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
 * $Id: ImageListCellRenderer.java,v 1.1 2008/11/22 02:19:56 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.list;

import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class ImageListCellRenderer extends JLabel implements ListCellRenderer {
    
    private static final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);
    
    /** Creates a new instance of ImageListCellRender */
    public ImageListCellRenderer() {
        setOpaque(true);
        setIconTextGap(10);
    }
    
    public Component getListCellRendererComponent(
        JList list, 
        Object value,
        int index, 
        boolean isSelected, 
        boolean cellHasFocus
    ) {
        ImageListEntry entry = (ImageListEntry) value;
        setText(entry.getName());
        ImageIcon icon = entry.getImage();
        setIcon(icon);
        if (isSelected) {
            setBackground(HIGHLIGHT_COLOR);
            setForeground(Color.WHITE);
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
        }
        return this;
    }
}
