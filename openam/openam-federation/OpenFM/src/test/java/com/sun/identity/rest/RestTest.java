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
 * $Id: RestTest.java,v 1.3 2009/11/24 23:08:35 veiming Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.rest;

import com.sun.identity.entitlement.util.AuthUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.JSONEntitlement;
import com.sun.identity.entitlement.NumericAttributeCondition;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.unittest.UnittestLog;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Hash;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.ws.rs.core.Cookie;

import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * To Test REST interface
 * @author Ravi Hingarajiya <ravi.hingarajiya@sun.com>
 */
public class RestTest {
    private static final String REALM = "/";
    private static final String AGENT_NAME = "RestTestAgent";
    private static final String PRIVILEGE_NAME = "RestTestPrivilege";
    private Subject adminSubject = SubjectUtils.createSuperAdminSubject();
    private static final String RESOURCE_NAME = "http://www.resttest.com";
    private static final String ATTR_NAME = "bankAcc";
    private static final float ATTR_VAL = 1234f;
    private AMIdentity user;
    private String userTokenIdHeader;
    private Cookie cookie;
    private String hashedUserTokenId;
    private WebResource entitlementClient;
    private WebResource entitlementsClient;
    private WebResource decisionClient;
    private WebResource decisionsClient;

    @BeforeClass
    public void setup() throws Exception {
        try {
            PrivilegeManager pm = PrivilegeManager.getInstance(REALM,
                adminSubject);
            Privilege privilege = Privilege.getNewInstance();
            privilege.setName(PRIVILEGE_NAME);

            Map<String, Boolean> actions = new HashMap<String, Boolean>();
            actions.put("GET", true);
            Entitlement entitlement = new Entitlement(RESOURCE_NAME + "/*",
                actions);
            privilege.setEntitlement(entitlement);
            EntitlementSubject sbj = new AuthenticatedUsers();
            privilege.setSubject(sbj);

            NumericAttributeCondition cond = new NumericAttributeCondition();
            cond.setAttributeName(ATTR_NAME);
            cond.setOperator(NumericAttributeCondition.Operator.EQUAL);
            cond.setValue(ATTR_VAL);
            privilege.setCondition(cond);
            pm.add(privilege);
            user = IdRepoUtils.createAgent(REALM, AGENT_NAME);
            SSOToken ssoToken = AuthUtils.authenticate(REALM, AGENT_NAME,
                AGENT_NAME);
            String userTokenId = ssoToken.getTokenID().toString();
            hashedUserTokenId = Hash.hash(userTokenId);
            userTokenIdHeader = RestServiceManager.SSOTOKEN_SUBJECT_PREFIX +
                RestServiceManager.SUBJECT_DELIMITER + userTokenId;
            String cookieValue = userTokenId;

            if (Boolean.parseBoolean(
                SystemProperties.get(Constants.AM_COOKIE_ENCODE, "false"))) {
                cookieValue = URLEncoder.encode(userTokenId, "UTF-8");
            }

            cookie = new Cookie(SystemProperties.get(Constants.AM_COOKIE_NAME),
                 cookieValue);

            String serverURL = SystemProperties.getServerInstanceName();
            decisionClient = Client.create().resource(serverURL +
                "/ws/1/entitlement/decision");
            decisionsClient = Client.create().resource(serverURL +
                "/ws/1/entitlement/decisions");
            entitlementClient = Client.create().resource(serverURL +
                "/ws/1/entitlement/entitlement");
            entitlementsClient = Client.create().resource(serverURL +
                "/ws/1/entitlement/entitlements");
        } catch (Exception e) {
            UnittestLog.logError("RestTest.setup() failed:", e);
            throw e;
        }
    }

    @AfterClass
    public void cleanup() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance(REALM,
            adminSubject);
        pm.remove(PRIVILEGE_NAME);
        IdRepoUtils.deleteIdentity(REALM, user);
    }

    @Test
    public void getDecisionTest() throws Exception {
        Form params = new Form();
        params.add("subject", hashedUserTokenId);
        params.add("resource", RESOURCE_NAME + "/index.html");
        params.add("action", "GET");
        params.add("env", ATTR_NAME + "=" + ATTR_VAL);
        params.add("realm", REALM);

        String decision = decisionClient
            .queryParams(params)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
            .cookie(cookie)
            .accept("text/plain")
            .get(String.class);
        if ((decision == null) || !decision.equals("allow")) {
            throw new Exception("RESTTest.getDecisionTest() failed");
        }
    }

    @Test
    public void getDecisionsTest() throws Exception {
        Form params = new Form();
        params.add("subject", hashedUserTokenId);
        params.add("resources", RESOURCE_NAME + "/index.html");
        params.add("action", "GET");
        params.add("env", ATTR_NAME + "=" + ATTR_VAL);
        params.add("realm", REALM);

        String json = decisionsClient
            .queryParams(params)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
            .cookie(cookie)
            .accept("application/json")
            .get(String.class);

        JSONObject jo = new JSONObject(json);
        if (jo.optInt("statusCode") != 200) {
            throw new Exception(
                "RESTTest.getDecisionsTest() failed, status code not 200");
        }
        JSONObject jbody = jo.optJSONObject("body");
        if (jbody == null) {
            throw new Exception(
                "RESTTest.getDecisionsTest() failed, body element is null");
        }
        JSONArray results = jbody.optJSONArray("results");
        if (results == null) {
            throw new Exception(
                "RESTTest.getDecisionsTest() failed, results array is null");
        }
        if (results.length() < 1) {
            throw new Exception(
                "RESTTest.getDecisionsTest() failed, results array is empty");
        }
        JSONEntitlement ent = new JSONEntitlement(results.getJSONObject(0));
        boolean result = ent.getActionValue("GET");
        if (!result) {
            throw new Exception("RESTTest.getDecisionsTest() failed");
        }
    }

    @Test
    public void getEntitlementTest() throws Exception {
        Form params = new Form();
        params.add("subject", hashedUserTokenId);
        params.add("resource", RESOURCE_NAME + "/index.html");
        params.add("action", "GET");
        params.add("env", ATTR_NAME + "=" + ATTR_VAL);
        params.add("realm", REALM);

        String json = entitlementClient
            .queryParams(params)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
            .cookie(cookie)
            .accept("application/json")
            .get(String.class);
        JSONObject jo = new JSONObject(json);
        if (jo.optInt("statusCode") != 200) {
            throw new Exception(
                "RESTTest.getEntitlementTest() failed, status code not 200");
        }
        JSONObject jbody = jo.optJSONObject("body");
        if (jbody == null) {
            throw new Exception(
                "RESTTest.getEntitlementTest() failed, body element is null");
        }
        JSONEntitlement ent = new JSONEntitlement(jbody);
        boolean result = ent.getActionValue("GET");
        if (!result) {
            throw new Exception("RESTTest.getEntitlementTest() failed");
        }
    }

    @Test
    public void getEntitlementsTest() throws Exception {
        Form params = new Form();
        params.add("subject", hashedUserTokenId);
        params.add("resource", RESOURCE_NAME);
        params.add("env", ATTR_NAME + "=" + ATTR_VAL);
        params.add("realm", REALM);

        String json = entitlementsClient
            .queryParams(params)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
            .cookie(cookie)
            .accept("application/json")
            .get(String.class);
        JSONObject jo = new JSONObject(json);
        if (jo.optInt("statusCode") != 200) {
            throw new Exception(
                "RESTTest.getEntitlementsTest: failed, status code not 200");
        }
        JSONObject jbody = jo.optJSONObject("body");
        if (jbody == null) {
            throw new Exception(
                "RESTTest.getEntitlementsTest: failed, body element is null");
        }
        JSONArray results = jbody.optJSONArray("results");
        if (results == null) {
            throw new Exception(
                "RESTTest.getEntitlementsTest: failed, results element is null");
        }
        if (results.length() < 1) {
            throw new Exception(
                "RESTTest.getEntitlementsTest: failed, results array is empty");
        }
        // dude, there are two entitlements returned.
        // the first one is the root resource which is http://www.resttest.com
        // and the action value is empty.
        // we need to get the second one, which is http://www.resttest.com:80/*
        JSONEntitlement ent = new JSONEntitlement(results.getJSONObject(1));
        Object resultObj = ent.getActionValue("GET");
        if (resultObj != null) {
            if (!ent.getActionValue("GET")) {
                throw new Exception(
                    "RESTTest.getEntitlementsTest: failed, action value is false");
            }
        } else {
            throw new Exception(
                "RESTTest.getEntitlementsTest: failed, action value is null");
        }
    }

    @Test
    public void negativeTest() throws Exception {
        Form params = new Form();
        params.add("subject", hashedUserTokenId);
        params.add("resource", RESOURCE_NAME + "/index.html");
        params.add("action", "GET");
        params.add("realm", REALM);

        String decision = decisionClient
            .queryParams(params)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
            .cookie(cookie)
            .accept("text/plain")
            .get(String.class);
        if ((decision != null) && decision.equals("allow")) {
            throw new Exception("RESTTest.negativeTest (/decision) failed");
        }

        String json = entitlementClient
            .queryParams(params)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
            .cookie(cookie)
            .accept("application/json")
            .get(String.class);

        JSONObject jo = new JSONObject(json);
        if (jo.optInt("statusCode") != 200) {
            throw new Exception("RESTTest.negativeTest() failed, status code not 200");
        }
        JSONObject jbody = jo.optJSONObject("body");
        if (jbody == null) {
            throw new Exception("RESTTest.negativeTest() failed, body element is null");
        }
        JSONEntitlement ent = new JSONEntitlement(jbody);
        boolean result = false;
        Object resultObj = ent.getActionValue("GET");
        if (resultObj != null) {
            result = ent.getActionValue("GET");
        }
        if (result) {
            throw new Exception("RESTTest.getnegativeTest() failed");
        }
        Map<String, Set<String>> advices = ent.getAdvices();
        Set<String> setNumericCondAdvice = advices.get(
            NumericAttributeCondition.class.getName());
        if ((setNumericCondAdvice == null) || setNumericCondAdvice.isEmpty()) {
            throw new Exception("RESTTest.negativeTest: no advice");
        }
        String advice = setNumericCondAdvice.iterator().next();
        if (!advice.equals(ATTR_NAME + "=" + ATTR_VAL)) {
            throw new Exception("RESTTest.negativeTest: incorrect advice");
        }
    }

    @Test
    public void missingResourceTest() throws Exception {
        Form params = new Form();
        params.add("subject", hashedUserTokenId);
        params.add("action", "GET");
        params.add("env", ATTR_NAME + "=" + ATTR_VAL);
        params.add("realm", REALM);

        try {
            entitlementClient
                .queryParams(params)
                .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
                .cookie(cookie)
                .accept("application/json")
                .get(String.class);
            throw new Exception(
                "RESTTest.missingResourceTest: no exception thrown.");
        } catch (UniformInterfaceException e) {
            int errorCode = e.getResponse().getStatus();
            if (errorCode != 400) {
                throw new Exception(
                    "RESTTest.missingResourceTest: incorrect error code");
            }
            String json = e.getResponse().getEntity(String.class);
            JSONObject jo = new JSONObject(json);
            if (jo.optInt("statusCode") != 420) {
                throw new Exception(
                    "RESTTest.missingResourceTest() failed, status code not 420");
            }
            if (jo.optJSONObject("body") != null) {
                throw new Exception(
                    "RESTTest.missingResourceTest() failed, body not empty");
            }
        }
    }

    @Test
    public void missingResourcesTest() throws Exception {
        Form params = new Form();
        params.add("subject", hashedUserTokenId);
        params.add("action", "GET");
        params.add("env", ATTR_NAME + "=" + ATTR_VAL);
        params.add("realm", REALM);

        try {
            decisionsClient
                .queryParams(params)
                .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
                .cookie(cookie)
                .accept("application/json")
                .get(String.class);
            throw new Exception(
                "RESTTest.missingResourceTest: no exception thrown.");
        } catch (UniformInterfaceException e) {
            int errorCode = e.getResponse().getStatus();
            if (errorCode != 400) {
                throw new Exception(
                    "RESTTest.missingResourceTest: incorrect error code");
            }
            String json = e.getResponse().getEntity(String.class);
            JSONObject jo = new JSONObject(json);
            if (jo.optInt("statusCode") != 424) { 
                throw new Exception(
                    "RESTTest.missingResourcesTest() failed, status code not 424");
            }
            if (jo.optJSONObject("body") != null) {
                throw new Exception(
                    "RESTTest.missingResourcesTest() failed, body not empty");
            }
        }
    }

    @Test
    public void missingActionTest() throws Exception {
        Form params = new Form();
        params.add("subject", hashedUserTokenId);
        params.add("resource", RESOURCE_NAME + "/index.html");
        params.add("env", ATTR_NAME + "=" + ATTR_VAL);
        params.add("realm", REALM);

        try {
            decisionClient
                .queryParams(params)
                .header(RestServiceManager.SUBJECT_HEADER_NAME, userTokenIdHeader)
                .cookie(cookie)
                .accept("text/plain")
                .get(String.class);
            throw new Exception(
                "RESTTest.missingActionTest: no exception thrown.");
        } catch (UniformInterfaceException e) {
            int errorCode = e.getResponse().getStatus();
            if (errorCode != 400) {
                throw new Exception(
                    "RESTTest.missingActionTest: incorrect error code");
            }
        }
    }
}
