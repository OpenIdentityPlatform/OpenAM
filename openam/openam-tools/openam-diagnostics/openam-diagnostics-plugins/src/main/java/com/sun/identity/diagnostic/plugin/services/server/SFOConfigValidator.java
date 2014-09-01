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
 * $Id: SFOConfigValidator.java,v 1.1 2008/11/22 02:41:22 ak138937 Exp $
 *
 */

/**
 * Portions copyright 2013 ForgeRock, Inc.
 */

package com.sun.identity.diagnostic.plugin.services.server;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ldap.api.CoreTokenConstants;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


/**
 * This is a supporting class to validate the Session Failover
 * configuration properties.
 */
public class SFOConfigValidator extends ServerConfigBase {
    
    private IToolOutput toolOutWriter;
    
    public SFOConfigValidator() {
    }
    
    /**
     * Validate the configuration.
     */
    public void validate(String path) {
        SSOToken ssoToken = null;
        toolOutWriter = ServerConfigService.getToolWriter();
        if (loadConfig(path)) {
            ssoToken = getAdminSSOToken();
            if (ssoToken != null) {
                processSFO(ssoToken);
            } else {
                toolOutWriter.printError("svr-auth-msg");
            }
        } else {
            toolOutWriter.printStatusMsg(false, "sfo-validate-cfg-prop");
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
    
    private void processSFO(SSOToken ssoToken) {
        boolean isSessionFailoverEnabled = false;
        String sessionStoreUserName = null;
        String sessionStorePassword = null;
        HashMap clusterMemberMap = new HashMap();
        int connectionMaxWaitTime = 5000; // in milli-second
        String jdbcDriverClass = null;
        String sessionRepositoryURL = null;
        int minPoolSize = 8;
        int maxPoolSize = 32;
        int maxWaitTimeForConstraint = 6000; // in milli-second
        boolean isPropertyNotificationEnabled = false;
        Set notificationProperties;
        toolOutWriter.printMessage("sfo-validate-cfg-prop");
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                amSessionService, ssoToken);
            ServiceConfig serviceConfig = scm.getGlobalConfig(null);
            String subCfgName = (ServerConfiguration.isLegacy(ssoToken)) ?
                getSessionServerURL() :
                SiteConfiguration.getSiteIdByURL(ssoToken,
                getSessionServerURL());
            ServiceConfig subConfig = serviceConfig.getSubConfig(subCfgName);            
            String[] params = {getSessionServerURL()};
            if (subConfig != null) {
                Map sessionAttrs = subConfig.getAttributes();
                boolean sfoEnabled = Boolean.valueOf(
                    CollectionHelper.getMapAttr(
                    sessionAttrs, CoreTokenConstants.IS_SFO_ENABLED, "false")
                    ).booleanValue();
                if (sfoEnabled) {
                    isSessionFailoverEnabled = true;
                    toolOutWriter.printMessage("sfo-cfg-enabled", params);
                    sessionStoreUserName = CollectionHelper.getMapAttr(
                        sessionAttrs, SESSION_STORE_USERNAME, "amsvrusr");
                    sessionStorePassword = CollectionHelper.getMapAttr(
                        sessionAttrs, SESSION_STORE_PASSWORD, "password");
                    connectionMaxWaitTime = Integer.parseInt(
                        CollectionHelper.getMapAttr(
                        sessionAttrs, CONNECT_MAX_WAIT_TIME, "5000"));
                    jdbcDriverClass = CollectionHelper.getMapAttr(
                        sessionAttrs, JDBC_DRIVER_CLASS, "");
                    sessionRepositoryURL = CollectionHelper.getMapAttr(
                        sessionAttrs, IPLANET_AM_SESSION_REPOSITORY_URL, "");
                    validateClusterList(sessionRepositoryURL);
                    minPoolSize = Integer.parseInt(CollectionHelper.getMapAttr(
                        sessionAttrs, MIN_POOL_SIZE, "8"));
                    maxPoolSize = Integer.parseInt(CollectionHelper.getMapAttr(
                        sessionAttrs, MAX_POOL_SIZE, "32"));




                    toolOutWriter.printMessage("sfo-cfg-prop-details");
                    String[] params1 = {sessionStoreUserName, sessionRepositoryURL,
                        Integer.toString(connectionMaxWaitTime),
                        Integer.toString(minPoolSize),
                        Integer.toString(maxPoolSize)};
                    toolOutWriter.printMessage("sfo-cfg-params", params1);
                } else {
                    toolOutWriter.printMessage("sfo-cfg-not-enabled", params);
                }
            } else {
                toolOutWriter.printMessage("sfo-cfg-not-enabled", params);
            }
        } catch (Exception ex) {
            toolOutWriter.printError("sfo-cfg-prop-notfound");
            toolOutWriter.printStatusMsg(false, "sfo-validate-cfg-prop");
            Debug.getInstance(DEBUG_NAME).error(
                "SFOConfigValidator.processSFO: " +
                "Exception in validating sfo configuration " +
                "information", ex);
        }
    }
    
    private void validateClusterList(String sessionRepositoryURL){
        boolean valid = true;
        if ((sessionRepositoryURL != null) && sessionRepositoryURL.length() > 0 ) {
            if (sessionRepositoryURL.indexOf(",") == -1) {
                int idx1 =  sessionRepositoryURL.indexOf(":");
                int idx2 =  sessionRepositoryURL.lastIndexOf(":");
                if (idx2 > idx1) {
                    toolOutWriter.printStatusMsg(false, "sfo-validate-jdbc-url");
                    toolOutWriter.printError("sfo-jdbc-url-invalid",
                        new String[] {sessionRepositoryURL});
                    valid = false;
                } else {
                    toolOutWriter.printMessage("sfo-jdbc-url-single");
                }
            }
            if (valid) {
                StringTokenizer st = new StringTokenizer(sessionRepositoryURL, ",");
                while (st.hasMoreTokens()) {
                    String str = st.nextToken();
                    if (str != null) {
                        if (isValidSyntax(str)) {
                            toolOutWriter.printStatusMsg(true,
                                "sfo-validate-jdbc-url");
                        } else {
                            toolOutWriter.printStatusMsg(false,
                                "sfo-validate-jdbc-url");
                        }
                        toolOutWriter.printMessage("sfo-jdbc-url",
                            new String[] {str});
                    }
                }
            }
        } else {
            toolOutWriter.printMessage("sfo-jdbc-url-empty");
            toolOutWriter.printStatusMsg(false, "sfo-validate-jdbc-url");
        }
    }
    
    private boolean isValidSyntax(String mqHost) {
        boolean valid = false;
        String[] params = {mqHost};
        if (mqHost.indexOf(":") == -1 ) {
            toolOutWriter.printError("sfo-jdbc-url-no-port", params);
        } else {
            String host = mqHost.substring(0, mqHost.indexOf(":"));
            String[] params1 = {host};
            if (!isValidHost(host)) {
                toolOutWriter.printError("sfo-jdbc-url-invalid-host", params1);
            } else {
                valid = true;
            }
            String port =  mqHost.substring(mqHost.indexOf(":") + 1);
            if (!isValidPort(port)){
                toolOutWriter.printError("sfo-jdbc-url-invalid-port",
                    new String[] {port});
                valid = false;
            } else {
                valid &= true;
            }
        }
        return valid;
    }
    
    private String getSessionServerURL() {
        boolean isSiteEnabled = false;
        String svrProtocol = SystemProperties.get(AM_SERVER_PROTOCOL);
        String svr = SystemProperties.get(AM_SERVER_HOST);
        String svrPort = SystemProperties.get(AM_SERVER_PORT);
        String svrURI = SystemProperties.get(
            AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        URL sessionServiceID = null;
        try {
            String sessionServerID = WebtopNaming.getServerID(svrProtocol,
                svr, svrPort, svrURI);
            isSiteEnabled = WebtopNaming.isSiteEnabled(svrProtocol,
                svr, svrPort, svrURI);
            if (isSiteEnabled) {
                sessionServerID = WebtopNaming.getSiteID(svrProtocol,
                    svr, svrPort, svrURI);
                sessionServiceID = new URL(WebtopNaming.getServerFromID(
                    sessionServerID));
            } else {
                sessionServiceID = new URL(WebtopNaming.getServerFromID(
                    sessionServerID));
            }
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "SFOConfigValidator.getSessionServerURL: " +
                "Exception in getting Session server URL", ex);
        }
        return sessionServiceID.toString();
    }
}
