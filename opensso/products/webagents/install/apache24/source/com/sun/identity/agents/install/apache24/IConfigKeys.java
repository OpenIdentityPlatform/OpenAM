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
 * $Id: IConfigKeys.java,v 1.2 2008/06/25 05:54:35 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.install.apache24;

/**
 * Interface to isolate the Apache's server specific config keys
 * These keys hold installation information which gets reused
 * throughout installation interactions.
 */
public interface IConfigKeys {
    
    /**
     * Key for the interaction to lookup config dir
     */
    public static String STR_KEY_APC24_INST_CONF_DIR = "CONFIG_DIR";
    
    /**
     * Key to store Apache home dir
     */
    public static String STR_KEY_APC24_HOME_DIR = "APC24_HOME_DIR";
    
    /**
     * Key to store Apache bin dir
     */
    public static String STR_KEY_APC24_BIN_DIR = "APC24_BIN_DIR";

    /**
     * Key to store Apache server instance name
     */
    public static String STR_KEY_APC24_INST_NAME = "INSTANCE_NAME";
    
    /**
     * Key to apache24 httpd conf file
     */
    public static String STR_KEY_APC24_HTTPD_FILE  = "APC24_HTTPD_FILE";
        
    /**
     * Key to agent instance name
     */
    public static final String STR_KEY_AGENT_INSTANCE_NAME =
            "AGENT_INSTANCE_NAME";
    /**
     * Key for log rotation 
     */
    public static final String STR_KEY_LOG_ROTATION =
            "LOG_ROTATION";
    /**
     * Key for notification enable 
     */
    public static final String STR_KEY_NOTIFICATION_ENABLE =
            "NOTIFICATION_ENABLE";
}
