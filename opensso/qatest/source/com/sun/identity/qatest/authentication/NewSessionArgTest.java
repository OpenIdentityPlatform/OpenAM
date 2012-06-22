/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.qatest.authentication;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.authentication.AuthenticationCommon;
import com.sun.identity.qatest.idm.IDMConstants;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 *
 * @author cmwesley
 */
public class NewSessionArgTest extends AuthenticationCommon {

    private IDMCommon idmc;
    private ResourceBundle testRb;
    private SSOToken adminToken;
    private String absoluteRealm;
    private String testUser;
    private String testPassword;

    public NewSessionArgTest() {
        super("NewSessionArgTest");
        idmc = new IDMCommon();
    }

    @Parameters({"testRealm", "testModule"})
    @BeforeClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void setup(String testRealm, String testModule)
    throws Exception {
        Object[] params = {testRealm, testModule};
        entering("setup", params);

        try {
            testRb = ResourceBundle.getBundle("authentication" + fileseparator +
                    "NewSessionArgTest");

            testUser = testRb.getString("am-auth-newsessionarg-user");
            testPassword = testRb.getString("am-auth-newsessionarg-password");

            log(Level.FINEST, "setup", "testModule = " + testModule);
            log(Level.FINEST, "setup", "testUser = " + testUser);
            log(Level.FINEST, "setup", "testPassword = " + testPassword);

            Reporter.log("TestModule: " + testModule);
            Reporter.log("TestUser: " + testUser);
            Reporter.log("TestPassword: " + testPassword);

            adminToken = getToken(adminUser, adminPassword, basedn);
            absoluteRealm = testRealm;
            if (!testRealm.equals("/")) {
                if (realm.endsWith("/")) {
                    absoluteRealm = realm + testRealm;
                } else {
                    absoluteRealm = realm + "/" + testRealm;
                }
                Map realmAttrMap = new HashMap();
                Set realmSet = new HashSet();
                realmSet.add("Active");
                realmAttrMap.put("sunOrganizationStatus", realmSet);
                log(Level.FINE, "setup", "Creating the realm " + testRealm);
                AMIdentity amid = idmc.createIdentity(adminToken,
                        realm, IdType.REALM, testRealm, realmAttrMap);
                log(Level.FINE, "setup",
                        "Verifying the existence of sub-realm " +
                        testRealm);
                if (amid == null) {
                    log(Level.SEVERE, "setup", "Creation of sub-realm " +
                            testRealm + " failed!");
                    assert false;
                }
            }            

            StringBuffer attrBuffer =  new StringBuffer("sn=" + testUser).
                    append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                    append("cn=" + testUser).
                    append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                    append("userpassword=" + testPassword).
                    append(IDMConstants.IDM_KEY_SEPARATE_CHARACTER).
                    append("inetuserstatus=Active");

            log(Level.FINE, "setup",
                    "Creating user " + testUser + " ...");
            if (!idmc.createID(testUser, "user", attrBuffer.toString(),
                    adminToken, testRealm)) {
                log(Level.SEVERE, "createUser", "Failed to create user " +
                        "identity " + testUser + "...");
                assert false;
            }
            exiting("setup");
        } catch (Exception e) {
            cleanup(testRealm, testModule);
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (adminToken != null) {
                destroyToken(adminToken);
            }
        }
    }

    @Parameters({"testRealm", "testModule"})
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec",
        "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testNewSessionArg(String testRealm, String testModule)
    throws Exception {
        Object[] params = {testRealm, testModule};
        entering("testNewSessionArg", params);
        String baseLoginString = null;
        String loginString = null;
        String newSessionString = null;
        WebClient wc = new WebClient();

        try {
            String authSuccessTitle =
                    testRb.getString("am-auth-newsessionarg-auth-success-msg");
            baseLoginString = protocol + ":" + "//" + host + ":" + port + uri +
                    "/UI/Login?";
            StringBuffer loginBuffer = new StringBuffer(baseLoginString);
            if (!testRealm.equals("/")) {
                loginBuffer.append("realm=").append(testRealm).append("&");
            }
            String moduleInstance = getAuthInstanceName(testModule);
            loginBuffer.append("module=").append(moduleInstance).
                    append("&IDToken1=").append(testUser);
            
            if (!testModule.equals("anonymous")) {
                loginBuffer.append("&IDToken2=").append(testPassword);
            }

            loginString = loginBuffer.toString();
            log(Level.FINEST, "testNewSessionArg", "Zero page login URL = " +
                    loginString);
            URL loginURL = new URL(loginString);
            HtmlPage page = (HtmlPage)wc.getPage(loginURL);
            String afterLoginTitle = page.getTitleText();

            log(Level.FINEST, "testNewSessionArg", "The page title after " +
                    "authentication = " + afterLoginTitle);
            if (afterLoginTitle.equals(authSuccessTitle)) {
                newSessionString = baseLoginString + "arg=newsession";
                URL newSessionURL = new URL(newSessionString);
                HtmlPage newSessionPage =
                        (HtmlPage)wc.getPage(newSessionURL);
                String loginMsg =
                        testRb.getString("am-auth-newsessionarg-login-msg");
                assert getHtmlPageStringIndex(newSessionPage, loginMsg) != -1;
            } else {
                log(Level.SEVERE, "testNewSessionArg",
                        "Unexpected page title after authentication = " +
                        afterLoginTitle);
                assert false;
            }
            exiting("testNewSessionArg");
        } catch (Exception e) {
            log(Level.SEVERE, "testNewSessionArg", e.getMessage(), params);
            e.printStackTrace();
            cleanup(testRealm, testModule);
            throw e;
        } finally {
            consoleLogout(wc, protocol + ":" + "//" + host + ":" + port + uri +
                    "/UI/Logout");
        }
    }

    @Parameters({"testRealm", "testModule"})
    @AfterClass(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad",
        "ad_sec", "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void cleanup(String testRealm, String testModule)
    throws Exception {
        Object[] params = {testModule, testRealm};
        entering("cleanup", params);

        try {
            log(Level.FINEST, "cleanup", "TestRealm: " + testRealm);
            log(Level.FINEST, "cleanup", "TestModule: " + testModule);
            Reporter.log("TestRealm: " + testRealm);
            Reporter.log("TestModule: " + testModule);

            List<IdType> idTypeList = new ArrayList();
            idTypeList.add(IdType.USER);
            List<String> idNameList = new ArrayList();
            idNameList.add(testUser);
            log(Level.FINE, "cleanup", "Deleting the user " + testUser);
            adminToken = getToken(adminUser, adminPassword, realm);
            idmc.deleteIdentity(adminToken, testRealm, idTypeList,
                    idNameList);


            if (!testRealm.equals("/")) {
                if (!absoluteRealm.startsWith("/")) {
                    absoluteRealm = "/" + absoluteRealm;
                }
                log(Level.FINE, "cleanup", "Deleting the sub-realm " +
                        absoluteRealm);
                idmc.deleteRealm(adminToken, absoluteRealm);
            }
            exiting("cleanup");
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (adminToken != null) {
                destroyToken(adminToken);
            }
        }

    }
}
