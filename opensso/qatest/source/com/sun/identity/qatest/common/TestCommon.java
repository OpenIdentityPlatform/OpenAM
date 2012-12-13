/* The contents of this file are subject to the terms
 (updated)
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
 * $Id: TestCommon.java,v 1.78 2009/08/05 21:42:36 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */


package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.jaxrpc.JAXRPCUtil;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.testng.Reporter;
import java.net.URLEncoder;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.nio.SelectChannelConnector;

/**
 * This class is the base for all <code>OpenSSO</code> QA testcases.
 * It has commonly used methods.
 */
public class TestCommon implements TestConstants {
    
    private String className;
    static private ResourceBundle rb_amconfig;
    static protected String adminUser;
    static protected String adminPassword;
    static protected String basedn;
    static protected String host;
    static protected String protocol;
    static protected String port;
    static protected String uri;
    static protected String realm;
    static protected String serverName;
    static protected int notificationSleepTime;
    static protected Level logLevel;
    static protected boolean distAuthEnabled = false;
    static private Logger logger;
    static private String logEntryTemplate;
    static protected Server server;
    private String productSetupResult;
    static private String uriseparator = "/";
    protected static String csDeployURI = "/IntClientSample";
 
    protected static String newline = System.getProperty("line.separator");
    protected static String fileseparator =
            System.getProperty("file.separator");
    private static String tableContents;
    protected static String serverProtocol;
    protected static String serverHost;
    protected static String serverPort;
    protected static String serverUri;
    protected static String keyAlias;
    protected static String clientURL;
    protected static int cltWarDeployPort;

    static {
        try {
            rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            logger = Logger.getLogger("com.sun.identity.qatest");
            serverName = rb_amconfig.getString(
                    TestConstants.KEY_ATT_SERVER_NAME);
            FileHandler fileH = new FileHandler(serverName + fileseparator +
                    "logs");
            SimpleFormatter simpleF = new SimpleFormatter();
            fileH.setFormatter(simpleF);
            logger.addHandler(fileH);
            String logL = rb_amconfig.getString(
                    TestConstants.KEY_ATT_LOG_LEVEL);
            if ((logL != null) && !logL.equals("")) {
                logger.setLevel(Level.parse(logL));
            } else {
                logger.setLevel(Level.FINE);
            }
            logLevel = logger.getLevel();
            adminUser = rb_amconfig.getString(
                    TestConstants.KEY_ATT_AMADMIN_USER);
            adminPassword = rb_amconfig.getString(
                    TestConstants.KEY_ATT_AMADMIN_PASSWORD);
            basedn = rb_amconfig.getString(TestConstants.KEY_AMC_BASEDN);
            serverProtocol = rb_amconfig.getString(
                    TestConstants.KEY_AMC_PROTOCOL);
            serverHost = rb_amconfig.getString(TestConstants.KEY_AMC_HOST);
            serverPort = rb_amconfig.getString(TestConstants.KEY_AMC_PORT);
            serverUri = rb_amconfig.getString(TestConstants.KEY_AMC_URI);
            keyAlias = rb_amconfig.getString(
                    TestConstants.KEY_AMC_XMLSIG_CERTALIAS);
            distAuthEnabled = ((String)rb_amconfig.getString(
                    TestConstants.KEY_DIST_AUTH_ENABLED)).equals("true");
            if (!distAuthEnabled) {
                protocol = serverProtocol;
                host = serverHost;
                port = serverPort;
                uri = serverUri;
            } else {
                String strDistAuthURL = rb_amconfig.getString(
                        TestConstants.KEY_DIST_AUTH_NOTIFICATION_SVC);
                
                int iFirstSep = strDistAuthURL.indexOf(":");
                protocol = strDistAuthURL.substring(0, iFirstSep);
                
                int iSecondSep = strDistAuthURL.indexOf(":", iFirstSep + 1);
                host = strDistAuthURL.substring(iFirstSep + 3, iSecondSep);
                
                int iThirdSep = strDistAuthURL.indexOf(uriseparator,
                        iSecondSep + 1);
                port = strDistAuthURL.substring(iSecondSep + 1, iThirdSep);
                
                int iFourthSep = strDistAuthURL.indexOf(uriseparator,
                        iThirdSep + 1);
                uri = uriseparator +
                        strDistAuthURL.substring(iThirdSep + 1, iFourthSep);
            }
            realm = rb_amconfig.getString(TestConstants.KEY_ATT_REALM);
            notificationSleepTime = new Integer(rb_amconfig.getString(
                    TestConstants.KEY_ATT_NOTIFICATION_SLEEP)).intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private TestCommon() {
    }
    
    protected TestCommon(String componentName) {
        logEntryTemplate = this.getClass().getName() + ".{0}: {1}";
        className = this.getClass().getName();
        productSetupResult = rb_amconfig.getString(
                TestConstants.KEY_ATT_PRODUCT_SETUP_RESULT);
        if (productSetupResult.equals("fail")) {
            log(Level.SEVERE, "TestCommon", "Product setup failed. Check logs" +
                    " for more detail...");
            assert false;
        }
    }
    
    /**
     * Writes a log entry for entering a test method.
     */
    protected void entering(String methodName, Object[] params) {
        if (params != null) {
            logger.entering(className, methodName, params);
        } else {
            logger.entering(className, methodName);
        }
    }
    
    /**
     * Writes a log entry for exiting a test method.
     */
    protected void exiting(String methodName) {
        logger.exiting(className, methodName);
    }
    
    /**
     * Writes a log entry.
     */
    protected static void log(Level level, String methodName, Object message) {
        Object[] args = {methodName, message};
        logger.log(level, MessageFormat.format(logEntryTemplate, args));
    }
    
    /**
     * Writes a log entry.
     */
    protected void log(
            Level level,
            String methodName,
            String message,
            Object[] params
            ) {
        Object[] args = {methodName, message};
        logger.log(level, MessageFormat.format(logEntryTemplate, args), params);
    }
    
    /**
     * Writes a log entry for testng report
     */
    protected void logTestngReport(Map m) {
        Set s = m.keySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            String value = (String)m.get(key);
            Reporter.log(key + "=" + value);
        }
    }
    
    /**
     * Returns single sign on token.
     */
    protected SSOToken getToken(String name, String password, String basedn)
    throws Exception {
        entering("SSOToken", null);
        log(Level.FINEST, "getToken", name);
        log(Level.FINEST, "getToken", password);
        log(Level.FINEST, "getToken", basedn);
        AuthContext authcontext = new AuthContext(basedn);
        authcontext.login();
        javax.security.auth.callback.Callback acallback[] =
                authcontext.getRequirements();
        for (int i = 0; i < acallback.length; i++){
            if (acallback[i] instanceof NameCallback) {
                NameCallback namecallback = (NameCallback)acallback[i];
                namecallback.setName(name);
            }
            if (acallback[i] instanceof PasswordCallback) {
                PasswordCallback passwordcallback =
                        (PasswordCallback)acallback[i];
                passwordcallback.setPassword(password.toCharArray());
            }
        }
        
        authcontext.submitRequirements(acallback);
        if (authcontext.getStatus() ==
                com.sun.identity.authentication.AuthContext.Status.SUCCESS)
            log(Level.FINEST, "getToken", "Successful authentication ....... ");
        SSOToken ssotoken = authcontext.getSSOToken();
        log(Level.FINEST, "getToken",
                (new StringBuilder()).append("TOKENCREATED>>> ").
                append(ssotoken).toString());
        exiting("SSOToken");
        return ssotoken;
    }
    
    /**
     * Validate single sign on token.
     */
    protected boolean validateToken(SSOToken ssotoken)
    throws Exception {
        entering("validateToken", null);
        SSOTokenManager stMgr = SSOTokenManager.getInstance();
        boolean bVal = stMgr.isValidToken(ssotoken);
        if (bVal)
            log(Level.FINE, "validateToken", "Token is Valid");
        else
            log(Level.FINE, "validateToken", "Token is Invalid");
        exiting("validateToken");
        return bVal;
    }
    
    /**
     * Destroys single sign on token.
     */
    protected void destroyToken(SSOToken ssotoken)
    throws Exception {
        destroyToken(null, ssotoken);
    }
    
    /**
     * Destroys single sign on token.
     */
    protected void destroyToken(SSOToken requester, SSOToken ssotoken)
    throws Exception {
        entering("destroyToken", null);
        if (validateToken(ssotoken)) {
            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            if (requester != null)
                stMgr.destroyToken(requester, ssotoken);
            else
                stMgr.destroyToken(ssotoken);
        }
        exiting("destroyToken");
    }
    
    /**
     * Returns the base directory where code base is
     * checked out.
     */
    protected String getBaseDir()
    throws Exception {
        entering("getBaseDir", null);
        String strCD =  System.getProperty("user.dir");
        log(Level.FINEST, "getBaseDir", "Current Directory: " + strCD);
        exiting("getBaseDir");
        return (strCD);
    }
    
    /**
     * Reads a file containing data-value pairs and returns that as a list
     * object.
     */
    protected List getListFromFile(String fileName)
    throws Exception {
        entering("getListFromFile", null);
        ArrayList list = null;
        if (fileName != null) {
            list = new ArrayList();
            BufferedReader input = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line=input.readLine()) != null) {
                if ((line.indexOf("=")) != -1)
                    list.add(line);
            }
            log(Level.FINEST, "getListFromFile", "List: " + list);
            if (input != null)
                input.close();
        }
        exiting("getListFromFile");
        return (list);
    }
    
    /**
     * Converts native session id  string to opensso specific url safe char66
     * encoded string.
     * This is not a general purpose utility.
     * This is meant only for internal use
     *
     * @param sidString plain text string
     * @return url safe modifed char66 encoded string
     *
     * @see #c66DecodeCookieString(String)
     *
     * Sample session id string:
     * AQIC5wM2LY4SfcxPEcjVKCEI7QdmYvlOZvKZpdEErxVPvx8=@AAJTSQACMDE=#
     *
     * We would replace
     * + with -
     * / with _
     * = with .
     * @ with star
     * # with star
     *
     * while reconstucting the original cookie value first occurence of
     * star would be replaced with @ and the subsequent occurunce star would
     * be replaced with #
     */
    protected String c66EncodeSidString(String sidString) {
        if (sidString == null || sidString.length() == 0) {
            return sidString;
        }
        int length = sidString.length();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            char c = sidString.charAt(i);
            if (c == '+') {
                chars[i] = '-';
            } else if (c == '/') {
                chars[i] = '_';
            } else if (c == '=') {
                chars[i] = '.';
            } else if (c == '@') {
                chars[i] = '*';
            } else if (c == '#') {
                chars[i] = '*';
            } else {
                chars[i] = c;
            }
        }
        return new String(chars);
    }

    /**
     * Login to admin console using htmlunit
     */
    protected HtmlPage consoleLogin(
            WebClient webclient,
            String amUrl,
            String amadmUser,
            String amadmPassword)
            throws Exception {
        entering("consoleLogin", null);
        log(Level.FINEST, "consoleLogin", "JavaScript Enabled: " +
                webclient.isJavaScriptEnabled());
        log(Level.FINEST, "consoleLogin", "Redirect Enabled: " +
                webclient.isRedirectEnabled());
        log(Level.FINEST, "consoleLogin", "URL: " + amUrl);
        URL url = new URL(amUrl);
        HtmlPage page = (HtmlPage)webclient.getPage(amUrl);
        log(Level.FINEST, "consoleLogin", "BEFORE CONSOLE LOGIN: " +
                page.getTitleText());
        HtmlForm form = page.getFormByName("Login");
        HtmlHiddenInput txt1 =
                (HtmlHiddenInput)form.getInputByName("IDToken1");
        txt1.setValueAttribute(amadmUser);
        HtmlHiddenInput txt2 =
                (HtmlHiddenInput)form.getInputByName("IDToken2");
        txt2.setValueAttribute(amadmPassword);
        ScriptResult scriptResult = page.executeJavaScript("document.forms['Login'].submit();");
        HtmlPage newPage = (HtmlPage)scriptResult.getNewPage();
        log(Level.FINEST, "consoleLogin", "AFTER CONSOLE LOGIN: " +
                newPage.getTitleText());
        exiting("consoleLogin");
        return (newPage);
    }
    
    /**
     * Creates a map object and adds all the configuration properties to that.
     */
    protected Map getConfigurationMap(String rb, String strProtocol,
            String strHost, String strPort, String strURI)
            throws Exception {
        entering("getConfigurationMap", null);
        
        ResourceBundle cfg = ResourceBundle.getBundle(rb);
        Map<String, String> map = new HashMap<String, String>();
        map.put("serverurl", strProtocol + ":" + "//" + strHost + ":" +
                strPort);
        map.put("serveruri", strURI);
        map.put(TestConstants.KEY_ATT_AMADMIN_USER, cfg.getString(
                TestConstants.KEY_ATT_AMADMIN_USER));
        map.put(TestConstants.KEY_ATT_AMADMIN_PASSWORD, cfg.getString(
                TestConstants.KEY_ATT_AMADMIN_PASSWORD));
        map.put(TestConstants.KEY_AMC_SERVICE_PASSWORD, cfg.getString(
                TestConstants.KEY_AMC_SERVICE_PASSWORD));
        map.put(TestConstants.KEY_ATT_CONFIG_DIR, cfg.getString(
                TestConstants.KEY_ATT_CONFIG_DIR));
        map.put(TestConstants.KEY_ATT_CONFIG_DATASTORE, cfg.getString(
                TestConstants.KEY_ATT_CONFIG_DATASTORE));
        map.put(TestConstants.KEY_ATT_AM_VERSION, cfg.getString(
                TestConstants.KEY_ATT_AM_VERSION));
        map.put(TestConstants.KEY_ATT_AM_ENC_PWD,
                cfg.getString(TestConstants.KEY_ATT_AM_ENC_PWD));
        map.put(TestConstants.KEY_ATT_DIRECTORY_SERVER, cfg.getString(
                TestConstants.KEY_ATT_DIRECTORY_SERVER));
        map.put(TestConstants.KEY_ATT_DIRECTORY_PORT, cfg.getString(
                TestConstants.KEY_ATT_DIRECTORY_PORT));
        map.put(TestConstants.KEY_ATT_DS_ADMINPORT, cfg.getString(
                TestConstants.KEY_ATT_DS_ADMINPORT));
        map.put(TestConstants.KEY_ATT_DS_JMXPORT, cfg.getString(
                TestConstants.KEY_ATT_DS_JMXPORT));
        map.put(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX,
                cfg.getString(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX));
        map.put(TestConstants.KEY_ATT_DS_DIRMGRDN, cfg.getString(
                TestConstants.KEY_ATT_DS_DIRMGRDN));
        map.put(TestConstants.KEY_ATT_DS_DIRMGRPASSWD,
                cfg.getString(TestConstants.KEY_ATT_DS_DIRMGRPASSWD));
        map.put(TestConstants.KEY_ATT_CONFIG_UMDATASTORE, cfg.getString(
                TestConstants.KEY_ATT_CONFIG_UMDATASTORE));
              
        exiting("getConfigurationMap");
        
        return map;
    }
    
    /**
     * Creates a map object and adds all the configuration properties to that.
     */
    protected Map getConfigurationMap(String rb)
    throws Exception {
        return (getConfigurationMap(rb, protocol, host, port, uri));
    }
    
    /**
     * Creates a URLencoded String of all the configuration parameters.
     * map               A map of all the configuration data
     * strServerNo       The id number of the server being configured in
     *                   multi server tests
     */
    protected String getPostString(Map map, String strServerNo)
    throws Exception {
        // Forming the Server protocol, host & port
        StringBuffer strBuff = new StringBuffer();
        String strServerURL = (String)map.get("serverurl");
        String strServerURI = (String)map.get("serveruri");
        log(Level.FINEST, "getPostString", "serverurl=" + strServerURL +
                " serveruri=" + strServerURI);
        String strLocalURL = strServerURL;
        int iIndex;
        iIndex = strServerURL.indexOf("://");
        String strServerProtocol = strLocalURL.substring(0, iIndex);
        log(Level.FINEST, "getPostString", "strServerProtocol=" +
                strServerProtocol);
        strLocalURL = strLocalURL.substring(iIndex + 3, strLocalURL.length());
        iIndex = strLocalURL.indexOf(":");
        String strServerHost = strLocalURL.substring(0, iIndex);
        log(Level.FINEST, "getPostString", "strServerHost=" +
                strServerHost);
        int iDot = strServerHost.indexOf(".");
        String cookieDomain;
        if (iDot == -1)
            cookieDomain = strServerHost;
        else
            cookieDomain = strServerHost.substring(iDot,
                    strServerHost.length());

        strLocalURL = strLocalURL.substring(iIndex + 1, strLocalURL.length());
        String strServerPort = strLocalURL;
        log(Level.FINEST, "getPostString", "strServerPort=" +
                strServerPort);
        String strURL = strServerURL + strServerURI + "/config/configurator";
        log(Level.FINEST, "getPostString", "strURL: " + strURL);
        strBuff.append(URLEncoder.encode("DEPLOYMENT_URI", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(strServerURI, "UTF-8"))
                .append("&")
                .append(URLEncoder.encode("SERVER_URL", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(strServerURL, "UTF-8"))
                .append("&")
                .append(URLEncoder.encode("COOKIES_DOMAIN", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(cookieDomain, "UTF-8"))
                .append("&")
                .append(URLEncoder.encode("BASE_DIR", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(
                    (String)map.get(TestConstants.KEY_ATT_CONFIG_DIR), "UTF-8"))
                .append("&");

        String strEncryptKey = (String)map.get(
                TestConstants.KEY_ATT_AM_ENC_PWD);
        if (!(strEncryptKey.equals(null)) && !(strEncryptKey.equals(""))) {
            strBuff.append(URLEncoder.encode("AM_ENC_KEY", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(strEncryptKey, "UTF-8"));
        } else {
            strBuff.append(URLEncoder.encode("AM_ENC_KEY", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(
                        "FederatedAccessManagerEncryptionKey", "UTF-8"));
        }

       strBuff.append("&")
                .append(URLEncoder.encode("PLATFORM_LOCALE", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode("en_US", "UTF-8"))
                .append("&")
                .append(URLEncoder.encode("locale", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode("en_US", "UTF-8"))
                .append("&")
                .append(URLEncoder.encode("ADMIN_PWD", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(
                    (String)map.get(
                    TestConstants.KEY_ATT_AMADMIN_PASSWORD), "UTF-8"))
                .append("&")
                .append(URLEncoder.encode("ADMIN_CONFIRM_PWD", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(
                    (String)map.get(
                    TestConstants.KEY_ATT_AMADMIN_PASSWORD), "UTF-8"))
                .append("&");

        //DIRECTORY SERVER PARAMETERS
        String strConfigStore = (String)map.get(
                    TestConstants.KEY_ATT_CONFIG_DATASTORE);
        log(Level.FINE, "getPostString", "Config store is: " +
                strConfigStore);
        String strConfigDirPort = (String)map.get(
                            TestConstants.KEY_ATT_DIRECTORY_PORT);
        if (strConfigStore.equals("embedded")) {
            strBuff.append(URLEncoder.encode("DIRECTORY_SERVER", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(host, "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("DIRECTORY_PORT", "UTF-8"))
                    .append("=");
            strConfigDirPort = Integer.toString(getUnusedPort());
            strBuff.append(URLEncoder.encode(strConfigDirPort,
                    "UTF-8"));
                    log(Level.FINEST, "getPostString", "Config datastore" +
                            "port is: " + strConfigDirPort);
            strBuff.append("&")
                    .append(URLEncoder.encode("DS_DIRMGRPASSWD", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(
                        (String)map.get(
                        TestConstants.KEY_ATT_AMADMIN_PASSWORD), "UTF-8"));

            //OPENDS 2.3 PARAMETERS (OpenAM 9.5 and later)
            boolean flag = true;
            try{
                String strAMVersion = (String)map.get(
                                    TestConstants.KEY_ATT_AM_VERSION);
                float floAMVersionNum = Float.parseFloat(strAMVersion);
                if (floAMVersionNum < 9.5) {
                    flag = false;
                }
            } catch (NumberFormatException ex) {
                log(Level.FINE, "GetPostString", "Invalid version number");
            } catch (NullPointerException ex) {
                log(Level.FINE, "GetPostString", "No version number");
            }
            if (flag){
                strBuff.append("&")
                        .append(URLEncoder.encode("DIRECTORY_ADMIN_PORT", "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(
                            (String)map.get(
                            TestConstants.KEY_ATT_DS_ADMINPORT), "UTF-8"))
                        .append("&")
                        .append(URLEncoder.encode("DIRECTORY_JMX_PORT", "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(
                            (String)map.get(
                            TestConstants.KEY_ATT_DS_JMXPORT), "UTF-8"));
            }   
        } else if (strConfigStore.equals("dirServer")) {
            strBuff.append(URLEncoder.encode("DIRECTORY_SERVER", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(
                        (String)map.get(TestConstants.KEY_ATT_DIRECTORY_SERVER),
                        "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("DIRECTORY_PORT", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(
                        (String)map.get(
                        TestConstants.KEY_ATT_DIRECTORY_PORT), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("DS_DIRMGRPASSWD", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(
                        (String)map.get(
                        TestConstants.KEY_ATT_DS_DIRMGRPASSWD), "UTF-8"));
        }
        strBuff.append("&")
            .append(URLEncoder.encode("ROOT_SUFFIX", "UTF-8"))
            .append("=")
            .append(URLEncoder.encode((String)map.get(
                TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX), "UTF-8"))
            .append("&")
            .append(URLEncoder.encode("DS_DIRMGRDN", "UTF-8"))
            .append("=")
            .append(URLEncoder.encode(
                (String)map.get(TestConstants.KEY_ATT_DS_DIRMGRDN),
                "UTF-8"))
            .append("&")
            .append(URLEncoder.encode("DIRECTORY_SSL", "UTF-8"))
            .append("=")
            .append(URLEncoder.encode("SIMPLE", "UTF-8"))
            .append("&")
            .append(URLEncoder.encode("DATA_STORE", "UTF-8"))
            .append("=")
            .append(URLEncoder.encode(strConfigStore, "UTF-8"));

        //USER STORE PARAMETERS
        ResourceBundle umCfgData = ResourceBundle.getBundle("config" +
                    fileseparator + "UMGlobalDatastoreConfig");
        ResourceBundle umGblCfgData = ResourceBundle.getBundle("config" +
                    fileseparator + "default" + fileseparator +
                    "UMGlobalDatastoreConfig");
        String strUMStore = (String)map.get(
                    TestConstants.KEY_ATT_CONFIG_UMDATASTORE);
        log(Level.FINE, "getPostString", "UM data store is: " + strUMStore);

        if (strUMStore.equals("embedded") &&
                (strConfigStore.equals("dirServer"))) {
            log(Level.SEVERE, "getPostString", "User config datastore cannot" +
                    " be set to embedded if config datastore is not set to" +
                    " embedded");
            return "blank";
        }

        if (strUMStore.equals("embedded") &&
                (strConfigStore.equals("embedded"))) {/*
            strBuff.append("&")
                    .append(URLEncoder.encode("USERSTORE_TYPE", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode("LDAPv3", "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_MGRDN", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode("cn=Directory Manager", "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_HOST", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(host, "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_PASSWD", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(
                        (String)map.get(
                        TestConstants.KEY_ATT_AMADMIN_PASSWORD), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_SSL", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode("SIMPLE", "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_PORT", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(strConfigDirPort, "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_SUFFIX", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode((String)map.get(
                        TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX), "UTF-8"));*/
        } else {
            String strUMDSType = umCfgData.getString(
                    SMSConstants.UM_DATASTORE_PARAMS_PREFIX + strServerNo + "."
                    + SMSConstants.UM_DATASTORE_TYPE + ".0");
            String strUMSSL = "SIMPLE";
            if (umGblCfgData.getString(SMSConstants.UM_DATASTORE_PARAMS_PREFIX
                    + "." + strUMDSType + "." +
                    SMSConstants.UM_LDAPv3_LDAP_SSL_ENABLED).equals("true")) {
                strUMSSL = "SSL";
            }
            strBuff.append("&")
                    .append(URLEncoder.encode("USERSTORE_TYPE", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(strUMDSType, "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_MGRDN", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(umGblCfgData.getString(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX
                            + "." + strUMDSType + "." +
                            SMSConstants.UM_DATASTORE_ADMINID), "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_HOST", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(umCfgData.getString(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                            strServerNo + "." +
                            SMSConstants.UM_LDAPv3_LDAP_SERVER + ".0"),
                            "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_PASSWD", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(umCfgData.getString(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                             strServerNo + "." +
                             SMSConstants.UM_DATASTORE_ADMINPW + ".0"),
                            "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_SSL", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(strUMSSL, "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_PORT", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(umCfgData.getString(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                             strServerNo + "." +
                             SMSConstants.UM_LDAPv3_LDAP_PORT + ".0"),
                            "UTF-8"))
                    .append("&")
                    .append(URLEncoder.encode("USERSTORE_SUFFIX", "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(umCfgData.getString(
                            SMSConstants.UM_DATASTORE_PARAMS_PREFIX +
                             strServerNo + "." +
                             SMSConstants.UM_DATASTORE_ROOT_SUFFIX +
                            ".0" ), "UTF-8"));
        }
            //AMLDAPUSERPASSWD
        strBuff.append("&")
                .append(URLEncoder.encode("AMLDAPUSERPASSWD", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode((String)map.get(
                    TestConstants.KEY_AMC_SERVICE_PASSWORD), "UTF-8"))
                .append("&")
                .append(URLEncoder.encode("AMLDAPUSERPASSWD_CONFIRM", "UTF-8"))
                .append("=")
                .append(URLEncoder.encode((String)map.get(
                    TestConstants.KEY_AMC_SERVICE_PASSWORD), "UTF-8"));
        return strBuff.toString();
    }
    
    /** 
     * Posts the configuration data to the configurator servlet
     * strURL            The URL to post the data
     * map               A map of all the configuration data
     * strServerNo       The id number of the server being configured in
     *                   multi server tests
     */
    protected boolean postConfigData(String strURL, Map map, String strServerNo) 
    throws Exception{
        String strURLParameters = getPostString(map, strServerNo);
        if (strURLParameters.equals("blank"))
            return false;

        URL url = new URL(strURL);
        log(Level.FINEST, "postConfigData", "Configuration strURLParameters: "
                + strURLParameters);        
        log(Level.FINEST, "postConfigData", "Configuration strURL: "
                + strURL);        

        HttpURLConnection urlConn = null;
        urlConn = (HttpURLConnection) url.openConnection();
        log(Level.FINEST, "postConfigData", "AFTER OPENING CONNECTION: ");

        urlConn.setRequestMethod("POST");
        urlConn.setRequestProperty("Content-Length", "" + Integer.toString(
                strURLParameters.getBytes().length));
        urlConn.setRequestProperty("Content-Language", "en-US");  
        urlConn.setRequestProperty("Content-Type", 
                "application/x-www-form-urlencoded");
        urlConn.setUseCaches (false);
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        DataOutputStream printout = new DataOutputStream 
                (urlConn.getOutputStream ());
        printout.writeBytes (strURLParameters);
        printout.flush ();
        printout.close ();        
        log(Level.FINEST, "postConfigData", "GETTING RESPONSE ");
        
        // getting the response is required to force the request, 
        //otherwise it might not even be sent at all
        BufferedReader in = new BufferedReader(new 
                InputStreamReader(urlConn.getInputStream()));
        String input;
        StringBuffer response = new StringBuffer(256);

        while((input = in.readLine()) != null) {
                response.append(input + "\r");
        }
        log(Level.FINEST, "postConfigData", "postConfigData Response : " + 
                response);
        if ((response.toString()).contains("Configuration complete!")) {
            return true;
        } else {
            return false;
        }         
    }
    
    /**
     * Configures opensso using the configurator servlet. 
     * The map needs the following values:
     * serverurl                 <protocol + ":" + "//" + host + ":" + port>
     * serveruri                 <URI for configured instance>
     * cookiedomain              <full cookie domain name>
     * amadmin_password          <password for amadmin user>
     * urlaccessagent_password   <password for UrlAccessAgent user>
     * config_dir                <directory where product will be installed>
     * datastore                 <type of statstore: faltfile, dirServer or
     *                            activeDir>
     * directory_server          <directory server hostname>
     * directory_port            <directory server port>
     * config_root_suffix        <suffix under which configuration data will
     *                            be stored>
     * sm_root_suffix            <suffix where sms data will be stored>
     * ds_dirmgrdn               <directory user with administration
     *                            privilages>
     * ds_dirmgrpasswd           <password for directory user with
     *                            administration privilages>
     * umdatastore                <type of userstore: embedded or dirServer>
     * 
     * strServerNo               The id number of the server being configured in
     *                           multi server tests
     */
    protected boolean configureProduct(Map map, String strServerNo)
    throws Exception {
        entering("configureProduct", null);
        log(Level.FINEST, "configureProduct", "Configuration Map: " + map);
        WebClient webclient = new WebClient();
        String strURL = (String)map.get("serverurl") +
                (String)map.get("serveruri") + "/config/options.htm";
        log(Level.FINEST, "configureProduct", "strURL: " + strURL);
        URL url = new URL(strURL);
        HtmlPage page = null;
        int pageIter = 0;
        try {
            // THIS WHILE IS WRITTEN BECUASE IT TAKES SOME TIME FOR INITIAL
            // CONFIGURATOR PAGE TO LOAD AND WEBCLIENT CALL DOES NOT WAIT
            // FOR SUCH A DURATION.
            while (page == null && pageIter <= 30) {
                try {
                    page = (HtmlPage)webclient.getPage(url);
                    Thread.sleep(10000);
                    pageIter++;
                } catch (com.gargoylesoftware.htmlunit.ScriptException e) {
                    log(Level.SEVERE, "configureProduct", strURL + " cannot " +
                            "be reached.");
                }
            }
        } catch(com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException e)
        {
            log(Level.SEVERE, "configureProduct", strURL +
                    " cannot be reached.");
            return false;
        }
        
        if (pageIter > 30) {
            log(Level.SEVERE, "configureProduct",
                    "Product Configuration was not" +
                    " successfull." + strURL + "was not found." +
                    " Please check if war is deployed properly.");
            exiting("configureProduct");
            return false;
        }
        
        if (getHtmlPageStringIndex(page, "Not Found") != -1) {
            log(Level.SEVERE, "configureProduct",
                    "Product Configuration was not" +
                    " successfull." + strURL + "was not found." +
                    " Please check if war is deployed properly.");
            exiting("configureProduct");
            return false;
        }
        
        if (getHtmlPageStringIndex(page, "configuration") != -1) {
            log(Level.FINE, "configureProduct", "Inside configurator.");
            log(Level.FINE, "configureProduct", "----New config start----");
            String baseDir = getBaseDir() + fileseparator
                    + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                    + fileseparator + "built" + fileseparator + "classes"
                    + fileseparator;
            String xmlfile = baseDir + "config" + fileseparator + 
                    "ConfigXMlData.xml";
            
            //Posting configuration parameters to the configurator servlet
            if (postConfigData((String)map.get("serverurl") +
                (String)map.get("serveruri") + "/config/configurator", map, 
                strServerNo)) {
              String strNewURL = (String)map.get("serverurl") +
                        (String)map.get("serveruri") + "/UI/Login" + "?" +
                        "IDToken1=" + map.get(
                        TestConstants.KEY_ATT_AMADMIN_USER) + "&IDToken2=" +
                        map.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD);
                log(Level.FINE, "configureProduct", "strNewURL: " + strNewURL);
                url = new URL(strNewURL);
                try {
                    page = (HtmlPage)webclient.getPage(url);
                } catch (com.gargoylesoftware.htmlunit.ScriptException e) {
                    log(Level.SEVERE, "configureProduct", strURL + " cannot " +
                            "be reached.");
                    return false;
                }
                if ((getHtmlPageStringIndex(page, "Authentication Failed") 
                        != -1) ||
                        (getHtmlPageStringIndex(page, "/config/options.htm")
                        != -1)) {
                    log(Level.SEVERE, "configureProduct",
                            "Product Configuration was" +
                            " not successfull. Configuration failed.");
                    exiting("configureProduct");
                    return false;
                } else {
                    log(Level.FINE, "configureProduct",
                            "Product Configuration was" +
                            " successfull. New bits were successfully " +
                            "configured.");
                createShowServerConfigDataFile((String)map.get("serverurl"),
                        (String)map.get("serveruri"), webclient);
                    strNewURL = (String)map.get("serverurl") +
                            (String)map.get("serveruri") + "/UI/Logout";
                    consoleLogout(webclient, strNewURL);
                    exiting("configureProduct");
                    return true;
                }                
            } else {
                log(Level.SEVERE, "configureProduct",
                    "Product Configuration was not" +
                    " successfull. The configurator servlet was not found." +
                    " Please check if war is deployed properly.");
                exiting("configureProduct");
                return false;
            }
        } else {
            String strNewURL = (String)map.get("serverurl") +
                    (String)map.get("serveruri") + "/UI/Login" + "?" +
                    "IDToken1=" + adminUser + "&IDToken2=" +
            map.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD);
            log(Level.FINE, "configureProduct", "strNewURL: " + 
                    strNewURL);
            url = new URL(strNewURL);
            page = (HtmlPage)webclient.getPage(url);
            if (getHtmlPageStringIndex(page, 
                    "Authentication Failed") != -1) {
                log(Level.FINE, "configureProduct", 
                        "Product was already configured. " + 
                        "Super admin login failed.");
                exiting("configureProduct");
                return false;
            } else {
                log(Level.FINE, "configureProduct", "Product was " + 
                        "already configured. " + 
                        "Super admin login successful.");
                createShowServerConfigDataFile((String)map.get("serverurl"),
                        (String)map.get("serveruri"), webclient);
                strNewURL = (String)map.get("serverurl") +
                        (String)map.get("serveruri") + "/UI/Logout";
                consoleLogout(webclient, strNewURL);
                exiting("configureProduct");
                return true;
            }
        }
    }

    /**
     * Creates an html file containing the information output from
     * showServerConfig.jsp 
     * @param server FQDN of server
     * @param uri URI of server
     * @param webclient handle to browser with super admin authenticated session
     * @throws java.lang.Exception
     */
    protected void createShowServerConfigDataFile(String server, String uri,
            WebClient webclient)
            throws Exception {
                String strSSC = server + uri + "/showServerConfig.jsp";
                URL url = new URL(server);
                String strHost = url.getHost();
                String strURI = uri;
                try {
                String strFilName = url.getProtocol() + "_" +
                        strHost.replace(".", "_") + "_" + url.getPort() +
                        "_" + strURI.replace("/", "");
                HtmlPage page = (HtmlPage)webclient.getPage(strSSC);
                BufferedWriter out =
                        new BufferedWriter(new FileWriter(getBaseDir() +
                        fileseparator + serverName + fileseparator + 
                        strFilName + ".html"));
                out.write(page.asXml());
                out.close();
                 } catch(Exception e)
                 {
                    log(Level.SEVERE, "configureProduct", ",showServer" +
                    "Config.jsp is not available Check server " + "instance");
                 }
    }
    
    /**
     * Logout from admin console using htmlunit
     */
    protected void consoleLogout(
            WebClient webclient,
            String amUrl)
            throws Exception {
        entering("consoleLogout", null);
        log(Level.FINEST, "consoleLogout", "JavaScript Enabled: " +
                webclient.isJavaScriptEnabled());
        log(Level.FINEST, "consoleLogout", "Redirect Enabled: " +
                webclient.isRedirectEnabled());
        log(Level.FINEST, "consoleLogout", "URL: " + amUrl);
        URL url = new URL(amUrl);
        HtmlPage page = (HtmlPage)webclient.getPage(amUrl);
        log(Level.FINEST, "consoleLogout", "Page title after logout: " +
                page.getTitleText());
        exiting("consoleLogout");
    }
    
    /**
     * Checks whether the string exists on the page
     */
    protected int getHtmlPageStringIndex(
            HtmlPage page,
            String searchStr)
            throws Exception {
        entering("getHtmlPageStringIndex", null);
        String strPage;
        try {
            strPage = page.asXml();
        } catch (java.lang.NullPointerException npe) {
            log(Level.FINEST, "getHtmlPageStringIndex", "Page object is NULL");
            return 0;
        }
        log(Level.FINEST, "getHtmlPageStringIndex", "Search string: " +
                searchStr);
        log(Level.FINEST, "getHtmlPageStringIndex", "Search page\n:" +
                strPage);
        int iIdx = strPage.indexOf(searchStr);
        if (iIdx != -1)
            log(Level.FINEST, "getHtmlPageStringIndex",
                    "Search string found on page: " + iIdx);
        else
            log(Level.FINEST, "getHtmlPageStringIndex",
                    "Search string not found on page: " + iIdx);
        exiting("getHtmlPageStringIndex");
        return iIdx;
    }
    
    /**
     * Checks whether the string exists on the page and optionally logs page
     */
    protected int getHtmlPageStringIndex(
            HtmlPage page,
            String searchStr,
            boolean isLog)
            throws Exception {
        entering("getHtmlPageStringIndex", null);
        String strPage;
        try {
            strPage = page.asXml();
        } catch (java.lang.NullPointerException npe) {
            log(Level.FINEST, "getHtmlPageStringIndex", "Page object is NULL");
            return 0;
        }
        int iIdx = strPage.indexOf(searchStr);
        if (isLog){
            log(Level.FINEST, "getHtmlPageStringIndex", "Search page\n:" +
                    strPage);
            if (iIdx != -1)
                log(Level.FINEST, "getHtmlPageStringIndex",
                        "Search string found on page: " + iIdx);
            else
                log(Level.FINEST, "getHtmlPageStringIndex",
                        "Search string not found on page: " + iIdx);
        }
        exiting("getHtmlPageStringIndex");
        return iIdx;
    }
    
    /**
     * Reads data from a Map object, creates a new file and writes data to that
     * file
     */
    protected void createFileFromMap(Map properties, String fileName)
    throws Exception {
        entering("createFileFromMap", null);
        log(Level.FINEST, "createFileFromMap", "Map: " + properties);
        log(Level.FINEST, "createFileFromMap", "fileName: " + fileName);
        StringBuffer buff = new StringBuffer();
        for (Iterator i = properties.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            String valueString = entry.getValue().toString();
            buff.append(entry.getKey());
            buff.append("=");
            if (valueString.length() != 0)
                buff.append(valueString.substring(0, valueString.length()));
            buff.append("\n");
        }
        
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
        out.write(buff.toString());
        out.close();
        exiting("createFileFromMap");
    }
    
    /**
     * Reads data from a ResourceBundle object and creates a Map containing all
     * the attribute keys and values. It also takes in a Set of attribute key
     * names, which if specified, are not put into the Map. This is to ensure
     * selective selection of attribute key and value pairs. One can further
     * specify to search for a key containig a specific string (str).
     */
    protected Map getMapFromResourceBundle(String rbName, String str, Set set)
    throws Exception {
        entering("getMapFromResourceBundle", null);
        Map map = new HashMap();
        ResourceBundle rb = ResourceBundle.getBundle(rbName);
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String value = (String)rb.getString(key);
            if (set != null) {
                if (!set.contains(key)) {
                    if (str != null) { 
                       if (key.indexOf(str) != -1)
                            map.put(key, value);
                    } else
                        map.put(key, value);
                }
            } else {
                if (str != null) {
                    if (key.indexOf(str + ".") != -1)
                        map.put(key, value);
                } else
                    map.put(key, value);
            }
        }
        exiting("getMapFromResourceBundle");
        return (map);
    }
    
    /**
     * Reads data from a ResourceBundle object and creates a Map containing all
     * the attribute keys and values.
     * @param resourcebundle name
     */
    protected Map getMapFromResourceBundle(String rbName)
    throws Exception {
        return (getMapFromResourceBundle(rbName, null, null));
    }

    /**
     * Reads data from a ResourceBundle object and creates a Map containing all
     * the attribute keys and values. One can further specify to search for a
     * key containig a specific string (str)
     * @param rbName name
     * @param str string to match contained in the key
     * @return Map containing key-value pairs
     * @throws java.lang.Exception
     */
    protected Map getMapFromResourceBundle(String rbName, String str)
    throws Exception {
        return(getMapFromResourceBundle(rbName, str, null));
    }
    
    /**
     * Returns a map of String to Set of String from a formatted string.
     * @param str. The format of the string is key1=val1,val2,val3;..;key2=val1,
     * val2,val3,;..
     * @return Map containing key-value pairs
     * @throws java.lang.Exception
     */
    protected Map<String, Set<String>> parseStringToMap(String str)
    throws Exception {
        return (parseStringToMap(str, ";", ","));
    }
    
    /**
     * Returns a map of String to Set of String from a formatted string.
     * @param str. The format of the string is key1=val1 sTok val2 sTok val3
     * mTok and so on
     * @param mTok. Seperator for single key-value pair
     * @param sTok. Seperator for values withing values string
     * @return Map containing key-value pairs
     * @throws java.lang.Exception
     */
    protected Map<String, Set<String>> parseStringToMap(String str, String mTok,
            String sTok)
    throws Exception {
        entering("parseStringToMap", null);
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        StringTokenizer st = new StringTokenizer(str, mTok);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int idx = token.indexOf("=");
            if (idx != -1) {
                Set<String> set = new HashSet<String>();
                map.put(token.substring(0, idx).trim(), set);
                StringTokenizer st1 = new StringTokenizer(
                        token.substring(idx+1), sTok);
                while (st1.hasMoreTokens()) {
                    set.add(st1.nextToken().trim());
                }
            }
        }
        exiting("parseStringToMap");
        return map;
    }
    
    /**
     * Returns a list of String from a formatted string.
     * @param str. The format of the string is key1=val1,val2,val3;..;key2=val1,
     * val2,val3,;..
     * @return List containing key-value pairs
     * @throws java.lang.Exception
     */
    protected List parseStringToList(String str)
    throws Exception {
        return (parseStringToList(str, ";", ","));
    }
    
    /**
     * Returns a list of String from a formatted string.
     * @param str. The format of the string is key1=val1 sTok val2 sTok val3
     * mTok and so on
     * @param mTok. Seperator for single key-value pair
     * @param sTok. Seperator for values withing values string
     * @return List containing key-value pairs
     * @throws java.lang.Exception
     */
    protected List parseStringToList(String str, String mTok,
            String sTok)
    throws Exception {   
        ArrayList list = new ArrayList();
        StringTokenizer st = new StringTokenizer(str, mTok);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int idx = token.indexOf("=");
            if (idx != -1) {
                String attrName = token.substring(0, idx).trim();     
                StringTokenizer st1 = new StringTokenizer(
                        token.substring(idx+1), sTok);
                while (st1.hasMoreTokens()) {
                    String attrValue = st1.nextToken().trim();
                    String attr = attrName + "=" + attrValue;
                    list.add(attr);
                }
            }
        }        
        return list;
    }        
    
    /**
     * Returns set of string. This is a convenient method for adding a set of
     * string into a map. In this project, we usually have the
     * <code>Map&lt;String, Set&lt;String&gt;&gt; and many times, we just
     * want to add a string to the map.
     */
    protected Set<String> putSetIntoMap(
            String key,
            Map<String, Set<String>> map,
            String value
            )
    throws Exception {
        entering("putSetIntoMap", null);
        Set<String> set = new HashSet<String>();
        set.add(value);
        map.put(key, set);
        exiting("putSetIntoMap");
        return set;
    }
    
    /**
     * Returns LoginURL based on the realm under test
     * @param realm
     * @return loginURL
     */
    protected String getLoginURL(String strOrg) {
        entering("getLoginURL", null);
        String loginURL;
        if ((strOrg.equals("")) || (strOrg.equalsIgnoreCase("/"))) {
            loginURL = protocol + ":" + "//" + host + ":" + port + uri
                    + "/UI/Login";
        } else {
            loginURL = protocol + ":" + "//" + host + ":" + port + uri
                    + "/UI/Login" + "?org=" + strOrg ;
        }
        exiting("getLoginURL");
        return loginURL;
    }
    
    /**
     * Returns the List for the given tokens
     * @param string tokens
     * @return list of the tokens
     */
    protected List getListFromTokens(StringTokenizer strTokens)
    throws Exception {
        entering("getListFromTokens", null);
        List<String> list = new ArrayList<String>();
        while (strTokens.hasMoreTokens()) {
            list.add(strTokens.nextToken());
        }
        exiting("getListFromTokens");
        return list;
    }
    
    /*
     * Gets the baseDirectory to create the XML files
     */
    protected String getTestBase()
    throws Exception {
        entering("getTestBase", null);
        String testbaseDir = null;
        ResourceBundle rbamconfig = ResourceBundle.getBundle(
                TestConstants.TEST_PROPERTY_AMCONFIG);
        testbaseDir = getBaseDir() + fileseparator
                + rbamconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                + fileseparator + "built"
                + fileseparator + "classes"
                + fileseparator ;
        exiting("getTestBase");
        return (testbaseDir);
    }
    
    /**
     * Takes a token separated string and returns each individual
     * token as part of a list.
     */
    protected List getAttributeList(String strList, String token)
    throws Exception {
        entering("getAttributeList", null);
        StringTokenizer stk = new StringTokenizer(strList, token);
        List<String> attList = new ArrayList<String>();
        while (stk.hasMoreTokens()) {
            attList.add(stk.nextToken());
        }
        exiting("getAttributeList");
        return (attList);
    }
    
    /**
     * Takes a token separated string and returns each individual
     * token as part of a Map.
     */
    protected Map getAttributeMap(String strList, String token)
    throws Exception {
        entering("getAttributeMap", null);
        StringTokenizer stk = new StringTokenizer(strList, token);
        Map map = new HashMap();
        int idx;
        String strToken;
        while (stk.hasMoreTokens()) {
            strToken = stk.nextToken();
            idx = strToken.indexOf("=");
            if (idx != -1) {
                map.put(strToken.substring(0, idx), strToken.substring(idx + 1,
                        strToken.length()));
            }
        }
        log(Level.FINEST, "getAttributeMap", map);
        exiting("getAttributeMap");
        return (map);
    }
    
    /**
     * Returns set of string. This is a convenient method for adding multipe set
     * of string into a map. The value contains the multiple string sepearete by
     * token string.
     */
    protected Set<String> putSetIntoMap(
            String key,
            Map<String, Set<String>> map,
            String value,
            String token
            )
            throws Exception {
        entering("putSetIntoMap", null);
        StringTokenizer stk = new StringTokenizer(value, token);
        Set<String> setValue = new HashSet<String>();
        while (stk.hasMoreTokens()) {
            setValue.add(stk.nextToken());
        }
        map.put(key, setValue);
        exiting("putSetIntoMap");
        return setValue;
    }
    
    /**
     * Concatenates second set to a first set
     * @param set1 first set
     * @param set2 second set to be concatenated with the first set
     */
    protected void concatSet(Set set1, Set set2)
    throws Exception {
        entering("concatSet", null);
        Iterator keyIter = set2.iterator();
        String item;
        while (keyIter.hasNext()) {
            item = (String)keyIter.next();
            set1.add(item);
        }
        exiting("concatSet");
    }
    
    /**
     * Returns true if the value set contained in the Set
     * contains the requested string.
     */
    protected boolean setValuesHasString(Set set, String str)
    throws Exception {
        entering("setValuesHasString", null);
        log(Level.FINEST, "setValuesHasString", "The values in the set are:\n" +
                set);
        boolean res = false;
        Iterator keyIter = set.iterator();
        String item;
        Object obj;
        while (keyIter.hasNext()) {
            obj = (Object)keyIter.next();
            item = obj.toString();
            if (item.indexOf(str) != 0) {
                res = true;
                break;
            }
        }
        exiting("setValuesHasString");
        return res;
    }
    
    /**
     * Returns protocol, host, port and uri from a given url.
     * Map contains value pairs in the form of:
     * protocol, protocol value
     * host, host value
     * port, port value
     * uri, uri value
     */
    protected Map getURLComponents(String strNamingURL)
    throws Exception {
        entering("getURLComponents", null);
        Map map = new HashMap();
        int iFirstSep = strNamingURL.indexOf(":");
        String strProtocol = strNamingURL.substring(0, iFirstSep);
        map.put("protocol", strProtocol);
        
        int iSecondSep = strNamingURL.indexOf(":", iFirstSep + 1);
        String strHost = strNamingURL.substring(iFirstSep + 3, iSecondSep);
        map.put("host", strHost);
        
        int iThirdSep = strNamingURL.indexOf(uriseparator, iSecondSep + 1);
        String strPort = strNamingURL.substring(iSecondSep + 1, iThirdSep);
        map.put("port", strPort);
        
        int iFourthSep = strNamingURL.indexOf(uriseparator, iThirdSep + 1);
        String strURI = uriseparator + strNamingURL.substring(iThirdSep + 1,
                iFourthSep);
        map.put("uri", strURI);
        exiting("getURLComponents");
        
        return (map);
    }
    
    /**
     * Replace all tags in a file with actual value that are defined in a map
     * @param inFile input file name to be replaced with the tag
     * @param outFile output file name with actual value
     * @param valMap a map contains tag name and value i.e.
     * [ROOT_SUFFIX, dc=sun,dc=com]
     */
    protected void replaceStringInFile(String inFile, String outFile,
            Map valMap)
    throws Exception {
        entering("replaceStringInFile", null);
        String key = null;
        String value = null;
        String outputStr = null;
        Iterator keyIter;
        BufferedReader buff = new BufferedReader(new FileReader(inFile));
        StringBuffer sb = new StringBuffer();
        for (String inputStr = buff.readLine(); (inputStr != null);
        inputStr = buff.readLine()) {
            keyIter = valMap.keySet().iterator();
            while (keyIter.hasNext()) {
                key = (String)keyIter.next();
                value = (String)valMap.get(key);
                inputStr = inputStr.replaceAll(key, value);
            }
            sb.append(inputStr + "\n");
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
        out.write(sb.toString());
        out.close();
        exiting("replaceStringInFile");
    }
    
    /**
     * Returns the SSOToken of a user.
     */
    protected SSOToken getUserToken(SSOToken requester, String userId)
    throws Exception {
        entering("getUserToken", null);
        SSOToken stok = null;
        if (validateToken(requester)) {
            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            Set set = stMgr.getValidSessions(requester, host);
            Iterator it = set.iterator();
            String strLocUserID;
            while (it.hasNext()) {
                stok = (SSOToken)it.next();
                strLocUserID = stok.getProperty("UserId");
                log(Level.FINEST, "getUserToken", "UserID: " + strLocUserID);
                if (strLocUserID.equals(userId))
                    break;
            }
        }
        exiting("getUserToken");
        return (stok);
    }

    /**
      * Returns a unused port on a given host.
      *    @return available port num if found. -1 of not found.
      */
    public int getUnusedPort()
        throws Exception
    {
        entering("getUnusedPort", null);

        int defaultPort = -1;
        int start = 44444;
        int incr = 100;
        InetAddress inetAdd = InetAddress.getLocalHost();

        for (int i = start; i < 65500 && (defaultPort == -1); i += incr) {
            Random rnd = new Random();
            int rNum = rnd.nextInt(1000);
            if (canUseAsPort(inetAdd.getHostAddress(), i+rNum)) {
                log(Level.FINEST, "getUnusedPort", "Random number is: " + rNum);
                defaultPort = i+rNum;
            }
        }
        exiting("getUnusedPort");

        return defaultPort; 
    }

     /**
      * Checks whether the given host:port is currenly under use.
      *    @param hostname (eg localhost)
      *    @param incr : port number.
      *    @return  true if not in use, false if in use.
      */
     private boolean canUseAsPort(String hostname, int port) 
             throws Exception {
        boolean canUseAsPort = false;
        ServerSocket serverSocket = null;
        try {
            InetSocketAddress socketAddress =
                new InetSocketAddress(hostname, port);
            serverSocket = new ServerSocket();
            serverSocket.bind(socketAddress);
            canUseAsPort = true;
     
            serverSocket.close();
       
            Socket s = null;
            try {
              s = new Socket();
              s.connect(socketAddress, 1000);
              canUseAsPort = false;
            } catch (Throwable t) {
            }
            finally {
              if (s != null) {
                try {
                  s.close();
                } catch (Throwable t) { }
              }
            }
        } catch (IOException ex) {
          canUseAsPort = false;
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (Exception ex) { }
        }
     
        return canUseAsPort;
    }

     /**
     * Start the notification (jetty) server for getting notifications from the
     * server.
     */
    protected Map startNotificationServer()
    throws Exception {
                Map map = new HashMap();
        String strNotURL = rb_amconfig.getString(
                TestConstants.KEY_AMC_NOTIFICATION_URL);
        log(Level.FINEST, "startNotificationServer", "Notification URI: " +
                strNotURL);
        Map notificationURLMap = getURLComponents(strNotURL + "/");

        String  strPort = (String)notificationURLMap.get("port");
        String  strURI = (String)notificationURLMap.get("uri");

        log(Level.FINEST, "startNotificationServer", "Notification Port: " +
                strPort + ", uri is " + strURI);

        int deployPort  = new Integer(strPort).intValue();
        server = new Server(deployPort);
	log(Level.FINE, "startNotificationServer", "Starting the notification" +
                " (jetty) server");

        String deployURI = rb_amconfig.getString(TestConstants.
                KEY_INTERNAL_WEBAPP_URI);
        log(Level.FINE, "startNotificationServer", "Deploy URI: " +
                deployURI);

        WebAppContext wac = new WebAppContext();
        wac.setContextPath(deployURI);
        String warFile = getBaseDir() + "/data/common/internalwebapp.war";
        log(Level.FINE, "startNotificationServer", "WAR File: " +
                warFile);
        wac.setWar(warFile);
        log(Level.FINE, "startNotificationServer", "Deploy URI: " +
                deployURI);
        server.setHandler(wac);
        server.start();

        // This is added as jetty takes some time to bootstrap and we
        // do not want to register for notification unless server is up
        Thread.sleep(10000);

        map = registerNotificationServerURL();
        log(Level.FINE, "startNotificationServer", "Registered the " +
               "notification url");
        return map;
    }

    /**
     * Stop the notification (jetty) server for getting notifications from the
     * server.
     */
    protected void stopNotificationServer(Map notificationIDMap)
    throws Exception
     {
       try {
       log(Level.FINE, "stopNotificationServer", "Stopping the notification" +
                " (jetty) server");
        String strNotURL = rb_amconfig.getString(
                TestConstants.KEY_AMC_NOTIFICATION_URL);
        server.stop();
        WebClient wc = new WebClient();
        HtmlPage jettypage = (HtmlPage)wc.getPage(strNotURL);
        int i = 0;
        while((jettypage.getWebResponse().getContentAsString().
                contains("jetty")) && (i < 60)){
            log(Level.FINE, "stopNotificationServer", "Jetty server is up. " +
                    "Waiting for the jetty process to die");
            Thread.sleep(5000);
            jettypage = (HtmlPage)wc.getPage(strNotURL);
            i++;
        }
        if (jettypage.getWebResponse().getContentAsString().contains("jetty")) {
             log(Level.SEVERE, "stopNotificationServer", "Jetty server is " +
                     "Still up. Couldn't shut down the server");
        }
        deregisterNotificationServerURL(notificationIDMap);
        } catch (ConnectException e) {
            log(Level.FINEST, "stopNotificationServer", "Notification server" +
                    " is stopped");
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Register the notification (jetty) server for getting notifications from
     * the server.
     */
    protected Map registerNotificationServerURL()
    throws Exception {
        Map notificationIDMap = new HashMap();
        log(Level.FINE, "registerNotificationServerURL", "Register the " +
                "notification (jetty) server");
        String strNotURL = rb_amconfig.getString(
                TestConstants.KEY_AMC_NOTIFICATION_URL);
         //For SMS
        SOAPClient client = new SOAPClient(JAXRPCUtil.SMS_SERVICE);
        String strNotificationID1 = (String)client.
                send("registerNotificationURL", strNotURL, null, null);
        notificationIDMap.put("ID1", strNotificationID1);
        //For IDRepo
        client = new SOAPClient("DirectoryManagerIF");
        // Register for AMSDK notifications
        String strNotificationID2 = (String)client.
                send("registerNotificationURL", strNotURL, null, null);
        notificationIDMap.put("ID2", strNotificationID2);
        // Register for IdRepo Service
        String strNotificationID3 = (String)client.
                send("registerNotificationURL_idrepo", strNotURL, null, null);
        notificationIDMap.put("ID3", strNotificationID3);
        return notificationIDMap;
    }

    /**
     * Deregister the notification (jetty) server for getting notifications from
     * the server.
     */
    protected void deregisterNotificationServerURL(Map notificationIDMap)
    throws Exception {
        log(Level.FINE, "deregisterNotificationServerURL", "Deregister the " +
                "notification (jetty) server");
        String strID1 = (String)notificationIDMap.get("ID1");
        SOAPClient client = new SOAPClient(JAXRPCUtil.SMS_SERVICE);
        client.send("deRegisterNotificationURL", strID1, null, null);
        //For IDRepo
        client = new SOAPClient("DirectoryManagerIF");
        // Register for AMSDK notifications
        String strID2 = (String)notificationIDMap.get("ID2");
        client.send("deRegisterNotificationURL", strID2, null, null);
        // Register for IdRepo Service
        String strID3 = (String)notificationIDMap.get("ID3");
        client.send("deRegisterNotificationURL_idrepo", strID3, null, null);
        log(Level.FINE, "deregisterNotificationServerURL", "Completed " +
                "deregistering the notification url's");
    }

    /**
     * This method deploys client sdk war on jetty server, if the war file type
     * is set to internal else it will point to the external url.
     * @param rb_client
     * @return client sdk url
     * @throws Exception
     */
    protected String deployClientSDKWar(ResourceBundle rb_client)
    throws Exception {
        cltWarDeployPort = getUnusedPort();
        log(Level.FINE, "deployClientSDKWar", "Deploy port: " +
                cltWarDeployPort);

        String strWarType = rb_client.getString("warfile_type");
        String strClientDomain = rb_client.getString("client_domain_name");
        String warFile = rb_client.getString("war_file");

        InetAddress addr = InetAddress.getLocalHost();
        String hostname = addr.getCanonicalHostName() + strClientDomain;
        log(Level.FINE, "deployClientSDKWar", "Deploy host: " + hostname);

        String userHomeDir = System.getProperty("user.home");
        deleteDirectory(userHomeDir + fileseparator + "OpenSSOClient",
                hostname.replace(".", "_") + "_" + cltWarDeployPort);

        if (strWarType.equals("internal")) {
            server = new Server();
            if (protocol.equals("https")) {
                SslSocketConnector sslconnector = new SslSocketConnector();
                sslconnector.setMaxIdleTime(30000);
                String trustCertType = rb_client.getString("trust_cert_type");
                String trustCertPassword =
                        rb_client.getString("trust_cert_password");
                String javaHome = System.getProperty("java.home");
                log(Level.FINE, "deployClientSDKWar", "JAVA HOME: " +
                        javaHome);
                String s = null;
                if (trustCertType.equals("external")) {
                    String strKeystore =
                            rb_client.getString("trust_cert_store");
                    sslconnector.setKeystore(strKeystore);
                    sslconnector.setTruststore(strKeystore);
                    sslconnector.setPassword(trustCertPassword);
                    sslconnector.setKeyPassword(trustCertPassword);
                    sslconnector.setTrustPassword(trustCertPassword);
                    String listKeyStore = javaHome + fileseparator + "bin" +
                            fileseparator + "keytool -list -keystore " +
                            strKeystore + " -storepass " + trustCertPassword;
                    log(Level.FINE, "deployClientSDKWar", "List keystore" +
                            " command: " + listKeyStore);
                    Process p = Runtime.getRuntime().exec(listKeyStore);
                    BufferedReader stdInput = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    BufferedReader stdError = new BufferedReader(
                            new InputStreamReader(p.getErrorStream()));

                    // read the output from the command                    
                    log(Level.FINEST, "deployClientSDKWar", "The following is" +
                            " the standard output of generate keystore" +
                            " command: ");                    
                    while ((s = stdInput.readLine()) != null) {
                        log(Level.FINEST, "deployClientSDKWar", s);
                        if (s.contains("Keystore file does not exist")) {
                            log(Level.SEVERE, "deployClientSDKWar",
                                    "Configuring war using external " +
                                    " certificate failed. The reason: Either" +
                                    " system could not find the sepcified" +
                                    " keystore or does not have read" +
                                    " permission to it.");
                            assert false;
                        } else if (s.contains("password was incorrect")) {
                            log(Level.SEVERE, "deployClientSDKWar",
                                    "Configuring war using external signed" +
                                    " certificate failed. The reason: The" +
                                    " password to access the keystore is" +
                                    " incorrect.");
                            assert false;
                        }                         
                    }
                    
                    // read any errors from the attempted command
                    log(Level.FINEST, "deployClientSDKWar", "The following is" +
                            " the standard error of generate keystore" +
                            " command: ");                    
                    while ((s = stdError.readLine()) != null) {
                        log(Level.FINEST, "deployClientSDKWar", s);
                    }                    
                } else {
 
                    String baseDir = getBaseDir() + fileseparator +
                            rb_amconfig.getString(
                            TestConstants.KEY_ATT_SERVER_NAME);
                    deleteDirectory(baseDir + fileseparator + "keystore");
                    createDirectory(baseDir + fileseparator + "keystore");
                    String genKeyStore = javaHome + fileseparator + "bin" +
                            fileseparator + "keytool -genkey -dname cn=" +
                            host + " -validity 365 -keystore " + baseDir +
                            fileseparator + "keystore" + fileseparator + 
                            "clientkeystore.jks -storepass " +
                            trustCertPassword + " -keypass " +
                            trustCertPassword;
                    log(Level.FINE, "deployClientSDKWar", "Generate keystore" +
                            " command: " + genKeyStore);
                    Process p = Runtime.getRuntime().exec(genKeyStore);
                    BufferedReader stdInput = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    BufferedReader stdError = new BufferedReader(
                            new InputStreamReader(p.getErrorStream()));

                    // read the output from the command                    
                    log(Level.FINEST, "deployClientSDKWar", "The following is" +
                            " the standard output of generate keystore" +
                            " command: ");                    
                    while ((s = stdInput.readLine()) != null) {
                        log(Level.FINEST, "deployClientSDKWar", s);
                    }
                    
                    // read any errors from the attempted command
                    log(Level.FINEST, "deployClientSDKWar", "The following is" +
                            " the standard error of generate keystore" +
                            " command: ");                    
                    while ((s = stdError.readLine()) != null) {
                        log(Level.FINEST, "deployClientSDKWar", s);
                    }
                    
                    String exportKeyStore = javaHome + fileseparator + "bin" +
                            fileseparator + "keytool -export -alias mykey" +
                            " -file " + baseDir + fileseparator + "keystore" +
                            fileseparator + "clientcert.cer" + " -keystore " +
                            baseDir + fileseparator + "keystore" +
                            fileseparator + "clientkeystore.jks" +
                            " -storepass " + trustCertPassword +
                            " -keypass " + trustCertPassword;
                    log(Level.FINE, "deployClientSDKWar", "Export keystore" +
                            " command: " + exportKeyStore);
                    
                    p = Runtime.getRuntime().exec(exportKeyStore);
                    
                    stdInput = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    stdError = new BufferedReader(
                            new InputStreamReader(p.getErrorStream()));

                    // read the output from the command
                    log(Level.FINEST, "deployClientSDKWar", "The following is" +
                            " the standard output of generate keystore" +
                            " command: ");                    
                    while ((s = stdInput.readLine()) != null) {
                        log(Level.FINEST, "deployClientSDKWar", s);
                    }
                    
                    // read any errors from the attempted command
                    log(Level.FINEST, "deployClientSDKWar", "The following is" +
                            " the standard error of generate keystore" +
                            " command: ");                    
                    while ((s = stdError.readLine()) != null) {
                        log(Level.FINEST, "deployClientSDKWar", s);
                    }

                    String importKeyStore = javaHome + fileseparator + "bin" +
                            fileseparator + "keytool -import -noprompt " + 
                            "-trustcacerts" + " -alias " + host + " -file " +
                            baseDir + fileseparator + "keystore" +
                            fileseparator + "clientcert.cer" + " -keystore " +
                            javaHome + fileseparator + "lib" + fileseparator + 
                            "security" + fileseparator + "cacerts" +
                            " -storepass " + trustCertPassword + " -keypass " +
                            trustCertPassword;
                    log(Level.FINE, "deployClientSDKWar", "Import keystore" +
                            " command: " + importKeyStore);
                    
                    p = Runtime.getRuntime().exec(importKeyStore);
                    
                    stdInput = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    stdError = new BufferedReader(
                            new InputStreamReader(p.getErrorStream()));

                    // read the output from the command
                    log(Level.FINEST, "deployClientSDKWar", "The following is" +
                            " the standard output of generate keystore" +
                            " command: ");                    
                    while ((s = stdInput.readLine()) != null) {
                        log(Level.FINEST, "deployClientSDKWar", s);
                        if (s.contains("Permission denied")) {
                            log(Level.SEVERE, "deployClientSDKWar",
                                    "Configuring war using self signed" +
                                    " certificate failed. The reason: Either" +
                                    " system could not find the sepcified" +
                                    " keystore or does not have write" +
                                    " permission to it.");
                            assert false;
                        } else if (s.contains("already exists")) {
                            log(Level.SEVERE, "deployClientSDKWar",
                                    "Configuring war using self signed" +
                                    " certificate failed. The reason: An" +
                                    " entry already exits in the keystore by" +
                                    " the same alias.");
                            assert false;
                        }                        
                    }
                    
                    // read any errors from the attempted command
                    log(Level.FINEST, "deployClientSDKWar", "The following is" +
                            " the standard error of generate keystore" +
                            " command: ");                    
                    while ((s = stdError.readLine()) != null) {
                        log(Level.FINEST, "deployClientSDKWar", s);
                    }                    
                }
                sslconnector.setPort(cltWarDeployPort);
                sslconnector.setHost(hostname);
                server.addConnector(sslconnector);
            } else {
                Connector connector = new SelectChannelConnector();
                connector.setPort(cltWarDeployPort);
                connector.setHost(hostname);
                server.addConnector(connector);
            }

            WebAppContext wac = new WebAppContext();

            //String deployURI = rb_client.getString("deploy_uri");
            log(Level.FINE, "deployClientSDKWar", "Deploy URI: " + csDeployURI);
            wac.setContextPath(csDeployURI);

            clientURL = protocol + "://" + hostname + ":" +
                    cltWarDeployPort + csDeployURI;
            log(Level.FINE, "deployClientSDKWar", "Client URL: " + clientURL);
            if (new File(warFile).exists()) {
                log(Level.FINE, "deployClientSDKWar", "WAR File: " + warFile);
                wac.setWar(warFile);

                server.setHandler(wac);
                server.setStopAtShutdown(true);

                log(Level.FINE, "deployClientSDKWar",
                        "Deploying war and starting jetty server");
                server.start();
                log(Level.FINE, "deployClientSDKWar", "Deployed war and " +
                        "started jetty server");
            } else {
                log(Level.SEVERE, "deployClientSDKWar", "The client war file"
                        + warFile + " does not exist.  Please verify the " +
                        "value of the war_file property");
                assert false;
            }
        } else {
            log(Level.FINE, "deployClientSDKWar", "Configuring an external " +
                    "war");
            clientURL = warFile;
            log(Level.FINE, "deployClientSDKWar", "Client URL: " + clientURL);
            try {
                WebClient webClient = new WebClient();
                HtmlPage page = (HtmlPage)webClient.getPage(clientURL
                        + "/index.html");
            } catch(com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
                    e) {
                log(Level.SEVERE, "deployClientSDKWar", clientURL + " cannot " +
                        "be reached.");
                assert false;
            }
        }
        configureWAR(clientURL, rb_client);
        exiting("deployClientSDKWar");
        return clientURL;
    }

    protected void undeployClientSDKWar(ResourceBundle rb_client)
    throws Exception {
        if (rb_client.getString("warfile_type").equals("internal")) {
            log(Level.FINE, "stopServer", "Stopping jetty server");
            server.stop();
            log(Level.FINE, "stopServer", "Stopped jetty server");

            String strClientDomain = rb_client.getString("client_domain_name");

            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getCanonicalHostName() + strClientDomain;
            log(Level.FINE, "undeployClientSDKWar", "Undeploying client sdk" +
                    " war on host:port " + hostname + ":" + cltWarDeployPort);

            String userHomeDir = System.getProperty("user.home");
            deleteDirectory(userHomeDir + fileseparator + "OpenSSOClient",
                    hostname.replace(".", "_") + "_" + cltWarDeployPort);

            // Time delay required by the jetty server process to die
            Thread.sleep(30000);
        }
    }

    /**
     * This method configures the war using the client samples configurator page
     */
    private void configureWAR(String clientURL, ResourceBundle rb_client)
    throws Exception {
        WebClient webClient = new WebClient();
        HtmlPage page = (HtmlPage)webClient.getPage(clientURL +
                "/Configurator.jsp");
        if (getHtmlPageStringIndex(page, rb_client.getString(TestConstants.
                KEY_CLIENT_TXT)) == -1) {
            log(Level.FINE, "configureWAR", "WAR file is not configured." +
                    " Configuring the deployed war.");
        FileWriter fstream = new FileWriter(getBaseDir() +
                "sampleconfigurator.xml");
        BufferedWriter out = new BufferedWriter(fstream);

        String debugDir = getBaseDir() + fileseparator + serverName +
                fileseparator + "debug" + fileseparator + "client";
        log(Level.FINEST, "configureWar", "Client debug directory: " +
                debugDir);
        createDirectory(debugDir);
        String appUser = rb_amconfig.getString(
                TestConstants.KEY_AMC_AGENTS_APP_USERNAME);
        String appPassword = rb_amconfig.getString(
                TestConstants.KEY_AMC_SERVICE_PASSWORD);
        String configResult = rb_client.getString(TestConstants.
                KEY_CONFIG_RESULT);

        log(Level.FINEST, "configureWAR", "Debug dir: " + debugDir);
        log(Level.FINEST, "configureWAR", "App username: " + appUser);
        log(Level.FINEST, "configureWAR", "App password: " + appPassword);
        log(Level.FINEST, "configureWAR", "Config result: " +
                configResult);
        log(Level.FINEST, "configureWAR", "Server protocol: " + protocol);
        log(Level.FINEST, "configureWAR", "Server host: " + host);
        log(Level.FINEST, "configureWAR", "Server port: " + port);
        log(Level.FINEST, "configureWAR", "Server URI: " + uri);

        out.write("<url href=\"" + clientURL + "/Configurator.jsp");
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"clientsampleconfigurator\"");
        out.write(" buttonName=\"submit\">");
        out.write(newline);
        out.write("<input name=\"famProt\" value=\"" + protocol + "\"/>");
        out.write(newline);
            out.write("<input name=\"famHost\" value=\"" + host + "\"/>");
            out.write(newline);
            out.write("<input name=\"famPort\" value=\"" + port + "\"/>");
            out.write(newline);
            out.write("<input name=\"famDeploymenturi\" value=\"" + uri +
                    "\"/>");
            out.write(newline);
            out.write("<input name=\"debugDir\" value=\"" + debugDir + "\"/>");
            out.write(newline);
            out.write("<input name=\"appUser\" value=\"" + appUser + "\"/>");
            out.write(newline);
            out.write("<input name=\"appPassword\" value=\"" + appPassword);
            out.write("\"/>");
            out.write(newline);
            out.write("<result text=\"" + configResult + "\"/>");
            out.write(newline);
            out.write("</form>");
            out.write(newline);
            out.write("</url>");
            out.write(newline);
            out.close();
            DefaultTaskHandler task = new DefaultTaskHandler(getBaseDir() +
                    "sampleconfigurator.xml");
            page = task.execute(webClient);
        } else
            log(Level.FINE, "configureWAR", "WAR file is already configured.");
   }

    /**
     * Replaces the Redirect uri & the search strings in the authentication
     * properites files under the directory
     * <QATESTHOME>/<SERVER_NAME1>/built/classes
     */
    public void replaceRedirectURIs(String strModule)
    throws Exception {
        entering("replaceRedirectURIs", null);
        try {
            File directory;
            String[] files;
            String ext = "properties";
            String directoryName;
            String fileName;
            String absFileName;
            String redirecturi[] = new String[6];
            for (int i = 0; i < 6; i++) {
                redirecturi[i] = "redirecturl" + i + ".html";
            }

            String strNotURL = rb_amconfig.getString(TestConstants.
                    KEY_AMC_NOTIFICATION_URL);
            Map notificationURLMap = getURLComponents(strNotURL + "/");
            log(Level.FINEST, "replaceRedirectURIs", "notificationURLMap: " +
                    notificationURLMap.toString());

            String  deployProto = (String)notificationURLMap.get("protocol");
            String  strPort = (String)notificationURLMap.get("port");
            String strHostname = (String)notificationURLMap.get("host");
            int deployPort  = new Integer(strPort).intValue();
            String deployURI = rb_amconfig.getString(TestConstants.
                    KEY_INTERNAL_WEBAPP_URI);

            String clientURL;
            if (strModule.equals("samlv2") ||
                    strModule.equals("samlv2idpproxy") ||
                    strModule.equals("idff"))
                clientURL = deployProto + "://" + strHostname +  ":" +
                        deployPort + deployURI + uriseparator + "federation";
            else
                clientURL = deployProto + "://" + strHostname +  ":" +
                        deployPort + deployURI + uriseparator + strModule;
            log(Level.FINEST, "replaceRedirectURIs", "clientURL: " + clientURL);

            Map replaceVals = new HashMap();
            for (int j = 0; j < 6; j++) {
                replaceVals.put("REDIRECT_URI" + j, clientURL + "/" +
                        redirecturi[j]);
              replaceVals.put("REDIRECT_URI_SEARCH_STRING" + j, "This is " +
                      redirecturi[j]);
            }
            log(Level.FINEST, "replaceRedirectURIs", "replaceVals: " +
                    replaceVals.toString());

            directoryName = getTestBase() + strModule;
            log(Level.FINEST, "replaceRedirectURIs", "directoryName: " +
                    directoryName);
            directory = new File(directoryName);
            assert (directory.exists());
            files = directory.list();

            for (int i = 0; i < files.length; i++) {
                fileName = files[i];
                if (fileName.endsWith(ext.trim())) {
                    absFileName = directoryName + fileseparator + fileName;
                    log(Level.FINEST, "replaceRedirectURIs", "Replacing the" +
                            " file :" + absFileName);
                    replaceString(absFileName, replaceVals, absFileName);
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "replaceRedirectURIs", e.getMessage());
            e.printStackTrace();
        }
        exiting("replaceRedirectURIs");
    }

    /**
     * Replace the directory server host, port, and root suffix tags in the
     * authenticationConfigData.properties file.
     * @param testModule - the QATest module which will have its properties 
     * files tag swapped for authentication tags.
     * @throws java.lang.Exception
     */
    public void replaceAuthTags(String testModule)
    throws Exception {
        Object[] params = {testModule};
        entering("replaceAuthTags", params);
        ResourceBundle umGlobalBundle = null;

        try {
            umGlobalBundle = ResourceBundle.getBundle("config" +
                    fileseparator + "UMGlobalDatastoreConfig");
        } catch (MissingResourceException mre) {
            log(Level.SEVERE, "replaceAuthTags", "Unable to retrieve the " +
                    "UMGlobalDatastoreConfig resource bundle.");
            assert false;
            throw mre;
        }

        log(Level.FINE, "replaceAuthTags", "Retrieving datastore " +
                "directory server host, port, and root suffix.");
        String keyPrefix = "UMGlobalDatastoreConfig1";
        String moduleDir = testModule;
        if (testModule.equals("samlv2")) {
            keyPrefix = "UMGlobalDatastoreConfig0";
            moduleDir = "authentication";
        }

        String umDirectoryHost = null;
        String umDirectoryPort = null;
        String umRootSuffix = null;
        String umBindPasswd = null;
        try {
           log(Level.FINE, "replaceAuthTags", "Retrieving value for key " +
                    keyPrefix + ".sun-idrepo-ldapv3-config-ldap-server.0");
           umDirectoryHost = umGlobalBundle.getString(keyPrefix +
                    ".sun-idrepo-ldapv3-config-ldap-server.0").trim();
           log(Level.FINE, "replaceAuthTags", "Retrieving value for key " +
                    keyPrefix + ".sun-idrepo-ldapv3-config-ldap-port.0");

           umDirectoryPort = umGlobalBundle.getString(keyPrefix +
                    ".sun-idrepo-ldapv3-config-ldap-port.0").trim();
           log(Level.FINE, "replaceAuthTags", "Retrieving value for key " +
                    keyPrefix + ".datastore-root-suffix.0");
           umRootSuffix = umGlobalBundle.getString(keyPrefix +
                    ".datastore-root-suffix.0").trim();
           umBindPasswd = umGlobalBundle.getString(keyPrefix +
                   ".sun-idrepo-ldapv3-config-authpw.0");
        } catch (MissingResourceException mre2) {
            log(Level.SEVERE, "replaceAuthTags", "Unable to retrieve values " +
                    "for one or more datastore keys");
            assert false;
            throw mre2;
        }

        log(Level.FINEST, "replaceAuthTags", "LDAP auth directory server = " +
                umDirectoryHost + ":" + umDirectoryPort);
        log(Level.FINEST, "replaceAuthTags", "LDAP auth root suffix = " +
                umRootSuffix);

        Map replaceVals = new HashMap();
        replaceVals.put("UM_DS_HOST", umDirectoryHost);
        replaceVals.put("UM_DS_PORT", umDirectoryPort);
        replaceVals.put("UM_ROOT_SUFFIX", umRootSuffix);
        replaceVals.put("SM_SUFFIX", basedn);
        replaceVals.put("UM_BIND_PASSWD", umBindPasswd);
        String directoryName = getTestBase() + moduleDir;
        log(Level.FINEST, "replaceAuthTags", "Examining files in directory " +
                    directoryName + "...");
        File directory = new File(directoryName);
        if (!directory.exists()) {
            log(Level.SEVERE, "replaceAuthTags", "Aborting tag replacement the "
                    + "directory " + directoryName + " does not exist.");
            assert false;
        }
        String[] files = directory.list();

        for (String fileName: files) {
            if (fileName.endsWith(".properties")) {
                String absFileName = directoryName + fileseparator + fileName;
                log(Level.FINE, "replaceRedirectURIs", "Searching the file " +
                        absFileName + " for UM_DS_HOST, UM_DS_PORT, " +
                        "UM_ROOT_SUFFIX, and SM_SUFFIX");
                boolean stringFound = false;
                Iterator keyIterator = replaceVals.keySet().iterator();
                while (keyIterator.hasNext()) {
                    log(Level.FINE, "replaceAuthTags", "Searching the file " + 
                            fileName + " for tokens to replace.");
                    String replaceTag = (String)keyIterator.next();
                    if (searchStringInFile(absFileName, replaceTag)) {
                        stringFound = true;
                    }
                }
                if (stringFound) {
                    log(Level.FINE, "replaceAuthTags", "Replacing tokens in " 
                            + absFileName);
                    String[] fileNameTokens = fileName.split("\\.");
                    String generatedFile = directoryName + fileseparator + 
                            fileNameTokens[0] + "-Generated." + 
                            fileNameTokens[1];
                    log(Level.FINEST, "replaceAuthTags", 
                            "The name of the generated file is " + 
                            generatedFile);
                    replaceString(absFileName, replaceVals, generatedFile);
                }
            }
        }        
        exiting("replaceAuthTags"); 
    }
    
    /**
     * Converts attrValPair into Map containing attrName as key
     * and values as set.
     * @param attrValPair Attribute value pair in the format
     * attrval1=val1,val2|attrval2=val11,val12
     * @return Map containing attrName and values as set.
     */
    protected Map attributesToMap(String attrValPair)
    throws Exception {
        entering("attributesToMap", null);
        Map attrMap = new HashMap();
        if ((attrValPair != null) && (attrValPair.length() > 0)) {
            StringTokenizer tokens = new StringTokenizer(attrValPair, "|");
            while (tokens.hasMoreTokens()) {
                StringTokenizer attrToken =
                        new StringTokenizer(tokens.nextToken(), "=");
                String attrName = attrToken.nextToken();
                Set valSet = new HashSet();
                StringTokenizer valueTokens =
                        new StringTokenizer(attrToken.nextToken(), ",");
                if (valueTokens.countTokens() <= 0) {
                    valSet.add(attrToken.nextToken());
                } else {
                    while (valueTokens.hasMoreTokens()) {
                        valSet.add(valueTokens.nextToken());
                    }
                }
                attrMap.put(attrName, valSet);
            }
        } else {
            throw new RuntimeException("Attributes value pair cannot be null");
        }
        exiting("attributesToMap");
        return attrMap;
    }
    
    /**
     * Compares two maps returns true if both are equal else false
     * @param newValMap Atrribute values this should be subset of updateValMap
     * @param updateValMap Atrribute values
     * @return true if bothe the maps are equal.
     */
    protected boolean isAttrValuesEqual(Map newValMap, Map updateValMap)
    throws Exception {
        entering("isAttrValuesEqual", null);
        boolean equal;
        if (newValMap != null && updateValMap != null){
            Set updatedKeys = newValMap.keySet();
            Iterator itr1 = updatedKeys.iterator();
            while (itr1.hasNext()) {
                String key = (String)itr1.next();
                Set val1Set = (Set)newValMap.get(key);
                Set val2Set = (Set)updateValMap.get(key);
                equal = val1Set.equals(val2Set);
                if (!equal) {
                    return false;
                }
            }
        } else {
            return false;
        }
        exiting("isAttrValuesEqual");
        return true;
    }
    
    /**
     * Returns set of properties from a given resource file
     * @param file resource file
     * @return set of properties
     */
    protected Properties getProperties(String file)
    throws MissingResourceException {
        entering("getProperties", null);
        Properties properties = new Properties();
        ResourceBundle bundle = ResourceBundle.getBundle(file);
        Enumeration e = bundle.getKeys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = bundle.getString(key);
            properties.put(key, value);
        }
        exiting("getProperties");
        return properties;
    }
    
    /**
     * Returns all SSOTokens of a user.
     */
    protected Set getAllUserTokens(SSOToken requester, String userId)
    throws Exception {
        entering("getAllUserTokens", null);
        SSOToken stok = null;
        Set setAllToken = new HashSet();
        if (validateToken(requester)) {
            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            Set set = stMgr.getValidSessions(requester, host);
            Iterator it = set.iterator();
            String strLocUserID;
            while (it.hasNext()) {
                stok = (SSOToken)it.next();
                strLocUserID = stok.getProperty("UserId");
                log(Level.FINEST, "getAllUserTokens", "UserID: " +
                        strLocUserID);
                if (strLocUserID.equalsIgnoreCase(userId)) {
                    setAllToken.add(stok);
                }
            }
        }
        exiting("getAllUserTokens");
        return setAllToken;
    }
    
    /**
     * Replaces the strings given in the map in the input file and wirtes to the
     * output file
     * @param file input file (absolute file path)
     * @param Map with name value pairs like ("SM_SUFFIX", basedn)
     * @param file output file (absolute file path)
     * @param encoding
     */
    protected void replaceString(String inputFN, Map nvp,
            String outputFN, String enc) 
    throws Exception {        
        entering("replaceString", null);
        FileInputStream in = null;
        BufferedWriter out = null;
        try {
            File file = new File(inputFN);
            byte[] data = new byte[(int)file.length()];
            in = new FileInputStream(file);
            in.read(data);
            StringBuffer buf = new StringBuffer(new String(data));
            Set keys = nvp.keySet();
            Object iter[] = sort(keys.toArray());
            for (int i = 0; i < iter.length; i++){
                String key = (String)iter[i];
                String value = (String)nvp.get(key);
                replaceToken(buf, key, value);
            }
            if(outputFN != null) {
                try {
                    out = new BufferedWriter(new OutputStreamWriter
                            (new FileOutputStream(outputFN),enc));
                } catch (java.io.UnsupportedEncodingException ex) {
                    out = new BufferedWriter(new OutputStreamWriter
                            (new FileOutputStream(outputFN),"ISO8859_1"));
                }
                String str = buf.toString();
                out.write(str, 0, str.length());
                out.flush();
            }
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            in.close();
            out.close();
        }
        exiting("replaceString");
        return;
    }
    
    /**
     * Replaces the strings given in the map in the input file and wirtes to the
     * output file
     * @param file input file (absolute file path)
     * @param Map with name value pairs like ("SM_SUFFIX", basedn)
     * @param file output file (absolute file path)
     */
    protected void replaceString(String inputFN, Map nvp,
            String outputFN) 
    throws Exception {
        String enc = System.getProperty("file.encoding");
        replaceString(inputFN, nvp, outputFN, enc);
    }
    
    /**
     * Replaces the strings in a string buffer
     * @param String buffer
     * @param key to be replaced
     * @param value to be replaced
     */
    protected void replaceToken(StringBuffer buf, String key,
            String value) {
        entering("replaceToken", null);
        if (key == null || value == null || buf == null)
            return;
        int loc = 0, keyLen = key.length(), valLen = value.length();
        while ((loc = buf.toString().indexOf(key, loc)) != -1) {
            buf.replace(loc, loc + keyLen, value);
            loc = loc + valLen;
        }
        exiting("replaceToken");
        return;
    }
    
    protected Set getListFromHtmlPage(HtmlPage page)
    throws Exception {
        entering("getListFromHtmlPage", null);
        Set set = new HashSet();
        String strP = page.asXml();
        int iExit = strP.indexOf("<!-- CLI Exit Code: 0 -->");
        int iColon = strP.lastIndexOf(":", iExit);
        String strSub = strP.substring(iColon +1, iExit).trim();
        StringTokenizer st = new StringTokenizer(strSub, "\n");
        while (st.hasMoreTokens()) {
            set.add(st.nextToken());
        }
        log(Level.FINEST, "getListFromHtmlPage", "The list is as follows: " +
                set.toString());
        exiting("getListFromHtmlPage");
        return (set);
    }

    /**
     * Get an Array of String from a String
     */
    protected String[] getArrayOfString(String str) 
            throws Exception {
        int i = 0;
        StringTokenizer strTokenComma = new StringTokenizer(str, ",");
        String[] iden = new String[strTokenComma.countTokens()];
        while (strTokenComma.hasMoreTokens()){
            iden[i] = strTokenComma.nextToken();
            i++;
        }
        return iden;
    }
    
    /**
     * Sorts the keys in the map
     * returns sorted object []
     */
    protected Object[] sort(Object objArray[])
    throws Exception {
        entering("sort", null);
        for (int i = objArray.length; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (((String)objArray[j]).length() <
                        ((String)objArray[j+1]).length()) {
                    Object T = (String)objArray[j+1];
                    objArray[j+1] = objArray[j];
                    objArray[j] = T;
                }
            }
        }
        exiting("setValuesHasString");
        return objArray;
    }
    
    /**
     * Get Attributes as Array of String
     */
    protected String[] getAttributesStr(String attString){
        int i = 0;
        String[] token = null;
        StringTokenizer strTokenComma = new StringTokenizer(attString, ",");
        token = new String[strTokenComma.countTokens()];
        while (strTokenComma.hasMoreTokens()){
            String temp = strTokenComma.nextToken();
            StringTokenizer strTokenEqual = 
                    new StringTokenizer(temp, "=");
            while (strTokenEqual.hasMoreTokens()){
                token[i] = strTokenEqual.nextToken();
                strTokenEqual.nextToken();
                i++;
            }
        }
        return token;
    }    
    
    /**
     * Get the property value of a property in showServerConfig.jsp
     * @webClient - the WebClient object that will used to emulate the client
     * browser
     * @propName - the name of the property whose value should be retrieved
     * from the contents of showServerConfig.jsp
     */
    public String getServerConfigValue(WebClient webClient, String propName)
    throws Exception {
        String propValue = null;
        String configJSPUrl = null;
        boolean accessConsole = false;
        try {
            if (tableContents == null) {
                accessConsole = true;
                HtmlPage consolePage = consoleLogin(webClient,
                        getLoginURL(realm), adminUser, adminPassword);
                if (consolePage != null) {
                    if (!distAuthEnabled) {
                        configJSPUrl = protocol + ":" + "//" + host + ":" + 
                                port + uri + "/showServerConfig.jsp";
                    } else {
                        configJSPUrl = getLoginURL(realm) + "?goto=" + 
                                serverProtocol + "://" + serverHost + ":" +
                                serverPort + serverUri + 
                                "/showServerConfig.jsp";
                    }
                    HtmlPage configJSPPage =
                            (HtmlPage) webClient.getPage(configJSPUrl);
                    if (configJSPPage != null) {
                        String jspContents =
                                configJSPPage.getWebResponse().
                                getContentAsString();
                        int tableStartIndex =
                                jspContents.indexOf("<table border=\"1\">");
                        if (tableStartIndex != -1) {
                            int tableEndIndex = jspContents.indexOf("</table>",
                                    tableStartIndex);
                            if (tableEndIndex != -1) {
                                tableContents =
                                        jspContents.substring(tableStartIndex,
                                        tableEndIndex);
                            } else {
                                log(Level.SEVERE, "getServerConfigValue",
                                        "Did not find the end of the table " +
                                        "in " + configJSPPage + ".");
                            }
                        } else {
                            log(Level.SEVERE, "getServerConfigValue",
                                    "Did not find the start of the table " +
                                    "in " + configJSPPage + ".");
                        }
                    } else {
                        log(Level.SEVERE, "getServerConfigValue",
                        "Unable to access " + configJSPPage + ".");
                    }
                } else {
                    log(Level.SEVERE, "getServerConfigValue",
                            "Unable to login to the console");
                }
            }

            int propIndex = tableContents.indexOf(propName);
            if (propIndex != -1) {
                int valueStartIndex =
                        tableContents.indexOf("<td>", propIndex +
                        propName.length());
                if (valueStartIndex != -1) {
                    int valueEndIndex =
                            tableContents.indexOf("</td>",
                            valueStartIndex);
                    if (valueEndIndex != -1) {
                        propValue =
                                tableContents.substring(
                                valueStartIndex + 4,
                                valueEndIndex).trim();
                        propValue = propValue.replace('\n', ' ').
                                replace("\r", "");

                        log(Level.FINEST,
                                "getServerConfigValue",
                                "The value of " + propName +
                                " is " + propValue  + ".");
                    } else {
                        log(Level.SEVERE,
                                "getServerConfigValue",
                                "Did not find end tag for " +
                                "property " + propName + ".");
                    }
                } else {
                    log(Level.SEVERE, "getServerConfigValue",
                            "Did not find start tag for " +
                            "property " + propName + ".");
                }
            } else {
                log(Level.SEVERE, "getServerConfigValue",
                        "Did not find the configuration " +
                        "property " + propName + ".");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (accessConsole) {
                String logoutURL = protocol + ":" + "//" + host + ":" + port +
                            uri + "/UI/Logout";
                consoleLogout(webClient, logoutURL);
            }
            return propValue;
        }
    }

    /**
     * Searches for a string in a file
     * @param file - path to the file name to search
     * @param strSearch String which needs to be searched for
     */
    protected boolean searchStringInFile(String file, String strSearch)
    throws Exception {
        entering("searchStringInFile", null);
        BufferedReader buff = new BufferedReader(new FileReader(file));
        for (String inputStr = buff.readLine(); (inputStr != null);
        inputStr = buff.readLine()) {
                if (inputStr.contains(strSearch)) {
                    log(Level.FINEST, "searchStringInFile", "String:" +
                            strSearch + "matched at " + 
                            inputStr.indexOf(inputStr));
                    return true;
                }
        }
        exiting("searchStringInFile");
        return false;
    }    
    /**
     * Reads data from a Properties object and creates a Map containing all
     * the attribute keys and values. It also takes in a Set of attribute key
     * names, which if specified, are not put into the Map. This is to ensure
     * selective selection of attribute key and value pairs. One can further
     * specify to search for a key containig a specific string (str).
     */
    protected Map getMapFromProperties(String propName, String str, Set set)
    throws Exception {
        entering("getMapFromProperties", null);
        Map map = new HashMap();
        FileInputStream fis = new FileInputStream(propName);
        Properties prop = new Properties();
        prop.load(fis);
        for (Enumeration e = prop.keys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String value = (String)prop.get(key);
            if (set != null) {
                if (!set.contains(key)) {
                    if (str != null) { 
                       if (key.indexOf(str) != -1)
                            map.put(key, value);
                    } else
                        map.put(key, value);
                }
            } else {
                if (str != null) {
                    if (key.indexOf(str + ".") != -1)
                        map.put(key, value);
                } else
                    map.put(key, value);
            }
        }
        exiting("getMapFromProperties");
        return (map);
    }

    /**
     * Reads data from a Properties object and creates a Map containing all
     * the attribute keys and values.
     * @param resourcebundle name
     */
    protected Map getMapFromProperties(String propName)
    throws Exception {
        return (getMapFromProperties(propName, null, null));
    }

    /**
     * Deletes the sepcified file in the specified directory
     * @param dirName The name of the directory
     * @param fileName The name of the file
     * @throws java.lang.Exception
     */
    protected void deleteDirectory(String dirName, String fileName)
    throws Exception {
        File configDir = new File(dirName);
        if (configDir.exists()) {
            String [] configFiles = configDir.list();
            if (configFiles.length > 0) {
                for (int i=0; i < configFiles.length; i++) {
                    if (fileName != null) {
                        if (configFiles[i].indexOf(fileName) != -1) {
                            log(Level.FINEST, "deleteDirectory", "Deleting: " +
                                    dirName + fileseparator + configFiles[i]);
                            File configFile = new File(dirName + fileseparator +
                                    configFiles[i]);
                            assert(configFile.delete());
                        }
                    } else {
                        log(Level.FINEST, "deleteDirectory", "Deleting: " +
                                dirName + fileseparator + configFiles[i]);
                        File configFile = new File(dirName + fileseparator +
                                configFiles[i]);
                        assert(configFile.delete());                        
                    }
                }
            }
        }
    }


    /**
     * Deletes all the files in the specified directory
     * @param dirName The name 
     * @throws java.lang.Exception
     */
    protected void deleteDirectory(String dirName)
    throws Exception {        
        deleteDirectory(dirName, null);
    }

    /**
     * Create a directory or nested directories
     * @param strDir the string stating complete directory path
     * @return success or failure as boolena result
     * @throws java.lang.Exception
     */
    protected boolean createDirectory(String strDir)
    throws Exception {
        boolean success = false;
        log(Level.FINE, "createDirectory", "Creating directory: " + strDir);
        if (!strDir.contains(fileseparator)) {
            // Create one directory
            success = (new File(strDir)).mkdir();
            if (success) {
              log(Level.FINE, "createDirectory", strDir + " created");
            }
        } else {
            // Create multiple directories
            success = (new File(strDir)).mkdirs();
            if (success) {
              log(Level.FINE, "createDirectory", strDir + " created");
            }
        }
        return success;
    }

    /**
     * Get config datastore details by making a call to get serverconfig.xml
     * details using famadm.jsp
     * @param famadm
     * @param webClient
     * @param url
     * @return
     * @throws java.lang.Exception
     */
    protected Map getSvrcfgDetails(FederationManager famadm,
            WebClient webClient, String url)
    throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        HtmlPage page = (HtmlPage) famadm.getSvrcfgXml(webClient, url);
        String strPage = page.asXml();
        int i1 = strPage.indexOf("sms");
        int i2 = strPage.indexOf("port", i1);
        int i21 = strPage.indexOf("&", i2 + 7);
        String strPort = strPage.substring(i2 + 11, i21);
        map.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                SMSConstants.UM_LDAPv3_LDAP_PORT, strPort);
        int i3 = strPage.indexOf("DirDN", i1);
        int i4 = strPage.indexOf("&", i3 + 7);
        String strDirDN = strPage.substring(i3 + 9, i4);
        map.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                SMSConstants.UM_LDAPv3_AUTHID, strDirDN);
        int i5 = strPage.indexOf("BaseDN", i1);
        int i6 = strPage.indexOf("&", i5 + 7);
        String strBaseDN = strPage.substring(i5 + 10, i6);
        map.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                SMSConstants.UM_LDAPv3_ORGANIZATION_NAME, strBaseDN);
        int i7 = strPage.indexOf("host", i1);
        int i8 = strPage.indexOf("&", i7 + 7);
        String strDirHost = strPage.substring(i7 + 11, i8);
        map.put(SMSConstants.UM_DATASTORE_PARAMS_PREFIX + "." +
                SMSConstants.UM_LDAPv3_LDAP_SERVER, strDirHost);
        return (map);
    }
}
