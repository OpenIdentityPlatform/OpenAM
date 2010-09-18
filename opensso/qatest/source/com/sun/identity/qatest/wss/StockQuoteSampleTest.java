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
 * $Id: 
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.wss;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.WSSCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import com.sun.identity.wss.provider.DiscoveryConfig;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.TrustAuthorityConfig;
import com.sun.identity.wss.security.PasswordCredential;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URL;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
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
 * providers using ProviderConfig class. This class verifies the security by
 * accessing the StockQuoteClient
 */
public class StockQuoteSampleTest extends TestCommon {
    private String testIndex;
    private boolean isAuthChain;
    private int wscIndex;
    private int wspIndex;
    private int stsClientIndex;
    private int stsServiceIndex;
    private String strTestDescription;
    private String strTestCaseDetail;
    private String strAuthChain ;
    private String strWSPDescription;
    private String strWSCDescription;
    private String strSTSClientDescription;
    private String strSTSServiceDescription;
    private String strClientURL;
    private String strExpResult;
    private String strWSCId;
    private String strWSPId;
    private String strSTSId;
    private String strReplay;
    private String baseDir;
    private ResourceBundle rbp;
    private ResourceBundle rbg;
    private SSOToken token;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private WSSCommon wssc;
    private AuthenticationCommon authCommon;
    private boolean isLibertyToken;
    private TrustAuthorityConfig taconfig;
    private TrustAuthorityConfig stsconfig;
    private DefaultTaskHandler task;
    private String strLocRB = "StockQuoteSampleTest";
    private String strGlbRB = "StockQuoteSampleGlobal";
    private String strUser = "sampletestuser";
    private String strSTSSecurity;
    private String strSTSConfigName;
    private String strSTSServiceType;
    private String strServiceURL;
    private String strUserAuth;
    private String strEvaluateClient;
    private WebClient webClient;
    private Set origAuthPostSet;
    private Map<String, Set> STSServiceMap;
    private long entryTime;
    /**
     * Default constructor. Creates admintoken and helper class instances.
     */
    public StockQuoteSampleTest()
    throws Exception{
        super("StockQuoteSampleTest");
        rbp = ResourceBundle.getBundle("wss" + fileseparator + strLocRB);
        rbg = ResourceBundle.getBundle("wss" + fileseparator + strGlbRB);
        idmc = new IDMCommon();
        wssc = new WSSCommon();
        authCommon = new AuthenticationCommon("wss");
        strServiceURL = rbg.getString(strGlbRB + ".RemoteSTSServiceURL");
        
    }
    
    /**
     * Updates bootstrap security mechanism in Discovery service to null:X509
     * and creates users.
     */
    @BeforeSuite(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", 
    "jdbc_sec"})
    public void createUser()
    throws Exception {
        try {
            token = getToken(adminUser, adminPassword, basedn);
            smsc = new SMSCommon(token);
            Set set = new HashSet();
            set.add(wssc.getBootsrapDiscoEntry("2005-02:null:X509"));
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
        } finally {
            destroyToken(token);
        }
    }
    
    /**
     * Changes the runtime application user from UrlAccessAgent to amadmin
     */
    @BeforeSuite(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", 
    "jdbc_sec"})
    public void changeRunTimeUser() throws
            Exception {
        try {
            SSOToken runtimeToken = null;
            runtimeToken = (SSOToken) AccessController.doPrivileged(
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
            SSOToken newToken = null;
            newToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            log(Level.FINEST, "changeRunTimeUser", "Runtime user name after " +
                    "change \n" + newToken.getPrincipal().getName());
        } catch(Exception e) {
            log(Level.SEVERE, "changeRunTimeUser", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Creates agent profiles for web service providers and web service clients.
     */
    @Parameters({"testIdx", "wscIdx", "wspIdx", "STSSecurity", "STSClientIdx",
    "STSServiceIdx", "UserAuth","replay","authchain", "EvaluateClient"})
    @BeforeClass(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", 
    "jdbc_sec"})
    public void setup(String testIdx, String wscIdx, String wspIdx,
            String STSSecurity, String  STSClientIdx, String STSServiceIdx,
            String UserAuth, String replay ,String authchain, String EvaluateClient)
            throws Exception {
        Object[] params = {testIdx, wscIdx, wspIdx, STSSecurity, STSClientIdx,
        STSServiceIdx, UserAuth, EvaluateClient};
        entering("setup", params);
        try {
            entryTime = System.currentTimeMillis();
            //changeRunTimeUser();
            testIndex = testIdx;
            wscIndex = new Integer(wscIdx).intValue();
            wspIndex = new Integer(wspIdx).intValue();
            if (!STSClientIdx.equals(""))
                stsClientIndex = new Integer(STSClientIdx).intValue();
            if (!STSServiceIdx.equals(""))
                stsServiceIndex = new Integer(STSServiceIdx).intValue();
            strSTSSecurity = STSSecurity;
            strUserAuth = UserAuth;
            strReplay = replay;
            strAuthChain = authchain;
            strEvaluateClient = EvaluateClient;
            log(Level.FINEST, "setup", "testIndex: " + testIndex);
            log(Level.FINEST, "setup", "wscIdx: " + wscIndex);
            log(Level.FINEST, "setup", "wspIdx: " + wspIndex);
            log(Level.FINEST, "setup", "stsClientIndex: " + stsClientIndex);
            log(Level.FINEST, "setup", "stsServiceIndex: " + stsServiceIndex);
            
            strWSPDescription = rbp.getString(strLocRB + wspIndex
                    + ".description");
            strWSCDescription = rbp.getString(strLocRB + wscIndex
                    + ".description");
            if (strSTSSecurity.equals("true"))
                strTestDescription = strWSPDescription + " at wsp \n " ;
            else
                strTestDescription = strWSPDescription + " at wsp \n " +
                        strWSCDescription + " at wsc \n";
            
            log(Level.FINEST, "setup", "Description: " + strTestDescription);
            
            strClientURL = rbg.getString(strGlbRB + ".clienturl");
            log(Level.FINEST, "setup", "Client URL: " + strClientURL);
            
            strSTSConfigName = rbg.getString(strGlbRB + ".stsclient.name");
            log(Level.FINEST, "setup", "STS config Name: " + strSTSConfigName);
            
            strExpResult = rbp.getString(strLocRB + testIndex + ".passmessage");
            log(Level.FINEST, "setup", "Expected Result: " + strExpResult);
            
            strTestCaseDetail = rbp.getString(strLocRB + testIndex +
                    ".testcasedetail");
            log(Level.FINEST, "setup", "Description: " + strTestCaseDetail);
            
            isLibertyToken = new Boolean(rbp.getString(strLocRB + wscIndex +
                    ".isLibertyToken")).booleanValue();
            log(Level.FINEST, "setup", "Is Liberty Token: " + isLibertyToken);
            
            if(strReplay.equals("true")){
                isAuthChain = true;
                Map chainMap = new HashMap();
                String[] configInstances = {"WSSAuthModule,REQUIRED"};
                authCommon.createAuthConfig("/","wssAuthChain", configInstances,
                        chainMap);
            }
            
            long startTime = System.currentTimeMillis();
            strWSCId = wssc.createAgentProfile("WSC", strGlbRB, strLocRB,
                    isLibertyToken, taconfig, wscIdx,strAuthChain);
            long endTime = System.currentTimeMillis();
            long timetaken =  (long)(endTime - startTime);
            log(Level.FINEST, "setup",
                    "Time to setup WSC : " + (timetaken/1000));
            log(Level.FINEST, "setup", "WSC Agent Id: " + strWSCId);
            
            startTime = System.currentTimeMillis();
            strWSPId = wssc.createAgentProfile("WSP", strGlbRB, strLocRB,
                    isLibertyToken, taconfig, wspIdx,strReplay);
            endTime = System.currentTimeMillis();
            timetaken =  (long)(endTime - startTime);
            log(Level.FINEST, "setup",
                    "Time to setup WSP : " + (timetaken/1000));
            log(Level.FINEST, "setup", "WSP Agent Id: " + strWSPId);
            if (isLibertyToken)
                wssc.registerWSPWithDisco(strWSPId, taconfig);
        } catch(Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }
    
    /**
     * Accesses the StockQuoteClient and submits the request and verifies the
     * expected result
     */
    @Test(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", "jdbc_sec"})
    public void evaluateStockQuoteClient()
    throws Exception {
        entering("evaluateStockQuoteClient", null);
        ProviderConfig stockPc = ProviderConfig.getProvider(strWSCId,
                ProviderConfig.WSC);
        ProviderConfig wspPc = ProviderConfig.getProvider(strWSPId,
                ProviderConfig.WSP);
        wssc.displayAgentProfile(wspPc);
        Reporter.log("******************************************");
        Reporter.log("Test Case detail:" + strTestCaseDetail);
        if (strEvaluateClient.equals("true")) {
            try {
                if (strSTSSecurity.equals("true")) {
                    //Creates the STSCLIENT profile
                    log(Level.FINEST, "setup", "creating STS client profile:");
                    long startTime = System.currentTimeMillis();
                    strSTSId = wssc.createAgentProfile("STSCLIENT", strGlbRB,
                            strLocRB, isLibertyToken, taconfig, "" +
                            stsClientIndex,strAuthChain);
                    long endTime = System.currentTimeMillis();
                    long timetaken =  (long)(endTime - startTime);
                    log(Level.FINEST, "setup",
                            "Time to setup STSCLIENT : " + (timetaken/1000));
                    log(Level.FINEST, "setup", "STSCLIENT Agent Id: " +
                            strSTSId);
                    List listSecMech = new ArrayList();
                    String secMechanism = "urn:sun:wss:sts:security";
                    listSecMech.add(secMechanism);
                    stockPc.setSecurityMechanisms(listSecMech);
                    //Add other code for setting the STS attributes
                    stsconfig = TrustAuthorityConfig.getConfig(strSTSConfigName,
                            TrustAuthorityConfig.STS_TRUST_AUTHORITY);
                    stockPc.setTrustAuthorityConfig(stsconfig);
                    ProviderConfig.saveProvider(stockPc);
                    stockPc = ProviderConfig.getProvider(strWSCId,
                            ProviderConfig.WSC);
                    wssc.displayAgentProfile(stockPc);
                    TrustAuthorityConfig stockpcConfig =
                            stockPc.getTrustAuthorityConfig();
                    Reporter.log("name of the trust authority from " +
                            "stockservice" + stockpcConfig.getName());
                    strSTSServiceType = rbp.getString( strLocRB +
                            stsClientIndex + ".stsclient.stsservicetype");
                    
                    if (strSTSServiceType.equals("remote")) {
                        //code for remote sts service
                        //updateSTSServiceLocalRemote
                    } else {
                        token = getToken(adminUser, adminPassword, basedn);
                        startTime = System.currentTimeMillis();
                        STSServiceMap = wssc.getSTSServiceLocalAttributes(
                                token);
                        wssc.updateSTSServiceLocal(strGlbRB, strLocRB,
                                "" + stsClientIndex, "" + stsServiceIndex,
                                token, strAuthChain);
                        endTime = System.currentTimeMillis();
                        timetaken =  (long)(endTime - startTime);
                        Map STSServiceLatestMap = new HashMap();
                        STSServiceLatestMap = wssc.getSTSServiceLocalAttributes(
                                token);
                        Reporter.log("STSService Attributes after modification:"
                                + STSServiceLatestMap);
                        log(Level.FINEST, "evaluateStockQuoteClient",
                                "STSService Attributes after modification: "
                                + STSServiceLatestMap);
                        
                        stockPc = ProviderConfig.getProvider(strWSCId,
                                ProviderConfig.WSC);
                        wspPc = ProviderConfig.getProvider(strWSPId,
                                ProviderConfig.WSP);
                    }
                    if(strReplay.equals("true")){
                        origAuthPostSet = wssc.updateAuthService(token);
                    }
                }
                if (strUserAuth.equals("true")) {                    
                    log(Level.FINEST, "evaluateStockQuoteClient","Using end " +
                            "user auth");
                    stockPc.setForceUserAuthentication(true);
                    ProviderConfig.saveProvider(stockPc);
                    Thread.sleep(1000);
                    String xmlFile = "generateUserAuthenticateXML" + testIndex
                            + ".xml";
                    String xmlFileLocation = getTestBase() +
                            System.getProperty("file.separator") + "wss" +
                            System.getProperty("file.separator") + xmlFile;
                    wssc.generateUserAuthenticateXML(strUser, strUser,
                            xmlFileLocation, strExpResult, strClientURL);
                    task = new DefaultTaskHandler(xmlFileLocation);
                    webClient = new WebClient();
                    long startTime = System.currentTimeMillis();
                    HtmlPage page = task.execute(webClient);
                    long endTime = System.currentTimeMillis();
                    long timetaken =  (long)(endTime - startTime);
                    log(Level.FINEST, "evaluateStockQuoteClient",
                            "Time to get StockQuote: " + (timetaken/1000));
                    log(Level.FINEST, "evaluateStockQuoteClient",
                            "evaluateStockQuoteClient page after " +
                            "login\n" + page.getWebResponse().
                            getContentAsString());
                    int iIdx = getHtmlPageStringIndex(page, strExpResult);                    
                    if (iIdx == -1) {
                        assert false;
                    } else
                        assert true;
                } else {
                    log(Level.FINEST, "evaluateStockQuoteClient","Using WSC" +
                            "auth");
                    stockPc.setForceUserAuthentication(false);
                    Thread.sleep(1000);
                    webClient = new WebClient();
                    webClient.setCookiesEnabled(true);
                    URL cmdUrl = new URL(strClientURL);
                    HtmlPage page = (HtmlPage) webClient.getPage(cmdUrl);
                    HtmlForm form = (HtmlForm) page.getFormByName("GetQuote");                    
                    HtmlTextInput txtagentname = (HtmlTextInput) form.
                            getInputByName("symbol");
                    txtagentname.setValueAttribute("JAVA");
                    long startTime = System.currentTimeMillis();
                    HtmlPage returnPage = (HtmlPage) form.submit();
                    long endTime = System.currentTimeMillis();
                    long timetaken = endTime - startTime;
                    log(Level.FINEST, "evaluateStockQuoteClient",
                            "Time to get StockQuote: " + (timetaken/1000));
                    log(Level.FINEST, "evaluateStockQuoteClient",
                            " Page after request submission \n" +
                            returnPage.getWebResponse().getContentAsString());
                    
                    int iIdx = getHtmlPageStringIndex(returnPage, strExpResult);
                    assert (iIdx != -1);
                }
            } catch (Exception e) {
                log(Level.SEVERE, "evaluateStockQuoteClient", e.getMessage());
                e.printStackTrace();
                throw e;
            } finally {
                long exitTime = System.currentTimeMillis();
                log(Level.FINEST, "evaluateStockQuoteClient",
                        "Time to execute " +
                        "test case: " + ((exitTime - entryTime)/1000));
                
                if (strSTSSecurity.equals("true")) {
                    strTestDescription = strTestDescription + ",using a STS " +
                            "Client profile and STSService is "+ rbp.getString(
                            strLocRB + stsClientIndex + ".stsclient." +
                            "stsservicetype");
                }
                Reporter.log("Test Description: "  + strTestDescription);
                Reporter.log("Test Id: " + testIndex );
                Reporter.log("WSC Index: " + wscIndex );
                Reporter.log("WSP Index: " + wspIndex );
                Reporter.log("Is sts security enabled: " + strSTSSecurity );
                if (strSTSSecurity.equals("true")) {
                    Reporter.log("STSClientIndex: " + stsClientIndex);
                    Reporter.log("STSServiceIndex: " + stsServiceIndex);
                }
                Reporter.log("Is user auth enabled: " + strUserAuth );
                Reporter.log("Expected Result: " + strExpResult);
                Reporter.log("Replay Pasword:" + strReplay);
                Reporter.log("Auth Chain:" +  strAuthChain);
            }
            exiting("evaluateStockQuoteClient");
        }
    }
    /**
     * Deletes the agent profiles for webservice clients and providers.
     */
    @AfterClass(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec", 
    "jdbc_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        token = getToken(adminUser, adminPassword, basedn);
        smsc = new SMSCommon(token);
        log(Level.FINEST, "cleanup", "WSC Agent Id: " + strWSCId + "WSC");
        log(Level.FINEST, "cleanup", "WSP Agent Id: " + strWSPId + "WSP");
        
        if (strSTSSecurity.equals("true")) {
            log(Level.FINEST, "cleanup", "After updating Auth->Core->Authn" +
                    "post processing classes: " );
            if(strReplay.equals("true")){
                smsc.updateSvcAttribute("/", "iPlanetAMAuthService",
                        "iplanet-am-auth-post-login-process-class",
                        origAuthPostSet, "Organization"
                        );
            }
            if (strSTSServiceType.equals("remote")) {
                //code to reset STS service in a remote machine
            } else {
                log(Level.FINEST, "cleanup", "Resetting STS LocalService");
                
                Set falseSet= new HashSet();
                falseSet.add("false");
                STSServiceMap. put("isRequestSign", falseSet);
                STSServiceMap. put("isResponseSign", falseSet);
                STSServiceMap. put("isRequestEncrypt", falseSet);
                STSServiceMap. put("isResponseEncrypt", falseSet);
                STSServiceMap. put("isResponseEncrypt", falseSet);
                if(isAuthChain==true){
                    falseSet= new HashSet();
                    falseSet.add("[Empty]");
                    STSServiceMap. put("AuthenticationChain", falseSet);
                }
                wssc.setSTSServiceLocalAttributes(token, STSServiceMap);
            }
        }
        if (isLibertyToken) {
            wssc.unregisterWSPWithDisco("urn:wsp", taconfig);
            ProviderConfig.deleteProvider("localDisco", "Discovery");
        }
        ProviderConfig.deleteProvider(strWSCId, ProviderConfig.WSC);
        ProviderConfig.deleteProvider(strWSPId, ProviderConfig.WSP);
        TrustAuthorityConfig.deleteConfig(strSTSId,
                TrustAuthorityConfig.STS_TRUST_AUTHORITY);
        destroyToken(token);
        if(isAuthChain==true){
            authCommon.deleteAuthConfig("/", "wssAuthChain");}
        Thread.sleep(1000);
        exiting("cleanup");
    }
    
    /**
     * Deletes users and resets bootstrap security mechanism in Discovery
     * service to null:null.
     */
    @AfterSuite(groups={"ldapv3_sec", "s1ds_sec", "ad_sec", "amsdk_sec"})
    public void deleteUser()
    throws Exception {
        try {
            token = getToken(adminUser, adminPassword, basedn);
            idmc.deleteIdentity(token, realm, IdType.USER, strUser);
            Set set = new HashSet();
            set.add(wssc.getBootsrapDiscoEntry("2003-08:null:null"));
            destroyToken(token);
        }catch (Exception e) {
            log(Level.SEVERE, "deleteUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(token);
        }
    }
}
