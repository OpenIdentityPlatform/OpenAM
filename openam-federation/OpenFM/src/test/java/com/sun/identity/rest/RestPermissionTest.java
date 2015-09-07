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
 * $Id: RestPermissionTest.java,v 1.4 2009/12/11 09:24:43 veiming Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */

package com.sun.identity.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPrivilege;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.rest.PrivilegeResource;
import com.sun.identity.entitlement.util.AuthUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Hash;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import java.net.URLEncoder;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.ws.rs.core.Cookie;

import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class RestPermissionTest {
    private static final String REALM = "/";
    private static final String PRIVILEGE_NAME = "RestPermissionTestPrivilege";
    private static final String USER_NAME = "RestPermissionTestUser";
    private static final String GROUP_NAME = "RestPermissionTestGroup";
    private static final String RESOURCE_NAME =
        "http://www.RestPermissionTest.com";
    private static final SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSuperAdminSubject();
    private WebResource decisionClient;
    private WebResource privilegeClient;
    private String hashedTokenId;
    private String tokenIdHeader;
    private Cookie cookie;
    private AMIdentity user;
    private AMIdentity group;

    @BeforeClass
    public void setup() throws Exception {
        group = IdRepoUtils.createGroup(REALM, GROUP_NAME);
        user = IdRepoUtils.createUser(REALM, USER_NAME);
        group.addMember(user);
        login();
        createPrivilege();

        decisionClient = Client.create().resource(
            SystemProperties.getServerInstanceName() +
            "/ws/1/entitlement/decision");
        privilegeClient = Client.create().resource(
            SystemProperties.getServerInstanceName() +
            "/ws/1/entitlement/privilege");
    }

    private void createPrivilege() throws EntitlementException {
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(PRIVILEGE_NAME);
        privilege.setDescription("desciption");
        Map<String, Boolean> actions = new HashMap<String, Boolean>();
        actions.put("GET", true);
        Entitlement entitlement = new Entitlement(RESOURCE_NAME + "/*",
            actions);
        privilege.setEntitlement(entitlement);
        EntitlementSubject sbj = new AuthenticatedUsers();
        privilege.setSubject(sbj);
        pm.add(privilege);
    }

    private void login() throws Exception {
        SSOToken ssoToken = AuthUtils.authenticate(REALM, USER_NAME,
            USER_NAME);
        String userTokenId = ssoToken.getTokenID().toString();
        hashedTokenId = Hash.hash(userTokenId);
        tokenIdHeader = RestServiceManager.SSOTOKEN_SUBJECT_PREFIX +
            RestServiceManager.SUBJECT_DELIMITER + userTokenId;
        String cookieValue = userTokenId;

        if (Boolean.parseBoolean(
            SystemProperties.get(Constants.AM_COOKIE_ENCODE, "false"))) {
            cookieValue = URLEncoder.encode(userTokenId, "UTF-8");
        }

        cookie = new Cookie(SystemProperties.get(Constants.AM_COOKIE_NAME),
            cookieValue);
    }

    @AfterClass
    public void cleanup() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        pm.remove(PRIVILEGE_NAME);
        IdRepoUtils.deleteIdentity(REALM, group);
        IdRepoUtils.deleteIdentity(REALM, user);
    }

    @Test
    public void negativeDecisionTest() throws Exception {
        try {
            decisionRestCall();
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse().getStatus() != 401) {
                throw ex;
            }
        }
    }

    @Test (dependsOnMethods="negativeDecisionTest")
    public void positiveDecisionTest() throws Exception {
        setPermission("EntitlementRestAccess", true);
        decisionRestCall();
        setPermission("EntitlementRestAccess", false);
    }

    @Test (dependsOnMethods="positiveDecisionTest")
    public void negativePrivilegeTest() throws Exception {
        try {
            getAndPutRestCall();
        } catch (UniformInterfaceException ex) {
            if (ex.getResponse().getStatus() != 401) {
                throw ex;
            }
        }
    }

    @Test (dependsOnMethods="negativePrivilegeTest")
    public void positivePrivilegeTest() throws Exception {
        setPermission("PolicyAdmin", true);
        getAndPutRestCall();
        setPermission("PolicyAdmin", true);
    }

    private void getAndPutRestCall() throws Exception {
        String result = privilegeClient
            .path(PRIVILEGE_NAME)
            .queryParam("subject", hashedTokenId)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .get(String.class);
        JSONObject jbody = parseResult(result);
        String jsonStr = jbody.getString(PrivilegeResource.RESULT);

        Privilege privilege = Privilege.getNewInstance(
            new JSONObject(jsonStr));
        privilege.setDescription("desciption1");

        Form form = new Form();
        form.add("privilege.json", privilege.toMinimalJSONObject());
        result = privilegeClient
            .path(PRIVILEGE_NAME)
            .queryParam("subject", hashedTokenId)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .put(String.class, form);
        JSONObject jo = new JSONObject(result);
        if (jo.optInt("statusCode") != 200) {
            throw new Exception("PrivilegeRestTest.getAndPutRestCall failed.");
        }
    }

    private JSONObject parseResult(String result) throws Exception {
        JSONObject jo = new JSONObject(result);
        if (jo.optInt("statusCode") != 200) {
            throw new Exception("PrivilegeRestTest.search failed.");
        }
        JSONObject jbody = jo.optJSONObject("body");
        if (jbody == null) {
            throw new Exception(
                "PrivilegeRestTest.search failed: body element is null");
        }
        return jbody;
    }
    
    private void decisionRestCall() throws Exception {
        Form params = new Form();
        params.add("subject", hashedTokenId);
        params.add("resource", "http://www.example.com/index.html");
        params.add("action", "GET");
        params.add("realm", REALM);

        decisionClient
            .queryParams(params)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .accept("text/plain")
            .get(String.class);
    }

    private void setPermission(String permissionName, boolean bAdd)
        throws Exception {
        DelegationManager mgr = new DelegationManager(
            adminToken, REALM);
        DelegationPrivilege dp = getDelegationPrivilege(permissionName,
            mgr.getPrivileges());
        if (dp == null) {
            dp = new DelegationPrivilege(permissionName,
                Collections.EMPTY_SET, REALM);
        }

        Set<String> subject = dp.getSubjects();
        if (bAdd) {
            subject.add(group.getUniversalId());
        } else {
            subject.remove(group.getUniversalId());
        }
        mgr.addPrivilege(dp);
    }

    private DelegationPrivilege getDelegationPrivilege(
        String name,
        Set<DelegationPrivilege> privilegeObjects
    ) {
        DelegationPrivilege dp = null;
        for (DelegationPrivilege p : privilegeObjects) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

}
