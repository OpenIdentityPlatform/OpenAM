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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn;

import com.sun.identity.authentication.AuthContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A persistent store for AuthContext instance that are being used in multi-step authentication requests.
 *
 * AuthContexts can be stored in an instance of the class, when the next requirements of the login process are sent
 * to the client and then retrieve on response from the client.
 *
 * The AuthContext map should regularly cleared of old authentication requests which are no longer valid.
 */
@Singleton
public class AuthContextStateMap {

    private Map<String, AuthContext> authContexts = new ConcurrentHashMap<String, AuthContext>();

    /**
     * Singleton approach by using a static inner class.
     */
    private static final class SingletonHolder {
        private static final AuthContextStateMap INSTANCE = new AuthContextStateMap();
    }

    /**
     * Private constructor to ensure AuthContextStateMap remains a Singleton.
     */
    @Inject
    private AuthContextStateMap() {
    }

    /**
     * Gets the AuthContextStateMap instance.
     *
     * @return The AuthContextStateMap singleton instance.
     */
    public static AuthContextStateMap getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Adds the AuthContext of a login request to the map keyed by the authId.
     *
     * The authId must be unique identifier which will not be re-used for keying another AuthContext instance.
     *
     * @param authId The identifier which is to be used to store the AuthContext for the login request.
     * @param authContext The AuthContext instance for the login process.
     */
    public void addAuthContext(String authId, AuthContext authContext) {
        authContexts.put(authId, authContext);
    }

    /**
     * Retrieves the AuthContext for the login request for the given authId.
     *
     * @param authId The identifier which was used to store the AuthContext for the login request.
     * @return The AuthContext.
     */
    public AuthContext getAuthContext(String authId) {
        return authContexts.get(authId);
    }
}
