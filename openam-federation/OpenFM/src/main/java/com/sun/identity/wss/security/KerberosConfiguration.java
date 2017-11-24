/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: KerberosConfiguration.java,v 1.2 2008/06/25 05:50:07 qcheng Exp $
 *
 */

package com.sun.identity.wss.security;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry;

/**
 * This class <code>KerberosConfiguration<code> represents Kerberos 
 * Configuration for web service clients and web service providers.
 */
public class KerberosConfiguration extends Configuration  {
    
    public static final String WSC_CONFIGURATION = 
                        "com.sun.identity.wss.webservicesclient";    
    public static final String WSP_CONFIGURATION =
                         "com.sun.identity.wss.webservicesprovider";
    private static final String kerberosModuleName =
        "com.sun.security.auth.module.Krb5LoginModule";
    private Configuration config = null;
    private String servicePrincipal = null;
    private String keytab = null;
    private String refreshConf = "false";
    private String ticketCacheDir = null;

    /**
     * Constructor
     *
     * @param config
     */
    public KerberosConfiguration (Configuration config) {
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
     * Sets a boolean value to refresh the configuration.
     * @param refresh
     */
    public void setRefreshConfig(String refresh) {
        refreshConf = refresh;
    }
    
    /**
     * Sets kerberos ticket cache dir. Typically this is users home directory
     * where the user's kerberos ticket is stored.
     * @param dirName the dierectory where the ticket cache is stored.
     */
    public void setTicketCacheDir(String dirName) {
       ticketCacheDir = dirName;
    }        

    /**
     * Returns AppConfigurationEntry array for the application
     * using Kerberos module.
     *
     * @param appName the configuration name.
     * @return Array of AppConfigurationEntry
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String appName){
        if (appName.equals(WSC_CONFIGURATION)) {
            HashMap hashmap = new HashMap();
            hashmap.put("doNotPrompt", "true");
            hashmap.put("useTicketCache", "true");
            hashmap.put("ticketCache", ticketCacheDir);           
            hashmap.put("refreshKrb5Config", refreshConf);
            hashmap.put("debug", "true");

            AppConfigurationEntry appConfigurationEntry =
                new AppConfigurationEntry(
                    kerberosModuleName,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    hashmap);
            return new AppConfigurationEntry[]{ appConfigurationEntry };
        } else if (appName.equals(WSP_CONFIGURATION)) {
            HashMap hashmap = new HashMap();
            hashmap.put("storeKey", "true");
            hashmap.put("principal", servicePrincipal);
            hashmap.put("useKeyTab", "true");
            //hashmap.put("keyTab", "c:/kerberos/wsp.HTTP.keytab");
            hashmap.put("keyTab", keytab);
            hashmap.put("doNotPrompt", "true");
            hashmap.put("debug", "true");
            hashmap.put("refreshKrb5Config", refreshConf);

            AppConfigurationEntry appConfigurationEntry =
                new AppConfigurationEntry(
                    kerberosModuleName,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    hashmap);
            return new AppConfigurationEntry[]{ appConfigurationEntry };
        }
        return config.getAppConfigurationEntry(appName);
    }
    
    public void refresh() {
        config.refresh();
    }
}


