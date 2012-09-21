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
 * $Id: ConfigFinder.java,v 1.2 2008/06/25 05:54:35 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.install.apache24;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.IDefaultValueFinder;
import com.sun.identity.install.tools.util.OSChecker;

/**
 * Displays default Apache 24 server instance's config directory.
 */
public class ConfigFinder implements IDefaultValueFinder,
        IConstants, IConfigKeys {
    
    /**
     * Returns Apache server's default config directory.
     *
     * @param key default location 
     * @param state installer state
     * @param value default location value 
     *
     * @return Apache server instance's conf directory
     */
    public String getDefaultValue(String key, IStateAccess state, 
        String value) {
        String result = null;
        if (value != null) {
            result = value;
        } else {
            result = getDefaultConfigDirectoryPath(state);
        }
        
        return result;
    }
    
    /**
     * Returns server instance's config directory.
     */
    private String getDefaultConfigDirectoryPath(IStateAccess state) {
        String result =  null;

        if (OSChecker.isWindows()) {
            result = STR_APC24_CONFIG_DIR_WINDOWS;
        } else {
            result = STR_APC24_CONFIG_DIR_UNIX;
        }
        return result;
    }
}
