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
 * $Id: IdSvcsSOAP.java,v 1.4 2009/01/27 00:06:32 nithyas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.idsvcs;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.idsvcs.IdentityServicesImplService_Impl;
import com.sun.identity.qatest.idsvcs.IdentityServicesImpl;
import com.sun.identity.qatest.idsvcs.Token;
import com.sun.identity.qatest.idsvcs.UserDetails;
import com.sun.identity.qatest.idsvcs.Attribute;
import java.io.BufferedWriter;
import java.io.FileWriter;
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
 * This class tests the SOAP interfaces associated with Identity Web Services
 */
public class IdSvcsSOAP extends TestCommon {
    
    private ResourceBundle rb_amconfig;
    private String baseDir;
    private String serverURI;
    private String polName = "idsvcsSOAPPolicyTest";
    private String userName = "idsvcssoaptest";
    private HtmlPage page;
    private SSOToken admintoken;
    private SSOToken usertoken;
    private SSOTokenManager stMgr;
    private IDMCommon idmc;
    private PolicyCommon pc;
    private WebClient webClient;
    private IdentityServicesImplService_Impl service;
    private IdentityServicesImpl isimpl;

    /**
     * Creates common objects.
     */
    public IdSvcsSOAP()
    throws Exception {
        super("IdSvcsSOAP");
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

        String xmlFile = "idsvcs-soap-policy-test.xml";
        createPolicyXML(xmlFile);
        assert (pc.createPolicy(xmlFile, realm));

        stMgr = SSOTokenManager.getInstance();
        service = new IdentityServicesImplService_Impl();
        isimpl = service.getIdentityServicesImplPort();

        exiting("setup");
    }

    /**
     * This test validates the authentication SOAP interface for super admin
     * user
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testSuperAdminAuthenticateSOAP()
    throws Exception {
        entering("testSuperAdminAuthenticateSOAP", null);
        try {
            Token token = isimpl.authenticate(adminUser, adminPassword,
                    "realm=" + realm);
            log(Level.FINEST, "testSuperAdminAuthenticateSOAP", "Token: " +
                    token);
            String tokID = token.getId();
            log(Level.FINEST, "testSuperAdminAuthenticateSOAP", "Token ID: " +
                    tokID);

            usertoken = stMgr.createSSOToken(tokID);
            if (!validateToken(usertoken))
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testSuperAdminAuthenticateSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (validateToken(usertoken))
                destroyToken(usertoken);
            Reporter.log("This test validates the authentication SOAP" +
                    " interface for super admin user");
        }
        exiting("testSuperAdminAuthenticateSOAP");
    }

    /**
     * This test validates the authentication SOAP interface for a normal user
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserAuthenticateSOAP()
    throws Exception {
        entering("testNormalUserAuthenticateSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserAuthenticateSOAP", "Token: " +
                    token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserAuthenticateSOAP", "Token ID: " +
                    tokID);

            usertoken = stMgr.createSSOToken(tokID);
            if (!validateToken(usertoken))
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserAuthenticateSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (validateToken(usertoken))
                destroyToken(usertoken);
            Reporter.log("This test validates the authentication SOAP" +
                    " interface for a normal user");
        }
        exiting("testNormalUserAuthenticateSOAP");
    }

    /**
     * This test validates the authorization SOAP interface for a normal user
     * where policy resource has allow for both GET and POST request. The
     * action under test is GET.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolAAGetSOAP()
    throws Exception {
        entering("testNormalUserPolAAGetSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserPolAAGetSOAP", "Token: " + token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserPolAAGetSOAP", "Token ID: " +
                    tokID);

            boolean polRes = isimpl.authorize("http://www.soapidsvcs1.com:80",
                    "GET", token);
            log(Level.FINEST, "testNormalUserPolAAGetSOAP",
                    "Policy evaluation result: " + polRes);
            assert polRes;
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolAAGetSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization SOAP" +
                    " interface for a normal user where policy resource has" +
                    " allow for both GET and POST request. The action under" +
                    " test is GET.");
            Reporter.log("Resource: http://www.soapidsvcs1.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Pass");
        }
        exiting("testNormalUserPolAAGetSOAP");
    }

    /**
     * This test validates the authorization SOAP interface for a normal user
     * where policy resource has allow for both GET and POST request. The
     * action under test is POST.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolAAPostSOAP()
    throws Exception {
        entering("testNormalUserPolAAPostSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserPolAAPostSOAP", "Token: " + token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserPolAAPostSOAP", "Token ID: " +
                    tokID);

            boolean polRes = isimpl.authorize("http://www.soapidsvcs1.com:80",
                    "POST", token);
            log(Level.FINEST, "testNormalUserPolAAPostSOAP",
                    "Policy evaluation result: " + polRes);
            assert polRes;
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolAAPostSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization SOAP" +
                    " interface for a normal user where policy resource" +
                    " has allow for both GET and POST request. The action" +
                    " under test is POST.");
            Reporter.log("Resource: http://www.soapidsvcs1.com:80");
            Reporter.log("Action: POST");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Pass");
        }
        exiting("testNormalUserPolAAPostSOAP");
    }

    /**
     * This test validates the authorization SOAP interface for a normal user
     * where policy resource has allow for GET and deny for POST request. The
     * action under test is GET.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolADGetSOAP()
    throws Exception {
        entering("testNormalUserPolADGetSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserPolADGetSOAP", "Token: " + token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserPolADGetSOAP", "Token ID: " +
                    tokID);

            boolean polRes = isimpl.authorize("http://www.soapidsvcs2.com:80",
                    "GET", token);
            log(Level.FINEST, "testNormalUserPolADGetSOAP",
                    "Policy evaluation result: " + polRes);
            assert polRes;
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolADGetSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization SOAP" +
                    " interface for a normal user where policy resource has" +
                    " allow for GET and deny for POST request. The action" +
                    " under test is GET.");
            Reporter.log("Resource: http://www.soapidsvcs2.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Pass");
        }
        exiting("testNormalUserPolADGetSOAP");
    }

    /**
     * This test validates the authorization SOAP interface for a normal user
     * where policy resource has allow for GET and deny for POST request. The
     * action under test is POST.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolADPostSOAP()
    throws Exception {
        entering("testNormalUserPolADPostSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserPolADPostSOAP", "Token: " + token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserPolADPostSOAP", "Token ID: " +
                    tokID);

            boolean polRes = isimpl.authorize("http://www.soapidsvcs2.com:80",
                    "POST", token);
            log(Level.FINEST, "testNormalUserPolADPostSOAP",
                    "Policy evaluation result: " + polRes);
            assert !(polRes);
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolADPostSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization SOAP" +
                    " interface for a normal user where policy resource has" +
                    " allow for GET and deny for POST request. The action" +
                    " under test is POST.");
            Reporter.log("Resource: http://www.soapidsvcs2.com:80");
            Reporter.log("Action: POST");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Fail");
        }
        exiting("testNormalUserPolADPostSOAP");
    }

    /**
     * This test validates the authorization SOAP interface for a normal user
     * where policy resource has deny for GET and allow for POST request. The
     * action under test is GET.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolDAGetSOAP()
    throws Exception {
        entering("testNormalUserPolDAGetSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserPolDAGetSOAP", "Token: " + token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserPolDAGetSOAP", "Token ID: " +
                    tokID);

            boolean polRes = isimpl.authorize("http://www.soapidsvcs3.com:80",
                    "GET", token);
            log(Level.FINEST, "testNormalUserPolDAGetSOAP",
                    "Policy evaluation result: " + polRes);
            assert !(polRes);
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolDAGetSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization SOAP" +
                    " interface for a normal user where policy resource has" +
                    " deny for GET and allow for POST request. The action" +
                    " under test is GET.");
            Reporter.log("Resource: http://www.soapidsvcs3.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Fail");
        }
        exiting("testNormalUserPolDAGetSOAP");
    }

    /**
     * This test validates the authorization SOAP interface for a normal user
     * where policy resource has deny for GET and allow for POST request. The
     * action under test is POST.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolDAPostSOAP()
    throws Exception {
        entering("testNormalUserPolDAPostSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserPolDAPostSOAP", "Token: " + token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserPolDAPostSOAP", "Token ID: " +
                    tokID);

            boolean polRes = isimpl.authorize("http://www.soapidsvcs3.com:80",
                    "POST", token);
            log(Level.FINEST, "testNormalUserPolDAPostSOAP",
                    "Policy evaluation result: " + polRes);
            assert polRes;
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolDAPostSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization SOAP" +
                    " interface for a normal user where policy resource has" +
                    " deny for GET and allow for POST request. The action" +
                    " under test is POST.");
            Reporter.log("Resource: http://www.soapidsvcs3.com:80");
            Reporter.log("Action: POST");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Pass");
        }
        exiting("testNormalUserPolDAPostSOAP");
    }

    /**
     * This test validates the authorization SOAP interface for a normal user
     * where policy resource has deny for both GET and POST request. The
     * action under test are both GET.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolDDGetSOAP()
    throws Exception {
        entering("testNormalUserPolDDGetSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserPolDDGetSOAP", "Token: " + token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserPolDDGetSOAP", "Token ID: " +
                    tokID);

            boolean polRes = isimpl.authorize("http://www.soapidsvcs4.com:80",
                    "GET", token);
            log(Level.FINEST, "testNormalUserPolDDGetSOAP",
                    "Policy evaluation result: " + polRes);
            assert !(polRes);
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolDDGetSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization SOAP" +
                    " interface for a normal user where policy resource has" +
                    " deny for both GET and POST request. The action under" +
                    " test are both GET.");
            Reporter.log("Resource: http://www.soapidsvcs4.com:80");
            Reporter.log("Action: GET");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Fail");
        }
        exiting("testNormalUserPolDDGetSOAP");
    }

    /**
     * This test validates the authorization SOAP interface for a normal user
     * where policy resource has deny for both GET and POST request. The action
     * under test is POST.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserPolDDPostSOAP()
    throws Exception {
        entering("testNormalUserPolDDPostSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserPolDDPostSOAP", "Token: " + token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserPolDDPostSOAP", "Token ID: " +
                    tokID);

            boolean polRes = isimpl.authorize("http://www.soapidsvcs4.com:80",
                    "POST", token);
            log(Level.FINEST, "testNormalUserPolDDPostSOAP",
                    "Policy evaluation result: " + polRes);
            assert !(polRes);
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserPolDDPostSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the authorization SOAP" +
                    " interface for a normal user where policy resource has" +
                    " deny for both GET and POST request. The action under" +
                    " test are both POST.");
            Reporter.log("Resource: http://www.soapidsvcs4.com:80");
            Reporter.log("Action: POST");
            Reporter.log("Subject: Authenticated Users");
            Reporter.log("Expected Result: Fail");
        }
        exiting("testNormalUserPolDDPostSOAP");
    }

    /**
     * This test validates the attributes SOAP interface for a normal user. The
     * current tests validates the retrival of multivalued attribute
     * iplanet-am-user-alias-list.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNormalUserAttributesSOAP()
    throws Exception {
        entering("testNormalUserAttributesSOAP", null);
        try {
            Token token = isimpl.authenticate(userName, userName, "realm=" +
                    realm);
            log(Level.FINEST, "testNormalUserAttributesSOAP", "Token: " +
                    token);
            String tokID = token.getId();
            log(Level.FINEST, "testNormalUserAttributesSOAP", "Token ID: " +
                    tokID);

            String[] attributeNames = {"iplanet-am-user-alias-list"};
            UserDetails ud = isimpl.attributes(attributeNames, token);
            Attribute[] attr = ud.getAttributes();
            for (int i=0; i < attr.length; i++) {
                log(Level.FINEST, "testNormalUserAttributesSOAP",
                        "Attribute name: " + attr[i].getName());
                String[] vals = attr[i].getValues();
                for (int j=0; j < vals.length; j++) {
                   log(Level.FINEST, "testNormalUserAttributesSOAP",
                           "Attribute value: " + vals[j]);
                   if (vals[j].indexOf(userName + "alias") == -1)
                       assert false;
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testNormalUserAttributesSOAP", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the attributes SOAP" +
                    " interface for a normal user. The current tests" +
                    " validates the retrival of multivalued attribute" +
                    " iplanet-am-user-alias-list.");
        }
        exiting("testNormalUserAttributesSOAP");
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
        out.write("<ResourceName name=\"http://www.soapidsvcs1.com:80\"/>");
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
        out.write("<ResourceName name=\"http://www.soapidsvcs2.com:80\"/>");
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
        out.write("<ResourceName name=\"http://www.soapidsvcs3.com:80\"/>");
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
        out.write("<ResourceName name=\"http://www.soapidsvcs4.com:80\"/>");
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
}
