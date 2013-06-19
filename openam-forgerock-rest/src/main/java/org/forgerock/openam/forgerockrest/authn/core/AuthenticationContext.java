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

package org.forgerock.openam.forgerockrest.authn.core;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.spi.AuthLoginException;

import javax.security.auth.callback.Callback;
import java.util.Map;
import java.util.Set;

/**
 * Interface for all Authentication Contexts.
 */
public interface AuthenticationContext {

    /**
     * Returns the wrapped AuthContextLocal.
     *
     * @return The wrapped AuthContextLocal.
     */
    AuthContextLocal getAuthContext();

    /**
     * Starts the login process for the given AuthContextLocal object.
     *
     * @throws com.sun.identity.authentication.spi.AuthLoginException If a problem occurred during login.
     */
    void login() throws AuthLoginException;

    /**
     * Starts the login process for the given AuthContextLocal object.
     *
     * @param indexType The authentication index type.
     * @param indexValue The authentication index value.
     * @throws com.sun.identity.authentication.spi.AuthLoginException If a problem occurred during login.
     */
    void login(AuthContext.IndexType indexType, String indexValue) throws AuthLoginException;

    /**
     * Starts the login process for the given AuthContextLocal object.
     *
     * @param indexType The authentication index type.
     * @param indexValue The authentication index value.
     * @param pCookieMode True if persistent cookie exists, otherwise false.
     * @param envMap The environment map, only applicable when the authentication index type is RESOURCE.
     * @param locale The locale to use in the authentication process.
     * @throws com.sun.identity.authentication.spi.AuthLoginException If a problem occurred during login.
     */
    void login(AuthContext.IndexType indexType, String indexValue, boolean pCookieMode, Map<String, Set<String>> envMap,
               String locale) throws AuthLoginException;

    /**
     * Checks if the login process requires more information from the user to complete the authentication.
     *
     * @return True if more credentials are required from the user, otherwise false.
     */
    boolean hasMoreRequirements();

    /**
     * Returns an array of Callbacks that must be populated by the user and returned back.
     * These objects are requested by the authentication plug-ins, and these are usually displayed to the user.
     * The user then provides the requested information for it to be authenticated.
     *
     * @return An array of Callbacks requesting credentials from user.
     */
    Callback[] getRequirements();

    /**
     * Returns an array of Callbacks that must be populated by the user and returned back.
     * These objects are requested by the authentication plug-ins, and these are usually displayed to the user.
     * The user then provides the requested information for it to be authenticated.
     *
     * @param noFilter Whether to filter out PagePropertiesCallbacks from the array of Callbacks or not.
     * @return An array of Callbacks requesting credentials from user.
     */
    Callback[] getRequirements(boolean noFilter);

    /**
     * Submit the populated Callback array to the authentication plug-in modules.
     *
     * @param callbacks An array of Callbacks.
     */
    void submitRequirements(Callback[] callbacks);

    /**
     * Returns the current authentication index type from the AuthContextLocal.
     *
     * @return The current authentication index type.
     */
    AuthIndexType getIndexType();

    /**
     * Returns the current status of the authentication process.
     *
     * @return The current status of the authentication process.
     */
    AuthContext.Status getStatus();

    /**
     * Returns the Organization DN.
     *
     * @return The Organization DN.
     */
    String getOrgDN();

    /**
     * Returns the SSO Token for the authenticated user.
     *
     * @return The SSO Token.
     */
    SSOToken getSSOToken();

    /**
     * Returns the error code for the login process.
     *
     * @return The error code.
     */
    String getErrorCode();

    /**
     * Returns the error message for the login process.
     *
     * @return The error message.
     */
    String getErrorMessage();

    /**
     * Returns the Session ID for the login process.
     *
     * @return The Session ID.
     */
    SessionID getSessionID();

    /**
     * Sets the organisation DN.
     *
     * @param orgDN The organisation DN.
     */
    void setOrgDN(String orgDN);
}
