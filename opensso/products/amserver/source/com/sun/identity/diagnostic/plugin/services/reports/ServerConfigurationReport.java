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
 * $Id: ServerConfigurationReport.java,v 1.2 2009/11/21 02:27:22 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.reports;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;
import com.sun.identity.shared.debug.Debug;


/**
 * This is a supporting class to print the server configuration
 * properties.
 */
public class ServerConfigurationReport extends ServiceBase implements
    ReportConstants, IGenerateReport {
    
    private IToolOutput toolOutWriter;
    private SSOToken ssoToken = null;
    
    public ServerConfigurationReport() {
    }
    
    /**
     * Server report generation.
     *
     * @param path Path to the configuration directory.
     */
    public void generateReport(String path) {
        toolOutWriter = ServerReportService.getToolWriter();
        generateSvrReport(path);
    }
    
    private boolean loadConfig(String path) {
        boolean loaded = false;
        try {
            if (!loadConfigFromBootfile(path).isEmpty()) {
                loaded = true;
            }
        } catch (Exception e) {
            toolOutWriter.printError("cannot-load-properties",
                new String[] {path});
        }
        return loaded;
    }
    
    private void generateSvrReport(String path) {
         if (loadConfig(path)) {
             ssoToken = getAdminSSOToken();
             if (ssoToken != null) {
                 //All the properties should be loaded at this point
                 Properties prop = SystemProperties.getAll();
                 Properties amProp = new Properties();
                 Properties sysProp = new Properties();
        
                 for (Enumeration e=prop.propertyNames(); 
                     e.hasMoreElements();) {
                     String key = (String) e.nextElement();
                     String val = (String) prop.getProperty(key);
                     if (key.startsWith(AM_PROP_SUN_SUFFIX) ||
                         key.startsWith(AM_PROP_SUFFIX) ||
                         key.startsWith(ENC_PWD_PROPERTY)) {
                         amProp.put(key, val);
                     } else {
                         sysProp.put(key, val);
                     }
                 }
                 printProperties(amProp, DEF_PROP);
                 printProperties(sysProp, SYS_PROP);
                 getInstanceProperties(ssoToken);
             } else {
                 toolOutWriter.printError("rpt-auth-msg");
             }
        } else {
             toolOutWriter.printStatusMsg(false, "rpt-svr-gen");
        }
    }
    
    private void getInstanceProperties(SSOToken ssoToken) {
        try {
            Set serverSet = ServerConfiguration.getServers(ssoToken);
            for (Iterator<String> items = serverSet.iterator();
                items.hasNext();) {
                String server = items.next();
                Properties sProps = ServerConfiguration.getServerInstance(
                    ssoToken, server);
                printProperties(sProps, server);
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerConfigurationReport.getInstanceProperties: " +
                "Exception", e);
        }      
    }
    
    private void printProperties(Properties prop, String type){
        toolOutWriter.printMessage(PARA_SEP);
        toolOutWriter.printMessage(type);
        toolOutWriter.printMessage(PARA_SEP + "\n");
        for (Enumeration e=prop.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String val = (String) prop.getProperty(key);
            if (key.equals(ENC_PWD_PROPERTY) ||
                key.equals(AM_SERVICES_SECRET)) {
                val= "xxxxxxxxxxxxxxxx";
            }
            String[] params = {key, val};
            toolOutWriter.printMessage("rpt-svr-print-prop", params);
        }
        toolOutWriter.printMessage(SMALL_LINE_SEP + "\n");
    }
}
