/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: IConfigKeys.java,v 1.2 2008/11/28 12:36:22 saueree Exp $
 */

package com.sun.identity.agents.tools.tomcat.v6;


/**
 *
 * Interface to isolate the app's server specific config keys
 *
 */
public interface IConfigKeys {
    /*
     * Key to store CATALINA home dir
     */
    public static String STR_KEY_CATALINA_HOME_DIR = "CATALINA_HOME";

    /*
     * Key to store CATALINA home dir
     */
    public static String STR_KEY_CATALINA_BASE_DIR = "$CATALINA_BASE_DIR";

    /*
     * Key for the interaction to lookup config dir
     */
    public static String STR_KEY_TOMCAT_SERVER_CONFIG_DIR = "CONFIG_DIR";

    /*
     * Key for the location of the tomcat server.xml file
     */
    public static final String STR_KEY_TOMCAT_SERVER_XML_FILE =
    	"TOMCAT_SERVER_XML_FILE";

    /*
     * Key for determining if filter has to be installed in the global web xml
     *
     */
    public static final String STR_KEY_INSTALL_GLOBAL_WEB_XML =
    	"INSTALL_GLOBAL_WEB_XML";

    /*
     * Key for the location of the tomcat web.xml file
     */
    public static final String STR_KEY_TOMCAT_GLOBAL_WEB_XML_FILE =
    	"TOMCAT_GLOBAL_WEB_XML_FILE";

    /*
     * Key for the location of the tomcat setAgentclasspath.bat file
     */
    public static final String STR_KEY_TOMCAT_AGENT_ENV_FILE_PATH =
    	"TOMCAT_AGENT_ENV_FILE_PATH";
}
