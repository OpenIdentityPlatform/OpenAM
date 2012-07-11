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
 * $Id: WebAgentConfigReport.java,v 1.1 2008/11/22 02:41:20 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.reports;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Properties;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.agent.AgentConstants;
import com.sun.identity.diagnostic.plugin.services.common.ClientBase.RESTResponse;
import com.sun.identity.diagnostic.plugin.services.common.ClientBase;
import com.sun.identity.shared.debug.Debug;


/**
 * This is a supporting class to print Web Agent configuration details
 */
public class WebAgentConfigReport extends ClientBase implements
    ReportConstants, AgentConstants, IGenerateReport {
    
    private IToolOutput toolOutWriter;
    private static String ssoToken = null;
    
    public WebAgentConfigReport() {
    }
    
    /**
     * Web agent report generation.
     *
     * @param path Path to the configuration directory.
     */
    public void generateReport(String path) {
        toolOutWriter = ServerReportService.getToolWriter();
        generateWebAgentReport(path);
    }
    
    private void generateWebAgentReport(String path) {
        if (loadConfig(path) && validateBootProperties()) {
            ssoToken = getAppSSOTokenStr();
            if (ssoToken != null) {
                processAgents(ssoToken);
            } else {
                toolOutWriter.printError("rpt-agt-auth-to-svr-fail" ,
                    new String[] {SystemProperties.get(
                        WEB_AGENT_USER_NAME)});
            }
        } else {
            toolOutWriter.printStatusMsg(false, "rpt-agt-gen");
        }
    }
    
    private String getAppSSOTokenStr() {
        String token = null;
        try {
            String encPwd = SystemProperties.get(WEB_AGENT_SECRET);
            String deskeystr = SystemProperties.get(WEB_AGENT_KEY);
            String pwd = (String)getAppPassword(encPwd, deskeystr);
            String uName = SystemProperties.get(WEB_AGENT_USER_NAME);
            String authURL = getBaseUrlStr(
                SystemProperties.get(WEB_AGENT_NAMING_URL)) +
                "/identity/authenticate";
            String data1 = "username=" + uName + "&password=" + pwd;
            // authenticate
            RESTResponse response = callServiceURL(authURL, data1);
            if (response.getResponseCode() != 200) {
                toolOutWriter.printStatusMsg(false, "rpt-agt-auth-to-svr");
            } else {
                token = (String) response.getContent().get(0);
                token = token.substring(9);
                toolOutWriter.printStatusMsg(true, "rpt-agt-auth-to-svr");
            }
        } catch (Exception e) {
            toolOutWriter.printStatusMsg(false, "rpt-agt-auth-to-svr");
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
            if (ValidateConnectToServer(nURLStr)) {
                valid = true;
            } else {
                //Fatal error cannot proceed: Naming URL
                toolOutWriter.printError("rpt-agt-fatal-connect-err" ,
                    new String[] {nURLStr});
            }
        }
        return valid;
    }
    
    private boolean ValidateServer(String nURLStr) {
        boolean valid = false;
        if (nURLStr.contains(AGENT_SERVICE_TAG)) {
            //entry is not swapped
            toolOutWriter.printError("rpt-agt-invalid-naming-url",
                new String[] {nURLStr});
            toolOutWriter.printStatusMsg(false, "rpt-agt-naming-url-validation");
        } else {
            toolOutWriter.printStatusMsg(true, "rpt-agt-naming-url-validation");
            valid = true;
        }
        return valid;
    }
    
    private boolean ValidateConnectToServer(String nURLStr) {
        boolean valid = false;
        try {
            URL url = new URL(nURLStr);
            if (isValidHost(url.getHost())) {
                if (canConnectToServer(nURLStr)) {
                    if (isServerURLUp(url)) {
                        valid = true;
                    }
                }
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "WebAgentConfigReport.ValidateConnectToServer: " +
                "Exception occured in connecting to server", e);
        }
        return valid;
    }
    
    private boolean isServerURLUp(URL sURL) {
        boolean valid = true;
        if (isServerRunning(sURL)) {
            toolOutWriter.printMessage("rpt-agt-naming-url-connect",
                new String[] {"OK"});
        } else {
            toolOutWriter.printMessage("rpt-agt-naming-url-connect",
                new String[] {"FAILED"});
            valid = false;
        }
        return valid;
    }
    
    private boolean canConnectToServer(String  svrName) {
        boolean connect = true;
        try {
            if (connectToServer(svrName)) {
                toolOutWriter.printMessage("rpt-agt-svr-connect-status",
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
            toolOutWriter.printMessage("rpt-agt-svr-connect-status",
                new String[] {"FAILED"});
        }
        return connect;
    }
    
    public void processAgents(String token) {
        try {
            String restUrl = getBaseUrlStr(
                SystemProperties.get(WEB_AGENT_NAMING_URL)) + "/identity/read";
            String data4 =
                "name=" + URLEncoder.encode(
                SystemProperties.get(WEB_AGENT_USER_NAME)) +
                "&attributes_names=objecttype" +
                "&attributes_values_objecttype=Agent" +
                "&admin=" + URLEncoder.encode(token);
            RESTResponse response = callServiceURL(restUrl, data4);
            Properties agtProp = processEntries(response.toString());
            if (!agtProp.isEmpty()) {
                toolOutWriter.printStatusMsg(true, "rpt-agt-loading-cfg-prop");
                SystemProperties.initializeProperties(agtProp);
                toolOutWriter.printMessage(PARA_SEP);
                toolOutWriter.printMessage("rpt-agt-name",
                    new String[] {SystemProperties.get(
                        WEB_AGENT_USER_NAME)});
                toolOutWriter.printMessage("rpt-agt-cfg-type",
                    new String[] {SystemProperties.get(AGENT_CFG_TYPE)});
                toolOutWriter.printMessage(PARA_SEP + "\n");
                for (Enumeration e = agtProp.propertyNames();
                    e.hasMoreElements(); ) {
                    String key = (String)e.nextElement();
                    String value = (String)agtProp.getProperty(key);
                    String[] params = {key, value};
                    toolOutWriter.printMessage("rpt-svr-print-prop",
                        params);
                }
            } else {
                toolOutWriter.printStatusMsg(false, "rpt-agt-loading-cfg-prop");
            }
        } catch (Exception ex) {
            Debug.getInstance(DEBUG_NAME).error(
                "WebAgentConfigReport.processAgents: " +
                "Exception occured in processing agent configuration", ex);
        }
    }
}
