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
 * $Id: SAMLConfigValidator.java,v 1.2 2008/12/16 17:56:44 ak138937 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.diagnostic.plugin.services.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.diagnostic.base.core.log.IToolOutput;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;

/**
 * This is a supporting class to validate the SAML configuration
 * properties.
 */
public class SAMLConfigValidator extends ServerConfigBase {
   
    private IToolOutput toolOutWriter;
    
    public SAMLConfigValidator() {
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
                processSAML(ssoToken);
            } else {
                toolOutWriter.printError("svr-auth-msg"); 
            }
        } else {
            toolOutWriter.printStatusMsg(false, "saml-validate-cfg");
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
    
    private void processSAML(SSOToken ssoToken) {
        try {
            Map<String, String> defaultProp = loadPropertiesToMap(
                ServerConfiguration.getServerInstance(ssoToken,
                ServerConfiguration.DEFAULT_SERVER_CONFIG));
            Map<String, String> samlMap = detectChangedSAMLProperties(
                defaultProp);
            if (!samlMap.isEmpty()){
                toolOutWriter.printStatusMsg(true, "saml-detect-prop");
                if (!validateEntries(samlMap, defaultProp)) {
                    toolOutWriter.printError("saml-invalid-cfg-prop");
                }
            } else {
                toolOutWriter.printStatusMsg(false, "saml-detect-prop");
                toolOutWriter.printError("saml-prop-not-cfgd");
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "SAMLConfigValidator.processSAML: " +
                "Exception in validating configuration information", e);
            toolOutWriter.printError("saml-exp-cfg-info-validation",
                new String[] {e.toString()});
        }
    }
    
    private Map detectChangedSAMLProperties(Map defProp) {
        Map <String, String> changeMap = new HashMap();
        //Identify changed default properties
        Map defFileProp = loadPropertiesToMap(DEFAULT_SERVER_PROP);
        Set filekeys = defFileProp.keySet();
        Iterator filekeyIter = filekeys.iterator();
        while (filekeyIter.hasNext()) {
            String key = (String)filekeyIter.next();
            String value = (String)defFileProp.get(key);
            value = (value != null && value.length() > 0) ?
                value.trim() : "";
            String defValue = (String)defProp.get(key);
            defValue = (defValue != null && defValue.length() > 0) ?
                defValue.trim() : "";
            if (!(defValue.equals(value))) {
                changeMap.put(key, defValue);
            }
        }
        //Check for added properties
        Set propKeys = defProp.keySet();
        Iterator propKeyIter = propKeys.iterator();
        while (propKeyIter.hasNext()) {
            String key = (String)propKeyIter.next();
            if (!filekeys.contains(key)) {
                changeMap.put(key, (String)defProp.get(key));
            }
        }
        return changeMap;
    }
    
    private boolean validateEntries(
        Map<String,String> samlMap,
        Map<String,String> defProp
    ) {
        boolean valid = false;
        String privateKeyPass = null;
        String keystorePass = null;
        String fName = null;
        String cfgPath = SystemProperties.get(CONFIG_PATH);
        String deployURI = SystemProperties.get(
            AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        deployURI = deployURI.replaceAll("/", "");

        toolOutWriter.printMessage("saml-validate-cfg-prop");
        if (samlMap.containsKey(SAML_XMLSIG_STORE_PASS)) {
            fName = samlMap.get(SAML_XMLSIG_STORE_PASS);
        } else {
            fName = defProp.get(SAML_XMLSIG_STORE_PASS);
        }
        if (fName != null) {
            try {
                fName = fName.replaceAll(BASE_DIR_PATTERN, cfgPath).replaceAll(
                    DEP_URI_PATTERN, deployURI);
                toolOutWriter.printMessage("saml-file-loc" , new String[]{fName});
                File storePassFile = new File(fName);
                if (!storePassFile.exists()) {
                    toolOutWriter.printError(
                        "saml-storepwd-file-not-exist" , new String[]{fName});
                } else {
                    if ((keystorePass = readPwdFile(fName)) == null) {
                        toolOutWriter.printError("saml-invalid-storepassd");
                    } else {
                        valid = true;
                    }
                }
            } catch (Exception e) {
                toolOutWriter.printError(
                    "saml-invalid-storepass-file" , new String[]{fName});
            }
        } else {
            toolOutWriter.printError("saml-invalid-storepwd-prop");
        }
        toolOutWriter.printStatusMsg(valid, "saml-validate-storepwd-prop");
        valid = false;
        
        if (samlMap.containsKey(SAML_XMLSIG_KEYPASS)) {
            fName = samlMap.get(SAML_XMLSIG_KEYPASS);
        } else {
            fName = defProp.get(SAML_XMLSIG_KEYPASS);
        }
        if (fName != null) {
            try {
                fName = fName.replaceAll(BASE_DIR_PATTERN, cfgPath).replaceAll(
                    DEP_URI_PATTERN, deployURI);
                toolOutWriter.printMessage("saml-file-loc" , new String[]{fName});
                File keyPassFile = new File(fName);
                if (!keyPassFile.exists())  {
                    toolOutWriter.printError("saml-keypwd-file-not-exist",
                        new String[]{fName});
                } else {
                    if ((privateKeyPass = readPwdFile(fName)) == null) {
                        toolOutWriter.printError("saml-invalid-keypassd");
                    } else {
                        valid = true;
                    }
                }
            } catch (Exception e) {
                toolOutWriter.printError(
                    "saml-invalid-keypass-file",new String[]{fName});
            }
        } else {
            toolOutWriter.printError("saml-invalid-keypwd-prop");
        }
        toolOutWriter.printStatusMsg(valid, "saml-validate-keypwd-prop");
        valid = false;
        
        if (samlMap.containsKey(SAML_XMLSIG_KEYSTORE)) {
            fName = samlMap.get(SAML_XMLSIG_KEYSTORE);
        } else {
            fName = defProp.get(SAML_XMLSIG_KEYSTORE);
        }
        if (fName != null) {
            try {
                fName = fName.replaceAll(BASE_DIR_PATTERN, cfgPath).replaceAll(
                    DEP_URI_PATTERN, deployURI);
                toolOutWriter.printMessage("saml-file-loc" , new String[]{fName});
                File keystoreFile = new File(fName);
                if (!keystoreFile.exists()) {
                    toolOutWriter.printError(
                        "saml-keystore-file-not-exist" , new String[]{fName});
                } else {
                    if (keystorePass != null && keystorePass.length() > 0) {
                        KeyStore kstore = loadKeyStore(fName, keystorePass);
                        try {
                            if (kstore.size() > 0 ) {
                                toolOutWriter.printStatusMsg(true,
                                    "saml-loading-keystore");
                                validateKeyAlg(kstore, samlMap);
                                if (!samlMap.containsKey(SAML_XMLSIG_CERT_ALIAS)){
                                    samlMap.put(SAML_XMLSIG_CERT_ALIAS,  
                                        defProp.get(SAML_XMLSIG_CERT_ALIAS));
                                }
                                if (!isValidPrivateKey(kstore, privateKeyPass,
                                    samlMap.get(SAML_XMLSIG_CERT_ALIAS))){
                                    toolOutWriter.printError(
                                        "saml-invalid-keypassd");
                                } else {
                                    valid = true;
                                }
                            } else {
                                toolOutWriter.printError("saml-loading-failed");
                            }
                        } catch (Exception e) {
                            Debug.getInstance(DEBUG_NAME).error(
                                "SAMLConfigValidator.validateEntries: " +
                                "Exception in getting key store count.", e);
                        }
                    } else {
                        toolOutWriter.printError("saml-open-ks-failed");
                    }
                }
            } catch (Exception e) {
                toolOutWriter.printError("saml-invalid-keystore-file" ,
                    new String[] {fName});
            }
        } else {
            toolOutWriter.printError("saml-invalid-keystore-prop");
        }
        toolOutWriter.printStatusMsg(valid, "saml-validate-keystore-prop");
        return valid;
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
                    "SAMLConfigValidator.readPwdFile: " +
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
                "SAMLConfigValidator.loadKeyStore: " +
                "Exception in loading keystore", e);
        }
        return keyStore;
    }
    
    private Map loadKeyTable(KeyStore kStore) {
        Map<String, Object> keyTable = new HashMap<String, Object>();
        try {
            // create key to Certificate mapping
            for (Enumeration e=kStore.aliases();e.hasMoreElements();) {
                String alias = (String) e.nextElement();
                Certificate cert = getCertificate(kStore, alias);
                PublicKey pk = getPublicKey(kStore, alias);
                String key =
                    Base64.encode(pk.getEncoded());
                keyTable.put(key, cert);
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "SAMLConfigValidator.loadKeyTable: " +
                "Exception in loading keystore", e);
        }
        return keyTable;
    }
    
    /**
     * Validate the cert alias configured.
     *
     * @param kStore Keystore containing the certs
     * @param alias Certificate's name Alias
     * @return <code>true</code> if certificate alias is valid
     */
    private boolean isValidAlias(
        KeyStore kStore,
        String alias
    ) {
        boolean valid = false;
        try {
            valid = (kStore.containsAlias(alias)) ? true : false;
            if (!valid) {
                toolOutWriter.printError("saml-cert-alias-mismatch" ,
                    new String[] {alias});
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "SAMLConfigValidator.validateAlias: " +
                "Exception in accessing keystore", e);
        }
        return valid;
    }
    
    /**
     * Validate the xml signature algorithm.
     *
     * @param kStore Keystore to validate against
     * @param samlMap Map containing all SAML related properties
     */
    private void validateKeyAlg(
        KeyStore kStore,
        Map<String, String> samlMap
    ) {
        boolean valid = false;
        if (samlMap.containsKey(XML_SIG_ALG_PROP)) {
            try {
                String algName = samlMap.get(XML_SIG_ALG_PROP);
                if (algName != null && algName.length() > 0 ) {
                    if (!isValidSyntax(algName)) {
                        toolOutWriter.printError("saml-inv-xml-alg-val",
                            new String[] {algName});
                    } else {
                        String algValue = algName.substring(
                            algName.lastIndexOf("#")+1);
                        if (isValidAlias(kStore, samlMap.get(
                            SAML_XMLSIG_CERT_ALIAS))) {
                            toolOutWriter.printStatusMsg(true, 
                                "saml-validate-cert-alias");
                            String certAlg = getSigAlgValuefromCert(
                                kStore, samlMap.get(SAML_XMLSIG_CERT_ALIAS));
                            certAlg = parseSigAlgStr(certAlg);
                            if (!algValue.equalsIgnoreCase(certAlg)) {
                                String[] params = {algValue, certAlg};
                                toolOutWriter.printError("saml-xml-alg-mismatch",
                                    params);
                            } else {
                                valid = true;
                            }
                        } else {
                            toolOutWriter.printError("saml-cannot-validate-sig");
                        }
                    }
                } else {
                    toolOutWriter.printError("saml-empty-prop-val" ,
                        new String[] {XML_SIG_ALG_PROP});
                }
            } catch (Exception e) {
                Debug.getInstance(DEBUG_NAME).error(
                    "SAMLConfigValidator.validateKeyAlg: " +
                    "Exception in validating XML key algorithm", e);
            }
        } else {
            toolOutWriter.printError("saml-missing-prop" ,
                new String[] {XML_SIG_ALG_PROP});
        }
        toolOutWriter.printStatusMsg(valid, "saml-validate-xml-sig");
    }
    
    private String getPublicKeyValuefromCert(
        KeyStore keyStore,
        String certAlias
    ) {
        PublicKey pKey = getCertificate(keyStore, certAlias).getPublicKey();
        return pKey.getAlgorithm();
    }
    
    private String getSigAlgValuefromCert(
        KeyStore keyStore,
        String certAlias
    ) {
        return ((X509Certificate)
        getCertificate(keyStore, certAlias)).getSigAlgName();
    }
    
    private String parseSigAlgStr(String certSAlg) {
        List algNames = new ArrayList();
        StringTokenizer st = new StringTokenizer(certSAlg, "with");
        while (st.hasMoreTokens()) {
            algNames.add(st.nextToken());
        }
        return (algNames.get(1) + "-" + algNames.get(0));
    }
    
    private boolean isValidSyntax(String algName) {
        int algIdx = algName.indexOf("#");
        boolean match = algName.substring(0, algIdx).equalsIgnoreCase(
            XML_SIG_ALG_VAL);
        if (match) {
            int idx = algName.lastIndexOf("#");
            if (idx != -1) {
                match = (algName.length() != idx) ? true: false;
            }
        }
        return match;
    }  
    
    /**
     * Get the Certificate named certAlias.
     *
     * @param keyStore KeyStore to use for certificate retrieval
     * @param certAlias Certificate's name Alias
     * @return the Certificate, If the keystore
     *         doesn't contain such certAlias, return null.
     */
    private Certificate getCertificate(
        KeyStore keyStore,
        String certAlias
    ) {
        try {
            return keyStore.getCertificate(certAlias);
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "SAMLConfigValidator.getCertificate: " +
                "Exception in retriving certificate from keystore", e);
        }
        return null;
    }
    
    
    /**
     * Return java.security.PublicKey for the specified keyAlias
     *
     * @param keyStore KeyStore to use for certificate retrieval
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
                "SAMLConfigValidator.getPublicKey: " +
                "Exception in retriving public-key from keystore", e);
        }
        return pkey;
    }
    
    /**
     * Validate if the private key password is correct.
     */
    private boolean isValidPrivateKey(
        KeyStore kStore,
        String pvtKeyPass,
        String certAlias
    ) {
        boolean valid = false;
        try {
            if (isValidAlias(kStore, certAlias)) {
                java.security.PrivateKey key = null;
                key = (PrivateKey) kStore.getKey(certAlias,
                    pvtKeyPass.toCharArray());
                if (key != null) {
                    valid = true;
                }
            } else {
                toolOutWriter.printError("saml-cannot-validate-ks-pwd",
                    new String[] {certAlias});
            }
        } catch (Exception e) {
            Debug.getInstance(DEBUG_NAME).error(
                "SAMLConfigValidator.isValidPrivateKey: " +
                "Exception in reading private key from keystore", e);
        }
        return valid;
    }
}
