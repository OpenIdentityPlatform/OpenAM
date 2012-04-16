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
 * $Id: AmWebPolicyResultStatus.java,v 1.2 2008/06/25 05:51:57 qcheng Exp $
 *
 */

package com.sun.identity.agents.policy;

import com.sun.identity.agents.util.ConstrainedSelection;

/**
 * The class represnts a web policy evaluation result status
 */
public class AmWebPolicyResultStatus extends ConstrainedSelection {
    
    /** Field INT_AM_POLICY_ALLOW **/
    public static final int INT_STATUS_ALLOW = 0;

    /** Field INT_AM_POLICY_DENY **/
    public static final int INT_STATUS_DENY = 1;

    /** Field INT_AM_POLICY_INSUFFICIENT_CREDENTIALS **/
    public static final int INT_STATUS_INSUFFICIENT_CREDENTIALS = 2;
    
    public static final String STR_STATUS_ALLOW = "ALLOW";
    
    public static final String STR_STATUS_DENY = "DENY";
    
    public static final String STR_STATUS_INSUFFICIENT_CREDENTIALS =
        "INSUFFICIENT CREDENTIALS";
    
    public static final AmWebPolicyResultStatus STATUS_ALLOW 
            = new AmWebPolicyResultStatus(STR_STATUS_ALLOW, INT_STATUS_ALLOW);
    
    public static final AmWebPolicyResultStatus STATUS_DENY
            = new AmWebPolicyResultStatus(STR_STATUS_DENY, INT_STATUS_DENY);
    
    public static final AmWebPolicyResultStatus STATUS_INSUFFICIENT_CREDENTIALS
            = new AmWebPolicyResultStatus(STR_STATUS_INSUFFICIENT_CREDENTIALS,
                    INT_STATUS_INSUFFICIENT_CREDENTIALS);
    
    private static final AmWebPolicyResultStatus[] values =
        new AmWebPolicyResultStatus[] { STATUS_ALLOW, STATUS_DENY, 
            STATUS_INSUFFICIENT_CREDENTIALS };
    
    public static AmWebPolicyResultStatus getMode(int status) {
        return (AmWebPolicyResultStatus) 
                                ConstrainedSelection.get(status, values);
    }
    
    public static AmWebPolicyResultStatus getMode(String status) {
        return (AmWebPolicyResultStatus) 
                                ConstrainedSelection.get(status, values);
    }
    
    private AmWebPolicyResultStatus(String name, int intValue) {
        super(name, intValue);
    }
}
