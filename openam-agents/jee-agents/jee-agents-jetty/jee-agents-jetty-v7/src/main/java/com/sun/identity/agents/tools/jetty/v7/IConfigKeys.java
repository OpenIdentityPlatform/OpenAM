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
 * $Id: IConfigKeys.java,v 1.1 2009/01/21 18:43:55 kanduls Exp $
 */

/**
 * Portions Copyrighted 2013 ForgeRock AS.
 */

package com.sun.identity.agents.tools.jetty.v7;

/**
 *
 * Interface to isolate the jetty webserver specific config keys
 *
 */
public interface IConfigKeys {
    /*
     * Key to store JETTY home dir
     */
    public static String STR_KEY_JETTY_HOME_DIR = "JETTY_HOME";

    /*
     * Key for the interaction to lookup config dir
     */
    public static String STR_KEY_JETTY_SERVER_CONFIG_DIR = "CONFIG_DIR";

    /*
     * Key for the location of the Jetty jetty.xml file
     */
    public static final String STR_KEY_JETTY_SERVER_XML_FILE = "JETTY_SERVER_XML_FILE";
    /*
     * Key for the location of the Jetty start.jar file
     */
    public static final String STR_KEY_JETTY_START_JAR_PATH = "JETTY_START_JAR_PATH";

    /*
     * Key for the location of the Jetty start.ini file
     */
    public static final String STR_KEY_JETTY_START_INI_PATH = "JETTY_START_INI_PATH";

    /*
     * Key for the interaction to lookup config dir
     */
    public static String STR_KEY_JETTY_INST_DEPLOY_DIR = "DEPLOY_DIR";
}

