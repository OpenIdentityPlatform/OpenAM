/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IConstants.java,v 1.5 2008/06/25 05:54:40 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.sjsws;

/**
 * Interface SWS's server specific constants,  
 * which gets reused throughout installation interactions.
 */
public interface IConstants {
    
    /**  SWS string **/
    public static String STR_SWS = "SWS";

    /** Field STR_SWS_GROUP **/
    public static String STR_SWS_GROUP = "sjswsTools";
    
    public static final String STR_SWS_MAGNUS_FILE = "magnus.conf";
    public static final String STR_SWS_OBJ_FILE = "obj.conf";
    public static final String STR_INSTANCE_CONFIG_DIR_NAME = "config";
    public static final String STR_INSTANCE_LOGS_DIR_NAME = "logs";
    public static final String STR_TRUE = "true";
    public static final String STR_FALSE = "false";
}


