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
 * $Id: SessionAttributeTests.java,v 1.4 2009/07/08 21:14:47 sridharev Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.AgentsCommon;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.Reporter;

/**
 * This class tests Header attributes related to Session, Profile
 * and Response. Attributes are tested using a webapp or a cgi script
 * which can read the header attributes in the browser. Attributes
 * are tested for new and updated values for different profile.
 */
public class SessionAttributeTests extends TestCommon {

    private String logoutURL;
    private String strGblRB = "agentsGlobal";
    private String resource;
    private URL url;
    private WebClient webClient;
    private int iIdx;
    private AgentsCommon mpc;
    private IDMCommon idmc;
    private ResourceBundle rbg;
    private SSOToken usertoken;
    private SSOToken admintoken;
    private int sleepTime = 2000;
    private int pollingTime;

    /**
     * Instantiated different helper class objects
     */
    public SessionAttributeTests()
            throws Exception {
        super("SessionAttributeTests");
        mpc = new AgentsCommon();
        idmc = new IDMCommon();
        rbg = ResourceBundle.getBundle("agents" + fileseparator + strGblRB);
        pollingTime = new Integer(rbg.getString(strGblRB +
                ".pollingInterval")).intValue();
        admintoken = getToken(adminUser, adminPassword, basedn);
        logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";

    }

    /**
     * Two Argument constructor initialising the ScriptURL 
     * and resource being tested
     * value
     */
    public SessionAttributeTests(String strScriptURL, String strResource)
            throws Exception {
        this();
        url = new URL(strScriptURL);
        resource = strResource;
    }

    /**
     * Evaluates a standard session attribute
     */
    public void evaluateUniversalIdSessionAttribute()
            throws Exception {
        entering("evaluateUniversalIdSessionAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "sauser",
                    "sauser");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            String strUniveraslId = "id=sauser," +
                    rbg.getString(strGblRB +
                    ".uuid.suffix.AMIdentitySubject.User") + "," + basedn;
            log(Level.FINEST,
                    "evaluateUniversalIdSessionAttribute", "strUniveraslId: " +
                    strUniveraslId);
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_SESSION_UNIVERSALIDENTIFIER:" + strUniveraslId, false);
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "sauser");
            Reporter.log("Password: " + "sauser");
            Reporter.log("Expected Result: " +
                    "HTTP_SESSION_UNIVERSALIDENTIFIER:" + strUniveraslId);
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUniversalIdSessionAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUniversalIdSessionAttribute");
    }

    /**
     * Evaluates newly created and updated custom session attribute
     */
    public void evaluateCustomSessionAttribute()
            throws Exception {
        entering("evaluateCustomSessionAttribute", null);
        webClient = new WebClient();
        SSOTokenManager stMgr;
        try {
            webClient.setThrowExceptionOnScriptError(false);
            webClient.setThrowExceptionOnFailingStatusCode(false);
            webClient.getCookieManager().setCookiesEnabled(true);
            HtmlPage page = consoleLogin(webClient, resource, "sauser",
                    "sauser");
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "sauser");
            Reporter.log("Password: " + "sauser");
            Reporter.log("Expected Result: " +
                    "HTTP_SESSION_MYPROPERTY:val1");
            Reporter.log("Expected Result (after updation): " +
                    "HTTP_SESSION_MYPROPERTY:val2");
            page = (HtmlPage) webClient.getPage(url);
            webClient.getCookieManager().setCookiesEnabled(true);
            WebResponse webResponse = page.getWebResponse();
            String test = webResponse.getContentAsString();
            log(Level.FINEST, "evaluateCustomSessionAttribute",
                    "webResponse is >>> " + test);
            String strCookie = rbg.getString(strGblRB + ".serverCookieName");
            log(Level.FINEST, "evaluateCustomSessionAttribute", "strCookie: " +
                    strCookie);
            String encodeType = page.getPageEncoding();
            log(Level.FINEST, "evaluateCustomSessionAttribute",
                    "encoding is : " + encodeType);
            String s0 = page.asXml();
            int i1;
            String s3;
            log(Level.FINEST, "evaluateCustomSessionAttribute", "s0:\n" + s0);
            i1 = s0.indexOf(strCookie + ":");
            log(Level.FINEST, "evaluateCustomSessionAttribute", "i1: " + i1);
            String s1 = s0.substring(i1, s0.length());
            log(Level.FINEST, "evaluateCustomSessionAttribute", "s1:\n" + s1);
            int i2 = s1.indexOf("|");
            log(Level.FINEST, "evaluateCustomSessionAttribute", "i2: " + i2);
            s3 = s1.substring(strCookie.length() + 1, i2);
            if (s3.contains(" ")) {
                s3 = s3.replace(" ", "+");
                log(Level.FINEST, "evaluateCustomSessionAttribute", "new s3 is " + s3);
            }
            log(Level.FINEST, "evaluateCustomSessionAttribute", "s3: " + s3);
            stMgr = SSOTokenManager.getInstance();
            String s3_decoded;
            log(Level.FINEST, "evaluateCustomSessionAttribute",
                    "usertoken before decode=" + s3);
            String strDecodeCookie = URLDecoder.decode(s3, "UTF-8");
            log(Level.FINEST, "evaluateCustomSessionAttribute",
                    "usertoken after UTF decode=" + strDecodeCookie);
            String strDecodeCookieascii = URLDecoder.decode(s3, "ASCII");
            log(Level.FINEST, "evaluateCustomSessionAttribute",
                    "usertoken after UTF decode ASCII =" + strDecodeCookieascii);
            if (s3.indexOf("%") != -1) {
                s3_decoded = URLDecoder.decode(s3);
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        "usertoken after decode=" + s3_decoded);
                if ((s3_decoded.indexOf("%") != -1)) {
                    log(Level.FINEST, "evaluateCustomSessionAttribute",
                            "usertoken after decode has %");
                    assert (s3_decoded.indexOf("%") != -1);
                }
                usertoken = stMgr.createSSOToken(s3_decoded);
            } else {
                usertoken = stMgr.createSSOToken(s3);
            }
            log(Level.FINE, "destroyToken", " usertoken TokenId is " +
                    usertoken.getTokenID());
            String strProperty;
            if (validateToken(usertoken)) {
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        "UserId property value: " +
                        usertoken.getProperty("UserId"));
                usertoken.setProperty("MyProperty", "val1");
                strProperty = usertoken.getProperty("MyProperty");
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        "Session property value: " + strProperty);
                assert (strProperty.equals("val1"));
            } else {
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        "User token is invalid");
                assert false;
            }
            boolean isFound = false;
            long time = System.currentTimeMillis();
            String strPage = "";
            while (System.currentTimeMillis() - time < pollingTime &&
                    !isFound) {
                log(Level.FINEST, "evaluateCustomSessionAttribute",
                        (System.currentTimeMillis() - time));
                page = (HtmlPage) webClient.getPage(url);
                strPage = page.asXml();
                if (strPage.contains("HTTP_SESSION_MYPROPERTY:val1")) {
                    isFound = true;
                }
                Thread.sleep(5000);
            }
            log(Level.FINEST, "evaluateUpdatedSessionAttribute",
                    "Waited for : " + (System.currentTimeMillis() - time));
            iIdx = -1;
            if (strPage.contains("HTTP_SESSION_MYPROPERTY:val1")) {
                assert true;
            } else {
                assert false;
            }
            log(Level.FINEST, "evaluateUpdatedSessionAttribute",
                    "Before Updating the user property");
            usertoken.setProperty("MyProperty", "val2");
            time = System.currentTimeMillis();
            isFound = false;
            while (System.currentTimeMillis() - time < sleepTime &&
                    !(usertoken.getProperty("MyProperty")).equals("val2")) {
            }
            log(Level.FINEST, "evaluateUpdatedSessionAttribute",
                    "waited for : " + (System.currentTimeMillis() - time));
            strProperty = usertoken.getProperty("MyProperty");
            log(Level.FINEST, "evaluateUpdatedSessionAttribute",
                    "Session property value: " + strProperty);
            assert (strProperty.equals("val2"));
            isFound = false;
            time = System.currentTimeMillis();
            log(Level.FINEST, "evaluateUpdatedSessionAttribute",
                    "Start-------System.currentTimeMillis(): " + System.currentTimeMillis());
            Thread.sleep(pollingTime);
            log(Level.FINEST, "evaluateUpdatedSessionAttribute",
                    "waited for : " + (System.currentTimeMillis() - time));
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page,
                    "HTTP_SESSION_MYPROPERTY:val2", false);
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedSessionAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUpdatedSessionAttribute");
    }

    /**
     * Deletes policies, identities and updates service attributes to default
     * values.
     */
    public void cleanup()
            throws Exception {
        entering("cleanup", null);
        try {
            if (idmc.searchIdentities(admintoken, "sauser",
                    IdType.USER).size() != 0) {
                idmc.deleteIdentity(admintoken, realm, IdType.USER, "sauser");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(admintoken);
        }
        exiting("cleanup");
    }
}
