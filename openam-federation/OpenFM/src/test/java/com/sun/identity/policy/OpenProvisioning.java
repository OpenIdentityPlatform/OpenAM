/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OpenProvisioning.java,v 1.1 2009/08/19 05:41:02 veiming Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */

package com.sun.identity.policy;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.server.AuthSPrincipal;
import com.sun.identity.entitlement.AttributeLookupCondition;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.UserSubject;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.constraints.ConstraintValidator;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.mockito.Mockito;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class OpenProvisioning {
    private static final String APPLICATION ="openProvisioning";
    private static final String PRIVILEGE_NAME = "openProvisioningTestPrivilege";
    private static final String RESOURCE = "/OP/cropLdap/person";
    private static final String RESOURCE1 = "/OP/cropLdap/person/johndoe";

    private AMIdentity branchMgr;
    private AMIdentity jSmith;
    private AMIdentity johnDoe;
    private ResourceTypeService resourceTypeService;
    private ConstraintValidator constraintValidator;
    private ApplicationServiceFactory applicationServiceFactory;

    @BeforeClass
    public void setup()
        throws SSOException, IdRepoException, EntitlementException {
        resourceTypeService = Mockito.mock(ResourceTypeService.class);
        constraintValidator = Mockito.mock(ConstraintValidator.class);
        applicationServiceFactory = Mockito.mock(ApplicationServiceFactory.class);
        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        AMIdentityRepository amir = new AMIdentityRepository(
                adminToken, "/");
        branchMgr = amir.createIdentity(IdType.GROUP, "openProvisionBranchMgr",
                Collections.EMPTY_MAP);
        johnDoe = createUser(amir, "openProvisionJohnDoe");
        jSmith = createUser(amir, "openProvisionJSmith");
        branchMgr.addMember(jSmith);

        createPolicy(adminToken);
    }

    private void createPolicy(SSOToken adminToken)
        throws EntitlementException {
        PrivilegeManager pMgr = new PolicyPrivilegeManager(
                applicationServiceFactory, resourceTypeService, constraintValidator);
        pMgr.initialize("/", SubjectUtils.createSubject(adminToken));
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("CREATE", Boolean.TRUE);
        actionValues.put("READ", Boolean.TRUE);
        actionValues.put("UPDATE", Boolean.TRUE);
        actionValues.put("DELETE", Boolean.TRUE);
        Entitlement entitlement = new Entitlement(APPLICATION,
                "/OP/*", actionValues);
        entitlement.setName("openProvisioningPrivilege");

        UserSubject sbj = new OpenSSOUserSubject();
        sbj.setID(jSmith.getUniversalId());
        AttributeLookupCondition cond = new AttributeLookupCondition(
            "$USER.postaladdress", "$RES.postaladdress");

        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(PRIVILEGE_NAME);
        privilege.setEntitlement(entitlement);
        privilege.setSubject(sbj);
        privilege.setCondition(cond);
        pMgr.add(privilege);
    }

    @AfterClass
    public void cleanup()
        throws SSOException, IdRepoException, EntitlementException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
        AMIdentityRepository amir = new AMIdentityRepository(
            adminToken, "/");
        Set<AMIdentity> identities = new HashSet<AMIdentity>();
        identities.add(jSmith);
        identities.add(johnDoe);
        identities.add(branchMgr);
        amir.deleteIdentities(identities);

        PrivilegeManager pMgr = new PolicyPrivilegeManager(
                applicationServiceFactory, resourceTypeService, constraintValidator);
        pMgr.initialize("/", SubjectUtils.createSubject(adminToken));
        pMgr.remove(PRIVILEGE_NAME);
    }

    private AMIdentity createUser(AMIdentityRepository amir, String id)
        throws SSOException, IdRepoException {
        Map<String, Set<String>> attrValues = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(id);
        attrValues.put("givenname", set);
        attrValues.put("sn", set);
        attrValues.put("cn", set);
        attrValues.put("userpassword", set);
        
        Set<String> setLocation = new HashSet<String>();
        setLocation.add("CA");
        attrValues.put("postaladdress", setLocation);
        return amir.createIdentity(IdType.USER, id, attrValues);
    }

    @Test
    public void testEval()
        throws Exception {
        SSOToken adminSSOToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        Subject adminSubject = SubjectUtils.createSubject(adminSSOToken);

        Set<Principal> userPrincipals = new HashSet<Principal>(2);
        userPrincipals.add(new AuthSPrincipal(jSmith.getUniversalId()));
        Subject userSubject = new Subject(false, userPrincipals, new HashSet(),
            new HashSet());

        Map<String, Set<String>> envParameters =
            new HashMap<String, Set<String>>();
        Evaluator eval = new Evaluator(adminSubject, APPLICATION);
        List entitlements = eval.evaluate("/", userSubject, RESOURCE,
            envParameters, false);
        Entitlement e1 = (Entitlement)entitlements.iterator().next();
        if (!e1.getActionValues().isEmpty()) {
            throw new Exception(
                "OpenProvisioning.test fails because action values is not empty");
        }
        Map<String, Set<String>> mapAdvices = e1.getAdvices();
        Set<String> setAdvices = mapAdvices.get(
            AttributeLookupCondition.class.getName());
        if (!setAdvices.contains("$USER.postaladdress=$RES.postaladdress")) {
            throw new Exception(
                "OpenProvisioning.test fails because missing advices");
        }
        Set publicCreds = userSubject.getPublicCredentials();
        publicCreds.add("postaladdress=CA");
        Set<String> setLocation = new HashSet<String>();
        setLocation.add("CA");
        envParameters.put("/OP/cropLdap/person/johndoe.postaladdress",
            setLocation);

        eval = new Evaluator(adminSubject, APPLICATION);
        entitlements = eval.evaluate("/", userSubject, RESOURCE1, envParameters,
            false);
        e1 = (Entitlement)entitlements.iterator().next();
        if (e1.getActionValues().isEmpty()) {
            throw new Exception(
                "OpenProvisioning.test fails.");
        }

    }
}
