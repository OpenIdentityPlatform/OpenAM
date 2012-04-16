/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IConstants.java,v 1.1 2009/01/12 09:25:26 ranajitgh Exp $
 *
 */

package com.sun.identity.agents.install.proxy40;

/**
 * Interface PROXY4's server specific constants,  
 * which gets reused throughout installation interactions.
 */
public interface IConstants {
    
    /**  PROXY4 string **/
    public static String STR_PROXY4 = "PROXY4";

    /** Field STR_SPS_GROUP **/
    public static String STR_PROXY40_GROUP = "proxy40Tools";
    
    public static final String STR_PROXY4_MAGNUS_FILE = "magnus.conf";
    public static final String STR_PROXY40_OBJ_FILE = "obj.conf";
    public static final String STR_INSTANCE_CONFIG_DIR_NAME = "config";
    public static final String STR_INSTANCE_LOGS_DIR_NAME = "logs";
    public static final String STR_TRUE = "true";
    public static final String STR_FALSE = "false";
}


