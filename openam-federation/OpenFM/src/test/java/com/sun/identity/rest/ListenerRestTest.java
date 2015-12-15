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
 * $Id: ListenerRestTest.java,v 1.4 2009/12/15 00:44:19 veiming Exp $
 *
 * Portions copyright 2014-2015 ForgeRock AS.
 */

package com.sun.identity.rest;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementListener;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.ListenerManager;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.AuthUtils;
import com.sun.identity.entitlement.util.IdRepoUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.unittest.UnittestLog;
import com.sun.identity.shared.encode.Hash;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.ws.rs.core.Cookie;

import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * To Test Listener Test interface
 */
public class ListenerRestTest {
    private static final String REALM = "/";
    private static final String AGENT_NAME = "ListenerRestTestAgent";
    private static final String NOTIFICATION_URL =
        "http://www.listenerresttest.com/notification";
    private static String ENC_NOTIFICATION_URL = null;
    private static final SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(AdminTokenAction.getInstance());

    private static final String PRIVILEGE_NAME = "ListenerRestTestPrivilege";
    private Subject adminSubject = SubjectUtils.createSuperAdminSubject();
    private static final String RESOURCE_NAME = "http://www.listenerresttest.com";
    private WebResource listenerClient;
    private String hashedTokenId;
    private String tokenIdHeader;
    private Cookie cookie;
    private AMIdentity agent;

    @BeforeClass
    public void setup() throws Exception {
        try {
            agent = IdRepoUtils.createAgent(REALM, AGENT_NAME);
            SSOToken ssoToken = AuthUtils.authenticate(REALM, AGENT_NAME,
                AGENT_NAME);
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
            pm.add(privilege);

            listenerClient = Client.create().resource(
                SystemProperties.getServerInstanceName() +
                "/ws/1/entitlement/listener");

            ENC_NOTIFICATION_URL = ESAPI.encoder().encodeForURL(NOTIFICATION_URL);
        } catch (Exception e) {
            UnittestLog.logError("ListenerRestTest.setup() failed:", e);
            throw e;
        }
    }

    @AfterClass
    public void cleanup() throws Exception {
        PrivilegeManager pm = PrivilegeManager.getInstance(REALM,
            adminSubject);
        pm.remove(PRIVILEGE_NAME);
        IdRepoUtils.deleteIdentity(REALM, agent);
    }


    @Test
    public void negativeTest() throws Exception {
        noURLInPost();
        noURLInGet();
        noURLInDelete();
    }

    private void noURLInPost() throws Exception {
        Form form = new Form();
        form.add("resources", RESOURCE_NAME + "/*");
        form.add("subject", hashedTokenId);
        try {
            listenerClient
                .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
                .cookie(cookie)
                .post(String.class, form);
        } catch (UniformInterfaceException e) {
            validateUniformInterfaceException(e, 426, "noURLInPost");
        }
    }

    private void noURLInGet() throws Exception {
        try {
            getListener("");
        } catch (UniformInterfaceException e) {
            int errorCode = e.getResponse().getStatus();
            if (errorCode != 405) {
                throw new Exception(
                    "ListenerRestTest.noURLInGet: incorrect error code");
            }
        }
    }

    private void noURLInDelete() throws Exception {
        try {
           listenerClient.path("")
               .queryParam("subject", hashedTokenId)
               .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
               .cookie(cookie).delete(String.class);
        } catch (UniformInterfaceException e) {
            int errorCode = e.getResponse().getStatus();
            if (errorCode != 405) {
                throw new Exception(
                    "ListenerRestTest.noURLInDelete: incorrect error code");
            }
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
                "ListenerRestTest." + methodName + ": incorrect error code");
        }
        String json = e.getResponse().getEntity(String.class);
        JSONObject jo = new JSONObject(json);
        if (jo.optInt("statusCode") != expectedStatusCode) {
            throw new Exception(
                "ListenerRestTest." + methodName + ", status code not " +
                expectedStatusCode);
        }
    }


    @Test(dependsOnMethods={"negativeTest"})
    public void test() throws Exception {
        Form form = new Form();
        form.add("resources", RESOURCE_NAME + "/*");
        form.add("subject", hashedTokenId);
        form.add("url", NOTIFICATION_URL);
        String result = listenerClient
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .post(String.class, form);
        JSONObject jo = new JSONObject(result);
        if (!jo.getString("statusCode").equals(IdRepoErrorCode.ILLEGAL_ARGUMENTS)) {
            throw new Exception("ListenerRESTTest.test failed to add");
        }

        Set<EntitlementListener> listeners =
            ListenerManager.getInstance().getListeners(adminSubject);

        if ((listeners == null) || listeners.isEmpty()) {
            throw new Exception("ListenerTestTest.test: no listeners");
        }

        try {
            Set<String> res = new HashSet<String>();
            res.add(RESOURCE_NAME + "/*");
            EntitlementListener listener = new EntitlementListener(
                new URL(NOTIFICATION_URL),
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, res);

            boolean found = false;
            for (EntitlementListener l : listeners) {
                if (l.equals(listener)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new Exception("ListenerTestTest.test: listener not found");
            }
        } catch (MalformedURLException e) {
            //ignore
        }
    }

    @Test(dependsOnMethods = {"test"})
    public void testAddMoreResources() throws Exception {
        Form form = new Form();
        form.add("resources", RESOURCE_NAME + "/a/*");
        form.add("subject", hashedTokenId);
        form.add("url", NOTIFICATION_URL);
        String result = listenerClient
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .post(String.class, form);
        JSONObject jo = new JSONObject(result);
        if (!jo.getString("statusCode").equals(IdRepoErrorCode.ILLEGAL_ARGUMENTS)) {
            throw new Exception(
                "ListenerRESTTest.testAddMoreResources failed to add");
        }

        Set<EntitlementListener> listeners =
            ListenerManager.getInstance().getListeners(adminSubject);

        if ((listeners == null) || listeners.isEmpty()) {
            throw new Exception(
                "ListenerTestTest.testAddMoreResources: no listeners");
        }

        try {
            Set<String> res = new HashSet<String>();
            res.add(RESOURCE_NAME + "/*");
            res.add(RESOURCE_NAME + "/a/*");
            EntitlementListener listener = new EntitlementListener(
                new URL(NOTIFICATION_URL),
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, res);

            boolean found = false;
            for (EntitlementListener l : listeners) {
                if (l.equals(listener)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new Exception(
                    "ListenerTestTest.testAddMoreResources: listener not found");
            }
        } catch (MalformedURLException e) {
            //ignore
        }
    }

    @Test(dependsOnMethods = {"testAddMoreResources"})
    public void testAddDifferentApp() throws Exception {
        Form form = new Form();
        form.add("application", "sunBank");
        form.add("subject", hashedTokenId);
        form.add("url", NOTIFICATION_URL);
        String result = listenerClient
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .post(String.class, form);
        JSONObject jo = new JSONObject(result);
        if (!jo.getString("statusCode").equals(IdRepoErrorCode.ILLEGAL_ARGUMENTS)) {
            throw new Exception(
                "ListenerRESTTest.testAddDifferentApp failed to add");
        }

        Set<EntitlementListener> listeners =
            ListenerManager.getInstance().getListeners(adminSubject);

        if ((listeners == null) || listeners.isEmpty()) {
            throw new Exception(
                "ListenerTestTest.testAddDifferentApp: no listeners");
        }
    }

    @Test(dependsOnMethods = {"testAddDifferentApp"})
    public void testGetListener() throws Exception {

        try {
            String result = getListener(NOTIFICATION_URL);
            JSONObject jo = new JSONObject(result);
            if (jo.optInt("statusCode") != 200) {
                throw new Exception("ListenerRESTTest.postDecisionsTest() failed,"
                        + " status code not 200");
            }
            JSONObject jbody = jo.optJSONObject("body");
            if (jbody == null) {
                throw new Exception("ListenerRESTTest.postDecisionsTest() failed,"
                        + " body element is null");
            }
        
            EntitlementListener retrieved = new EntitlementListener(jbody);

            Set<String> res = new HashSet<String>();
            res.add(RESOURCE_NAME + "/*");
            res.add(RESOURCE_NAME + "/a/*");
            EntitlementListener listener = new EntitlementListener(
                new URL(NOTIFICATION_URL),
                ApplicationTypeManager.URL_APPLICATION_TYPE_NAME, res);
            Map<String, Set<String>> mapAppToRes = listener.getMapAppToRes();
            mapAppToRes.put("sunBank", new HashSet());

            if (!listener.equals(retrieved)) {
                throw new Exception(
                    "ListenerTestTest.testGetListener: listener not found");
            }
        } catch (MalformedURLException e) {
            //ignore
        }
    }

    private String getListener(String url)
        throws UnsupportedEncodingException, EncodingException {
        String adminTokenId = adminToken.getTokenID().toString();
        String adminHashedTokenId = Hash.hash(adminTokenId);
        String adminTokenIdHeader = RestServiceManager.SSOTOKEN_SUBJECT_PREFIX +
            RestServiceManager.SUBJECT_DELIMITER + adminTokenId;
        String cookieValue = adminTokenId;

        if (Boolean.parseBoolean(
            SystemProperties.get(Constants.AM_COOKIE_ENCODE, "false"))) {
            cookieValue = URLEncoder.encode(adminTokenId, "UTF-8");
        }

        cookie = new Cookie(SystemProperties.get(Constants.AM_COOKIE_NAME),
            cookieValue);
        String encodedURL = ESAPI.encoder().encodeForURL(url);
        String result = listenerClient.path(encodedURL)
            .queryParam("subject", adminHashedTokenId)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, adminTokenIdHeader)
            .cookie(cookie)
            .get(String.class);
        return result;
    }


    @Test(dependsOnMethods = {"testAddDifferentApp"})
    public void testRemove() throws Exception {
        String result = listenerClient
            .path(ENC_NOTIFICATION_URL)
            .queryParam("subject", hashedTokenId)
            .header(RestServiceManager.SUBJECT_HEADER_NAME, tokenIdHeader)
            .cookie(cookie)
            .delete(String.class);
        JSONObject jo = new JSONObject(result);
        if (!jo.getString("statusCode").equals("200")) {
            throw new Exception(
                "ListenerRESTTest.testRemove failed to remove");
        }

        Set<EntitlementListener> listeners =
            ListenerManager.getInstance().getListeners(adminSubject);

        if ((listeners != null) && !listeners.isEmpty()) {
            throw new Exception(
                "ListenerTestTest.testRemove: no listeners");
        }
    }
}
