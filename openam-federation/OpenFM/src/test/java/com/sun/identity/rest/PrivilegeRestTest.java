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
 * $Id: PrivilegeRestTest.java,v 1.5 2009/12/15 00:44:20 veiming Exp $
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */

package com.sun.identity.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.rest.PrivilegeResource;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Hash;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import java.net.URLEncoder;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.ws.rs.core.Cookie;

import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author dennis
 */
public class PrivilegeRestTest {
    private static final String PRIVILEGE_NAME = "PrivilegeRestTestPrivilege";
    private Subject adminSubject = SubjectUtils.createSuperAdminSubject();
    private static final SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(AdminTokenAction.getInstance());

    private static final String RESOURCE_NAME =
        "http://www.PrivilegeRestTest.com";
    private WebResource webClient;
    private String tokenIdHeader;
    private String hashedTokenId;
    private Cookie cookie;

    @BeforeClass
    public void setup() throws Exception {
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
        webClient = Client.create().resource(
            SystemProperties.getServerInstanceName() +
            "/ws/1/entitlement/privilege");
    }

    @AfterClass
    public void cleanup() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance("/",
            adminSubject);
        pm.remove(PRIVILEGE_NAME);
    }

    @Test
    public void negativeTest() throws Exception {
        noJSONStringInPost();
        noJSONStringInPut();
    }

    private void noJSONStringInPost() throws Exception {
        Form form = new Form();
        form.add("subject", hashedTokenId);
        try {
            webClient.header(
                RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
                .cookie(cookie)
                .post(String.class, form);
        } catch (UniformInterfaceException e) {
            validateUniformInterfaceException(e, 9, "noJSONStringInPost");
        }
    }
    
    private void noJSONStringInPut() throws Exception {
        try {
            Form form = new Form();
            webClient
                .path(PRIVILEGE_NAME)
                .queryParam("subject", hashedTokenId)
                .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
                .cookie(cookie)
                .put(String.class, form);
        } catch (UniformInterfaceException e) {
            validateUniformInterfaceException(e, 9, "noJSONStringInPut");
        }
    }

    private void validateUniformInterfaceException(
        UniformInterfaceException e,
        int expectedStatusCode,
        String methodName
    ) throws Exception {
        int errorCode = e.getResponse().getStatus();
        if (errorCode != 400) {
            throw new Exception(
                "PrivilegeRestTest." + methodName + ": incorrect error code");
        }
        String json = e.getResponse().getEntity(String.class);
        JSONObject jo = new JSONObject(json);
        if (jo.optInt("statusCode") != expectedStatusCode) {
            throw new Exception(
                "PrivilegeRestTest." + methodName + ", status code not " +
                expectedStatusCode);
        }
    }

    @Test(dependsOnMethods={"negativeTest"})
    public void search() throws Exception {
        String result = webClient
            .path("/")
            .queryParam("filter",
                Privilege.NAME_ATTRIBUTE + "=" + PRIVILEGE_NAME)
            .queryParam("subject", hashedTokenId)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .get(String.class);
        
        JSONObject jbody = parseResult(result);
        JSONArray array = jbody.getJSONArray(PrivilegeResource.RESULT);
        if ((array == null) || (array.length() == 0)) {
            throw new Exception(
                "PrivilegeRestTest.search failed: cannot get privilege name");
        }

        String privilegeName = (String)array.get(0);
        if (!privilegeName.equals(PRIVILEGE_NAME)) {
            throw new Exception(
                "PrivilegeRestTest.search failed: incorrect privilege name");
        }
    }

    @Test (dependsOnMethods="search")
    public void getAndPut() throws Exception {
        String result = webClient
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
        result = webClient
            .path(PRIVILEGE_NAME)
            .queryParam("subject", hashedTokenId)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .put(String.class, form);
        validateResult(result, 200, "OK"); //OK
    }

    @Test (dependsOnMethods="getAndPut")
    public void getAndDeleteAndAdd()
        throws Exception {
        String result = webClient
            .path(PRIVILEGE_NAME)
            .queryParam("subject", hashedTokenId)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .get(String.class);
        JSONObject jbody = parseResult(result);
        String jsonStr = jbody.getString(PrivilegeResource.RESULT);

        result = webClient.path(PRIVILEGE_NAME)
            .queryParam("subject", hashedTokenId)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .delete(String.class);
        validateResult(result, 200, "OK"); //OK

        Form form = new Form();
        form.add("privilege.json", jsonStr);
        form.add("subject", hashedTokenId);
        result = webClient
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .post(String.class, form);
        validateResult(result, 201, "Created");
    }

    private void validateResult(String result, int code, String msg)
        throws Exception {
        JSONObject jo = new JSONObject(result);
        if (jo.optInt("statusCode") != code) {
            throw new Exception("PrivilegeRestTest.validateResult failed.");
        }
        String message = jo.optString("body");
        if ((message == null) || !message.equals(msg)) {
            throw new Exception(
                "PrivilegeRestTest.validateResult: body element is null");
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
}
