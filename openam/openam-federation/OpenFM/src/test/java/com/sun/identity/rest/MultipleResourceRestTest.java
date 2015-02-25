/**
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
 * $Id: MultipleResourceRestTest.java,v 1.1 2009/11/12 18:37:36 veiming Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.JSONEntitlement;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Hash;
import java.net.URLEncoder;
import java.security.AccessController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;

import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * To Test REST interface
 */
public class MultipleResourceRestTest {
    private static final String REALM = "/";
    private static final String PRIVILEGE_NAME =
        "MultipleResourceRestTestPrivilege";
    private static final SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSuperAdminSubject();
    private static final String RESOURCE_NAME =
        "http://www.MultipleResourceRestTest.com";
    private AMIdentity user;
    private WebResource decisionsClient;
    private WebResource entitlementsClient;
    private String hashedTokenId;
    private String tokenIdHeader;
    private Cookie cookie;

    @BeforeClass
    public void setup() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance(REALM,
            adminSubject);

        {
            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME + "1");
            Map<String, Boolean> actions = new HashMap<String, Boolean>();
            actions.put("GET", true);
            Entitlement entitlement = new Entitlement(RESOURCE_NAME + "/*",
                actions);
            privilege.setEntitlement(entitlement);
            EntitlementSubject sbj = new AuthenticatedUsers();
            privilege.setSubject(sbj);
            pm.add(privilege);
        }
        {
            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME  + "2");
            Map<String, Boolean> actions = new HashMap<String, Boolean>();
            actions.put("GET", false);
            Entitlement entitlement = new Entitlement(RESOURCE_NAME +
                "/index.html", actions);
            privilege.setEntitlement(entitlement);
            EntitlementSubject sbj = new AuthenticatedUsers();
            privilege.setSubject(sbj);
            pm.add(privilege);
        }

        String tokenId = adminToken.getTokenID().toString();
        hashedTokenId = Hash.hash(tokenId);
        tokenIdHeader = RestServiceManager.SSOTOKEN_SUBJECT_PREFIX +
            RestServiceManager.SUBJECT_DELIMITER + tokenId;
        String cookieValue = tokenId;

        if (Boolean.parseBoolean(
            SystemProperties.get(Constants.AM_COOKIE_ENCODE, "false"))) {
            cookieValue = URLEncoder.encode(tokenId, "UTF-8");
        }

        cookie = new Cookie(SystemProperties.get(Constants.AM_COOKIE_NAME),
            cookieValue);

        user = IdRepoUtils.createUser(REALM, "MultipleResourceRestTestUser");

        decisionsClient = Client.create().resource(
            SystemProperties.getServerInstanceName() +
            "/ws/1/entitlement/decisions");
        entitlementsClient = Client.create().resource(
            SystemProperties.getServerInstanceName() +
            "/ws/1/entitlement/entitlements");
    }

    @AfterClass
    public void cleanup() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance(REALM,
            adminSubject);
        pm.remove(PRIVILEGE_NAME + "1");
        pm.remove(PRIVILEGE_NAME + "2");
        IdRepoUtils.deleteIdentity(REALM, user);
    }

    @Test
    public void testDecisions() throws Exception {
        MultivaluedMap params = new MultivaluedMapImpl();
        params.add("resources", RESOURCE_NAME + "/index.html");
        params.add("resources", RESOURCE_NAME + "/a");
        params.add("action", "GET");
        params.add("realm", REALM);
        params.add("subject", hashedTokenId);

        String json = decisionsClient
            .queryParams(params)
            .accept("application/json")
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .get(String.class);

        List<JSONEntitlement> entitlements = JSONEntitlement.getEntitlements(
            new JSONObject(json));
        for (JSONEntitlement e : entitlements) {
            String res = e.getResourceName();
            Map<String, Boolean> actionValues = e.getActionValues();

            if (res.equals(RESOURCE_NAME + "/index.html")) {
                if (actionValues.get("GET")) {
                    throw new Exception(
                        "MultipleResourceRestTest.testDecisions: incorrect result for /index.html");
                }
            } else if (res.equals(RESOURCE_NAME + "/a")) {
                if (!actionValues.get("GET")) {
                    throw new Exception(
                        "MultipleResourceRestTest.testDecisions: incorrect result for /a");
                }
            }

        }
    }

    @Test
    public void testEntitlements() throws Exception {
        MultivaluedMap params = new MultivaluedMapImpl();
        params.add("resource", RESOURCE_NAME);
        params.add("realm", REALM);
        params.add("subject", hashedTokenId);

        String json = entitlementsClient
            .queryParams(params)
            .accept("application/json")
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .get(String.class);
        List<JSONEntitlement> entitlements = JSONEntitlement.getEntitlements(
            new JSONObject(json));
        for (JSONEntitlement e : entitlements) {
            String res = e.getResourceName();
            Map<String, Boolean> actionValues = e.getActionValues();

            if (res.equals(RESOURCE_NAME)) {
                if (!actionValues.isEmpty()) {
                    throw new Exception(
                        "MultipleResourceRestTest.testEntitlements: incorrect result for root");
                }
            } else if (res.equals(RESOURCE_NAME + ":80/index.html")) {
                if (actionValues.get("GET")) {
                    throw new Exception(
                        "MultipleResourceRestTest.testEntitlements: incorrect result for /index.html");
                }
            } else if (res.equals(RESOURCE_NAME + ":80/*")) {
                if (!actionValues.get("GET")) {
                    throw new Exception(
                        "MultipleResourceRestTest.testEntitlements: incorrect result for /*");
                }
            }
        }
    }
}
