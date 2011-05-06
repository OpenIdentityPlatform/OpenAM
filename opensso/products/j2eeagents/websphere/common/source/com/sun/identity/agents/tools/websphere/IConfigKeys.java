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
 * $Id: IConfigKeys.java,v 1.2 2008/11/21 22:21:44 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere;

/**
 * Interface to isolate the app's server specific config keys.
 *
 */
public interface IConfigKeys {
    
    public static String STR_KEY_WAS_HOME_DIR = "HOME_DIRECTORY";
    
    public static String STR_KEY_WAS_LIB_EXT = "LIB_EXT_DIRECTORY";
    
    public static String STR_KEY_SERVER_INSTANCE_DIR = "SERVER_INSTANCE_DIR";
    
    public static String STR_SERVER_INSTANCE_NAME = "SERVER_INSTANCE_NAME";
    
    public static String STR_KEY_PRE_AGENT_CP = "PRE_AGENT_CP";
    
    public static String STR_KEY_WAS_SERVER_XML_FILE = "WAS_SERVER_XML_FILE";
    
}
