/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock. All rights reserved.
 */

package org.forgerock.openam.ext.cts;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.ext.cts.repo.MockTokenRepo;
import org.forgerock.json.fluent.JsonValue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jonathan Scudder
 */
public class CoreTokenServiceTest {

    final static Logger LOGGER = Logger.getLogger(CoreTokenService.class.toString());
    
    private static CoreTokenService cts = null;
    private static String testUUID = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        cts = new CoreTokenService(new MockTokenRepo());
        testUUID = UUID.randomUUID().toString();
        LOGGER.log(Level.INFO, "Created testUUID: " + testUUID);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {

    }

    @Test(groups = "create")
    public void testCreate() throws Exception {
        Set<String> scopes = new HashSet<String>();
        scopes.add("read");
        scopes.add("write");

        JsonValue testAuthzCode = new JsonValue(new HashMap<String, Object>());
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.ID, testUUID);
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, "authorization_code");
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.SCOPE, scopes);
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.REALM, "/");
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.EXPIRE_TIME, System.currentTimeMillis() + 1000 * 60 * 10); // Current time + 10 minutes

        JsonValue request = new JsonValue(new HashMap<String, Object>());
        request.put("id", "/token/oauth2/authorization_code");
        request.put("method", "create");
        request.put("value", testAuthzCode);

        LOGGER.log(Level.INFO, "Running creation test with object: " + request.toString());
        
        cts.create(request);
    }

    @Test(groups = "needsExisting", dependsOnGroups = "create")
    public void testRead() throws Exception {
        JsonValue request = new JsonValue(new HashMap<String, Object>());
        request.put("id", testUUID);
        request.put("method", "read");

        LOGGER.log(Level.INFO, "Running read test with object: " + request.toString());

        JsonValue response = cts.read(request);

        LOGGER.log(Level.INFO, "Response: " + response.toString());

        assert response != null;
        assert response.get(OAuth2Constants.CoreTokenParams.ID).asString().equals(testUUID);
        assert response.get(OAuth2Constants.CoreTokenParams.TOKEN_TYPE).asString().equals("authorization_code");
        assert response.get(OAuth2Constants.CoreTokenParams.REALM).asString().equals("/");

        if (response == null) {
            LOGGER.log(Level.INFO, "Returned with null");
        } else {
            LOGGER.log(Level.INFO, "Returned with object: " + response.toString());
        }
    }

    @Test(groups = "needsExisting", dependsOnGroups = "create")
    public void testUpdate() throws Exception {
        Set<String> scopes = new HashSet<String>();
        scopes.add("read");
        
        String testmod = "/testrealm";

        JsonValue testAuthzCode = new JsonValue(new HashMap<String, Object>());
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.ID, testUUID);
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, "authorization_code");
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.SCOPE, scopes);
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.REALM, testmod);
        testAuthzCode.put(OAuth2Constants.CoreTokenParams.EXPIRE_TIME, System.currentTimeMillis() + 1000 * 60 * 10); // Current time + 10 minutes

        JsonValue request = new JsonValue(new HashMap<String, Object>());
        request.put("id", "/token/oauth2/authorization_code");
        request.put("method", "update");
        request.put("value", testAuthzCode);

        LOGGER.log(Level.INFO, "Running update test with object: " + request.toString());

        JsonValue response = cts.update(request);

        JsonValue request2 = new JsonValue(new HashMap<String, Object>());
        request2.put("id", testUUID);
        request2.put("method", "read");

        LOGGER.log(Level.INFO, "Running post-update read test with object: " + request2.toString());

        JsonValue response2 = cts.read(request2);

        assert response2 != null;
        LOGGER.log(Level.INFO, "Returned with object: " + response2.toString());
        
        // Check that the realm has been updated
        assert response2.get(OAuth2Constants.CoreTokenParams.REALM).asString() == testmod;


    }

    @Test(dependsOnGroups = "needsExisting")
    public void testDelete() throws Exception {
        JsonValue request = new JsonValue(new HashMap<String, Object>());
        request.put("id", testUUID);
        request.put("method", "delete");

        JsonValue response = cts.update(request);
        assert response != null;
        LOGGER.log(Level.INFO, "Received confirmation of deletion: " + response.toString());

        JsonValue request2 = new JsonValue(new HashMap<String, Object>());
        request2.put("id", testUUID);
        request2.put("method", "read");

        LOGGER.log(Level.INFO, "Running post-update read test with object: " + request2.toString());

        JsonValue response2 = cts.read(request2);

        assert response2 == null;
        LOGGER.log(Level.INFO, "Object has been deleted");
    }

    // TODO: PAQ tests

}
