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
 * $Id: 
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.wss.provider.DiscoveryConfig;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.provider.STSConfig;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.qatest.common.SMSCommon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.testng.Reporter;

/**
 * This class contains common helper methods for WSFed tests
 */
public class WSSCommon extends TestCommon {
    
    /** Creates a new instance of WSSCommon */
    public WSSCommon() {
        super("WSSCommon");
    }
    
    /**
     * Registers webservice agent with Discovery service.
     */
    public void registerWSPWithDisco(String name, TrustAuthorityConfig
            taconfig)
            throws Exception {
        entering("registerWSPWithDisco", null);
        DiscoveryConfig discoConfig = (DiscoveryConfig)taconfig;
        ProviderConfig pc = ProviderConfig.getProvider(name, "WSP");
        discoConfig.registerProviderWithTA(pc, pc.getServiceType());
        exiting("registerWSPWithDisco");
    }
    
    /**
     * unregisters webservice agent with Discovery service.
     */
    public void unregisterWSPWithDisco(String name, TrustAuthorityConfig
            taconfig)
            throws Exception {
        entering("unregisterWSPWithDisco", null);
        DiscoveryConfig discoConfig = (DiscoveryConfig)taconfig;
        discoConfig.unregisterProviderWithTA(name);
        exiting("unregisterWSPWithDisco");
    }
    
    /**
     * Creates agent profile for webservice clients and providers and stsclient
     */
    public String createAgentProfile(String agentType, String
            strGblRB, String strLocRB, boolean isLibertyToken,
            TrustAuthorityConfig taconfig, String wscwspstsIndex, String replay)
            throws Exception {
        try {
            ProviderConfig pc = null ;
            TrustAuthorityConfig stsconfig = null;
            Map map = getAgentMap(strGblRB, strLocRB, agentType, wscwspstsIndex);
            log(Level.FINEST, "createAgentProfile",
                    "map : " + map);
            
            ResourceBundle rbp = ResourceBundle.getBundle("wss" +
                    fileseparator + strGblRB);
            log(Level.FINEST, "createAgentProfile",
                    "Agent type being created is : " + agentType);
            StringBuffer buff = new StringBuffer();
            buff.append(strGblRB)
            .append(".")
            .append(agentType.toLowerCase())
            .append(".");
            String prefix = buff.toString();
            String name = (String)map.get(WSSConstants.KEY_WSC_NAME);
            String secMechanism = (String)map.get(
                    WSSConstants.KEY_SEC_MECHANISM);
            boolean hasUserCredential = new Boolean((String)map.get(
                    WSSConstants.KEY_HAS_USER_CREDENTIAL)).booleanValue();
            boolean isRequestSigned = false;
            boolean isRequestEncrypted = false;
            boolean isResponseDecrypted = false;
            boolean isResponseSigVerified = false;
            String keyStoreFileName = "";
            String keyStorePassword = "";
            String keyPassword = "";
            boolean keepPrivateSecHeaderInMsg = false;
            String svcType = "";
            if (!agentType.equals("STSCLIENT")) {
                keepPrivateSecHeaderInMsg = new Boolean((String)map.get(
                        WSSConstants.KEY_PRIVATE_SEC_HEADERS)).booleanValue();
                svcType = (String)map.get(WSSConstants.KEY_SVC_TYPE);
            }
            String keystoreUsage = (String)map.get(
                    WSSConstants.KEY_KEYSTORE_USAGE);
            boolean isDefaultKeystore = (keystoreUsage.equals(
                    "default"))?true:false;
            String publicKeyAlias = (String)map.get(
                    WSSConstants.KEY_PUBLIC_KEY_ALIAS);
            String keyAlias = (String)map.get(
                    WSSConstants.KEY_PRIVATE_KEY_ALIAS);
            isRequestSigned = new Boolean((String)map.get(
                    WSSConstants.KEY_IS_REQ_SIGNED)).booleanValue();
            isRequestEncrypted = new Boolean((String)map.get(
                    WSSConstants.KEY_IS_REQ_ENCRYPTED)).booleanValue();
            isResponseDecrypted = new Boolean((String)map.get(
                    WSSConstants.KEY_IS_RESP_DECRYPTED)).booleanValue();
            isResponseSigVerified = new Boolean((String)map.get(
                    WSSConstants.KEY_IS_RESP_SIG_VERIFIED)).booleanValue();
            keyStoreFileName = (String)map.get(
                    WSSConstants.KEY_KEYSTORE_FILE);
            keyStorePassword = (String)map.get(
                    WSSConstants.KEY_KEYSTORE_PASSWORD);
            keyPassword = (String)map.get( WSSConstants.KEY_KEYPASSWORD);
            String signingRefType = (String)map.get(
                    WSSConstants.KEY_SIGNING_REF_TYPE);
            int encryptionStrength = new Integer((String)map.get(
                    WSSConstants.KEY_ENC_STRENGTH)).intValue();
            String encryptionAlgorithm = (String)map.get(
                    WSSConstants.KEY_ENC_ALGORITHM);
            String wspEndPoint = "default";
            if (agentType.equals("WSC")) {
                wspEndPoint = (String)map.get(WSSConstants.KEY_WSP_ENDPOINT);
            }
            if (agentType.equals("WSP")) {
                pc = ProviderConfig.getProvider(name, ProviderConfig.WSP);
            } else if  (agentType.equals("WSC")) {
                pc = ProviderConfig.getProvider(name, ProviderConfig.WSC);
            } else if  (agentType.equals("STS")) {
                log(Level.FINEST, "createAgentProfile", "stsconfig = " +
                        "TrustAuthorityConfig.getConfig(name, ");
                stsconfig = TrustAuthorityConfig.getConfig(name,
                        TrustAuthorityConfig.STS_TRUST_AUTHORITY);
            }
            if (agentType.equals("WSP") || agentType.equals("WSC")) {
                List listSec = new ArrayList();
                if (secMechanism.contains("&")) {
                    String strSplit[] = secMechanism.split("&");
                    for (int i = 0; i < strSplit.length; i++) {
                        listSec.add(strSplit[i]);
                    }
                } else {
                    listSec.add(secMechanism);
                }
                pc.setSecurityMechanisms(listSec);
                if (hasUserCredential) {
                    List listUsers = new ArrayList();
                    int noOfCred = new Integer(rbp.getString(prefix +
                            "noUserCredential")).intValue();
                    String strUsername;
                    String strPassword;
                    PasswordCredential cred;
                    for (int i = 0; i < noOfCred; i++) {
                        strUsername = rbp.getString(prefix +
                                "UserCredential" + i + ".username");
                        strPassword = rbp.getString(prefix +
                                "UserCredential" + i + ".password");
                        cred = new PasswordCredential(strUsername, strPassword);
                        listUsers.add(cred);
                    }
                    log(Level.FINEST, "createAgentProfile", "UserCredential: " +
                            listUsers);
                    if (map.containsKey(
                            WSSConstants.KEY_END_USER_CREDENTIALS)) {
                        String strEndUserCred = (String) map.get(
                                WSSConstants.KEY_END_USER_CREDENTIALS);
                        String strSplit[] = strEndUserCred.split("&");
                        cred = new PasswordCredential(strSplit[0],
                                strSplit[1]);
                        listUsers.add(cred);
                    }
                    pc.setUsers(listUsers);
                }
                pc.setRequestSignEnabled(isRequestSigned);
                pc.setRequestEncryptEnabled(isRequestEncrypted);
                pc.setResponseSignEnabled(isResponseSigVerified);
                pc.setResponseEncryptEnabled(isResponseDecrypted);
                pc.setDefaultKeyStore(isDefaultKeystore);
                if (!isDefaultKeystore) {
                    pc.setKeyStore(keyStoreFileName, keyStorePassword,
                            keyPassword);
                }
                pc.setPreserveSecurityHeader(keepPrivateSecHeaderInMsg);
                pc.setServiceType(svcType);
                pc.setKeyAlias(keyAlias);
                pc.setPublicKeyAlias(publicKeyAlias);
                pc.setWSPEndpoint("default");
                pc.setSigningRefType(signingRefType);
                pc.setEncryptionStrength(encryptionStrength);
                pc.setEncryptionAlgorithm(encryptionAlgorithm);
                pc.setWSPEndpoint(wspEndPoint);
                if (agentType.equals("WSP") && replay.equals("true")) {
                    pc.setAuthenticationChain("wssAuthChain");
                }
                if (agentType.equals("WSC")) {
                    if (isLibertyToken) {
                        //Trust AuthoritiyConfig
                        taconfig = TrustAuthorityConfig.getConfig("localDisco",
                                TrustAuthorityConfig.DISCOVERY_TRUST_AUTHORITY);
                        taconfig.setEndpoint(protocol + ":" + "//" + host +
                                ":" + port + uri + "/Liberty/disco");
                        TrustAuthorityConfig.saveConfig(taconfig);
                        pc.setTrustAuthorityConfig(taconfig);
                    }
                }
                ProviderConfig.saveProvider(pc);
                if (agentType.equals("WSP")) {
                    if (ProviderConfig.isProviderExists(name, 
                            ProviderConfig.WSP)) {
                        log(Level.FINEST, "createAgentProfile", "WSP provider " +
                                "successfully created, name:" + name);
                        ProviderConfig WSPpc = null ;
                        WSPpc = ProviderConfig.getProvider(name, 
                                ProviderConfig.WSP);
                        //displayAgentProfile (WSPpc);
                        Reporter.log("this is a test message");
                        Reporter.log("Displaying the attributes for the agent "
                                + WSPpc.getProviderName());
                        Reporter.log("Security Mechanisms " + 
                                WSPpc.getSecurityMechanisms());
                        Reporter.log("IsRequestSigned " + 
                                WSPpc.isRequestSignEnabled());
                        Reporter.log("IsRequestEncryptEnabled " + 
                                WSPpc.isRequestEncryptEnabled());
                        Reporter.log("isReponseSignEnabled" + 
                                WSPpc.isResponseSignEnabled());
                        Reporter.log("isResponseEncryptEnabled " + 
                                WSPpc.isResponseEncryptEnabled());
                        
                    } else {
                        log(Level.SEVERE, "createAgentProfile", 
                               "WSP provider " + "not created, name:" + name);
                        assert false;
                    }
                }
                if (agentType.equals("WSC")) {
                    if (ProviderConfig.isProviderExists(name, 
                            ProviderConfig.WSC)) {
                        log(Level.FINEST, "createAgentProfile", "WSC profile" +
                                "successfully created, name:" + name);
                        ProviderConfig WSCpc = null ;
                        WSCpc = ProviderConfig.getProvider(name, 
                                ProviderConfig.WSC);
                        Reporter.log("this is a test message");
                        Reporter.log("Displaying the attributes for agent " +
                                WSCpc.getProviderName());
                        Reporter.log("Security Mechanisms " + 
                                WSCpc.getSecurityMechanisms());
                        Reporter.log("IsRequestSigned " + 
                                WSCpc.isRequestSignEnabled());
                        Reporter.log("IsRequestEncryptEnabled " + 
                                WSCpc.isRequestEncryptEnabled());
                        Reporter.log("isReponseSignEnabled" + 
                                WSCpc.isResponseSignEnabled());
                        Reporter.log("isResponseEncryptEnabled " + 
                                WSCpc.isResponseEncryptEnabled());
                        
                        //displayAgentProfile (WSCpc);
                    } else {
                        log(Level.SEVERE, "createAgentProfile", "WSC profile" +
                                "not created, name:" + name);
                        assert false;
                    }
                }
            } else if (agentType.equals("STSCLIENT")) {
                stsconfig = TrustAuthorityConfig.getConfig(name,
                        TrustAuthorityConfig.STS_TRUST_AUTHORITY);
                log(Level.FINEST, "createAgentProfile", "initialised stsconfig");
                
                stsconfig.setName(name);
                List listSec = new ArrayList();
                listSec.add(secMechanism);
                stsconfig.setSecurityMechs(listSec);
                if (hasUserCredential) {
                    List listUsers = new ArrayList();
                    int noOfCred = new Integer(rbp.getString(prefix +
                            "noUserCredential")).intValue();
                    String strUsername;
                    String strPassword;
                    PasswordCredential cred;
                    for (int i = 0; i < noOfCred; i++) {
                        strUsername = rbp.getString(prefix + "UserCredential" +
                                i + ".username");
                        strPassword = rbp.getString(prefix + "UserCredential" +
                                i + ".password");
                        cred = new PasswordCredential(strUsername, strPassword);
                        listUsers.add(cred);
                        if (map.containsKey(
                                WSSConstants.KEY_END_USER_CREDENTIALS)) {
                            String strEndUserCred = (String) map.get(
                                    WSSConstants.KEY_END_USER_CREDENTIALS);
                            String strSplit[] = strEndUserCred.split("&");
                            cred = new PasswordCredential(strSplit[0],
                                    strSplit[1]);
                            listUsers.add(cred);
                        }
                    }
                    log(Level.FINEST, "createAgentProfile", "UserCredential: " +
                            listUsers);
                    stsconfig.setUsers(listUsers);
                }
                stsconfig.setRequestSignEnabled(isRequestSigned);
                stsconfig.setRequestEncryptEnabled(isRequestEncrypted);
                stsconfig.setResponseSignEnabled(isResponseSigVerified);
                stsconfig.setResponseEncryptEnabled(isResponseDecrypted);
                log(Level.FINEST, "createAgentProfile", "setting keyAlias = " +
                        keyAlias +"publicKeyAlias = " + publicKeyAlias);
                
                stsconfig.setKeyAlias(keyAlias);
                stsconfig.setPublicKeyAlias(publicKeyAlias);
                log(Level.FINEST, "createAgentProfile", "getKeyAlias" +
                        stsconfig.getKeyAlias());
                log(Level.FINEST, "createAgentProfile", "getPublicKeyAlias()" +
                        stsconfig.getPublicKeyAlias());
                stsconfig.setSigningRefType(signingRefType);
                stsconfig.setEncryptionStrength(encryptionStrength);
                stsconfig.setEncryptionAlgorithm(encryptionAlgorithm);
                String stsEndPoint = (String)map.get(
                        WSSConstants.KEY_STSENDPOINT);
                log(Level.FINEST, "createAgentProfile", "stsEndPoint: " +
                        stsEndPoint);
                stsconfig.setEndpoint(stsEndPoint);
                TrustAuthorityConfig.saveConfig(stsconfig);
                log(Level.FINEST, "createAgentProfile", "User ceredentials: " +
                        stsconfig.getUsers());
                //now display the newly created agent after saving in the server
                TrustAuthorityConfig stsconfig1 = null ;
                stsconfig1 = TrustAuthorityConfig.getConfig(name,
                        TrustAuthorityConfig.STS_TRUST_AUTHORITY);
                Reporter.log("************************");
                Reporter.log("Displaying the attributes for STSClient agent "
                        + stsconfig1.getName());
                Reporter.log("isRequestSigned" +
                        stsconfig1.isRequestSignEnabled());
                Reporter.log("isRequestEncrypted" +
                        stsconfig1.isRequestEncryptEnabled());
                Reporter.log("isResponseSigVerified" +
                        stsconfig1.isResponseSignEnabled());
                Reporter.log("isResponseDecrypted" +
                        stsconfig1.isResponseEncryptEnabled());
                Reporter.log("Security Mechanisms " +
                        stsconfig1.getSecurityMech());
                Reporter.log(" End Point" + stsconfig1.getEndpoint());
            }
            return (name);
        } catch(Exception e) {
            log(Level.SEVERE, "createAgentProfile", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Creates the XML for updating the Discovery service security bootstrap
     * entry.
     */
    public String getBootsrapDiscoEntry(String strSec) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"");
        sb.append("standalone=\"yes\"?>");
        sb.append("<DiscoEntry xmlns=" +
                "\"urn:com:sun:identityserver:liberty:ws:disco:discoentry\">");
        sb.append("<ResourceOffering xmlns=\"urn:liberty:disco:2003-08\">");
        sb.append("<ResourceID>");
        sb.append("</ResourceID>");
        sb.append("<ServiceInstance>");
        sb.append("<ServiceType>urn:liberty:disco:2003-08");
        sb.append("</ServiceType>");
        sb.append("<ProviderID>" +  protocol + ":" + "//" + host + ":" +
                port + uri + "/Liberty/disco");
        sb.append("</ProviderID>");
        sb.append("<Description>");
        sb.append("<SecurityMechID>urn:liberty:security:" + strSec);
        sb.append("</SecurityMechID>");
        sb.append("<Endpoint>" +  protocol + ":" + "//" + host + ":" + port +
                uri + "/Liberty/disco");
        sb.append("</Endpoint>");
        sb.append("</Description>");
        sb.append("</ServiceInstance>");
        sb.append("</ResourceOffering>");
        sb.append("</DiscoEntry>");
        log(Level.FINEST, "getBootsrapDiscoEntry", "Discovery Bootstrap" +
                " Resource Offering:" + sb.toString());
        return (sb.toString());
    }
    
    /**
     * Generates XML for end user authentication testcases.
     */
    public void generateUserAuthenticateXML( String username,
            String password, String xmlFile, String result, String strClientURL)
            throws Exception {
        
        FileWriter fstream = new FileWriter(xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);
        
        log(Level.FINEST, "generateUserAuthenticateXML", "Result: " + result);
        log(Level.FINEST, "generateUserAuthenticateXML", "Username: "
                + username);
        log(Level.FINEST, "generateUserAuthenticateXML", "Password: "
                + password);
        log(Level.FINEST, "generateUserAuthenticateXML", "XML File: "
                + xmlFile);
        
        out.write("<url href=\"" + strClientURL);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"Login\" IDButton=\"\" >");
        out.write(newline);
        out.write("<input name=\"IDToken1\" value=\"" + username + "\" />");
        out.write(newline);
        out.write("<input name=\"IDToken2\" value=\"" + password + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write("<form name=\"GetQuote\" IDButton=\"\" >");
        out.write(newline);
        out.write("<input name=\"symbol\" value=\"" + "JAVA" + "\" />");
        out.write(newline);
        out.write("<result text=\"" + result + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }
    
    /**
     * Generates a map of the wsc/wsp/stsclient agent profile attributes
     */
    protected Map getAgentMap(String strGblRB, String strLocRb,
            String agentType, String wscwspstsIndex)
            throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        StringBuffer buff = new StringBuffer();
        ResourceBundle gblRb = ResourceBundle.getBundle("wss" +
                fileseparator + strGblRB);
        ResourceBundle locRb = ResourceBundle.getBundle("wss" +
                fileseparator + strLocRb);
        buff.append(strGblRB)
        .append(".")
        .append(agentType.toLowerCase())
        .append(".");
        String prefix = buff.toString();
        
        map.put(WSSConstants.KEY_WSC_NAME,
                gblRb.getString(prefix + WSSConstants.KEY_WSC_NAME));
        map.put(WSSConstants.KEY_SEC_MECHANISM,
                gblRb.getString(prefix + WSSConstants.KEY_SEC_MECHANISM));
        //USER CRESENDTAILS USE FROM THE FILE DIRECTLY
        map.put(WSSConstants.KEY_HAS_USER_CREDENTIAL,
                gblRb.getString(prefix + WSSConstants.KEY_HAS_USER_CREDENTIAL));
        if (!agentType.equals("STSCLIENT")) {
            map.put(WSSConstants.KEY_PRIVATE_SEC_HEADERS,
                    gblRb.getString(prefix + 
                    WSSConstants.KEY_PRIVATE_SEC_HEADERS));
            map.put(WSSConstants.KEY_SVC_TYPE,
                    gblRb.getString(prefix + WSSConstants.KEY_SVC_TYPE));
        }
        map.put(WSSConstants.KEY_IS_REQ_SIGNED,
                gblRb.getString(prefix + WSSConstants.KEY_IS_REQ_SIGNED));
        map.put(WSSConstants.KEY_IS_REQ_ENCRYPTED,
                gblRb.getString(prefix + WSSConstants.KEY_IS_REQ_ENCRYPTED));
        map.put(WSSConstants.KEY_IS_RESP_DECRYPTED,
                gblRb.getString(prefix + WSSConstants.KEY_IS_RESP_DECRYPTED));
        map.put(WSSConstants.KEY_IS_RESP_SIG_VERIFIED,
                gblRb.getString(prefix + 
                WSSConstants.KEY_IS_RESP_SIG_VERIFIED));
        map.put(WSSConstants.KEY_KEYSTORE_FILE,
                gblRb.getString(prefix + WSSConstants.KEY_KEYSTORE_FILE));
        map.put(WSSConstants.KEY_KEYSTORE_PASSWORD,
                gblRb.getString(prefix + WSSConstants.KEY_KEYSTORE_PASSWORD));
        map.put(WSSConstants.KEY_KEYPASSWORD,
                gblRb.getString(prefix + WSSConstants.KEY_KEYPASSWORD));
        
        map.put(WSSConstants.KEY_KEYSTORE_USAGE,
                gblRb.getString(prefix + WSSConstants.KEY_KEYSTORE_USAGE));
        if (agentType.equals("WSC")) {
            map.put(WSSConstants.KEY_WSP_ENDPOINT,
                    gblRb.getString(prefix + WSSConstants.KEY_WSP_ENDPOINT));
        }
        map.put(WSSConstants.KEY_SIGNING_REF_TYPE,
                gblRb.getString(prefix + WSSConstants.KEY_SIGNING_REF_TYPE));
        map.put(WSSConstants.KEY_ENC_STRENGTH,
                gblRb.getString(prefix + WSSConstants.KEY_ENC_STRENGTH));
        map.put(WSSConstants.KEY_ENC_ALGORITHM,
                gblRb.getString(prefix + WSSConstants.KEY_ENC_ALGORITHM));
        map.put(WSSConstants.KEY_PUBLIC_KEY_ALIAS,
                gblRb.getString(prefix + WSSConstants.KEY_PUBLIC_KEY_ALIAS));
        map.put(WSSConstants.KEY_PRIVATE_KEY_ALIAS,
                gblRb.getString(prefix + WSSConstants.KEY_PRIVATE_KEY_ALIAS));
        
        if (agentType.equals("STSCLIENT")) {
            StringBuffer buf = new StringBuffer(gblRb.getString(prefix +
                    WSSConstants.KEY_STSENDPOINT));
            log(Level.FINEST, "getAgentMap", "KEY_STSENDPOINT: " +
                    buf.toString());
            String stsservtype = locRb.getString(strLocRb + wscwspstsIndex +
                    ".stsclient." + WSSConstants.KEY_STSSERVICETYPE);
            if (stsservtype.equals("remote")) {
                //form urls from StockQuoteSampleGlobal.RemoteSTSServiceURL
                String stsserviceproto = "";
                String stsservicednsname = "";
                String stsserviceport = "";
                String stsserviceuri = "";
                String strSTSSericeURL = gblRb.getString(
                        "StockQuoteSampleGlobal.RemoteSTSServiceURL");
                
            } else {
                replaceToken(buf, "@stsserviceproto@", serverProtocol);
                replaceToken(buf, "@stsservicednsname@", serverHost);
                replaceToken(buf, "@stsserviceport@", serverPort);
                replaceToken(buf, "@stsserviceuri@", serverUri);
                log(Level.FINEST, "getAgentMap", "KEY_STSENDPOINT: " +
                        buf.toString());
                map.put(WSSConstants.KEY_STSENDPOINT, buf.toString());
                buf = new StringBuffer(gblRb.getString(prefix +
                        WSSConstants.KEY_STSMEXENDPOINT));
                replaceToken(buf, "@stsserviceproto@", serverProtocol);
                replaceToken(buf, "@stsservicednsname@", serverHost);
                replaceToken(buf, "@stsserviceport@", serverPort);
                replaceToken(buf, "@stsserviceuri@", serverUri);
                log(Level.FINEST, "getAgentMap", "KEY_STSMEXENDPOINT: " +
                        buf.toString());
                map.put(WSSConstants.KEY_STSMEXENDPOINT, buf.toString());
            }
        }
        log(Level.FINEST, "getAgentMap", "Global map: " + map);
        buff = new StringBuffer();
        if (agentType.equals("WSC")) {
            buff.append(strLocRb)
            .append(wscwspstsIndex)
            .append(".")
            .append("wsc.Properties");
        } else if (agentType.equals("WSP")) {
            buff.append(strLocRb)
            .append(wscwspstsIndex)
            .append(".")
            .append("wsp.Properties");
        } else if (agentType.equals("STSCLIENT")) {
            buff.append(strLocRb)
            .append(wscwspstsIndex)
            .append(".")
            .append("stsclient.Properties");
        }
        
        String strSplit[] = (locRb.getString(buff.toString())).split("\\|");
        String strPropName = "";
        String strPropVal = "";
        Map<String, String> testMap = new HashMap<String, String>();
        for (int i = 0; i < strSplit.length; i++) {
            strPropName =  strSplit[i].substring(0, strSplit[i].indexOf("="));
            strPropVal = strSplit[i].substring(strSplit[i].indexOf("=") + 1,
                    strSplit[i].length());
            testMap.put(strPropName, strPropVal);
        }
        log(Level.FINEST, "getAgentMap", "test map: " + testMap);
        map.putAll(testMap);
        log(Level.FINEST, "getAgentMap", "New global map: " + map);
        //now create the file and add the properties to the test file
        BufferedWriter bw = null;
        
        try {
            bw = new BufferedWriter(new FileWriter("WSStestcases.dat", true));
            bw.write( "=======Begin of test Data For test case id=====" +
                    wscwspstsIndex);
            bw.newLine();
            bw.write("Final Properties for ****" + agentType + "****"  );
            bw.newLine();
            bw.write("New global map: " + map);
            bw.newLine();
            bw.write( "=======End of test Data For test case id====="
                    + wscwspstsIndex);
            bw.newLine();
            bw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (bw != null) try {
                bw.close();
            } catch (IOException ioe2) {
                
            }
        }
        
        return map;
    }
    
    /**
     * Updates the STS service running locally in the server
     */
    public void updateSTSServiceLocal(String strGblRB, String strLocRb,
            String stsClientIndex, String stsServiceIndex, SSOToken admintoken,
            String replay)
            throws Exception {
        
        Map<String, Set> map = new HashMap<String, Set>();
        StringBuffer buff = new StringBuffer();
        String strSecMechChk = "";
        ResourceBundle gblRb = ResourceBundle.getBundle("wss" +
                fileseparator + strGblRB);
        ResourceBundle locRb = ResourceBundle.getBundle("wss" +
                fileseparator + strLocRb);
        buff.append(strLocRb)
        .append(stsServiceIndex)
        .append(".stsservice.Properties");
        String prefix = buff.toString();
        SMSCommon smsc = new SMSCommon(admintoken);
        entering("updateSTSServiceLocal", null);
        //Create STSService map
        String strSplit[] = (locRb.getString(buff.toString())).split("\\|");
        String strPropName = "";
        String strPropVal = "";
        Set set;
        for (int i = 0; i < strSplit.length; i++) {
            strPropName =  strSplit[i].substring(0, strSplit[i].indexOf("="));
            strPropVal = strSplit[i].substring(strSplit[i].indexOf("=") + 1,
                    strSplit[i].length());
            if (strPropVal.contains("&")) {
                String strSecMech[] = strPropVal.split("&");
                set = new HashSet();
                for (int j = 0; j < strSecMech.length; j++) {
                    set.add(strSecMech[j]);
                }
                map.put(strPropName, set);
            } else {
                set = new HashSet();
                set.add(strPropVal);
                map.put(strPropName, set);
            }
        } if(replay.equals("true")){
            set = new HashSet();
            set.add("wssAuthChain");
            map.put("AuthenticationChain", set);}
        buff = new StringBuffer();
        buff.append("UserName:")
        .append(gblRb.getString(strGblRB + ".stsclient." +
                "UserCredential0.username"))
                .append("|UserPassword:")
                .append(gblRb.getString(strGblRB + ".stsclient." +
                "UserCredential0.password"));
        set = new HashSet();
        set.add(buff.toString());
        map.put(WSSConstants.KEY_STS_USERCRED, set);
        log(Level.FINEST, "updateSTSServiceLocal", "After creating map: " + 
                map);
        smsc.updateSvcSchemaAttribute("sunFAMSTSService", map, "Global");
        Thread.sleep(notificationSleepTime);
        log(Level.FINEST, "updateSTSServiceLocal", "SecurityMech: "
                + smsc.getAttributeValueFromSchema("sunFAMSTSService",
                "SecurityMech", "Global"));
        log(Level.FINEST, "updateSTSServiceLocal", "After updating service: "
                + smsc.getAttributeValueFromSchema("sunFAMSTSService",
                "SecurityMech", "Global"));
        log(Level.FINEST, "isRepsonseSign", "After updating service: "
                + smsc.getAttributeValueFromSchema("sunFAMSTSService",
                "isRepsonseSign", "Global"));
        log(Level.FINEST, "isRequestSign", "After updating service: "
                + smsc.getAttributeValueFromSchema("sunFAMSTSService",
                " isRequestSign", "Global"));
        log(Level.FINEST, "isRequestEncrypt:", "After updating service: "
                + smsc.getAttributeValueFromSchema("sunFAMSTSService",
                "isRequestEncrypt", "Global"));
        log(Level.FINEST, "isRepsonseDecrypt:" ,"After updating service: "
                + smsc.getAttributeValueFromSchema("sunFAMSTSService",
                "isRepsonseDecrypt", "Global"));
        
        //write code for checking if updation is proper
    }
    
    /**
     * Updates the
     * property - iplanet-am-auth-post-login-process-class
     * in the Authentication Service
     */
    public Set updateAuthService(SSOToken admintoken) throws Exception {
        Set set = new HashSet();
        Set origSet = new HashSet();
        SMSCommon smsc = new SMSCommon(admintoken);
        origSet = smsc.getAttributeValue("/", "iPlanetAMAuthService",
                "iplanet-am-auth-post-login-process-class",
                "Organization");
        if (!origSet.isEmpty()) {
            set = (HashSet)origSet;
        }
        set.add("com.sun.identity.authentication.spi.WSSReplayPasswd");
        smsc.updateSvcAttribute("/", "iPlanetAMAuthService",
                "iplanet-am-auth-post-login-process-class",
                set, "Organization");
        log(Level.FINEST, "updateAuthService", "After updating Auth->Core->" +
                "Authn post processing classes: " + smsc.getAttributeValue("/",
                "iPlanetAMAuthService",
                "iplanet-am-auth-post-login-process-class", "Organization"));
        return origSet;
        
    }
    
    /**
     * Creates a map of the current STS service in the local server
     */
    public Map<String, Set> getSTSServiceLocalAttributes(SSOToken admintoken)
    throws Exception {
        Map<String, Set> map;
        ServiceConfigManager scm;
        ServiceSchema ss;
        ServiceManager sm = new ServiceManager(admintoken);
        ServiceSchemaManager ssm = sm.getSchemaManager("sunFAMSTSService", 
                "1.0");
        log(Level.FINEST, "getSTSServiceLocalAttributes", "get " +
                "GlobalConfig");
        ss = ssm.getGlobalSchema();
        map = ss.getAttributeDefaults();
        log(Level.FINEST, "getSTSServiceLocalAttributes", "map : " +
                map);
        return map;
    }
    
    /**
     * Sets the STS Service in the local server with the values in the
     * map supplied
     */
    public void setSTSServiceLocalAttributes(SSOToken admintoken,
            Map<String, Set> map) throws Exception {
        log(Level.FINEST, "setSTSServiceLocalAttributes", "Entering");
        SMSCommon smsc = new SMSCommon(admintoken);
        smsc.updateSvcSchemaAttribute("sunFAMSTSService", map, "Global");
        //smsc.updateSvcAttribute(null, "sunFAMSTSService", map, "Global");
        log(Level.FINEST, "setSTSServiceLocalAttributes", "map after updation "
                + ": " + smsc.getAttributes("sunFAMSTSService", "/", "Global"));
    }
    /**
     * Retrieve the agent profile from Server and Print to the reporter
     * map supplied
     */
    public void displayAgentProfile(ProviderConfig agentPc)
    throws Exception {
        log(Level.FINEST, "displayAgentProfile", "Entering");
        Reporter.log("**********************" );
        Reporter.log("Displaying the attributes for the agent "
                + agentPc.getProviderName());
        Reporter.log("Security Mechanisms " +
                agentPc.getSecurityMechanisms());
        Reporter.log("IsRequestSigned " +
                agentPc.isRequestSignEnabled());
        Reporter.log("IsRequestEncryptEnabled " +
                agentPc.isRequestEncryptEnabled());
        Reporter.log("isReponseSignEnabled" +
                agentPc.isResponseSignEnabled());
        Reporter.log("isResponseEncryptEnabled " +
                agentPc.isResponseEncryptEnabled());       
        
    }
}
