/* The contents of this file are subject to the terms
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
 * $Id: SessionConstants.java,v 1.1 2008/05/30 22:53:07 srivenigan Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.session;

/**
 * <code>SessionConstants</code> contains the strings for session constraints
 */
public interface SessionConstants {
    
    /**
     * String - Schema name of session service
     */
    public static final String SESSION_SRVC = "iPlanetAMSessionService";
    
    /**
     * String - Global service type
     */
    public static final String GLOBAL_SRVC_TYPE = "Global";
    
    /**
     * String - Dynamic service type
     */
    public static final String DYNAMIC_SRVC_TYPE = "Dynamic";
    
    /**
     * String - Schema name of session quota attribute
     */
    public static final String SESSION_QUOTA_ATTR = 
            "iplanet-am-session-quota-limit";
    
    /**
     * String - Schema name of enable session constraint
     */
    public static final String ENABLE_SESSION_CONST = 
            "iplanet-am-session-enable-session-constraint";
    
    /**
     * String - Schema name of by-pass toplevel admin
     */
    public static final String BYPASS_TOPLEVEL_ADMIN = 
    	"iplanet-am-session-enable-session-constraint-bypass-topleveladmin";
    
    /**
     * String - Schema name of resulting behavior attribute
     */
    public static final String RESULTING_BEHAVIOR =
            "iplanet-am-session-constraint-resulting-behavior";
    
}
