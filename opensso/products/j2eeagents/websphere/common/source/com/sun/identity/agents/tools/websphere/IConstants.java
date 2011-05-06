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
 * $Id: IConstants.java,v 1.2 2008/11/21 22:21:44 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere;

/**
 * Interface containing constants.
 */
public interface IConstants {
    
    public static final String STR_CONFIG_DIR_LEAF = "config";
    
    public static final String STR_AGENT_JAR = "agent.jar";
    
    public static final String STR_SERVER_XML_FILE = "server.xml";
    
    public static final String STR_CLASSPATH_SEP = System.getProperty(
            "path.separator");
    
    public static final String STR_OPENSSOCLIENTSDK_JAR = "openssoclientsdk.jar";
    
    public static final String STR_WAS_GROUP = "amWebsphere";
    
    public static final String STR_OS_NAME_PROPERTY = "os.name";
    
    public static final String STR_WINDOWS = "windows";
    
    public static final String STR_FILE_SEP = "/";
    
    public static final String STR_CLASSPATH_ATTR = "classpath";
    
    public static final String STR_CLASSPATH_ELEM = "classpath";
    
    public static final String STR_ADMIN_AUTHZ_XML_FILE = "admin-authz.xml";
    
}
