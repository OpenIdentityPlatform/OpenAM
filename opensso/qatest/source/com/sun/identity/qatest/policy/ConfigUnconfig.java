/* The contents of this file are subject to the terms
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
 * $Id: ConfigUnconfig.java,v 1.6 2009/01/27 00:11:05 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.policy;

import com.sun.identity.qatest.common.TestCommon;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * This class configures the Policy service if the UMdatastore is dirServer.
 * Policy service is configured based on the UMDatastore properties.
 * This class also starts and stops the notification server.
 */
public class ConfigUnconfig extends TestCommon {
  
    private Map notificationMap;
    
    /**
     * Creates a new instance of ConfigUnconfig
     */
    public ConfigUnconfig() {
        super("ConfigUnconfig");
    }
    
    /**
     * Start the notification (jetty) server for getting notifications from the
     * server.
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void startServer()
    throws Exception {
        entering("startServer", null);
        notificationMap = startNotificationServer();
        exiting("startServer");
    }
    
    /**
     * Replaces the dn in the Policy xml files under the directory
     * <QATESTHOME>/xml/policy
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void replaceDn()
    throws Exception {
        entering("replaceDn", null);
        try {
            File directory;
            String[] files;
            String ext = "xml";
            String directoryName;
            String fileName;
            String absFileName;
            Map replaceVals = new HashMap();
            replaceVals.put("SM_SUFFIX", basedn);
            directoryName = getBaseDir() + fileseparator + "xml" +
                    fileseparator + "policy";
            directory = new File(directoryName);
            assert (directory.exists());
            files = directory.list();            
            for (int i = 0; i < files.length; i++) {
                fileName = files[i];
                if (fileName.endsWith(ext.trim())) {
                    absFileName = directoryName + fileseparator + fileName;
                    log(Level.FINEST, "replaceDn", "Replacing the file :" +
                            absFileName);
                    replaceString(absFileName, replaceVals, absFileName);
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "replaceDn", e.getMessage());
            e.printStackTrace();
        }
        exiting("replaceDn");
    }
    
    
    /**
     * Stop the notification (jetty) server for getting notifications from the
     * server.
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void stopServer()
    throws Exception {
        entering("stopServer", null);
        stopNotificationServer(notificationMap);
        exiting("stopServer");
    }
}
