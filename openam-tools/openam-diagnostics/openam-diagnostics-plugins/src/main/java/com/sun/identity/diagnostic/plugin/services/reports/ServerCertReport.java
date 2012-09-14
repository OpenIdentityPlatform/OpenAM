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
 * $Id: ServerCertReport.java,v 1.1 2008/11/22 02:41:20 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.plugin.services.reports;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.diagnostic.plugin.services.common.ServiceBase;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;


/**
 * This is a supporting class to print cert store details
 */

public class ServerCertReport extends ServiceBase implements 
    ReportConstants, IGenerateReport {
    
    private IToolOutput toolOutWriter;
    private SSOToken ssoToken = null;
    
    public ServerCertReport() {
    }
    
    /**
     * Certificate store report generation.
     *
     * @param path Path to the configuration directory.
     */
    public void generateReport(String path) {
        toolOutWriter = ServerReportService.getToolWriter();
        generateCertReport(path);
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
    
    private void generateCertReport(String path) {
        if (loadConfig(path)) {
            ssoToken = getAdminSSOToken();
            if (ssoToken != null) {
                getStoreDetails(ssoToken);
            } else {
                toolOutWriter.printError("rpt-auth-msg");
            }
        } else {
            toolOutWriter.printStatusMsg(false, "rpt-cert-gen");
        }
    }
    
    private void getStoreDetails(SSOToken ssoToken) {
        String keystorePass = null;
        String fName = SystemProperties.get(SAML_XMLSIG_STORE_PASS);
        try {
            File storePassFile = new File(fName);
            if (!storePassFile.exists()) {
                toolOutWriter.printError(
                    "rpt-storepwd-file-not-exist" , new String[] {fName});
            } else {
                if ((keystorePass = readPwdFile(fName)) == null) {
                    toolOutWriter.printError("rpt-invalid-storepassd");
                }
            }
        } catch (Exception e) {
            toolOutWriter.printError(
                "rpt-invalid-storepass-file" , new String[]{fName});
        }
        //Read the keystore file
        fName = SystemProperties.get(SAML_XMLSIG_KEYSTORE);
        try {
            File keystoreFile = new File(fName);
            if (!keystoreFile.exists()) {
                toolOutWriter.printError(
                    "rpt-keystore-file-not-exist" , new String[]{fName});
            } else {
                if (keystorePass != null && keystorePass.length() > 0) {
                    KeyStore kstore = loadKeyStore(fName, keystorePass);
                    try {
                        if (kstore.size() > 0 ) {
                            toolOutWriter.printStatusMsg(true,
                                "rpt-loading-keystore");
                            Map keyTable = loadKeyTable(kstore);
                            Set keys = keyTable.keySet();
                            Iterator keyIter = keys.iterator();
                            while (keyIter.hasNext()) {
                                Object key = keyIter.next();
                                Object value = keyTable.get(key);
                                toolOutWriter.printMessage(key + " : " + 
                                    value.toString());
                            }
                        } else {
                            toolOutWriter.printError("rpt-loading-failed");
                        }
                    } catch (Exception e) {
                        Debug.getInstance(DEBUG_NAME).error(
                            "ServerCertReport.getStoreDetails: " +
                            "Exception in getting key store count.", e); 
                    }
                } else {
                    toolOutWriter.printError("rpt-invalid-storepassd");
                }
            }
        } catch (Exception e) {
            toolOutWriter.printError("rpt-invalid-keystore-file" ,
                new String[] {fName});
        }
    }
    
    /**
     * Get the Certificate named certAlias.
     *
     * @param keyStore Keystore to be used
     * @param certAlias Certificate's name Alias
     * @return the Certificate, If the keystore
     *       doesn't contain such certAlias, return null.
     */
    private Certificate getCertificate(
        KeyStore keyStore,
        String certAlias
    ) {
        try {
            return keyStore.getCertificate(certAlias);
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerCertReport.getCertificate: " +
                "Exception in retriving certificate from keystore", e);
        }
        return null;
    }
    
    private Map loadKeyTable(
        KeyStore kStore
    ) {
        Map<String, Object> keyTable = new HashMap<String, Object>();
        try {
            // create key to Certificate mapping
            for (Enumeration e=kStore.aliases();e.hasMoreElements();) {
                String alias = (String) e.nextElement();
                Certificate cert = getCertificate(kStore, alias);
                PublicKey pk = getPublicKey(kStore, alias);
                String key =
                    Base64.encode(pk.getEncoded());
                keyTable.put(alias, cert);
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerCertReport.loadKeyTable: " +
                "Exception in loading keystore", e);
        }
        return keyTable;
    }
    
    private String readPwdFile(String pfile) {
        String pwdStr = null;
        if (pfile != null) {
            try {
                FileInputStream fis = new FileInputStream(pfile);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);
                pwdStr = (String) AccessController.doPrivileged(
                    new DecodeAction(br.readLine()));
                fis.close();
            } catch (Exception e) {
                Debug.getInstance(DEBUG_NAME).error(
                    "ServerCertReport.readPwdFile: " +
                    "Exception in reading password file information", e);
            }
        }
        return pwdStr;
    }
    
    private KeyStore loadKeyStore(
        String ksFile,
        String storePass
    ) {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(ksFile);
            keyStore.load(fis, storePass.toCharArray());
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerCertReport.loadKeyStore: " +
                "Exception in loading keystore", e);
        }
        return keyStore;
    }
    
    private void printCertStoreDetails(Properties prop, String type){
        toolOutWriter.printMessage(PARA_SEP);
        toolOutWriter.printMessage(type);
        toolOutWriter.printMessage(PARA_SEP + "\n");
        for (Enumeration e=prop.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String val = (String) prop.getProperty(key);
            String[] params = {key, val};
            toolOutWriter.printMessage("rpt-svr-print-prop", params);
        }
        toolOutWriter.printMessage(SMALL_LINE_SEP + "\n");
    }
    
    /**
     * Return java.security.PublicKey for the specified keyAlias
     * 
     * @param keyStore KeyStore to be used
     * @param keyAlias Key alias name
     * @return PublicKey which matches the keyAlias, return null if
     *         the PublicKey could not be found.
     */
    private java.security.PublicKey getPublicKey(
        KeyStore keyStore,
        String keyAlias
    ) {
        if (keyAlias == null || keyAlias.length() == 0) {
            return null;
        }
        java.security.PublicKey pkey = null;
        try {
            java.security.cert.X509Certificate cert =
                (X509Certificate) keyStore.getCertificate(keyAlias);
            pkey = cert.getPublicKey();
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "ServerCertReport.getPublicKey: " +
                "Exception in retriving public key from keystore", e);
        }
        return pkey;
    }
}
