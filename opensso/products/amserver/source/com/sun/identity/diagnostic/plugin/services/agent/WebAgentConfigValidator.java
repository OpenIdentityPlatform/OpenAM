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
 * $Id: WebAgentConfigValidator.java,v 1.1 2008/11/22 02:41:19 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.agent;

import java.util.Properties;
import java.net.URL;
import java.net.URLEncoder;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.common.ClientBase.RESTResponse;
import com.sun.identity.diagnostic.plugin.services.server.IServerValidate;


/**
 * This is a supporting class to validate the Web Agent configuration
 * properties.
 */
public class WebAgentConfigValidator extends  AgentBase implements 
    IServerValidate {
    
    private IToolOutput toolOutWriter;
    private static String ssoToken = null;
    
    public WebAgentConfigValidator() {
    }
    
    /**
     * Validate the configuration.
     */
    public void validate(String fName) {
        toolOutWriter = AgentConfigService.getToolWriter();
        if (loadConfig(fName) && validateBootProperties()) {
            ssoToken = getAppSSOTokenStr();
            if (ssoToken != null) {
                processAgents(ssoToken);
            } else {
                toolOutWriter.printError("agt-auth-to-svr-fail" ,
                    new String[] {SystemProperties.get(WEB_AGENT_USER_NAME)});
            }
        } else {
            toolOutWriter.printMessage("agt-fatal-err" ,
                new String[] {fName});
        }
    }
    
    private String getAppSSOTokenStr() {
        String token = null;
        try {
            String encPwd = SystemProperties.get(WEB_AGENT_SECRET);
            String deskeystr = SystemProperties.get(WEB_AGENT_KEY);
            String pwd = (String)getAppPassword(encPwd, deskeystr);
            String uName = SystemProperties.get(WEB_AGENT_USER_NAME);
            String authURL=
                getBaseUrlStr(SystemProperties.get(WEB_AGENT_NAMING_URL)) +
                "/identity/authenticate";
            String data1 = "username=" + uName + "&password=" + pwd;
            // authenticate
            RESTResponse response = callServiceURL(authURL, data1);
            
            if (response.getResponseCode() != 200) {
                toolOutWriter.printStatusMsg(false, "agt-auth-to-svr");
            } else {
                token = (String) response.getContent().get(0);
                token = token.substring(9);
                toolOutWriter.printStatusMsg(true, "agt-auth-to-svr");
            }
        } catch (Exception e) {
            toolOutWriter.printStatusMsg(false, "agt-auth-to-svr");
        }
        return token;
    }
    
    private boolean loadConfig(String fName) {
        boolean loaded = true;
        try {
            loadAgentConfigFromBootfile(fName);
        } catch (Exception e) {
            loaded = false;
            toolOutWriter.printError("cannot-load-boot-properties",
                new String[] {fName});
        }
        return loaded;
    }
    
    private boolean validateBootProperties() {
        boolean valid = false;
        //At this point the properties are already initilized
        String nURLStr = SystemProperties.get(WEB_AGENT_NAMING_URL);
        if (ValidateServer(nURLStr)) {
            if (getBaseUrlStr(nURLStr) != null ){
                if (ValidateConnectToServer(nURLStr)) {
                    valid = true;
                } else {
                    //Fatal error cannot proceed: Naming URL
                    toolOutWriter.printError("agt-fatal-connect-err" ,
                        new String[] {nURLStr});
                }
            } else {
                toolOutWriter.printError("agt-naming-url-corrupt",
                    new String[] {SystemProperties.get(
                        WEB_AGENT_NAMING_URL)});
            }
        }
        return valid;
    }
    
    private boolean ValidateServer(String nURLStr) {
        boolean valid = false;
        if (nURLStr.contains(AGENT_SERVICE_TAG)) {
            //entry is not swapped
            toolOutWriter.printError("agt-invalid-naming-url",
                new String[] {nURLStr});
            toolOutWriter.printStatusMsg(false, "agt-naming-url-syntax");
        } else {
            toolOutWriter.printStatusMsg(true, "agt-naming-url-syntax");
            valid = true;
        }
        return valid;
    }
    
    private boolean ValidateConnectToServer(String nURLStr) {
        boolean valid = false;
        try {
            URL url = new URL(nURLStr);
            if (!isValidHost(url.getHost())) {
                toolOutWriter.printError("agt-invalid-host-url",
                    new String[] {url.getHost()});
            } else {
                toolOutWriter.printMessage("agt-host-name-validation",
                    new String[] {url.getHost()});
                if (canConnectToServer(nURLStr)) {
                    if (isServerURLUp(url)) {
                        valid = true;
                    }
                }
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "WebAgentConfigValidator.ValidateConnectToServer: " +
                "Server connection Exception", e);
        }
        return valid;
    }
    
    private boolean isServerURLUp(URL sURL) {
        boolean valid = true;
        if (isServerRunning(sURL)) {
            toolOutWriter.printMessage("agt-naming-url-connect",
                new String[] {"OK"});
        } else {
            toolOutWriter.printMessage("agt-naming-url-connect",
                new String[] {"FAILED"});
            valid = false;
        }
        return valid;
    }
    
    private boolean canConnectToServer(String  svrName) {
        boolean connect = true;
        try {
            if (connectToServer(svrName)) {
                toolOutWriter.printMessage("agt-svr-connect-status",
                    new String[] {"OK"});
            } else {
                connect = false;
            }
        } catch (javax.net.ssl.SSLHandshakeException sslex) {
            connect = false;
        } catch (Exception e) {
            connect = false;
        }
        if (!connect) {
            toolOutWriter.printMessage("agt-svr-connect-status",
                new String[] {"FAILED"});
        }
        return connect;
    }
    
    public void processAgents(String token) {
        try {
            String restUrl =
                getBaseUrlStr(SystemProperties.get(WEB_AGENT_NAMING_URL)) +
                "/identity/read";
            String data4 =
                "name=" + URLEncoder.encode(
                SystemProperties.get(WEB_AGENT_USER_NAME)) +
                "&attributes_names=objecttype" +
                "&attributes_values_objecttype=Agent" +
                "&admin=" + URLEncoder.encode(token);
            RESTResponse response = callServiceURL(restUrl, data4);
            Properties agtProp =
                processEntries(response.toString());
            if (!agtProp.isEmpty()) {
                toolOutWriter.printStatusMsg(true, "agt-loading-cfg-prop");
                SystemProperties.initializeProperties(agtProp);
                validateProperties(agtProp);
            } else {
                toolOutWriter.printStatusMsg(false, "agt-loading-cfg-prop");
            }
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "WebAgentConfigValidator.processAgents: " +
                "Exception", ex);
            toolOutWriter.printStatusMsg(false, "agt-get-agt-profile");
        }
    }
    
    private void validateProperties(Properties agtProp) {
        checkServerTime();
        checkCDSSO(agtProp);
    }
    
    private void checkCDSSO(Properties agtProp) {
        boolean valid = true;
        boolean enable = isCDSSOenabled(agtProp);
        toolOutWriter.printMessage("agt-cdc-enable-check",
            new String[] {Boolean.valueOf(enable).toString()});
        String nURL = SystemProperties.get(WEB_AGENT_NAMING_URL);
        if (enable) {
            String cdcURL = (String)agtProp.get(AGENT_CDSSO_URL);
            toolOutWriter.printMessage("agt-cdc-check",
                new String[] {cdcURL});
            int idx = cdcURL.indexOf("=");
            if (!cdcURL.substring(idx + 1).contains(getBaseUrlStr(nURL))) {
                toolOutWriter.printError("agt-cdc-url-invalid",
                    new String[] {cdcURL});
                valid = false;
            }
            toolOutWriter.printStatusMsg(valid, "agt-cdc-url-validation");
        }
    }
    
    private void checkServerTime() {
        toolOutWriter.printMessage("agt-time-diff-check");
        try {
            Properties headers = getHeadersFromURL(
                SystemProperties.get(WEB_AGENT_NAMING_URL));
            toolOutWriter.printMessage("agt-time-on-svr",
                new String[] {headers.getProperty("Date")});
            toolOutWriter.printMessage("agt-time-on-client",
                new String[] {getLocalDateAsGMTString()});
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "WebAgentConfigValidator.checkServerTime: " +
                "Getting server time Exception", e);
            toolOutWriter.printStatusMsg(false, "agt-get-svr-time-fail");
        }
    }
}
