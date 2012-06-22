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
 * $Id: LabelTableCellRenderer.java,v 1.1 2008/11/22 02:19:57 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.table;

import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class LabelTableCellRenderer extends JLabel implements TableCellRenderer {
    
    /** Creates a new instance of LabelTableCellRenderer */
    public LabelTableCellRenderer() {
    }
    
    public Component getTableCellRendererComponent(
        JTable table, 
        Object value, 
        boolean isSelected, 
        boolean hasFocus, 
        int row, 
        int colimn
    ) {
        if (value == null) {
            setText(null);
            setIcon(null);
            setForeground(Color.BLACK);
            return this;
        }
        if (value instanceof LabelTableCell) {
            String text;
            if ((text = ((LabelTableCell) value).getText()) != null) {
                setText(text);
            }
            ImageIcon icon;
            if ((icon = ((LabelTableCell) value).getIcon()) != null) {
                setIcon(icon);
            }
            setForeground( ((LabelTableCell) value).getColor());
        }
        return this;
    }  
}
