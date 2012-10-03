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
 * $Id: TestsTableModel.java,v 1.2 2009/07/24 22:08:44 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.table;

import java.util.Vector;
import java.util.ResourceBundle;
import javax.swing.table.AbstractTableModel;

import com.sun.identity.diagnostic.base.core.common.ToolConstants;



public class TestsTableModel extends AbstractTableModel {
    
    public static final int START_TIME_COLUMN = 0;
    public static final int Test_COLUMN = 1;
    public static final int TIME_ELAPSED_COLUMN = 2;
    public static final int RESULT_COLUMN = 3;
    public static final int CARET_POSITION = 4;
    
    private static final Class columnClasses[] = {LabelTableCell.class, 
        String.class, LabelTableCell.class, LabelTableCell.class, 
            LabelTableCell.class};
    
    private Vector data;
    private static ResourceBundle rb;
    private static String[] headers = new String[4];

    static {
        rb = ResourceBundle.getBundle(ToolConstants.RESOURCE_BUNDLE_NAME);
        headers[0] = rb.getString("lbl_start_time");
        headers[1] = rb.getString("lbl_test");
        headers[2] = rb.getString("lbl_time_elapsed");
        headers[3] = rb.getString("lbl_result");
    }

    
    /** Creates a new instance of TestsTableModel */
    public TestsTableModel() {
        data = new Vector();
    }
    
    public Class getColumnClass(int c) {
        return columnClasses[c];
    }
    
    public String getColumnName(int c) {
        return headers[c];
    }
    
    public int getColumnCount() {
        return headers.length;
    }
    
    public int getRowCount() {
        return data.size();
    }
    
    public Object getValueAt(int r, int c) {
        if (data.size() > r) {
            Object[] row = (Object[]) data.get(r);
            return row[c];
        }
        return null;
    }
    
    public boolean isCellEditable(int r, int c) {
        return false;
    }
    
    public void addRow(Object[] rowData) {
        data.add(rowData);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }
    
    public void removeAll() {
        data.clear();
        fireTableDataChanged();
    } 
}
