/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WindowsDesktopSSOConfig.java,v 1.3 2009/04/07 22:55:13 beomsuk Exp $
 *
 */


package com.sun.identity.authentication.modules.windowsdesktopsso;

import java.util.HashMap;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;

public class WindowsDesktopSSOConfig extends Configuration {
    public static final String defaultAppName = 
        "com.sun.identity.authentication.windowsdesktopsso";
    private static final String kerberosModuleName = 
        SystemProperties.get(Constants.KRB5_LOGINMODULE, 
            Constants.DEFAULT_KRB5_LOGINMODULE);    
    private static final String credsType = 
        SystemProperties.get(Constants.KRB5_CREDENTIAL_TYPE, "acceptor");
                    
    private Configuration config = null;
    private String servicePrincipal = null;
    private String keytab = null;
    private String refreshConf = "false";

    /**
     * Constructor
     *
     * @param config
     */
    public WindowsDesktopSSOConfig(Configuration config) {
        this.config = config;
    }

    /**
     * Sets principal name.
     *
     * @param principalName
     */
    public void setPrincipalName(String principalName) {
        servicePrincipal = principalName;
    }

    /**
     * Sets key tab file.
     *
     * @param keytabFile
     */
    public void setKeyTab(String keytabFile) {
        keytab = keytabFile;
    }

    /**
     * TODO-JAVADOC
     */
    public void setRefreshConfig(String refresh) {
        refreshConf = refresh;
    }

    /**
     * Returns AppConfigurationEntry array for the application <I>appName</I>
     * using Kerberos module.
     *
     * @param appName
     * @return Array of AppConfigurationEntry
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String appName){
        if (appName.equals(defaultAppName)) {
            HashMap hashmap = new HashMap();
            hashmap.put("principal", servicePrincipal);
            if (kerberosModuleName.equalsIgnoreCase("com.ibm.security.auth.module.Krb5LoginModule")) {
                hashmap.put("useKeytab", keytab);
                hashmap.put("credsType", credsType);
                hashmap.put("refreshKrb5Config", "false");
            } else {
                hashmap.put("storeKey", "true");
                hashmap.put("useKeyTab", "true");
                hashmap.put("keyTab", keytab);
                hashmap.put("doNotPrompt", "true");
                hashmap.put("refreshKrb5Config", refreshConf);
            }

            AppConfigurationEntry appConfigurationEntry =
                new AppConfigurationEntry(
                    kerberosModuleName,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    hashmap);
            return new AppConfigurationEntry[]{ appConfigurationEntry };
        }
        return config.getAppConfigurationEntry(appName);
    }

    /**
     * TODO-JAVADOC
     */
    public void refresh() {
        config.refresh();
    }
}

