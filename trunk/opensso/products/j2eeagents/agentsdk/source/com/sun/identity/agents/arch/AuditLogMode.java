/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuditLogMode.java,v 1.2 2008/06/25 05:51:35 qcheng Exp $
 *
 */

package com.sun.identity.agents.arch;

import com.sun.identity.agents.util.ConstrainedSelection;

/**
 * This class represents an audit log mode.
 */

public class AuditLogMode extends ConstrainedSelection {

    public static final int INT_MODE_NONE = 0;
    public static final int INT_MODE_ALLOW = 1;
    public static final int INT_MODE_DENY = 2;
    public static final int INT_MODE_BOTH = 3;
    
    public static final String STR_MODE_NONE = "LOG_NONE";
    public static final String STR_MODE_ALLOW = "LOG_ALLOW";
    public static final String STR_MODE_DENY = "LOG_DENY";
    public static final String STR_MODE_BOTH = "LOG_BOTH";
    
    public static final AuditLogMode MODE_NONE = 
        new AuditLogMode(STR_MODE_NONE, INT_MODE_NONE);
    public static final AuditLogMode MODE_ALLOW = 
        new AuditLogMode(STR_MODE_ALLOW, INT_MODE_ALLOW);
    public static final AuditLogMode MODE_DENY = 
        new AuditLogMode(STR_MODE_DENY, INT_MODE_DENY);
    public static final AuditLogMode MODE_BOTH = 
        new AuditLogMode(STR_MODE_BOTH, INT_MODE_BOTH);
    
    private static final AuditLogMode[] values = new AuditLogMode[] {
            MODE_NONE, MODE_ALLOW, MODE_DENY, MODE_BOTH
    };
    
    public static AuditLogMode get(int mode) {
        return (AuditLogMode) ConstrainedSelection.get(mode, values);
    }
    
    public static AuditLogMode get(String mode) {
        return (AuditLogMode) ConstrainedSelection.get(mode, values);
    }
    
    private AuditLogMode(String name, int value) {
        super(name, value);
    }
}
