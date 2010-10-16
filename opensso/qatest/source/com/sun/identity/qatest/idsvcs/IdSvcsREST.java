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
 * $Id: IdSvcsREST.java,v 1.11 2009/02/24 06:57:17 vimal_67 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.idsvcs;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.TestCommon;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class tests the REST interfaces associated with Identity Web Services
 */
public class IdSvcsREST extends TestCommon {
    
    private ResourceBundle rb_amconfig;
    private String baseDir;
    private String serverURI;
    private String polName = "idsvcsRESTPolicyTest";
    private String userName = "idsvcsresttest";
    private String identity_user = "idsvcsuser";
    private String identity_agent = "idsvcsJ2EEAgent";
    private String identity_group = "idsvcsGroup";
    private String objecttype_agent = "AgentOnly";
    private String objecttype_user = "user";
    private String objecttype_group = "group";
    private String identity_attribute_values_userpassword = "secret12";
    private String identity_attribute_values_sn = "sn_for_rest_user";
    private String identity_attribute_values_cn = "cn_of_REST_user";
    private String identity_realm = "/";
    private String identity_type_user = "user";
    private String identity_type_agent = "AgentOnly";
    private String identity_type_group = "group";    
    private String identity_attribute_values_SERVERURL = "http://serverurl." +
            "red.iplanet.com:8080/opensso";
    private TextPage page;
    private SSOToken admintoken;
    private SSOToken usertoken;
    private IDMCommon idmc;
    private PolicyCommon pc;
    private WebClient webClient;
    
    /**
     * Creates common objects.
     */
    public IdSvcsREST()
    throws Exception {
        super("IdSvcsREST");
        rb_amconfig = ResourceBundle.getBundle(
                TestConstants.TEST_PROPERTY_AMCONFIG);
        admintoken = getToken(adminUser, adminPassword, basedn);
        idmc = new IDMCommon();
        pc = new PolicyCommon();
        baseDir = getBaseDir() + System.getProperty("file.separator")
            + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
            + System.getProperty("file.separator") + "built"
            + System.getProperty("file.separator") + "classes"
            + System.getProperty("file.separator");
    }
    
    /**
     *  Creates required users and policy.
     */
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup()
    throws Exception {
        entering("setup", null);

        serverURI = protocol + ":" + "//" + host + ":" + port + uri;

        Map map = new HashMap();
        Set set = new HashSet();
        set.add(userName);
        map.put("sn", set);
        set = new HashSet();
        set.add(userName);
        map.put("cn", set);
        set = new HashSet();
        set.add(userName);
        map.put("userpassword", set);
        set = new HashSet();
        set.add("Active");
        map.put("inetuserstatus", set);
        set = new HashSet();
        set.add(userName + "alias1");
        set.add(userName + "alias2");
        set.add(userName + "alias3");
        map.put("iplanet-am-user-alias-list", set);

        idmc.createIdentity(admintoken, realm, IdType.USER, userName, map);

        String xmlFile = "idsvcs-rest-policy-test.xml";
        createPolicyXML(xmlFile);
        assert(pc.createPolicy(xmlFile, realm));

        exiting("setup");
    }

    /**
     * This test validates the authentication REST interface for super admin
     * user
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSuperAdminAuthenticateREST()
    throws Exception {
        entering("testSuperAdminAuthenticateREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + adminUser +
                    "&password=" + adminPassword);
            String s0 = page.getContent();
            log(Level.FINEST, "testSuperAdminAuthenticateREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();
            log(Level.FINEST, "testSuperAdminAuthenticateREST",
                    "Token string:" + s1);

            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            usertoken = stMgr.createSSOToken(s1);
            if (!validateToken(usertoken))
                assert false;
                       
        } catch (Exception e) {
            log(Level.SEVERE, "testSuperAdminAuthenticateREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (validateToken(usertoken))
                destroyToken(usertoken);
            Reporter.log("This test validates the authentication REST" +
                    " interface for super admin user");
        }
        exiting("testSuperAdminAuthenticateREST");
    }

    /**
     * This test validates the authentication REST interface for a normal user
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserAuthenticateREST()
    throws Exception {
        entering("testNormalUserAuthenticateREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserAuthenticateREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();
            log(Level.FINEST, "testSuperAdminAuthenticateREST",
                    "Token string: " + s1);

            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            usertoken = stMgr.createSSOToken(s1);
            if (!validateToken(usertoken))
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserAuthenticateREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (validateToken(usertoken))
                destroyToken(usertoken);
            Reporter.log("This test validates the authentication REST" +
                    " interface for a normal user");
        }
        exiting("testNormalUserAuthenticateREST");
    }

    /**
     * This test validates the authorization REST interface for a normal user
     * where policy resource has allow for both GET and POST request. The
     * action under test is GET.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolAAGetREST()
    throws Exception {
        entering("testNormalUserPolAAGetREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserPolAAGetREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authorize?uri=http://www.restidsvcs1.com:80" +
                    "&action=GET&subjectid=" + URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserPolAAGetREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf("boolean=true") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolAAGetREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization REST" +
                    " interface for a normal user where policy resource has" +
                    " allow for both GET and POST request. The action under" +
                    " test is GET.");
            Reporter.log("Resource: http://www.restidsvcs1.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Pass");
        }
        exiting("testNormalUserPolAAGetREST");
    }

    /**
     * This test validates the authorization REST interface for a normal user
     * where policy resource has allow for both GET and POST request. The
     * action under test is POST.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolAAPostREST()
    throws Exception {
        entering("testNormalUserPolAAPostREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserPolAAPostREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authorize?uri=http://www.restidsvcs1.com:80" +
                    "&action=POST&subjectid=" + URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserPolAAPostREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf("boolean=true") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolAAPostREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization REST" +
                    " interface for a normal user where policy resource" +
                    " has allow for both GET and POST request. The action" +
                    " under test is POST.");
            Reporter.log("Resource: http://www.restidsvcs1.com:80");
            Reporter.log("Action: POST");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Pass");
        }
        exiting("testNormalUserPolAAPostREST");
    }

    /**
     * This test validates the authorization REST interface for a normal user
     * where policy resource has allow for GET and deny for POST request. The
     * action under test is GET.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolADGetREST()
    throws Exception {
        entering("testNormalUserPolADGetREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserPolADGetREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authorize?uri=http://www.restidsvcs2.com:80" +
                    "&action=GET&subjectid=" + URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserPolADGetREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf("boolean=true") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolADGetREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization REST" +
                    " interface for a normal user where policy resource has" +
                    " allow for GET and deny for POST request. The action" +
                    " under test is GET.");
            Reporter.log("Resource: http://www.restidsvcs2.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Pass");
        }
        exiting("testNormalUserPolADGetREST");
    }

    /**
     * This test validates the authorization REST interface for a normal user
     * where policy resource has allow for GET and deny for POST request. The
     * action under test is POST.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolADPostREST()
    throws Exception {
        entering("testNormalUserPolADPostREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserPolADPostREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authorize?uri=http://www.restidsvcs2.com:80" +
                    "&action=POST&subjectid=" + URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserPolADPostREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf("boolean=false") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolADPostREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization REST" +
                    " interface for a normal user where policy resource has" +
                    " allow for GET and deny for POST request. The action" +
                    " under test is POST.");
            Reporter.log("Resource: http://www.restidsvcs2.com:80");
            Reporter.log("Action: POST");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Fail");
        }
        exiting("testNormalUserPolADPostREST");
    }

    /**
     * This test validates the authorization REST interface for a normal user
     * where policy resource has deny for GET and allow for POST request. The
     * action under test is GET.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolDAGetREST()
    throws Exception {
        entering("testNormalUserPolDAGetREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserPolDAGetREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authorize?uri=http://www.restidsvcs3.com:80" +
                    "&action=GET&subjectid=" + URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserPolDAGetREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf("boolean=false") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolDAGetREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization REST" +
                    " interface for a normal user where policy resource has" +
                    " deny for GET and allow for POST request. The action" +
                    " under test is GET.");
            Reporter.log("Resource: http://www.restidsvcs3.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Fail");
        }
        exiting("testNormalUserPolDAGetREST");
    }

    /**
     * This test validates the authorization REST interface for a normal user
     * where policy resource has deny for GET and allow for POST request. The
     * action under test is POST.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolDAPostREST()
    throws Exception {
        entering("testNormalUserPolDAPostREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserPolDAPostREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authorize?uri=http://www.restidsvcs3.com:80" +
                    "&action=POST&subjectid=" + URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserPolDAPostREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf("boolean=true") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolDAPostREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization REST" +
                    " interface for a normal user where policy resource has" +
                    " deny for GET and allow for POST request. The action" +
                    " under test is POST.");
            Reporter.log("Resource: http://www.restidsvcs3.com:80");
            Reporter.log("Action: POST");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Pass");
        }
        exiting("testNormalUserPolDAPostREST");
    }

    /**
     * This test validates the authorization REST interface for a normal user
     * where policy resource has deny for both GET and POST request. The
     * action under test are both GET.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolDDGetREST()
    throws Exception {
        entering("testNormalUserPolDDGetREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserPolDDGetREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authorize?uri=http://www.restidsvcs4.com:80" +
                    "&action=GET&subjectid=" + URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserPolDDGetREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf("boolean=false") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolDDGetREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization REST" +
                    " interface for a normal user where policy resource has" +
                    " deny for both GET and POST request. The action under" +
                    " test are both GET.");
            Reporter.log("Resource: http://www.restidsvcs4.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Fail");
        }
        exiting("testNormalUserPolDDGetREST");
    }

    /**
     * This test validates the authorization REST interface for a normal user
     * where policy resource has deny for both GET and POST request. The action
     * under test is POST.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolDDPostREST()
    throws Exception {
        entering("testNormalUserPolDDPostREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName + 
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserPolDDPostREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authorize?uri=http://www.restidsvcs4.com:80" +
                    "&action=POST&subjectid=" + URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserPolDDPostREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf("boolean=false") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolDDPostREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization REST" +
                    " interface for a normal user where policy resource has" +
                    " deny for both GET and POST request. The action under" +
                    " test are both POST.");
            Reporter.log("Resource: http://www.restidsvcs4.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Fail");
        }
        exiting("testNormalUserPolDDPostREST");
    }

    /**
     * This test validates the attributes REST interface for a normal user. The
     * current tests validates the retrival of multivalued attribute
     * iplanet-am-user-alias-list.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserAttributesREST()
    throws Exception {
        entering("testNormalUserAttributesREST", null);
        try {
            webClient = new WebClient();
            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + userName +
                    "&password=" + userName);
            String s0 = page.getContent();
            log(Level.FINEST, "testNormalUserAttributesREST", "Token: " + s0);
            int i1 = s0.indexOf("=");
            String s1 = s0.substring(i1 + 1, s0.length()).trim();

            page = (TextPage)webClient.getPage(serverURI +
                    "/identity/attributes?subjectid=" +
                    URLEncoder.encode(s1, "UTF-8"));
            log(Level.FINEST, "testNormalUserAttributesREST", "Page: " +
                    page.getContent());
            if (page.getContent().indexOf(userName + "alias1") == -1)
                assert false;
            if (page.getContent().indexOf(userName + "alias2") == -1)
                assert false;
            if (page.getContent().indexOf(userName + "alias3") == -1)
                assert false;

        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserAttributesREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the attributes REST" +
                    " interface for a normal user. The current tests" +
                    " validates the retrival of multivalued attribute" +
                    " iplanet-am-user-alias-list.");
        }
        exiting("testNormalUserAttributesREST");
    }

    /**
     * This function tests the search interface for the J2EE agents
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSearchAgentsREST()
            throws Exception {
        entering("testSearchAgentsREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            String[] agents = {"idsvcsAgent1", "idsvcsAgent2", "idsvcsAgent3"};
            s1 = authenticateREST(adminUser, adminPassword);

            // Creating agents
            for (int i = 0; i < agents.length; i++) {
                log(Level.FINEST, "testSearchAgentsREST", 
                        "Creating agent: " + agents[i]);
                page = (TextPage) webClient.getPage(serverURI +
                        "/identity/create?identity_name=" + agents[i] +
                        "&identity_attribute_names=userpassword" +
                        "&identity_attribute_values_userpassword=" +
                        identity_attribute_values_userpassword +
                        "&identity_realm=" + URLEncoder.encode("/") + 
                        "&identity_type=" + identity_type_agent + 
                        "&identity_attribute_names=AgentType" +
                        "&identity_attribute_values_AgentType=J2EEAgent" +
                        "&identity_attribute_names=AGENTURL" +
                        "&identity_attribute_values_AGENTURL=" +
                        serverURI + "&identity_attribute_names=SERVERURL" +
                        "&identity_attribute_values_SERVERURL=" +
                        identity_attribute_values_SERVERURL +
                        "&admin=" + URLEncoder.encode(s1, "UTF-8"));
            }
            log(Level.FINEST, "testSearchAgentsREST", "Page: " +
                    page.getContent());
            
            // Searching agents
            log(Level.FINEST, "testSearchAgentsREST",
                    "Searching agents: ");
            commonSearchREST(objecttype_agent, s1, "*", 
                    agents, Boolean.TRUE);

            // Deleting agents
            for (int i = 0; i < agents.length; i++) {
                log(Level.FINEST, "testSearchAgentsREST", 
                        "Deleting agent: " + agents[i]);
                commonDeleteREST(agents[i], "AgentOnly", s1);
            }
         
            // Searching again to see if the agents are deleted 
            log(Level.FINEST, "testSearchAgentsREST", 
                    "Searching agents: ");
            commonSearchREST(objecttype_agent, s1, "*", 
                    agents, Boolean.FALSE);

        } catch (Exception e) {
            log(Level.SEVERE, "testSearchAgentsREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the search REST" +
                    " interface to search for J2EE Agents after creating" +
                    " three J2EE agents.");
            commonLogOutREST(s1);
        }
        exiting("testSearchAgentsREST");
    }

    /** 
     * This function tests the search interface for the user
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSearchUsersREST()
            throws Exception {
        entering("testSearchUsersREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            String[] users = {"idsvcsUsr1", "idsvcsUsr2", "idsvcsUsr3"};
            s1 = authenticateREST(adminUser, adminPassword);

            // Creating users
            for (int i = 0; i < users.length; i++) {
                log(Level.FINEST, "testSearchUsersREST",
                        "Creating user: " + users[i]);
                page = (TextPage) webClient.getPage(serverURI +
                        "/identity/create?identity_name=" + users[i] +
                        "&identity_attribute_names=userpassword" +
                        "&identity_attribute_values_userpassword=" +
                        identity_attribute_values_userpassword + 
                        "&identity_attribute_names=sn" + 
                        "&identity_attribute_values_sn=" + 
                        identity_attribute_values_sn +
                        "&identity_attribute_names=cn" +
                        "&identity_attribute_values_cn=" +
                        identity_attribute_values_cn + "&identity_realm=" + 
                        identity_realm + "&identity_type=" + 
                        identity_type_user + "&admin=" +
                        URLEncoder.encode(s1, "UTF-8"));
            }
            log(Level.FINEST, "testSearchUsersREST", "Page: " +
                    page.getContent());
            
            // Searching users 
            log(Level.FINEST, "testSearchUsersREST",
                    "Searching users: ");
            commonSearchREST(objecttype_user, s1, "*", users, Boolean.TRUE);

            // Deleting users 
            for (int i = 0; i < users.length; i++) {
                log(Level.FINEST, "testSearchUsersREST", 
                        "Deleting user: " + users[i]);
                commonDeleteREST(users[i], "user", s1);
            }

            // Searching again to see if the users are deleted 
            log(Level.FINEST, "testSearchUsersREST", "Searching users: ");
            commonSearchREST(objecttype_user, s1, "*",
                    users, Boolean.FALSE);

        } catch (Exception e) {
            log(Level.SEVERE, "testSearchUsersREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the search REST" +
                    " interface to search for users after creating" +
                    " three users.");
            commonLogOutREST(s1);
        }
        exiting("testSearchUsersREST");
    }

    /**
     * This function tests the search interface for the group 
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSearchGroupREST()
            throws Exception {
        entering("testSearchGroupREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            String[] groups = {"idsvcsGroup1", "idsvcsGroup2", "idsvcsGroup3"};
            s1 = authenticateREST(adminUser, adminPassword);
          
            // Creating Groups 
            for (int i = 0; i < groups.length; i++) {
                log(Level.FINEST, "testSearchGroupREST",
                        "Creating Group: " + groups[i]);
                page = (TextPage) webClient.getPage(serverURI +
                        "/identity/create?identity_name=" + groups[i] +
                        "&identity_attribute_names=userpassword" +
                        "&identity_attribute_values_userpassword=" +
                        identity_attribute_values_userpassword + 
                        "&identity_attribute_names=sn" +
                        "&identity_attribute_values_sn=" +
                        identity_attribute_values_sn +
                        "&identity_attribute_names=cn" +
                        "&identity_attribute_values_cn=" +
                        identity_attribute_values_cn + "&identity_realm=" + 
                        identity_realm + "&identity_type=" + 
                        identity_type_group + "&admin=" +
                        URLEncoder.encode(s1, "UTF-8"));
            }
            log(Level.FINEST, "testSearchGroupREST", "Page: " +
                    page.getContent());
            
            // Searching groups 
            log(Level.FINEST, "testSearchGroupREST", "Searching groups: ");
            commonSearchREST(objecttype_group, s1, "*", 
                    groups, Boolean.TRUE);

            // Deleting Groups 
            for (int i = 0; i < groups.length; i++) {
                log(Level.FINEST, "testSearchGroupREST", 
                        "Deleting Group: " + groups[i]);
                commonDeleteREST(groups[i], "group", s1);
            }

            // Searching again to see if the groups are deleted 
            log(Level.FINEST, "testSearchGroupREST", "Searching groups: ");
            commonSearchREST(objecttype_group, s1, "*", 
                    groups, Boolean.FALSE);

        } catch (Exception e) {
            log(Level.SEVERE, "testSearchGroupREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the search REST" +
                    " interface to search for groups after creating" +
                    " three groups.");
            commonLogOutREST(s1);
        }
        exiting("testSearchGroupREST");
    }

    /** 
     * Create an J2EE Agent type
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCreateAgentREST()
            throws Exception {
        entering("testCreateAgentREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            String agent = identity_agent;
            s1 = authenticateREST(adminUser, adminPassword);

            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/create?identity_name=" + agent +
                    "&identity_attribute_names=userpassword" +
                    "&identity_attribute_values_userpassword=" +
                    identity_attribute_values_userpassword +
                    "&identity_realm=" + URLEncoder.encode("/") + 
                    "&identity_type=" + identity_type_agent + 
                    "&identity_attribute_names=AgentType" +
                    "&identity_attribute_values_AgentType=J2EEAgent" +
                    "&identity_attribute_names=AGENTURL" +
                    "&identity_attribute_values_AGENTURL=" +
                    serverURI + "&identity_attribute_names=SERVERURL" +
                    "&identity_attribute_values_SERVERURL=" +
                    identity_attribute_values_SERVERURL +
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));

            // Searching agent 
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/search?&filter=*&attributes_names=objecttype" +
                    "&attributes_values_objecttype=" + objecttype_agent + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));
            String str = page.getContent();
            log(Level.FINEST, "testCreateAgentREST", "Page: " +
                    page.getContent());
            if (!str.contains(agent)) {
                log(Level.FINEST, "testCreateAgentREST", 
                        "Agent not created successfully: " +
                        agent);
                assert false;
            }
            
        } catch (Exception e) {
            log(Level.SEVERE, "testCreateAgentREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the create REST" +
                    " interface to create a J2EE agent type");
             commonLogOutREST(s1);
        }
        exiting("testCreateAgentREST");
    }

    /**
     * Delete the agent Identity
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"testCreateAgentREST"})
    public void testAgentDeleteIdentityREST()
            throws Exception {
        entering("testAgentDeleteIdentityREST", null);
        String s1 = null;
        try {
           
            webClient = new WebClient();
            String agent = identity_agent;
            s1 = authenticateREST(adminUser, adminPassword);

            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/search?&filter=*&attributes_names=objectclass" +
                    "&attributes_values_objectclass=" + objecttype_agent + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));

            // Deleting Agent 
            log(Level.FINEST, "testAgentDeleteIdentityREST", 
                    "Deleting Agent: " + agent);
            commonDeleteREST(agent, "AgentOnly", s1);
          
            // Searching again to see if the agent is deleted 
            log(Level.FINEST, "testAgentDeleteIdentityREST", 
                    "Searching agent: ");
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/search?&filter=*&attributes_names=objectclass" +
                    "&attributes_values_objectclass=" + objecttype_agent + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));
            String str = page.getContent();
            log(Level.FINEST, "testAgentDeleteIdentityREST", "Page: " +
                    page.getContent());
            if (str.contains(agent)) {
                log(Level.FINEST, "testAgentDeleteIdentityREST", 
                    "Agent not Deleted Successfully: " + agent);
                assert false;
            }
                       
        } catch (Exception e) {
            log(Level.SEVERE, "testAgentDeleteIdentityREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the delete REST" +
                    " interface for deleting an agent");
            commonLogOutREST(s1);
        }
        exiting("testAgentDeleteIdentityREST");
    }

    /**
     * Create a User
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCreateUserREST()
            throws Exception {
        entering("testCreateUserREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            String user = identity_user;
            s1 = authenticateREST(adminUser, adminPassword);

            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/create?identity_name=" + user +
                    "&identity_attribute_names=userpassword" +
                    "&identity_attribute_values_userpassword=" +
                    identity_attribute_values_userpassword + 
                    "&identity_attribute_names=sn" +
                    "&identity_attribute_values_sn=" + 
                    identity_attribute_values_sn +
                    "&identity_attribute_names=cn" +
                    "&identity_attribute_values_cn=" +
                    identity_attribute_values_cn + "&identity_realm=" + 
                    identity_realm + "&identity_type=" + identity_type_user +    
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));

            // Searching user 
            page = (TextPage)webClient.getPage(serverURI +
            "/identity/search?&filter=*&attributes_names=objecttype" + 
            "&attributes_values_objecttype=" + objecttype_user + "&admin=" +
            URLEncoder.encode(s1, "UTF-8"));
            String str = page.getContent();
            log(Level.FINEST, "testCreateUserREST", "Page: " +
                    page.getContent());
            if(!str.contains(user)){
                log(Level.FINEST, "testCreateUserREST", 
                        "User not created successfully: " + user);
                assert false;
            }
                   
        } catch (Exception e) {
            log(Level.SEVERE, "testCreateUserREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the create REST" +
                    " interface to create a user");
            commonLogOutREST(s1);
        }
        exiting("testCreateUserREST");
    }

    /**
     * Delete the user Identity
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"testCreateUserREST"})
    public void testUserDeleteIdentityREST()
            throws Exception {
        entering("testUserDeleteIdentityREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            String user = identity_user;
            s1 = authenticateREST(adminUser, adminPassword);

            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/search?&filter=*&attributes_names=objectclass" +
                    "&attributes_values_objectclass=" + objecttype_user + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));

            // Deleting User 
            log(Level.FINEST, "testUserDeleteIdentityREST", "Deleting User: " +
                    user);
            commonDeleteREST(user, "user", s1);
          
            // Searching again to see if the user is deleted 
            log(Level.FINEST, "testUserDeleteIdentityREST", "Searching user: ");
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/search?&filter=*&attributes_names=objectclass" +
                    "&attributes_values_objectclass=" + objecttype_user + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));
            String str = page.getContent();
            log(Level.FINEST, "testUserDeleteIdentityREST", "Page: " +
                    page.getContent());
            if (str.contains(user)) {
                log(Level.FINEST, "testUserDeleteIdentityREST", 
                    "User not Deleted Successfully: " + user);
                assert false;
            }
                      
        } catch (Exception e) {
            log(Level.SEVERE, "testUserDeleteIdentityREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the delete REST" +
                    " interface for deleting a user");
            commonLogOutREST(s1);
        }
        exiting("testUserDeleteIdentityREST");
    }
   
    /** 
     * Create a Group
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCreateGroupREST()
            throws Exception {
        entering("testCreateGroupREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            String group = identity_group;
            s1 = authenticateREST(adminUser, adminPassword);

            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/create?identity_name=" + group + 
                    "&identity_attribute_names=userpassword" +
                    "&identity_attribute_values_userpassword=" +
                    identity_attribute_values_userpassword + 
                    "&identity_attribute_names=sn" +
                    "&identity_attribute_values_sn=" + 
                    identity_attribute_values_sn +
                    "&identity_attribute_names=cn" +
                    "&identity_attribute_values_cn=" +
                    identity_attribute_values_cn + "&identity_realm=" + 
                    identity_realm + "&identity_type=" + identity_type_group + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));

            // Searching group
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/search?&filter=*&attributes_names=objecttype" +
                    "&attributes_values_objecttype=" + objecttype_group + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));
            String str = page.getContent();
            log(Level.FINEST, "testCreateGroupREST", "Page: " +
                    page.getContent());
            if (!str.contains(group)) {
                log(Level.FINEST, "testCreateGroupREST", 
                        "Group not created successfully: " + group);
                assert false;
            }
            
        } catch (Exception e) {
            log(Level.SEVERE, "testCreateGroupREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the create REST" +
                    " interface to create a group");
            commonLogOutREST(s1);
        }
        exiting("testCreateGroupREST");
    }

    /**
     * Delete the Group Identity
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"},
    dependsOnMethods = {"testCreateGroupREST"})
    public void testGroupDeleteIdentityREST()
            throws Exception {
        entering("testGroupDeleteIdentityREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            String group = identity_group;
            s1 = authenticateREST(adminUser, adminPassword);

            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/search?&filter=*&attributes_names=objectclass" +
                    "&attributes_values_objectclass=" + objecttype_group + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));

            // Deleting Group 
            log(Level.FINEST, "testGroupDeleteIdentityREST", 
                    "Deleting Group: " + group);
            commonDeleteREST(group, "group", s1);
          
            // Searching again to see if the Group is deleted 
            log(Level.FINEST, "testGroupDeleteIdentityREST", 
                    "Searching group: ");
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/search?&filter=*&attributes_names=objectclass" +
                    "&attributes_values_objectclass=" + objecttype_group + 
                    "&admin=" + URLEncoder.encode(s1, "UTF-8"));
            String str = page.getContent();
            log(Level.FINEST, "testGroupDeleteIdentityREST", "Page: " +
                    page.getContent());
            if (str.contains(group)) {
                log(Level.FINEST, "testGroupDeleteIdentityREST", 
                    "Group not Deleted Successfully: " + group);
                assert false;
            }
            
        } catch (Exception e) {
            log(Level.SEVERE, "testGroupDeleteIdentityREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the delete REST" +
                    " interface for deleting a group");
            commonLogOutREST(s1);
        }
        exiting("testGroupDeleteIdentityREST");
    }
    
    /**
     * Cleanup method. This method:
     * (a) Delete users
     * (b) Deletes policies
     */
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", 
      "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup()
            throws Exception {
        entering("cleanup", null);
        try {
            log(Level.FINEST, "cleanup", "Deleting User: " + userName);
            Reporter.log("Deleting User :" + userName);
            idmc.deleteIdentity(admintoken, realm, IdType.USER, userName);

            log(Level.FINEST, "cleanup", "Deleting Policy: " + polName);
            Reporter.log("Deleting Policy :" + polName);
            pc.deletePolicy(polName, realm);

            if (validateToken(admintoken)) 
                destroyToken(admintoken);
                                                
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }

    /**
     * Generates XML for creating the policy.
     */
    private void createPolicyXML(String xmlFile)
            throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);

        out.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        out.write(newline);
        out.write("<!DOCTYPE Policies");
        out.write(newline);
        out.write("PUBLIC \"-//Sun Java System Access Manager 7.1 2006Q3");
        out.write("Admin CLI DTD//EN\"");
        out.write(newline);
        out.write("\"jar://com/sun/identity/policy/policyAdmin.dtd\">");
        out.write(newline);

        out.write("<Policies>");
        out.write(newline);

        out.write("<Policy name=\"" + polName + "\" referralPolicy=\"false\"");
        out.write(" active=\"true\">");
        out.write(newline);

        out.write("<Rule name=\"idsvcs1\">");
        out.write(newline);
        out.write("<ServiceName name=\"iPlanetAMWebAgentService\"/>");
        out.write(newline);
        out.write("<ResourceName name=\"http://www.restidsvcs1.com:80\"/>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"POST\"/>");
        out.write(newline);
        out.write("<Value>allow</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"GET\"/>");
        out.write(newline);
        out.write("<Value>allow</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("</Rule>");
        out.write(newline);

        out.write("<Rule name=\"idsvcs2\">");
        out.write(newline);
        out.write("<ServiceName name=\"iPlanetAMWebAgentService\"/>");
        out.write(newline);
        out.write("<ResourceName name=\"http://www.restidsvcs2.com:80\"/>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"POST\"/>");
        out.write(newline);
        out.write("<Value>deny</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"GET\"/>");
        out.write(newline);
        out.write("<Value>allow</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("</Rule>");
        out.write(newline);

        out.write("<Rule name=\"idsvcs3\">");
        out.write(newline);
        out.write("<ServiceName name=\"iPlanetAMWebAgentService\"/>");
        out.write(newline);
        out.write("<ResourceName name=\"http://www.restidsvcs3.com:80\"/>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"POST\"/>");
        out.write(newline);
        out.write("<Value>allow</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"GET\"/>");
        out.write(newline);
        out.write("<Value>deny</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("</Rule>");
        out.write(newline);

        out.write("<Rule name=\"idsvcs4\">");
        out.write(newline);
        out.write("<ServiceName name=\"iPlanetAMWebAgentService\"/>");
        out.write(newline);
        out.write("<ResourceName name=\"http://www.restidsvcs4.com:80\"/>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"POST\"/>");
        out.write(newline);
        out.write("<Value>deny</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"GET\"/>");
        out.write(newline);
        out.write("<Value>deny</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("</Rule>");
        out.write(newline);

        out.write("<Subjects name=\"idsvcssubjects\" description=\"\">");
        out.write(newline);
        out.write("<Subject name=\"idsvcssubj\" type=\"AuthenticatedUsers\"");
        out.write(" includeType=\"inclusive\">");
        out.write(newline);
        out.write("</Subject>");
        out.write(newline);
        out.write("</Subjects>");
        out.write(newline);

        out.write("</Policy>");
        out.write(newline);
        out.write("</Policies>");
        out.close();
    }

    /** 
     * This function authenticates using admin user and returns
     * the Token as String. It is a common admin authenticate 
     * function used in all methods
     */
    private String authenticateREST(String user, String password)
            throws Exception {
        entering("authenticateREST", null);
        String s1 = null;
        try {
            webClient = new WebClient();
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/authenticate?username=" + user +
                    "&password=" + password);
            String s0 = page.getContent();
            log(Level.FINEST, "authenticateREST",
                    "Token: " + s0);
            int i1 = s0.indexOf("=");
            s1 = s0.substring(i1 + 1, s0.length()).trim();
        } catch (Exception e) {
            log(Level.SEVERE, "authenticateREST",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("authenticateREST");
        return s1;
    }

    /** 
     * This function deletes the Identity using Identity Type.
     * It is a common delete method for all kinds of Identities 
     */
    private void commonDeleteREST(String identity, String identity_type, 
            String token) throws Exception {
        try {
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/delete?&identity_name=" + identity + "&admin=" +
                    URLEncoder.encode(token, "UTF-8") + "&identity_type=" +
                    identity_type);
            log(Level.FINEST, "commonDeleteREST", "Page: " +
                    page.getContent());
        } catch (Exception e) {
            log(Level.SEVERE, "commonDeleteREST", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /** 
     * This function searches the Identity.
     * It is a common search method for all kinds of Identities 
     */
    private void commonSearchREST(String objecttype, String admintoken,
            String filter, String[] identities, Boolean contains)
            throws Exception {
        try {
            if (filter.equals("*")) {
                page = (TextPage) webClient.getPage(serverURI +
                        "/identity/search?&filter=*" +
                        "&attributes_names=objecttype" +
                        "&attributes_values_objecttype=" + objecttype + 
                        "&admin=" + URLEncoder.encode(admintoken, "UTF-8"));
                log(Level.FINEST, "commonSearchREST", "Page: " +
                        page.getContent());
                String str = page.getContent();
                for (int i = 0; i < identities.length; i++) {
                                        
                    //  For Identities Created
                    if (contains) {
                        if (!str.contains(identities[i])) {
                            log(Level.FINEST, "commonSearchREST", 
                                    "Identity is not created successfully: " +
                                    identities[i]);
                            assert false;
                        } else {
                            log(Level.FINEST, "commonSearchREST",
                                    "Identity is created successfully: " +
                                    identities[i]);
                        }
                    } 
                    //  For Identities Deleted
                    else {
                        if (str.contains(identities[i])) {
                            log(Level.FINEST, "commonSearchREST",
                                    "Identity is not deleted successfully: " +
                                    identities[i]);
                            assert false;
                        } else {
                            log(Level.FINEST, "commonSearchREST", 
                                    "Identity is deleted successfully: " +
                                    identities[i]);
                        }
                    }
                }
            } else {
                for (int i = 0; i < identities.length; i++) {
                    page = (TextPage) webClient.getPage(serverURI +
                            "/identity/search?&filter=" + identities[i] + 
                            "&attributes_names=objecttype" +
                            "&attributes_values_objecttype=" + objecttype + 
                            "&admin=" + URLEncoder.encode(admintoken, "UTF-8"));
                    log(Level.FINEST, "commonSearchREST", "Page: " +
                            page.getContent());
                    String str = page.getContent();

                    //  For Identities Created
                    if (contains.TRUE) {
                        if (!str.contains(identities[i])) {
                            log(Level.FINEST, "commonSearchREST", 
                                    "Identity is not created successfully: " +
                                    identities[i]);
                            assert false;
                        }
                    } 
                    //  For Identities Deleted 
                    else {
                        if (str.contains(identities[i])) {
                            log(Level.FINEST, "commonSearchREST", 
                                    "Identity is not deleted successfully: " +
                                    identities[i]);
                            assert false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "commonSearchREST", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /** 
     * This function logs out the User.
     * It is a common logout method for all kinds of Users 
     */
    private void commonLogOutREST(String token)
            throws Exception {
        entering("commonLogOutREST", null);
        try{
            page = (TextPage) webClient.getPage(serverURI +
                    "/identity/logout?subjectid=" +
                    URLEncoder.encode(token, "UTF-8"));
                
        } catch (Exception e) {
            log(Level.SEVERE, "commonLogOutREST", e.getMessage());
            e.printStackTrace();
            throw e;
        } 
        exiting("commonLogOutREST");
    }
}
