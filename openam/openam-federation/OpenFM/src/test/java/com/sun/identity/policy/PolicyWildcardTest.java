/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.policy;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.unittest.Util;
import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;

/**
 * To support the testing of OPENAM-726 - Multi-threaded entitlement evaluation gives wrong result
 * @author Mark de Reeper
 */
public class PolicyWildcardTest {
        
    private static String POLICY_NAME_USER = "PolicyWildcardTestUser";
    private static String POLICY_NAME_ADMIN = "PolicyWildcardTestAdmin";

    private static final String REALM = "/";

    private static String URL_RESOURCE_USER1 = "https://www.forgerock.com:443/example";
    private static String URL_RESOURCE_USER2 = "https://www.forgerock.com:443/example/*";
    private static String URL_RESOURCE_USER3 = "https://www.forgerock.com:443/example/*?*";
    
    private static String URL_RESOURCE_ADMIN1 = "https://www.forgerock.com:443/example/admin";
    private static String URL_RESOURCE_ADMIN2 = "https://www.forgerock.com:443/example/admin/*";
    private static String URL_RESOURCE_ADMIN3 = "https://www.forgerock.com:443/example/admin/*?*";
    private static String URL_RESOURCE_ADMIN4 = "https://www.forgerock.org:443/example/admin";
    private static String URL_RESOURCE_ADMIN5 = "https://www.forgerock.org:443/example/admin/*";
    private static String URL_RESOURCE_ADMIN6 = "https://www.forgerock.org:443/example/admin/*?*";
    
    private static String URL_USER_TEST1 = "https://www.forgerock.com:443/example/results?cid=14";
    private static String URL_USER_TEST2 = "https://www.forgerock.com:443/example/results";
    private static String URL_USER_TEST3 = "https://www.forgerock.com:443/example/user/maintainUser?cid=200";
    private static String URL_USER_TEST4 = "https://www.forgerock.com:443/example/user/maintainUser";
    
    private static String URL_ADMIN_USER_TEST1 = "https://www.forgerock.com:443/example/admin/results?cid=14";
    private static String URL_ADMIN_USER_TEST2 = "https://www.forgerock.com:443/example/admin/maintainUser?cid=200";
    private static String URL_ADMIN_USER_TEST3 = "https://www.forgerock.com:443/example/admin/maintainUser";
    
    private static String TEST_ADMIN_GRP_NAME = "policyTestAdminGroup";
    private static String TEST_ADMIN_USER_NAME = "policyTestAdminUser";
    private static String TEST_USER_NAME = "policyTestUser";

    private AMIdentity testAdminGroup;
    private AMIdentity testAdminUser;
    private AMIdentity testUser;
    private SSOToken userSSOToken;
    private SSOToken adminUserSSOToken;
    private SSOToken adminToken;

    @BeforeClass
    public void setup() throws Exception {
        
        adminToken = Util.getAdminToken();
        
        testUser = Util.createATestUser(adminToken, TEST_USER_NAME, REALM);
        testAdminUser = Util.createATestUser(adminToken, TEST_ADMIN_USER_NAME, REALM);
        testAdminGroup = Util.createATestGroup(adminToken, TEST_ADMIN_GRP_NAME, REALM);
        
        testAdminGroup.addMember(testAdminUser);
        
        userSSOToken = Util.datastoreLogin(TEST_USER_NAME, TEST_USER_NAME, REALM);
        adminUserSSOToken = Util.datastoreLogin(TEST_ADMIN_USER_NAME, TEST_ADMIN_USER_NAME, REALM);

        PolicyManager pm = new PolicyManager(adminToken, REALM);
        
        Policy policy = new Policy(POLICY_NAME_USER, "user - discard", false, true);
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_USER1, URL_RESOURCE_USER1));
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_USER2, URL_RESOURCE_USER2));
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_USER3, URL_RESOURCE_USER3));
        policy.addSubject("AllUsers", Util.createAuthenticatedUsersSubject(pm));
        pm.addPolicy(policy);

        policy = new Policy(POLICY_NAME_ADMIN, "admin - discard", false, true);
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_ADMIN1, URL_RESOURCE_ADMIN1));
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_ADMIN2, URL_RESOURCE_ADMIN2));
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_ADMIN3, URL_RESOURCE_ADMIN3));
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_ADMIN4, URL_RESOURCE_ADMIN4));
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_ADMIN5, URL_RESOURCE_ADMIN5));
        policy.addRule(Util.createAGetPostRule(URL_RESOURCE_ADMIN6, URL_RESOURCE_ADMIN6));
        policy.addSubject("Admins", Util.createAMIdentitySubject(pm, testAdminGroup), true);
        pm.addPolicy(policy);
    }
    
    @AfterClass
    public void cleanup() throws PolicyException, SSOException, IdRepoException{
        
        SSOTokenManager.getInstance().destroyToken(userSSOToken);
        SSOTokenManager.getInstance().destroyToken(adminUserSSOToken);
        
        PolicyManager pm = new PolicyManager(adminToken, REALM);
        pm.removePolicy(POLICY_NAME_USER);
        pm.removePolicy(POLICY_NAME_ADMIN);

        AMIdentityRepository amir = new AMIdentityRepository(adminToken, REALM);
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(testAdminGroup);
        identities.add(testAdminUser);
        identities.add(testUser);
        amir.deleteIdentities(identities);
    }

    @Test
    public void testIsAllowedTest1() throws Exception {
        
        PolicyEvaluator pe = new PolicyEvaluator(Util.IPLANETAMWEBAGENTSERVICE);        
        Assert.assertTrue(pe.isAllowed(userSSOToken, URL_USER_TEST1, Util.GET_ACTION), "Expecting " + URL_USER_TEST1 + " to be allowed.");        
    }
    
    @Test
    public void testIsAllowedTest2() throws Exception {
        
        PolicyEvaluator pe = new PolicyEvaluator(Util.IPLANETAMWEBAGENTSERVICE);        
        Assert.assertTrue(pe.isAllowed(userSSOToken, URL_USER_TEST2, Util.GET_ACTION), "Expecting " + URL_USER_TEST2 + " to be allowed.");        
    }
    
    @Test
    public void testIsAllowedTest3() throws Exception {
        
        PolicyEvaluator pe = new PolicyEvaluator(Util.IPLANETAMWEBAGENTSERVICE);        
        Assert.assertTrue(pe.isAllowed(userSSOToken, URL_USER_TEST3, Util.GET_ACTION), "Expecting " + URL_USER_TEST3 + " to be allowed.");        
    }

    @Test
    public void testIsAdminAllowedTest1() throws Exception {
        
        PolicyEvaluator pe = new PolicyEvaluator(Util.IPLANETAMWEBAGENTSERVICE);        
        Assert.assertTrue(pe.isAllowed(adminUserSSOToken, URL_ADMIN_USER_TEST1, Util.GET_ACTION), "Expecting " + URL_ADMIN_USER_TEST1 + " to be allowed.");        
    }

    @Test
    public void testIsAdminAllowedTest2() throws Exception {
        
        PolicyEvaluator pe = new PolicyEvaluator(Util.IPLANETAMWEBAGENTSERVICE);        
        Assert.assertTrue(pe.isAllowed(adminUserSSOToken, URL_ADMIN_USER_TEST2, Util.GET_ACTION), "Expecting " + URL_ADMIN_USER_TEST2 + " to be allowed.");        
    }

    @Test
    public void testIsAdminAllowedTest3() throws Exception {
        
        PolicyEvaluator pe = new PolicyEvaluator(Util.IPLANETAMWEBAGENTSERVICE);        
        Assert.assertTrue(pe.isAllowed(adminUserSSOToken, URL_ADMIN_USER_TEST3, Util.GET_ACTION), "Expecting " + URL_ADMIN_USER_TEST3 + " to be allowed.");        
    }

    @Test
    public void testResourceSelfTest1() throws Exception {
        
        Assert.assertTrue(
                Util.isGetPostAllowed(userSSOToken, URL_USER_TEST1, ResourceResult.SELF_SCOPE), "Expecting SELF " + URL_USER_TEST1 + " to be allowed.");
    }
    
    @Test
    public void testResourceSelfTest2() throws Exception {
        
        Assert.assertTrue(
                Util.isGetPostAllowed(userSSOToken, URL_USER_TEST2, ResourceResult.SELF_SCOPE), "Expecting SELF " + URL_USER_TEST2 + " to be allowed.");
    }
    
    @Test
    public void testResourceSelfTest3() throws Exception {
        
        Assert.assertTrue(
                Util.isGetPostAllowed(userSSOToken, URL_USER_TEST3, ResourceResult.SELF_SCOPE), "Expecting SELF " + URL_USER_TEST3 + " to be allowed.");
    }
 
    @Test
    public void testResourceSubTreeTest1() throws Exception {
        
        Assert.assertTrue(
                Util.isGetPostAllowed(userSSOToken, URL_USER_TEST1, ResourceResult.SUBTREE_SCOPE), "Expecting SUBTREE " + URL_USER_TEST1 + " to be allowed.");
    }
 
    @Test
    public void testResourceSubTreeTest2() throws Exception {
        
        Assert.assertTrue(
                Util.isGetPostAllowed(userSSOToken, URL_USER_TEST2, ResourceResult.SUBTREE_SCOPE), "Expecting SUBTREE " + URL_USER_TEST2 + " to be allowed.");
    }
 
    @Test
    public void testResourceSubTreeTest3() throws Exception {
        
        Assert.assertTrue(
                Util.isGetPostAllowed(userSSOToken, URL_USER_TEST3, ResourceResult.SUBTREE_SCOPE), "Expecting SUBTREE " + URL_USER_TEST3 + " to be allowed.");
    }

    @Test
    public void testResourceSubTreeTest4() throws Exception {
        
        Assert.assertTrue(
                Util.isGetPostAllowed(userSSOToken, URL_USER_TEST4, ResourceResult.SUBTREE_SCOPE), "Expecting SUBTREE " + URL_USER_TEST4 + " to be allowed.");
    }
}