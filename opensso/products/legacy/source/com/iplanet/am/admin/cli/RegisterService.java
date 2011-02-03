/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RegisterService.java,v 1.2 2008/06/25 05:52:33 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.SMSException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.AccessController;


/**
 * Registers service during setup time.
 */
public class RegisterService {

    private static SSOToken adminToken = null;

    public static final String[] servicesToLoad = new String [] {
        "ums.xml", 
        "amAuthConfig.xml",
        "amAuthHTTPBasic.xml",
        "amAdminConsole.xml",
        "amAgent.xml",
        "amAgent70.xml",
        "idRepoService.xml",
        "amAuth.xml",
        "amAuthAD.xml",
        "amAuthAnonymous.xml",
        "amAuthCert.xml",
        "amAuthDataStore.xml",
        "amAuthJDBC.xml",
        "amAuthLDAP.xml",
        "amAuthMSISDN.xml",
        "amAuthMembership.xml",
        "amAuthNT.xml",
        "amAuthRadius.xml",
        "amAuthSAML.xml",
        "amAuthSafeWord.xml",
        "amAuthSecurID.xml",
        "amAuthUnix.xml",
        "amAuthWindowsDesktopSSO.xml",
        "amAuthenticationDomainConfig.xml",
        "amAuthnSvc.xml",
        "amClientData.xml",
        "amClientDetection.xml",
        "amDelegation.xml",
        "amDisco.xml",
        "amEntrySpecific.xml",
        "amFilteredRole.xml",
        "amG11NSettings.xml",
        "amLibertyPersonalProfile.xml",
        "amLogging.xml",
        "amNaming.xml",
        "amPasswordReset.xml",
        "amPlatform.xml",
        "amPolicy.xml",
        "amPolicyConfig.xml",
        "amProviderConfig.xml",
        "amRealmService.xml",
        "amSAML.xml",
        "amSOAPBinding.xml",
        "amSession.xml",
        "amUser.xml",
        "amWebAgent.xml",
        "identityLocaleService.xml"
        };

    public static final String[] dataToLoad = new String [] {
        "defaultDelegationPolicies.xml",
        "idRepoDefaults.xml",
        "SunAMClientData.xml",
        "mobileRequest.xml"
        };

    private static Debug debug = Debug.getInstance("amSetupServlet");

    public static void main(String[] args) throws Exception {
        if (debug.messageEnabled()) {
            debug.message("RegisterService.main: " +
                "Starting configuration setup");
        }
    }

    /**
     * Helper method to return Admin token.  
     *
     * @return Admin Token. 
     */
    public static SSOToken getAdminSSOToken() {
        if (adminToken == null) {
            adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        }
        return adminToken;
    }


    /**
     * Registers one or more services, defined by the XML input stream
     * that follows the SMS DTD.
     *
     * @throws SMSException if an error occurred while performing 
     *         the operation.
     * @throws SSOException if the user's single sign on token is 
     *         invalid.
     * @throws Exception if the service schema file cannot be found.
     */
    public static void regService()
        throws SMSException, SSOException, Exception 
    {
        SSOToken ssoToken = getAdminSSOToken();
        if (debug.messageEnabled()) {
            debug.message("RegisterService.regService: " +
                "Starting configuration setup");
        }

        // Now register services
        ServiceManager mgr = new ServiceManager(ssoToken);
        for (int i = 0; i < servicesToLoad.length; i++) {
            String serviceFileName = servicesToLoad[i];
            if (debug.messageEnabled()) {
                debug.message("RegisterService.regService: " +
                    "Attempting to load: " + serviceFileName);
            }
            InputStream serviceStream = null;
            InputStream servRawStream = null;
            try {
                servRawStream = 
                    RegisterService.class.
                        getClassLoader().getResourceAsStream(serviceFileName);
                byte [] buffer = new byte[1024];
                int bytesRead = 0;
                StringBuffer strBuff = new StringBuffer();
                while ((bytesRead = servRawStream.read(buffer)) != -1) {
                    strBuff.append(new String(buffer, 0, bytesRead));
                }
                String strXML=ServicesDefaultValues.tagSwap(strBuff.toString());
                serviceStream = (InputStream)new ByteArrayInputStream
                    (strXML.getBytes("UTF-8"));
                if (serviceStream == null) {
                    throw new Exception("RegisterService.regService: " +
                        "Failed to find " + serviceFileName);
                }
                System.setProperty("installTime", "true");
                mgr.registerServices(serviceStream);
            } catch (Exception ex) {
                debug.error("RegisterService.regService: " +
                    "Exception in registering service ", ex);
                throw ex;
            } finally {
                if (serviceStream != null) {
                    try {
                        serviceStream.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
                if (servRawStream != null) {
                    try {
                        servRawStream.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("RegisterService.regService: " +
                "Configuration load complete.");
        }
        mgr.clearCache();
    }

    /**
     * This method takes the name of XML file, process each 
     * request object one by one immediately after parsing.
     *
     * @param xmlBaseDir is the location of request xml files.
     * @throws AdminException if an error occurred while performing 
     *         the operation.
     */
    public static void processDataRequests(String xmlBaseDir) 
        throws AdminException 
    {
        SSOToken ssot = getAdminSSOToken();
        for (int i=0; i<dataToLoad.length; i++) {
            String dataFileName = dataToLoad[i];
            String xmlFile = xmlBaseDir + "/" + dataFileName;

            if (debug.messageEnabled()) {
                debug.message("RegisterService.processDataRequests: " +
                    "Attempting to load Datafile: " + xmlFile);
            }
            try {
                AMStoreConnection sconnection=null;
                AdminXMLParser dpxp = new AdminXMLParser();
                System.setProperty("installTime", "true");
                com.iplanet.am.admin.cli.AdminUtils.setSSOToken(ssot);
                dpxp.processAdminReqs(xmlFile, null, ssot, true, null);
            } catch (AdminException adminexp) {
                debug.error("RegisterService.processDataRequests: " +
                    "Exception in loading data files ", adminexp);
                throw adminexp;
            }
        }
    }
}

