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
 * $Id: ProfileAttributeTests.java,v 1.4 2009/07/08 21:13:52 sridharev Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.qatest.agents;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.net.URL;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;

/**
 * This class tests Header attributes related to Profile
 * Attributes are tested using a webapp or a cgi script
 * which can read the header attributes in the browser. Attributes
 * are tested for new and updated values for different profile.
 */
public class ProfileAttributeTests extends TestCommon {

    private String logoutURL;
    private String strGblRB = "agentsGlobal";
    private String resource;
    private URL url;
    private WebClient webClient;
    private int iIdx;
    private IDMCommon idmc;
    private ResourceBundle rbg;
    private SSOToken usertoken;
    private SSOToken admintoken;
    private int pollingTime;

    /**
     * Instantiated different helper class objects
     */
    public ProfileAttributeTests()
            throws Exception {
        super("ProfileAttributeTests");
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
     */
    public ProfileAttributeTests(String strScriptURL, String strResource)
            throws Exception {
        this();
        url = new URL(strScriptURL);
        resource = strResource;
    }

    /**
     * Evaluates newly created static single valued profile attribute
     */
    public void evaluateNewSingleValuedProfileAttribute()
            throws Exception {
        entering("evaluateNewSingleValuedProfileAttribute", null);
        webClient = new WebClient();
        try {
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "pauser");
            Reporter.log("Password: " + "pauser");
            Reporter.log("Expected Result: " + "HTTP_PROFILE_CN:pauser");
            idmc.createIdentity(admintoken, realm, IdType.ROLE, "parole1",
                    new HashMap());
            idmc.addUserMember(admintoken, "pauser", "parole1", IdType.ROLE);
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("(objectclass=person)");
            map.put("nsRoleFilter", set);
            idmc.createIdentity(admintoken, realm, IdType.FILTEREDROLE,
                    "filparole1", map);
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_CN:pauser");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewSingleValuedProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewSingleValuedProfileAttribute");
    }

    /**
     * Evaluates newly created static multi valued profile attribute
     */
    public void evaluateNewMultiValuedProfileAttribute()
            throws Exception {
        entering("evaluateNewMultiValuedProfileAttribute", null);
        webClient = new WebClient();
        try {
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "pauser");
            Reporter.log("Password: " + "pauser");
            Reporter.log("Expected Result: " +
                    "HTTP_PROFILE_ALIAS:pauseralias1|pauseralias2");
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_ALIAS:pauseralias1|pauseralias2");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewMultiValuedProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewMultiValuedProfileAttribute");
    }

    /**
     * Evaluates newly created dynamic multi valued profile attribute related
     * to static roles
     */
    public void evaluateNewNsRoleProfileAttribute()
            throws Exception {
        entering("evaluateNewNsRoleProfileAttribute", null);
        webClient = new WebClient();
        try {
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "pauser");
            Reporter.log("Password: " + "pauser");
            String strNsrole = "cn=filparole1," + basedn;
            String strNsrole1 = "cn=parole1," + basedn;
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute",
                    "NSROLE expected: " + strNsrole);
            Reporter.log("Expected Result(Role): " + strNsrole1);
            Reporter.log("Expected Result(Filtered Role): " + strNsrole);
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            String s0 = page.asXml();
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute", "s0:\n" + s0);
            int i1 = s0.indexOf("HTTP_PROFILE_NSROLE:");
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute", "i1: " + i1);
            String s1 = s0.substring(i1, s0.length());
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute", "s1:\n" + s1);
            int i2 = s1.indexOf("$$");
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute", "i2: " + i2);
            String s2 = s1.substring(20, i2);
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute",
                    "NSROLE from server : " + s2);
            iIdx = s2.indexOf(strNsrole);
            assert (iIdx != -1);
            iIdx = -1;
            iIdx = s2.indexOf(strNsrole1);
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewNsRoleProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewNsRoleProfileAttribute");
    }

    /**
     * Evaluates newly created dynamic multi valued profile attribute related
     * to dynamic roles
     */
    public void evaluateNewFilteredRoleProfileAttribute()
            throws Exception {
        entering("evaluateNewFilteredRoleProfileAttribute", null);
        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            String strNsrole = "cn=filparole1," + basedn;
            String strNsrole1 = "cn=parole1," + basedn;
            String s0 = page.asXml();
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute", "s0:\n" + s0);
            int i1 = s0.indexOf("HTTP_PROFILE_NSROLE:");
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute", "i1: " + i1);
            String s1 = s0.substring(i1, s0.length());
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute", "s1:\n" + s1);
            int i2 = s1.indexOf("$$");
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute", "i2: " + i2);
            String s2 = s1.substring(20, i2);
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute",
                    "NSROLE from server : " + s2);
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute",
                    "NSROLE expected: " + strNsrole + " and " + strNsrole1);
            iIdx = s2.indexOf(strNsrole);
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "pauser");
            Reporter.log("Password: " + "pauser");
            Reporter.log("Expected Result(Role): " + strNsrole);
            Reporter.log("Expected Result(Filtered Role): " + strNsrole1);
            assert (iIdx != -1);
            iIdx = -1;
            iIdx = s2.indexOf("cn=filparole1");
            assert (iIdx != -1);
            iIdx = -1;
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateNewFilteredRoleProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateNewFilteredRoleProfileAttribute");
    }

    /**
     * Evaluates updated static single valued profile attribute
     */
    public void evaluateUpdatedSingleValuedProfileAttribute()
            throws Exception {
        entering("evaluateUpdatedSingleValuedProfileAttribute", null);
        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_CN:pauser");
            assert (iIdx != -1);
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("pauserupdated");
            map.put("cn", set);
            log(Level.FINEST, "evaluateUpdatedSingleValuedProfileAttribute",
                    "Update Attribute List: " + map);
            idmc.modifyIdentity(idmc.getFirstAMIdentity(admintoken, "pauser",
                    IdType.USER, realm), map);
            usertoken = getToken("pauser", "pauser", basedn);
            set = idmc.getIdentityAttribute(usertoken, "iPlanetAMUserService",
                    "cn");
            assert (set.contains("pauserupdated"));
            boolean isFound = false;
            long time = System.currentTimeMillis();
            String strPage = "";
            String strSearch = "HTTP_PROFILE_CN:" + "pauserupdated";
            while (System.currentTimeMillis() - time < pollingTime &&
                    !isFound) {
                log(Level.FINEST, "evaluateUpdatedSingleValuedProfileAttribute",
                        (System.currentTimeMillis() - time));
                page = (HtmlPage) webClient.getPage(url);
                strPage = page.asXml();
                if (strPage.contains(strSearch)) {
                    isFound = true;
                }
                Thread.sleep(5000);
            }
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "pauser");
            Reporter.log("Password: " + "pauser");
            Reporter.log("Expected Result: " + "HTTP_PROFILE_CN:" +
                    "pauserupdated");
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_CN:" +
                    "pauserupdated");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedSingleValuedProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
            destroyToken(usertoken);
        }
        exiting("evaluateUpdatedSingleValuedProfileAttribute");
    }

    /**
     * Evaluates updated static multi valued profile attribute
     */
    public void evaluateUpdatedMultiValuedProfileAttribute()
            throws Exception {
        entering("evaluateUpdatedMultiValuedProfileAttribute", null);

        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "HTTP_PROFILE_ALIAS:" +
                    "pauseralias1|pauseralias2");
            assert (iIdx != -1);
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("pauseralias3");
            map.put("iplanet-am-user-alias-list", set);
            log(Level.FINEST, "evaluateUpdatedMultiValuedProfileAttribute",
                    "Update Attribute List: " + map);
            idmc.modifyIdentity(idmc.getFirstAMIdentity(admintoken, "pauser",
                    IdType.USER, realm), map);
            boolean isFound = false;
            long time = System.currentTimeMillis();
            String strPage = "";
            while (System.currentTimeMillis() - time < pollingTime &&
                    !isFound) {
                log(Level.FINEST, "evaluateUpdatedMultiValuedProfileAttribute",
                        (System.currentTimeMillis() - time));
                page = (HtmlPage) webClient.getPage(url);
                strPage = page.asXml();
                if (strPage.contains("HTTP_PROFILE_ALIAS:" + "pauseralias3")) {
                    isFound = true;
                }
                Thread.sleep(5000);
            }
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "pauser");
            Reporter.log("Password: " + "pauser");
            Reporter.log("Expected Result: " + "HTTP_PROFILE_ALIAS:" +
                    "pauseralias3");
            if (strPage.contains("HTTP_PROFILE_ALIAS:" + "pauseralias3")) {
                assert true;
            } else {
                assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedMultiValuedProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUpdatedMultiValuedProfileAttribute");
    }

    /**
     * Evaluates updated dynamic multi valued profile attribute related to
     * static roles
     */
    public void evaluateUpdatedNsRoleProfileAttribute()
            throws Exception {
        entering("evaluateUpdatedNsRoleProfileAttribute", null);
        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage) webClient.getPage(url);
            String strNsrole = "cn=filparole1," + basedn;
            String strNsrole1 = "cn=parole1," + basedn;
            String strNsrole2 = "cn=parole2," + basedn;
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                    "NSROLE: " + strNsrole + ", " + strNsrole1 + ", " +
                    strNsrole2);
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "pauser");
            Reporter.log("Password: " + "pauser");
            Reporter.log("Expected Result(Filtered Role): " + strNsrole);
            Reporter.log("Expected Result(Role before updation): " +
                    strNsrole1);
            Reporter.log("Expected Result(Role after updation): " + strNsrole2);
            String s0;
            int i1;
            int i2;
            s0 = page.asXml();
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute", "s0:\n" +
                    s0);
            i1 = s0.indexOf("HTTP_PROFILE_NSROLE:");
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute", "i1: " + i1);
            String s1 = s0.substring(i1, s0.length());
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute", "s1:\n" +
                    s1);
            i2 = s1.indexOf("$$");
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute", "i2: " + i2);
            String s2 = s1.substring(20, i2);
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                    "NSROLE from server(before user profile updation) : " + s2);
            log(Level.FINEST, "evaluateNewNsRoleProfileAttribute",
                    "NSROLE expected(before user profile updation) : " +
                    strNsrole);
            iIdx = s2.indexOf(strNsrole);
            assert (iIdx != -1);
            iIdx = s2.indexOf(strNsrole1);
            assert (iIdx != -1);
            idmc.createIdentity(admintoken, realm, IdType.ROLE, "parole2",
                    new HashMap());
            idmc.addUserMember(admintoken, "pauser", "parole2", IdType.ROLE);
            iIdx = -1;
            boolean isFound = false;
            long time = System.currentTimeMillis();
            String strPage = "";
            while (System.currentTimeMillis() - time < pollingTime &&
                    !isFound) {
                log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                        (System.currentTimeMillis() - time));
                page = (HtmlPage) webClient.getPage(url);
                strPage = page.asXml();
                if (strPage.contains(strNsrole2)) {
                    isFound = true;
                }
                Thread.sleep(5000);
            }
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                    "Ran loop for :" + (System.currentTimeMillis() - time));
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                    "page after profile update" + page.asXml());
            i1 = strPage.indexOf("HTTP_PROFILE_NSROLE:");
            s1 = strPage.substring(i1, strPage.length());
            i2 = s1.indexOf("$$");
            s2 = s1.substring(20, i2);
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                    "NSROLE from server(after user profile updation) : " + s2);
            log(Level.FINEST, "evaluateUpdatedNsRoleProfileAttribute",
                    "NSROLE expected(after user profile updation) : " +
                    strNsrole2);
            iIdx = -1;
            iIdx = s2.indexOf(strNsrole2);
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedNsRoleProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUpdatedNsRoleProfileAttribute");
    }

    /**
     * Evaluates updated dynamic multi valued profile attribute related to
     * dynamic roles
     */
    public void evaluateUpdatedFilteredRoleProfileAttribute()
            throws Exception {
        entering("evaluateUpdatedFilteredRoleProfileAttribute", null);
        webClient = new WebClient();
        try {
            HtmlPage page = consoleLogin(webClient, resource, "pauser",
                    "pauser");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "cn=filparole1");
            assert (iIdx != -1);
            Map map = new HashMap();
            Set set = new HashSet();
            set.add("(mail=abc@def.com)");
            map.put("nsRoleFilter", set);
            log(Level.FINEST, "evaluateUpdatedFilteredRoleProfileAttribute",
                    "Update Attribute List: " + map);
            log(Level.FINEST, "evaluateUpdatedFilteredRoleProfileAttribute",
                    "Recreating adminToken");
            destroyToken(admintoken);
            admintoken = getToken(adminUser, adminPassword, basedn);
            idmc.modifyIdentity(idmc.getFirstAMIdentity(admintoken,
                    "filparole1", IdType.FILTEREDROLE, realm), map);
            boolean isFound = false;
            long time = System.currentTimeMillis();
            while (System.currentTimeMillis() - time < pollingTime &&
                    !isFound) {
                page = (HtmlPage) webClient.getPage(url);
                iIdx = -1;
                iIdx = getHtmlPageStringIndex(page, "cn=filparole1", false);
                if (iIdx != -1) {
                    isFound = true;
                    log(Level.FINE, "evaluateUpdatedFilteredRole" +
                            "ProfileAttribute", "Found cn=filparole1 after " +
                            "modifying identity");
                }
            }
            Reporter.log("Resource: " + url);
            Reporter.log("Username: " + "pauser");
            Reporter.log("Password: " + "pauser");
            Reporter.log("Expected Result: " + "cn=filparole1");
            page = (HtmlPage) webClient.getPage(url);
            iIdx = -1;
            iIdx = getHtmlPageStringIndex(page, "cn=filparole1");
            assert (iIdx != -1);
        } catch (Exception e) {
            log(Level.SEVERE, "evaluateUpdatedFilteredRoleProfileAttribute",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
        exiting("evaluateUpdatedFilteredRoleProfileAttribute");
    }

    /**
     * Deletes policies, identities and updates service attributes to default
     * values.
     */
    public void cleanup()
            throws Exception {
        entering("cleanup", null);
        try {
            if ((idmc.searchIdentities(admintoken, "pauser",
                    IdType.USER)).size() != 0) {
                idmc.deleteIdentity(admintoken, realm, IdType.USER, "pauser");
            }
            if (idmc.searchIdentities(admintoken, "parole1",
                    IdType.ROLE).size() != 0) {
                idmc.deleteIdentity(admintoken, realm, IdType.ROLE, "parole1");
            }
            if (idmc.searchIdentities(admintoken, "parole2",
                    IdType.ROLE).size() != 0) {
                idmc.deleteIdentity(admintoken, realm, IdType.ROLE, "parole2");
            }
            if (idmc.searchIdentities(admintoken, "filparole1",
                    IdType.FILTEREDROLE).size() != 0) {
                idmc.deleteIdentity(admintoken, realm, IdType.FILTEREDROLE,
                        "filparole1");
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
