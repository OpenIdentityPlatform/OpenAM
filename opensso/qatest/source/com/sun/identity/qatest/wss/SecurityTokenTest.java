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
 * "Portions Copyrighted [year] [ of copyright owner]"
 *
 * $Id: SecurityTokenTest.java,v 1.7 2009/06/29 17:07:51 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.wss;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.wss.provider.DiscoveryConfig;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.wss.security.handler.SOAPRequestHandler;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.MessageFactory;
import javax.security.auth.Subject;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests securing the resquest for different security tokens. This
 * includes both WS-* and Liberty tokens. Further this also test configuring
 * providers using ProviderConfig class.
 */
public class SecurityTokenTest extends TestCommon {
    
    private int testIndex;
    private String strServiceURL;
    private String strWSCId;
    private String strWSPId;
    private ResourceBundle rbp;
    private SSOToken token;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private boolean isLibertyToken;
    private TrustAuthorityConfig taconfig;
    private String strLocRB = "SecurityTokenTest";
    private String strUser = "wsstestuser";
    
    /**
     * Default constructor. Creates admintoken and helper class instances.
     */
    public SecurityTokenTest()
    throws Exception{
        super("SecurityTokenTest");
        rbp = ResourceBundle.getBundle("wss" + fileseparator + strLocRB);
        token = getToken(adminUser, adminPassword, basedn);
        idmc = new IDMCommon();
        smsc = new SMSCommon(token);        
    }
    
    /**
     * Updates bootstrap security mechanism in Discovery service to null:X509
     * and creates users.
     */
    @BeforeSuite(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void createUser()
    throws Exception {
        try {
            Set set = new HashSet();
            set.add(getBootsrapDiscoEntry("2005-02:null:X509"));
            smsc.updateSvcSchemaAttribute(
                    "sunIdentityServerDiscoveryService",
                    "sunIdentityServerBootstrappingDiscoEntry", set, "Global");
            
            Map map = new HashMap();
            set = new HashSet();
            set.add(strUser);
            map.put("sn", set);
            set = new HashSet();
            set.add(strUser);
            map.put("cn", set);
            set = new HashSet();
            set.add(strUser);
            map.put("userpassword", set);
            set = new HashSet();
            set.add("Active");
            map.put("inetuserstatus", set);
            idmc.createIdentity(token, realm, IdType.USER, strUser, map);
        } catch(Exception e) {
            log(Level.SEVERE, "createUser", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Changes the runtime application user from UrlAccessAgent to amadmin
     */
    @BeforeSuite(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void changeRunTimeUser() throws
            Exception{
        
        try {
            SSOToken runtimeToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            log(Level.FINEST, "changeRunTimeUser", "Runtime user name is" +
                    runtimeToken.getPrincipal().getName());
            SSOTokenManager.getInstance().destroyToken(runtimeToken);
            
            try{
                SSOTokenManager.getInstance().refreshSession(runtimeToken);
            } catch (SSOException s) {
                log(Level.FINEST, "RefreshSession Exception", s.getMessage());
            }
            SystemProperties.initializeProperties("com.sun.identity." +
                    "agents.app.username", adminUser);
            SystemProperties.initializeProperties("com.iplanet.am." +
                    "service.password", adminPassword);
            
            SSOToken newToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            log(Level.FINEST, "changeRunTimeUser", "Runtime user name after " +
                    "change \n" + runtimeToken.getPrincipal().getName());
            
        } catch(Exception e) {
            log(Level.SEVERE, "changeRunTimeUser", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    
    /**
     * Creates agent profiles for web service providers and web service clients.
     */
    @Parameters({"testIdx"})
    @BeforeClass(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void setup(String testIdx)
    throws Exception {
        Object[] params = {testIdx};
        entering("setup", params);
        try {
            
            changeRunTimeUser();
            testIndex = new Integer(testIdx).intValue();
            
            strServiceURL = rbp.getString(strLocRB + testIndex + ".url");
            log(Level.FINEST, "setup", "Service URL: " + strServiceURL);
            
            isLibertyToken = new Boolean(rbp.getString(strLocRB + testIndex +
                    ".isLibertyToken")).booleanValue();
            log(Level.FINEST, "setup", "Is Liberty Token: " + isLibertyToken);
            
            strWSCId = createAgentProfile(testIndex, "WSC");
            log(Level.FINEST, "setup", "WSC Agent Id: " + strWSCId);
            
            strWSPId = createAgentProfile(testIndex, "WSP");
            log(Level.FINEST, "setup", "WSP Agent Id: " + strWSPId);
            
            if (isLibertyToken)
                registerWSPWithDisco(strWSPId);
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        exiting("setup");
    }
    
    /**
     * Creates and makes the actual web service call.
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void evaluateSecurityToken()
    throws Exception {
        entering("evaluateSecurityToken", null);
        StringBuffer soapMessage = getStockQuoteRequest("JAVA");
        log(Level.FINEST, "evaluateSecurityToken", "SOAPMessage before" +
                " security headers\n" + soapMessage);
        
        // Constrcut the SOAP Message
        MimeHeaders mimeHeader = new MimeHeaders();
        mimeHeader.addHeader("Content-Type", "text/xml");
        MessageFactory msgFactory = MessageFactory.newInstance();
        SOAPMessage message = msgFactory.createMessage(mimeHeader,
                new ByteArrayInputStream(soapMessage.toString().getBytes()));
        
        // Construct Access Manager's SOAPRquestHandler to
        // secure the SOAP message
        SOAPRequestHandler handler = new SOAPRequestHandler();
        HashMap params = new HashMap();
        // Use "wsc" as the configuration provider
        // In AM console the configuration would be "wscWSC" agent
        params.put("providername", strWSCId);
        handler.init(params);
        
        // Secure the SOAP message using "wsc" configuration
        Subject subj = new Subject();
        SSOToken locToken = getToken(strUser, strUser, basedn);
        if (isLibertyToken) {
            subj.getPrivateCredentials().add(locToken);
        }
        SOAPMessage encMessage = handler.secureRequest(message, subj,
                Collections.EMPTY_MAP);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encMessage.writeTo(baos);
        String request = baos.toString();
        log(Level.FINEST, "evaluateSecurityToken", "\nEncoded Message: \n" +
                request);
        
        // Send the SOAP message to Stock Quote Service
        log(Level.FINEST, "evaluateSecurityToken", "\nConnection URL:" +
                strServiceURL + "\n");
        String response = getStockQuote(strServiceURL, request);
        log(Level.FINEST, "evaluateSecurityToken", "\n\nStock Service" +
                " Response:\n" + response);
        if (response == null)
            assert false;
        
        exiting("evaluateSecurityToken");
    }
    
    /**
     * Deletest the agent profiles for webservice clients and providers.
     */
    @AfterClass(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        
        log(Level.FINEST, "cleanup", "WSC Agent Id: " + strWSCId + "WSC");
        log(Level.FINEST, "cleanup", "WSP Agent Id: " + strWSPId + "WSP");
        
        if (isLibertyToken) {
            unregisterWSPWithDisco("urn:wsp");
            ProviderConfig.deleteProvider("localDisco", "Discovery");
        }
        ProviderConfig.deleteProvider(strWSCId, ProviderConfig.WSC);
        ProviderConfig.deleteProvider(strWSPId, ProviderConfig.WSP);
        
        exiting("cleanup");
    }
    
    /**
     * Deletes users and resets bootstrap security mechanism in Discovery
     * service to null:null.
     */
    @AfterSuite(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void deleteUser()
    throws Exception {
        idmc.deleteIdentity(token, realm, IdType.USER, strUser);
        Set set = new HashSet();
        set.add(getBootsrapDiscoEntry("2003-08:null:null"));
        smsc.updateSvcSchemaAttribute("sunIdentityServerDiscoveryService",
                "sunIdentityServerBootstrappingDiscoEntry", set, "Global");
        destroyToken(token);
    }
    
    /**
     * Registers webservice agent with Discovery service.
     */
    private void registerWSPWithDisco(String name)
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
    private void unregisterWSPWithDisco(String name)
    throws Exception {
        entering("unregisterWSPWithDisco", null);
        DiscoveryConfig discoConfig = (DiscoveryConfig)taconfig;
        discoConfig.unregisterProviderWithTA(name);
        exiting("unregisterWSPWithDisco");
    }
    
    /**
     * Creates agent profile for webservice clients and providers.
     */
    private String createAgentProfile(int testIndex, String agentType)
    throws Exception {
        
        try {
            ProviderConfig pc = null ;
            String strIdx =  strLocRB + testIndex + "." +
                    agentType.toLowerCase() + ".";
            log(Level.FINEST, "createAgentProfile",
                    "Property file string index: " + strIdx);
            
            String name = rbp.getString(strIdx + "name");
            String secMechanism = rbp.getString(strIdx + "secMechanism");
            boolean hasUserCredential = new Boolean(rbp.getString(strIdx +
                    "hasUserCredential")).booleanValue();
            boolean isRequestSigned = new Boolean(rbp.getString(strIdx +
                    "isRequestSigned")).booleanValue();
            boolean isRequestEncrypted = new Boolean(rbp.getString(strIdx +
                    "isRequestEncrypted")).booleanValue();
            boolean isResponseSigVerified = new Boolean(rbp.getString(strIdx +
                    "isResponseSigVerified")).booleanValue();
            boolean isResponseDecrypted = new Boolean(rbp.getString(strIdx +
                    "isResponseDecrypted")).booleanValue();
            boolean keystoreUsage = new Boolean(rbp.getString(strIdx +
                    "keystoreUsage")).booleanValue();
            boolean keepPrivateSecHeaderInMsg = new Boolean(rbp.getString(strIdx +
                    "keepPrivateSecHeaderInMsg")).booleanValue();
            String svcType = rbp.getString(strIdx + "svcType");
            
            log(Level.FINEST, "createAgentProfile", "name: " + name);
            log(Level.FINEST, "createAgentProfile", "secMechanism: " +
                    secMechanism);
            log(Level.FINEST, "createAgentProfile", "hasUserCredential: " +
                    hasUserCredential);
            log(Level.FINEST, "createAgentProfile", "isRequestSigned: " +
                    isRequestSigned);
            log(Level.FINEST, "createAgentProfile", "isRequestEncrypted: " +
                    isRequestEncrypted);
            log(Level.FINEST, "createAgentProfile", "isResponseSigVerified: " +
                    isResponseSigVerified);
            log(Level.FINEST, "createAgentProfile", "isResponseDecrypted: " +
                    isResponseDecrypted);
            log(Level.FINEST, "createAgentProfile", "keystoreUsage: " +
                    keystoreUsage);
            log(Level.FINEST, "createAgentProfile", "keepPrivateSecHeaderInMsg: " +
                    keepPrivateSecHeaderInMsg);
            log(Level.FINEST, "createAgentProfile", "svcType: " +
                    svcType);
            log(Level.FINEST, "createAgentProfile", "isLibertyToken: " +
                    isLibertyToken);
            if (agentType.equals("WSP")) {
                pc = ProviderConfig.getProvider(name, ProviderConfig.WSP);
            } else if  (agentType.equals("WSC")) {
                pc = ProviderConfig.getProvider(name, ProviderConfig.WSC);
            }
            
            List listSec = new ArrayList();
            listSec.add(secMechanism);
            pc.setSecurityMechanisms(listSec);
            if (hasUserCredential) {
                List listUsers = new ArrayList();
                int noOfCred = new Integer(rbp.getString(strIdx +
                        "noUserCredential")).intValue();
                String strUsername;
                String strPassword;
                PasswordCredential cred;
                for (int i = 0; i < noOfCred; i++) {
                    strUsername = rbp.getString(strIdx + "UserCredential" + i +
                            ".username");
                    strPassword = rbp.getString(strIdx + "UserCredential" + i +
                            ".password");
                    cred = new PasswordCredential(strUsername, strPassword);
                    listUsers.add(cred);
                }
                log(Level.FINEST, "createAgentProfile", "UserCredential: " +
                        listUsers);
                pc.setUsers(listUsers);
            }
            pc.setRequestSignEnabled(isRequestSigned);
            pc.setRequestEncryptEnabled(isRequestEncrypted);
            pc.setResponseSignEnabled(isResponseSigVerified);
            pc.setResponseEncryptEnabled(isResponseDecrypted);
            pc.setDefaultKeyStore(keystoreUsage);
            pc.setPreserveSecurityHeader(keepPrivateSecHeaderInMsg);
            pc.setServiceType(svcType);
            pc.setKeyAlias(keyAlias);
            pc.setPublicKeyAlias(keyAlias);
            
            if (agentType.equals("WSP"))
                pc.setWSPEndpoint("http://wsp.com");
            
            if (agentType.equals("WSC")) {
                if (isLibertyToken) {
                    //Trust AuthoritiyConfig
                    taconfig = TrustAuthorityConfig.getConfig("localDisco",
                            TrustAuthorityConfig.DISCOVERY_TRUST_AUTHORITY);
                    taconfig.setEndpoint(protocol + ":" + "//" + host + ":" +
                            port + uri + "/Liberty/disco");
                    TrustAuthorityConfig.saveConfig(taconfig);
                    pc.setTrustAuthorityConfig(taconfig);
                }
            }
            ProviderConfig.saveProvider(pc);
            if (agentType.equals("WSP")) {
                log(Level.FINEST, "createAgentProfile",
                        "WSP provider is exists()\n" +
                        ProviderConfig.isProviderExists(name,
                        ProviderConfig.WSP));
            }
            if (agentType.equals("WSC")) {
                log(Level.FINEST, "createAgentProfile",
                        "WSC provider is exists()\n" +
                        ProviderConfig.isProviderExists(name,
                        ProviderConfig.WSC));
            }
            return (name);
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
    }
    
    /**
     * Convienence method to make web service call.
     */
    private String getStockQuote(String url, String message)
    throws Exception {
        InputStream in_buf = null;
        URL endpoint = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)
        endpoint.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "text/xml; charset=\"utf-8\"");
        
        // Output
        byte[] data = message.getBytes("UTF-8");
        int requestLength = data.length;
        connection.setRequestProperty("Content-Length", Integer
                .toString(requestLength));
        OutputStream out = null;
        try {
            out = connection.getOutputStream();
        } catch (ConnectException ce) {
            ce.printStackTrace();
            return (null);
        }
        
        // Write out the message
        out.write(data);
        out.flush();
        
        // Get the response
        try {
            in_buf = connection.getInputStream();
        } catch (IOException ioe) {
            // Could be receiving SOAP fault
            // Debug the exception
            ioe.printStackTrace();
            return (null);
        }
        
        // Return the response
        StringBuffer inbuf = new StringBuffer();
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                in_buf, "UTF-8"));
        while ((line = reader.readLine()) != null) {
            inbuf.append(line).append("\n");
        }
        return (new String(inbuf));
    }
    
    /**
     * Creates the XML for making the webservice resquest.
     */
    private StringBuffer getStockQuoteRequest(String symbol) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("<env:Envelope ")
        .append("xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\" ")
        .append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ")
        .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
        .append("xmlns:enc=\"http://schemas.xmlsoap.org/soap/encoding/\" ")
        .append("xmlns:ns0=\"http://sun.com/stockquote.xsd\" ")
        .append("env:encodingStyle=\"http://schemas.xmlsoap.org")
        .append("/soap/encoding/\"><env:Header>")
        .append(getAddressingHeader())
	.append("</env:Header>")
        .append("<env:Body><ns0:QuoteRequest><Symbol>")
        .append(symbol)
        .append("</Symbol></ns0:QuoteRequest>")
        .append("</env:Body></env:Envelope>");
        return sb;
    }
    
    /**
     * Creates a unique message id for each request
     */
    private String getAddressingHeader() {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("<To xmlns=\"http://www.w3.org/2005/08/addressing\"")
	  .append(">" + strServiceURL + "</To>")
          .append("<Action xmlns=\"http://www.w3.org/2005/08/addressing\"")
          .append(">http://sun.com/GetStockQuote</Action>")
          .append("<ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\">")
          .append("<Address>http://www.w3.org/2005/08/addressing/anonymous")
          .append("</Address>")
          .append("</ReplyTo><MessageID ")
          .append("xmlns=\"http://www.w3.org/2005/08/addressing\">")
          .append(java.util.UUID.randomUUID().toString() + "</MessageID>");
        return sb.toString();
    }    
    
 
    /**
     * Creates the XML for updating the Discovery service security bootstrap
     * entry.
     */
    private String getBootsrapDiscoEntry(String strSec) {
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
    
}
