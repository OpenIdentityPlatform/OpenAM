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
 * $Id: IAmWebsphereModuleConstants.java,v 1.2 2008/11/21 22:21:46 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import com.sun.identity.agents.arch.IBaseModuleConstants;

/**
 * Interface with constants.
 */
public interface IAmWebsphereModuleConstants extends IBaseModuleConstants {
//-------------- AmWebsphere Module Constatns -----------------------//
     
    /**
     * AmWebsphere Module: module code
     */
    public static final byte AM_WEBSPHERE_MODULE_CODE = 0x12;
    /**
     * AmWebsphere: resource
     */
    public static final String AM_WEBSPHERE_RESOURCE = "amWebsphere";
    /**
     * AmWebsphere: offset
     */
    public static final int AM_WEBSPHERE_OFFSET = 
            OPTIONAL_MODULE_OFFSET + 2 * OFFSET_MULTIPLIER;

    
    public static final int STR_AM_WEBSPHERE_MODULE =
            AM_WEBSPHERE_OFFSET + 1;
    
    public static final int MSG_AM_WEBSPHERE_AUTH_SUCCESS =
            AM_WEBSPHERE_OFFSET + 2;
    
    public static final int MSG_AM_WEBSPHERE_AUTH_FAILED =
            AM_WEBSPHERE_OFFSET + 3;
}
