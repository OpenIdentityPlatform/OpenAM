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
 * $Id: IConstants.java,v 1.5 2009/12/09 23:01:00 krishna_indigo Exp $
 *
 */
/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.install.apache24;

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
    public static String STR_APC24_GROUP = "apache24Tools";
    
    /**
     * default apache24 config directory on Windows
     */   
    public static final String STR_APC24_CONFIG_DIR_WINDOWS =
            "C:\\apache24\\conf";
    
    /**
     * default apache24 config directory on Solaris
     */   
    public static final String STR_APC24_CONFIG_DIR_UNIX =
            "/opt/apache24/conf";
   
    /**
     * Apache's httpd.conf file
     */   
    public static final String STR_APC24_HTTPD_FILE = "httpd.conf";
    
    /**
     * Apache's bin directory name
     */   
    public static final String STR_APC24_BIN_DIR = "bin";

    /**
     * Apache's config directory name
     */   
    public static final String STR_APC24_CONF_DIR = "conf";

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
     * Apache agent libray name suffix, for Apache 24 version.
     */   
    public static final String STR_APC24_TWO_LIB_SUFFIX = "24";

    /**
     * Apache agent libray extension for Windows platform
     */   
    public static final String STR_APC24_WIN_LIB_EXTN = "dll";

    /**
     * Apache agent libray extension for Unix platforms    
     */   
    public static final String STR_APC24_UNIX_LIB_EXTN = "so";

    /**
     * Apache agent libray extension for HPUX platform    
     */   
    public static final String STR_APC24_HPUX_LIB_EXTN = "sl";

    public static final String STR_TRUE = "true";
    public static final String STR_FALSE = "false";
}
