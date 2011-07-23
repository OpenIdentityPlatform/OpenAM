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
 * $Id: ServerConnectValidation.java,v 1.1 2008/11/22 02:41:20 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.connect;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;
import com.sun.identity.shared.debug.Debug;


/**
 * This is a supporting class to validate the connectivity
 * to the server.
 */
public class ServerConnectValidation extends ServiceBase implements
    IConnectService {
    
    private IToolOutput toolOutWriter;
    
    public ServerConnectValidation() {
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
                processServerEntries(ssoToken);
            } else {
                toolOutWriter.printError("cnt-auth-msg");
            }
        } else {
            toolOutWriter.printStatusMsg(false, "cnt-svr-msg");
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
    
    private void processServerEntries(SSOToken ssoToken) {
        try {
            Set serverSet = ServerConfiguration.getServers(ssoToken);
            for (Iterator items = serverSet.iterator(); items.hasNext();) {
                String server = (String) items.next();
                toolOutWriter.printMessage("cnt-process-svr-ins",
                    new String[] {server});
                checkServerSites(ssoToken, server);
            }
        } catch (Exception e) {
             Debug.getInstance(DEBUG_NAME).error(
                "ServerConnectValidation.processServerEntries: " +
                "Exception during processing server entries ", e);
        }
    }
    
    private void canConnectToServer(String svrName) {
        try {
            connectToServer(svrName);
            toolOutWriter.printMessage("cnt-svr-connect-status",
                new String[] {"OK"});
        } catch (javax.net.ssl.SSLHandshakeException sslex) {
            toolOutWriter.printMessage("cnt-svr-connect-ssl-fail",
                new String[] {"FAILED"});
            toolOutWriter.printMessage("cnt-svr-connect-status",
                new String[] {"FAILED"});
        } catch (Exception e) {
            toolOutWriter.printMessage("cnt-svr-connect-status",
                new String[] {"FAILED"});
        }
    }
    
    private void checkServerSites(SSOToken ssoToken, String svrInstance) {
        Set<String> allHosts = new HashSet<String>();
        allHosts.add(svrInstance);
        try {
            String site = ServerConfiguration.getServerSite(
                ssoToken, svrInstance);
            if (site != null && site.length() > 0) {
                allHosts.add(SiteConfiguration.getSitePrimaryURL(
                    ssoToken, site));
                Set failoverURLs =
                    SiteConfiguration.getSiteSecondaryURLs(ssoToken,
                    site);
                if ((failoverURLs != null) && !failoverURLs.isEmpty()){
                    for (Iterator<String> i = failoverURLs.iterator();
                        i.hasNext(); ) {
                        allHosts.add(i.next());
                    }
                }
            }
            if ((allHosts != null) && !allHosts.isEmpty()) {
                for (Iterator<String> i = allHosts.iterator();
                    i.hasNext(); ) {
                    String hName = i.next();
                    toolOutWriter.printMessage("cnt-svr-current",
                        new String[] {hName});
                    canConnectToServer(hName);
                }
            }
        } catch (Exception e) {
             Debug.getInstance(DEBUG_NAME).error(
                "ServerConnectValidation.checkServerSites: " +
                "Exception during processing site entries ", e);
        }
    }
}
