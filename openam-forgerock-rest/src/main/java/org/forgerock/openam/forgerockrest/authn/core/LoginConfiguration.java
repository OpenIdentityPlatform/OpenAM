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

import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.service.LoginState;

import javax.servlet.http.HttpServletRequest;

/**
 * Contains all the configuration required to start or continue a login process.
 */
public class LoginConfiguration {

    private HttpServletRequest httpRequest;
    private AuthIndexType indexType;
    private String indexValue;
    private String sessionId = "";
    private String ssoTokenId = "";

    /**
     * Sets the HttpServletRequest which initiated the login process.
     *
     * @param httpRequest The HttpServletRequest.
     * @return This LoginConfiguration object.
     */
    public LoginConfiguration httpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
        return this;
    }

    /**
     * Returns the HttpServletRequest which initiated the login process.
     *
     * @return The HttpServletRequest.
     */
    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    /**
     * Sets the Authentication Index Type to use in the login process.
     *
     * @param indexType The Authentication Index Type
     * @return This LoginConfiguration object.
     */
    public LoginConfiguration indexType(AuthIndexType indexType) {
        this.indexType = indexType;
        return this;
    }

    /**
     * Returns the Authentication Index Type to use in the login process.
     *
     * @return The Authentication Index Type
     */
    public AuthIndexType getIndexType() {
        return indexType;
    }

    /**
     * Sets the Authentication Index value to use in the login process.
     * <p>
     * Also checks to see if the AuthIndexType is COMPOSITE and if it is will add another parameter to the request
     * with the same indexValue but using the legacy key "sunamcompositeadvice". This then allows composite
     * advices to work correctly.
     *
     * @param indexValue The Authentication Index value.
     * @return This LoginConfiguration object.
     */
    public LoginConfiguration indexValue(String indexValue) {
        switch (indexType) {
        case COMPOSITE: {
            httpRequest = new RestAuthHttpRequestWrapper(httpRequest);
            ((RestAuthHttpRequestWrapper) httpRequest).addParameter(AuthClientUtils.COMPOSITE_ADVICE, indexValue);
        }
        }
        this.indexValue = indexValue;
        return this;
    }

    /**
     * Returns the Authentication Index value to use in the login process.
     *
     * @return The Authentication Index value.
     */
    public String getIndexValue() {
        return indexValue;
    }

    /**
     * Sets the session id of the current login process.
     *
     * Must not be set on the start of a login process, i.e. the first authenticate request, and on subsequent requests
     * must be set with the session id returned from the previous authentication request.
     *
     * Must NOT be used for a user's current session, i.e. for session upgrade authentication requests.
     *
     * @param sessionId The session id of the current login process.
     * @return This LoginConfiguration object.
     */
    public LoginConfiguration sessionId(String sessionId) {
        if (sessionId != null) {
            this.sessionId = sessionId;
        }
        return this;
    }

    /**
     * Returns the session id for the current login process.
     *
     * Must not be set on the start of a login process, i.e. the first authenticate request, and on subsequent requests
     * must be set with the session id returned from the previous authentication request.
     *
     * @return The session id of the current login process.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the user's current session SSO Token Id to use in session upgrade.
     *
     * @param ssoTokenId The SSO Token Id of the user's current session.
     * @return This LoginConfiguration object.
     */
    public LoginConfiguration sessionUpgrade(String ssoTokenId) {
        if (ssoTokenId != null) {
            this.ssoTokenId = ssoTokenId;
        }
        return this;
    }

    /**
     * Determines if the login request is requesting the user's current session to be upgraded.
     *
     * @return If the SSO Token Id of the user's current session is provided and not null or empter string then true is
     *          returned, otherwise false.
     */
    public boolean isSessionUpgradeRequest() {
        return !"".equals(ssoTokenId);
    }

    /**
     * Returns the SSO Token Id used to configure the login process, of the user's current session.
     *
     * @return The SSO Token Id of the user's current session.
     */
    public String getSSOTokenId() {
        return ssoTokenId;
    }
}
