/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConfigFinder.java,v 1.2 2009/03/05 23:28:20 subbae Exp $
 *
 */

package com.sun.identity.agents.install.proxy40;
import java.net.InetAddress;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.IDefaultValueFinder;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.OSChecker;


/**
 * Displays default Proxy server instance's config directory.
 */

public class ConfigFinder implements IDefaultValueFinder,
        IConstants, IConfigKeys {
    
    /**
     * Returns SPS server's default config directory.
     *
     * @param key default location 
     * @param state installer state
     * @param value default location value 
     *
     * @return Proxy server instance's conf directory
     */
    public String getDefaultValue(String key, IStateAccess state, 
        String value) {
        String result = null;
        if (value != null) {
            result = value;
        } else {
            result = getDefaultConfigDirectoryPath();
        }
        
        return result;
    }
    
    /**
     * Returns server instance's config directory.
     */
    private String getDefaultConfigDirectoryPath() {
        String result = null;
        if (OSChecker.isWindows()) {
                result = "C:\\Sun\\ProxyServer40\\"
                + "proxy-server1" + "\\config";
        } else {
            result = "/opt/sun/proxyserver40/"
                + "proxy-server1" + "/config";
        }

        return result;
    }

}
