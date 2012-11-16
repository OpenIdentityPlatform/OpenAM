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
 * $Id: InitializeSystem.java,v 1.3 2008/08/07 17:22:01 arviranga Exp $
 *
 */

package com.sun.identity.cli;

import com.iplanet.am.util.AdminUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.authentication.internal.InvalidAuthContextException;
import com.sun.identity.authentication.internal.server.SMSAuthModule;
import com.sun.identity.setup.Bootstrap;
import com.sun.identity.setup.BootstrapData;
import com.sun.identity.setup.SetupConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Properties;
import javax.security.auth.login.LoginException;

public class InitializeSystem {
    private String rootsuffix;
    private BootstrapData bData;
    private String instanceName;

    public InitializeSystem() 
        throws FileNotFoundException, IOException, UnsupportedEncodingException,
        LDAPServiceException {
        String basedir = System.getProperty(Bootstrap.JVM_OPT_BOOTSTRAP);
        load(basedir);
    }

    private void load(String basedir) 
        throws FileNotFoundException, IOException, UnsupportedEncodingException,
        LDAPServiceException {
        if (!basedir.endsWith(File.separator)) {
            basedir = basedir + File.separator;
        }

        String amConfigProperties = basedir +
            SetupConstants.AMCONFIG_PROPERTIES;
        File file = new File(amConfigProperties);
        if (file.exists()) {
            Properties prop = new Properties();
            InputStream propIn = new FileInputStream(amConfigProperties);
            try {
                prop.load(propIn);
            } finally {
                propIn.close();
            }
            SystemProperties.initializeProperties(prop);
        } else {
            bData = new BootstrapData(basedir);
            bData.initSMS(false);
            AdminUtils.initialize();
            SMSAuthModule.initialize();

            rootsuffix = bData.getBaseDN();
            instanceName = bData.getInstanceName();
        }
    }
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public String getServerConfigXML() 
        throws UnsupportedEncodingException, MalformedURLException {
        return bData.getServerConfigXML(true);
    }
    
    public String getRootSuffix() {
        return rootsuffix;
    }
    
    public SSOToken getSSOToken(String bindPwd) 
        throws LoginException, InvalidAuthContextException {
        SSOToken ssoToken = null;
        String userRootSuffix = bData.getUserBaseDN();
        AuthPrincipal principal = new AuthPrincipal(
            "cn=dsameuser,ou=DSAME Users," + userRootSuffix);
        AuthContext ac = new AuthContext(
            userRootSuffix, principal, bindPwd.toCharArray());
        if (ac.getLoginStatus() == AuthContext.AUTH_SUCCESS) {
            ssoToken = ac.getSSOToken();
        }
        return ssoToken;
    }
}
