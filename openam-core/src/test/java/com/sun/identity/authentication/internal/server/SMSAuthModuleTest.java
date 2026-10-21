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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.authentication.internal.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.encode.Hash;

/**
 * The advisory rests on the module refusing to authenticate {@code anonymous} even where the
 * installation still carries the shipped hash, so that existing deployments are closed by deploying
 * this version rather than by an upgrade step. The e2e spec runs on a fresh install and no longer
 * stores that hash; without a unit test the refusal branch is exercised nowhere.
 */
public class SMSAuthModuleTest {

    @AfterMethod
    public void resetCaches() throws Exception {
        cache("users").clear();
        cache("userNameToDN").clear();
        setFlag("loadedInternalUsers", false);
        setFlag("initialized", false);
    }

    @Test
    public void shouldRefuseAnonymousWhereTheShippedHashIsStillStored() throws Exception {
        // Pretend the internal user store has been loaded and still carries the value the installer
        // used to ship: Base64(SHA-1("anonymous")).
        setFlag("initialized", true);
        setFlag("loadedInternalUsers", true);
        cache("users").put("anonymous", Hash.hash("anonymous"));
        cache("userNameToDN").put("anonymous", "ou=anonymous,ou=users");
        Map<String, Object> sharedState = new HashMap<>();
        sharedState.put(ISAuthConstants.SHARED_STATE_USERNAME, "anonymous");
        sharedState.put(ISAuthConstants.SHARED_STATE_PASSWORD, "anonymous");

        SMSAuthModule module = new SMSAuthModule();
        module.initialize(null, null, sharedState, null);

        // Without the refusal the stored hash would match and login() would return true.
        assertThat(module.login()).isFalse();
    }

    @Test
    public void shouldRefuseAnonymousWhereTheNameIsGivenAsADN() throws Exception {
        // The DN branch strips the RDN value and then the check has to fire on the value, not on
        // the whole DN.
        setFlag("initialized", true);
        setFlag("loadedInternalUsers", true);
        cache("users").put("anonymous", Hash.hash("anonymous"));
        cache("userNameToDN").put("anonymous", "ou=anonymous,ou=users");
        Map<String, Object> sharedState = new HashMap<>();
        sharedState.put(ISAuthConstants.SHARED_STATE_USERNAME, "ou=anonymous,ou=users");
        sharedState.put(ISAuthConstants.SHARED_STATE_PASSWORD, "anonymous");

        SMSAuthModule module = new SMSAuthModule();
        module.initialize(null, null, sharedState, null);

        assertThat(module.login()).isFalse();
    }

    @Test
    public void shouldNotCacheAUserWithoutAPassword() throws Exception {
        // ConcurrentSkipListMap.put(name, null) throws NullPointerException; the swallowed NPE in
        // loadInternalUsers() would then abandon the loading of every special user not yet read.
        Method addUserToCache = SMSAuthModule.class.getDeclaredMethod("addUserToCache",
                String.class, String.class);
        addUserToCache.setAccessible(true);

        addUserToCache.invoke(null, "anonymous", null);

        assertThat(cache("users")).doesNotContainKey("anonymous");
        assertThat(cache("userNameToDN")).doesNotContainKey("anonymous");
    }

    @Test
    public void shouldNotCacheAUserWithAnEmptyPassword() throws Exception {
        // An empty string is not a valid stored hash either.
        Method addUserToCache = SMSAuthModule.class.getDeclaredMethod("addUserToCache",
                String.class, String.class);
        addUserToCache.setAccessible(true);

        addUserToCache.invoke(null, "someone", "");

        assertThat(cache("users")).doesNotContainKey("someone");
    }

    @Test
    public void shouldCacheAUserWithAStoredPassword() throws Exception {
        // The guard above must skip only the users that cannot authenticate: one that returned early for every
        // user would leave amAdmin and dsameuser out of the cache, and the tests above would stay green.
        Method addUserToCache = SMSAuthModule.class.getDeclaredMethod("addUserToCache",
                String.class, String.class);
        addUserToCache.setAccessible(true);

        addUserToCache.invoke(null, "uid=someone,ou=people,dc=openam", "c29tZWhhc2g=");

        assertThat(cache("users")).containsEntry("someone", "c29tZWhhc2g=");
        assertThat(cache("userNameToDN")).containsEntry("someone", "uid=someone,ou=people,dc=openam");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> cache(String name) throws Exception {
        Field field = SMSAuthModule.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }

    private static void setFlag(String name, boolean value) throws Exception {
        Field field = SMSAuthModule.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(null, value);
    }
}
