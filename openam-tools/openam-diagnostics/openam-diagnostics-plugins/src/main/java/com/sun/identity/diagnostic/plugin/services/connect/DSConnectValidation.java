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
 * $Id: DSConnectValidation.java,v 1.2 2009/01/28 05:34:58 ww203982 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.connect;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.LDAPConnection;

import com.iplanet.sso.SSOToken;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;


/**
 * This is a supporting class to validate the connectivity
 * to the server.
 */
public class DSConnectValidation extends ServiceBase implements
   ConnectionConstants, IConnectService {
    
    private IToolOutput toolOutWriter;
    
    public DSConnectValidation() {
    }
    
    /**
     * Validate the configuration.
     *
     * @param path Configuration directory path location
     */
    public void testConnection(String path) {
        SSOToken ssoToken = null;
        toolOutWriter = ServerConnectionService.getToolWriter();
        if (loadConfig(path)) {
           ssoToken = getAdminSSOToken();
           if (ssoToken != null) {
               processDSServers(ssoToken);
           } else {
               toolOutWriter.printError("cnt-auth-msg");
           }
        } else {
           toolOutWriter.printStatusMsg(false, "cnt-ds-msg");
        }
    }
    
    private boolean loadConfig(String path) {
        boolean loaded = false;
        try {
            if (!loadConfigFromBootfile(path).isEmpty()) {
                loaded = true;
            }
        } catch (Exception e) {
            toolOutWriter.printError("cannot-load-properties" ,
                new String[] {path});
        }
        return loaded;
    }
    
    private void processDSServers(SSOToken ssoToken) {
        String amService = IDREPO_SERVICE;
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                amService, ssoToken);
            ServiceConfig serviceConfig =
                scm.getOrganizationConfig("/", null);
            Set subCfgNames = serviceConfig.getSubConfigNames();
            if ((subCfgNames != null) && !subCfgNames.isEmpty()) {
                for (Iterator i = subCfgNames.iterator(); i.hasNext(); ) {
                    String dStore = (String)i.next();
                    toolOutWriter.printMessage("cnt-ds-datastore",
                        new String[] {dStore});
                    ServiceConfig subConfig = 
                        serviceConfig.getSubConfig(dStore);
                    if  ((subConfig != null)) {
                        Map dsAttrs = subConfig.getAttributes();
                        processEntries(dsAttrs);
                    }
                }
            }
        } catch (Exception e) {
             Debug.getInstance(DEBUG_NAME).error(
                "DSConnectValidation.processDSServers: " +
                "Exception during processing DS entries ", e);
        }
    }
    
    private void processEntries(Map dsAttrs) {
        Set ldapServerSet = new HashSet((Set)dsAttrs.get(
            LDAPv3Config_LDAP_SERVER));
        if ((ldapServerSet != null) && !ldapServerSet.isEmpty()) {
            Iterator it = ldapServerSet.iterator();
            while (it.hasNext()) {
                String ldapServer = null;
                String ldapPortStr = null;
                String pwd = null;
                String id =  null;
                String type = null;
                int ldapPort = 389;
                String curr = (String) it.next();
                StringTokenizer tk = new StringTokenizer(curr, "|");
                String hostAndPort = tk.nextToken().trim();
                int index = hostAndPort.indexOf(':');
                if (index > -1) {
                    ldapServer = hostAndPort.substring(0, index);
                    ldapPortStr = hostAndPort.substring(index+1);
                    try {
                        ldapPort = Integer.parseInt(ldapPortStr);
                    } catch(NumberFormatException e) {
                        toolOutWriter.printWarning("cnt-ds-invalid-port");
                    }
                } else {
                    ldapServer = hostAndPort;
                    //invalid syntax - no PORT
                    toolOutWriter.printWarning("cnt-ds-no-port");
                }
                pwd = getPropertyStringValue(dsAttrs, LDAPv3Config_AUTHPW);
                id =  getPropertyStringValue(dsAttrs, LDAPv3Config_AUTHID);
                type = (getPropertyStringValue(dsAttrs, 
                    LDAPv3Config_LDAP_SSL_ENABLED) == null) ? "ldap" : "ldaps";
                String [] params = {curr};
                toolOutWriter.printMessage("cnt-svr-current", params);
                doConnect(ldapServer, ldapPort, type, id, pwd);
            }
        } else {
            //DS server list is empty
            toolOutWriter.printMessage("cnt-ds-no-entries");
        }
    }
    
    private String getPropertyStringValue(Map configParams, String key) {
        String value = null;
        Set valueSet = (Set) configParams.get(key);
        if (valueSet != null && !valueSet.isEmpty()) {
            value = (String) valueSet.iterator().next();
        }
        return value;
    }  
    
    private void doConnect(
        String ldapServer,
        int ldapPort,
        String type,
        String id,
        String pwd
    ) {
        LDAPConnection ldc = getLDAPConnection(ldapServer,
            ldapPort, type, id, pwd);
        if (ldc != null) {
            toolOutWriter.printMessage("cnt-svr-connect-status",
                new String[] {"OK"});
        } else {
            toolOutWriter.printMessage("cnt-svr-connect-status",
                new String[] {"FAILED"});
        }
    }
}
