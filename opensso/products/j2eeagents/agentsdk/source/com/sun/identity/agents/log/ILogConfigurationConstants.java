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
 * $Id: ILogConfigurationConstants.java,v 1.3 2008/06/25 05:51:54 qcheng Exp $
 *
 */

package com.sun.identity.agents.log;

import com.sun.identity.agents.arch.IConfigurationKeyConstants;

/**
 * The interface for defining logging configuration constants
 */
public interface ILogConfigurationConstants extends
        IConfigurationKeyConstants {
    
    
    public static final String CONFIG_LOG_LOCAL_FILE = "local.logfile";
    
    public static final String CONFIG_LOG_LOCAL_FILE_ROTATE_ENABLE = 
        "local.log.rotate";
    
    public static final String CONFIG_LOG_LOCAL_FILE_ROTATE_SIZE =
        "local.log.size";
    
    public static final String CONFIG_REMOTE_LOG_FILE_NAME =
        "remote.logfile";
    
    public static final String CONFIG_LOG_DISPOSITION =
        "log.disposition"; //LOCAL, REMOTE, ALL, default = LOCAL

    
    // ----------------- Default Values --------------
    
    public static final boolean DEFAULT_LOG_LOCAL_FILE_ROTATE_ENABLE = false;
    
    public static final long DEFAULT_LOG_LOCAL_FILE_ROTATE_SIZE = 52428800L;
    
}
