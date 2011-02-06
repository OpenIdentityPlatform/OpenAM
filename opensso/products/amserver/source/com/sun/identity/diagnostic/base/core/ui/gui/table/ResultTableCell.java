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
 * $Id: ResultTableCell.java,v 1.2 2009/07/24 22:07:43 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core.ui.gui.table;

import java.awt.Color;
import java.awt.Point;
import java.util.ResourceBundle;

public class ResultTableCell extends LabelTableCell {
    
    private Point point;
    private ResourceBundle rb;
    
    public static final Color GREEN = new Color(0, 128, 0);
    
    /** Creates a new instance of ResultTableCell */
    public ResultTableCell(ResourceBundle rb) {
        super();
        this.rb = rb;
        point = null;
        setText(rb.getString("test_wait_msg"));
        setColor(Color.BLUE);
    }
    
    public void start() {
        setText(rb.getString("test_process_msg"));
        setColor(Color.BLUE);
    }
    
    public void setViewPosition(Point point) {
        this.point = point;
    }
    
    public Point getViewPosition() {
        return point;
    }
    
    public void setResult(boolean pass) {
        if (pass) {
            setText(rb.getString("test_pass_msg"));
            setColor(GREEN);
        } else {
            setText(rb.getString("test_fail_msg"));
            setColor(Color.RED);
        }
    }
}
