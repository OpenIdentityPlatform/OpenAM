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
 * $Id: ConfigFinder.java,v 1.3 2008/06/25 05:52:11 qcheng Exp $
 *
 */

package com.sun.identity.agents.install.appserver.v81;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.IDefaultValueFinder;
import com.sun.identity.install.tools.util.OSChecker;

/**
 * The class used by the installer to find the configuration information of 
 * the Sun App Server
 */

public class ConfigFinder implements IDefaultValueFinder {
    
    public String getDefaultValue(String key, IStateAccess state, String value) {
        String result = null;
        if (value != null) {
            result = value;
        } else {
            result = getDefaultConfigDirectoryPath();
        } 

        return result;
    }
    
    
    private String getDefaultConfigDirectoryPath() {
        String result = null;
        if (OSChecker.isWindows()) {
            result = STR_CONFIG_DIR_WINDOWS;
        } else if (OSChecker.isSolaris()){
            result = STR_CONFIG_DIR_SOLARIS;
        } else if (OSChecker.isLinux()) {
            result = STR_CONFIG_DIR_LINUX;
        } else {
            // better to keep it empty since
            // any other platforms are not supported
        }
        
        return result;
    }
    
    public static final String STR_CONFIG_DIR_WINDOWS = 
        "C:\\Sun\\AppServer\\domains\\domain1\\config";
    public static final String STR_CONFIG_DIR_SOLARIS = 
        "/opt/SUNWappserver/domains/domain1/config";
    public static final String STR_CONFIG_DIR_LINUX    = 
        "/opt/SUNWappserver/domains/domain1/config";
 
}
