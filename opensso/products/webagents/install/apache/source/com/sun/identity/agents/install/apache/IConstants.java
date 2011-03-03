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
 * $Id: IConstants.java,v 1.5 2009/07/22 22:59:07 subbae Exp $
 *
 */

package com.sun.identity.agents.install.apache;

/**
 * Interface Apache's server specific constants,  
 * which gets reused throughout installation interactions.
 */
public interface IConstants {
    
    /**
     * Apache string
     */
    public static String STR_APACHE = "Apache";

    /**
     * Apache agent locale file
     */
    public static String STR_APC_GROUP = "apacheTools";
    
    /**
     * default apache config directory on Windows
     */   
    public static final String STR_APC_CONFIG_DIR_WINDOWS =
            "C:\\apache\\conf";
    
    /**
     * default apache config directory on Solaris
     */   
    public static final String STR_APC_CONFIG_DIR_UNIX =
            "/opt/apache/conf";
   
    /**
     * Apache's httpd.conf file
     */   
    public static final String STR_APC_HTTPD_FILE = "httpd.conf";
    
    /**
     * Apache's bin directory name
     */   
    public static final String STR_APC_BIN_DIR = "bin";

    /**
     * Apache's config directory name
     */   
    public static final String STR_APC_CONF_DIR = "conf";

    /**
     * dsame.template name
     */   
    public static final String STR_DSAME_FILE_TEMPLATE = "dsame.template";

    /**
     * dsame.conf name
     */   
    public static final String STR_DSAME_CONF_FILE = "dsame.conf";

    /**
     * Agent instance's config directory name
     */   
    public static final String STR_INSTANCE_CONFIG_DIR_NAME = "config";

    /**
     * Agent instance's logs directory name
     */   
    public static final String STR_INSTANCE_LOGS_DIR_NAME = "logs";

    /**
     * Apache agent libray name suffix, for Apache 2 version.
     */   
    public static final String STR_APC_TWO_LIB_SUFFIX = "2";

    /**
     * Apache agent libray extension for Windows platform
     */   
    public static final String STR_APC_WIN_LIB_EXTN = "dll";

    /**
     * Apache agent libray extension for Unix platforms    
     */   
    public static final String STR_APC_UNIX_LIB_EXTN = "so";

    /**
     * Apache agent libray extension for HPUX platform    
     */   
    public static final String STR_APC_HPUX_LIB_EXTN = "sl";

    public static final String STR_TRUE = "true";
    public static final String STR_FALSE = "false";
}
