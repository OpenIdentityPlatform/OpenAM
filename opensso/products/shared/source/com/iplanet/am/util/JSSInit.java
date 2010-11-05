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
 * $Id: JSSInit.java,v 1.2 2008/06/25 05:52:42 qcheng Exp $
 *
 */
package com.iplanet.am.util;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.security.Security;
import java.security.Provider;

import org.mozilla.jss.crypto.AlreadyInitializedException;
import org.mozilla.jss.CertDatabaseException;
import org.mozilla.jss.CryptoManager;
import org.mozilla.jss.crypto.CryptoToken;
import org.mozilla.jss.KeyDatabaseException;
import org.mozilla.jss.util.Password;

import com.iplanet.am.util.JSSPasswordCallback;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;

/**
 * A initialization class for JSS.
 * Check configuration and initialize JSS as configured.
 **/
public class JSSInit {

    /**
     * Default directory of security databases (current dir).
     */
    public static final String defaultDBdir = 
            System.getProperty("java.io.tmpdir"); 
    private static Debug debug = Debug.getInstance("amJSS");

    private static boolean initialized = false;
    
    private static CryptoManager cm = null;
    private static CryptoToken token = null;
    
    public static synchronized boolean
    initialize()
    {
        if (initialized) {
            return true;
        }
        
        final String method = "JSSInit.initialize";
        // JSS, initialize cert db
        String certdbDir = 
            SystemPropertiesManager.get("com.iplanet.am.admin.cli.certdb.dir");
        if (certdbDir == null) {
            certdbDir = defaultDBdir; 
        }

        String certdbPrefix = 
            SystemPropertiesManager.get(
                "com.iplanet.am.admin.cli.certdb.prefix");
        if (certdbPrefix == null) {
            certdbPrefix = "";
        }

        // Property to determine if JSS needs to installed with highest priority
        // at initialization of JSS. If not, it needs to added explicitly
        // at the end
        boolean donotInstallJSSProviderAt0 = 
                Boolean.valueOf(SystemPropertiesManager.get(
            "com.sun.identity.jss.donotInstallAtHighestPriority", 
            "false")).booleanValue();

        String passfile = 
            SystemPropertiesManager.get(
                "com.iplanet.am.admin.cli.certdb.passfile");

        String ocspCheckValue =
            SystemPropertiesManager.get(
                "com.sun.identity.authentication.ocspCheck");

        String fipsMode =
            SystemPropertiesManager.get(
                "com.sun.identity.security.fipsmode", null);

        if (ocspCheckValue != null && ocspCheckValue.trim().length() == 0) {
            ocspCheckValue = null;
        }

        boolean ocspCheck =
            (ocspCheckValue != null && ocspCheckValue.equalsIgnoreCase("true"));

        String responderURL = 
            SystemPropertiesManager.get(
                "com.sun.identity.authentication.ocsp.responder.url");
        if (responderURL != null && responderURL.trim().length() == 0) {
            responderURL = null;
        }

        String responderNickName = 
            SystemPropertiesManager.get(
                "com.sun.identity.authentication.ocsp.responder.nickname");
        if (responderNickName != null && 
                responderNickName.trim().length() == 0) {
            responderNickName = null;
        }

        if (debug.messageEnabled()) {
            debug.message(method + "certdbDir = " + 
                                   certdbDir);
            debug.message(method + "certdbPrefix = " + 
                                   certdbPrefix);
            debug.message(method + "certdbPassfile = " + 
                                   passfile);
            debug.message(method + "responderURL = " + 
                                   responderURL);
            debug.message(method + "responderNickName = " + 
                                   responderNickName);
            debug.message(method + "fipsMode = " + fipsMode);
        }

        String password = null;

        if (passfile != null) {
            try {
                FileInputStream fis = new FileInputStream(passfile);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);
                password = br.readLine();
            }
            catch (Exception ex) {
                if (debug.messageEnabled()) {
                    debug.message(method + "Unable to " +
                                           "read JSS password file " +
                                           passfile);
                }
            }
        }

        String keydbPrefix = certdbPrefix;

        String moddb = "secmod.db";

        try {
            cm = CryptoManager.getInstance();
        } catch (CryptoManager.NotInitializedException exp) {
            try {
                CryptoManager.InitializationValues iv = null;

                if (certdbPrefix.length() == 0) {
                    iv = new CryptoManager.InitializationValues(certdbDir);
                }
                else {
                    iv = new CryptoManager.InitializationValues(certdbDir, 
                                     certdbPrefix, keydbPrefix, moddb);
                }

                if (debug.messageEnabled()) {
                    debug.message(method + 
                            "output of Initilization values ");
                    debug.message(method +  "Manufacturer ID: " + 
                            iv.getManufacturerID());
                    debug.message(method + "Library: " + 
                            iv.getLibraryDescription());
                    debug.message(method + "Internal Slot: " + 
                            iv.getInternalSlotDescription());
                    debug.message(method + "Internal Token: " + 
                            iv.getInternalTokenDescription());
                    debug.message(method + "Key Storage Slot: "  + 
                            iv.getFIPSKeyStorageSlotDescription());
                    debug.message(method + "Key Storage Token: "  +
                            iv.getInternalKeyStorageTokenDescription());
                    debug.message(method + "FIPS Slot: " +
                            iv.getFIPSSlotDescription());
                    debug.message(method + "FIPS Key Storage: " +
                            iv.getFIPSKeyStorageSlotDescription());
                }
                
                if (fipsMode == null) {
                    iv.fipsMode = 
                        CryptoManager.InitializationValues.FIPSMode.UNCHANGED;
                } else if (fipsMode.equalsIgnoreCase("true")) {
                    iv.fipsMode = 
                        CryptoManager.InitializationValues.FIPSMode.ENABLED;
                } else if (fipsMode.equalsIgnoreCase("false")){
                    iv.fipsMode = 
                        CryptoManager.InitializationValues.FIPSMode.DISABLED;
                }
                
                iv.removeSunProvider = false;

                // Since we would like to support other JCE providers
                // for XML signature and encryption, need to check
                // if other providers are being used
                if (donotInstallJSSProviderAt0) {
                    iv.installJSSProvider = false;
                }

                // set open mode of the databases
                iv.readOnly= true;

                // enable OCSP
                iv.ocspCheckingEnabled = ocspCheck;
                // responderURL & responderNickname must both present
                if (ocspCheck &&
                    responderURL != null && responderNickName != null) {
                    iv.ocspResponderCertNickname = responderNickName;
                    iv.ocspResponderURL = responderURL;
                }

                CryptoManager.initialize(iv);
                
                // If JSS provider is not installed by default
                // add it to the list of JCE providers at the end
                if (donotInstallJSSProviderAt0) {
                     Provider provider = null;
                     try {
                         provider = (Provider) Class.forName(
                             "org.mozilla.jss.JSSProvider").newInstance();
                     } catch (ClassNotFoundException e) {
                         provider = (Provider) Class.forName(
                             "org.mozilla.jss.provider.Provider").newInstance();
                     }
                     Security.addProvider(provider);
                }
                
                cm = CryptoManager.getInstance();
                if (password != null) {
                    cm.setPasswordCallback(
                        new JSSPasswordCallback(password));
                }
                token = cm.getInternalKeyStorageToken();
                if (cm.FIPSEnabled()) {
                    token.login(cm.getPasswordCallback()); 
                }
                cm.setThreadToken(token);
                
                if (debug.messageEnabled()) {
                    if (cm.FIPSEnabled() == true ) {
                        debug.message(method + "FIPS enabled.");
                    } else {
                        debug.message(method + "FIPS not enabled.");
                    }
                }
                
                initialized = true;
            }
            catch (KeyDatabaseException kdbe) {
                debug.error(method + 
                    "Couldn't open the key database.", kdbe);
            }
            catch (CertDatabaseException cdbe) {
                debug.error(method + 
                    "Couldn't open the certificate database.", cdbe);
            }
            catch (AlreadyInitializedException aie) {
                debug.error(method + 
                    "CryptoManager already initialized.", aie);
            }
            catch (Exception e) {
                debug.error(method + 
                    "Exception occurred: ", e);
            }
        }
                
        return initialized;
    }
    
    /**
     * Returns <code>CryptoManager</code> object after initialize it.
     * 
     * @return <code>CryptoManager</code> object.
     */
    static public CryptoManager getCryptoManager() {
        if (cm == null) {
            initialize();
        }
        
        return cm;
    }

    /**
     * Returns <code>CryptoToken</code> object after initialize
     * <code>CryptoManager</code>.
     * 
     * @return <code>CryptoToken</code> object.
     */
    static public CryptoToken getCryptoToken() {
        if (cm == null) {
            initialize();
        }
        
        return token;
    }
}
