/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: TestAttributeEvaluator.java,v 1.2 2009/06/22 10:14:36 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.server.AuthSPrincipal;
import com.sun.identity.entitlement.opensso.PolicyPrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestAttributeEvaluator {
    private static final String APPL_NAME = "TestEvaluatorAppl";
    private static final String PRIVILEGE1_NAME = "entitlementPrivilege1";
    private static final String USER1_NAME = "privilegeEvalTestUser1";
    private static final String URL1 = "http://www.testevaluator.com:80/private";

    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private boolean migrated = EntitlementConfiguration.getInstance(
        adminSubject, "/").migratedToEntitlementService();

    private AMIdentity user1;
    private String attrName = "mail";
    private String attrValue = "u1@sun.com";

    @BeforeClass
    public void setup() throws Exception {
        if (!migrated) {
            return;
        }
        
        Application appl = new Application("/", APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));

        Set<String> avaliableResources = new HashSet<String>();
        avaliableResources.add("http://www.testevaluator.com:80/*");
        appl.addResources(avaliableResources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationManager.saveApplication(adminSubject, "/", appl);

        PrivilegeManager pm = new PolicyPrivilegeManager();
        pm.initialize("/", adminSubject);
        Map<String, Boolean> actions = new HashMap<String, Boolean>();
        actions.put("GET", Boolean.TRUE);
        Entitlement ent = new Entitlement(APPL_NAME, URL1, actions);
        user1 = createUser(USER1_NAME);
        AttributeSubject as = new AttributeSubject();
        as.setID(attrName);
        as.setValue(attrValue);
        EntitlementSubject es1 = as;
        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(PRIVILEGE1_NAME);
        privilege.setEntitlement(ent);
        privilege.setSubject(es1);
        pm.addPrivilege(privilege);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }
        PrivilegeManager pm = new PolicyPrivilegeManager();
        pm.initialize("/", SubjectUtils.createSubject(adminToken));
        pm.removePrivilege(PRIVILEGE1_NAME);

        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(user1);
        amir.deleteIdentities(identities);

        ApplicationManager.deleteApplication(adminSubject, "/", APPL_NAME);
    }

    @Test
    public void postiveTest()
        throws Exception {
        if (!migrated) {
            return;
        }
        if (!evaluate(URL1)) {
            throw new Exception("TestEvaluator.postiveTest failed");
        }
    }

    private boolean evaluate(String res)
        throws EntitlementException {
        Subject subject = createSubject(user1.getUniversalId());
        Set actions = new HashSet();
        actions.add("GET");
        Evaluator evaluator = new Evaluator(
            SubjectUtils.createSubject(adminToken),
            APPL_NAME);
        return evaluator.hasEntitlement("/", subject,
            new Entitlement(res, actions), Collections.EMPTY_MAP);
    }

    
    private AMIdentity createUser(String name)
        throws SSOException, IdRepoException {
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Map<String, Set<String>> attrValues =new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(name);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);
        set = new HashSet<String>();
        set.add(attrValue);
        attrValues.put(attrName, set);
        return amir.createIdentity(IdType.USER, name, attrValues);
    }

    private static Subject createSubject(String uuid) {
        Set<Principal> userPrincipals = new HashSet<Principal>(2);
        userPrincipals.add(new AuthSPrincipal(uuid));
        return new Subject(false, userPrincipals, new HashSet(),
            new HashSet());
    }

}
