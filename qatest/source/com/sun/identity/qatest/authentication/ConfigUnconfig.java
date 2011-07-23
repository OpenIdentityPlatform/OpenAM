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
 * $Id: ConfigUnconfig.java,v 1.3 2009/06/02 17:08:18 cmwesley Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.authentication;

import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import java.util.Map;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * This class performs the following actions before the test suite ...
 * 1) starts the notification server
 * 2) generates the global authentication instances map from the
 * AuthenticationConfig.properties files.
 * 3) creates the authentication module instances from the global authentication
 * instances map
 * 4) tag-swaps the REDIRECT_URI tags in properties files
 *
 * This class performs the following actions after the test suite ...
 * 1) deletes the authentication module instances created before suite
 * 2) stops the notification server
 */
public class ConfigUnconfig extends TestCommon {
    
    AuthenticationCommon authCommon;
    private static String MODULE_NAME = "authentication";
    Map notificationMap;
    
    /**
     * Creates a new instance of ConfigUnconfig
     */
    public ConfigUnconfig() {
        super("ConfigUnconfig");
        authCommon = new AuthenticationCommon(MODULE_NAME);
    }
    
    /**
     * Start the notification (jetty) server for getting notifications from the
     * server.
     */
    @BeforeSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
            "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setupAuthTests()
    throws Exception {
        entering("startServer", null);
        notificationMap = startNotificationServer();
        replaceRedirectURIs("authentication");
        authCommon.createAuthInstancesMap();
        authCommon.createAuthInstances();
        exiting("startServer");
    }


    /**
     * Stop the notification (jetty) server for getting notifications from the
     * server.
     */
    @AfterSuite(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
            "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanupAuthTests()
    throws Exception {
        entering("stopServer", null);
        authCommon.deleteAuthInstances();
        stopNotificationServer(notificationMap);
        exiting("stopServer");
    }
}
