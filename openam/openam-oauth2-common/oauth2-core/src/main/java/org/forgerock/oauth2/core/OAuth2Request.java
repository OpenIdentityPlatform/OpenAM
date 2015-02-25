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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.forgerock.guava.common.collect.ClassToInstanceMap;
import org.forgerock.guava.common.collect.MutableClassToInstanceMap;
import org.forgerock.json.fluent.JsonValue;

import java.util.Locale;

/**
 * An abstraction of the actual request so as to allow the core of the OAuth2 provider to be agnostic of the library
 * used to translate the HTTP request.
 *
 * @since 12.0.0
 */
public abstract class OAuth2Request {

    private final ClassToInstanceMap<Token> tokens = MutableClassToInstanceMap.create();

    /**
     * Gets the actual underlying request.
     *
     * @param <T> The type of the underlying request.
     * @return The underlying request.
     */
    public abstract <T> T getRequest();

    /**
     * Gets the specified parameter from the request.
     * <br/>
     * It is up to the implementation to determine how and where it gets the parameter from on the request, i.e.
     * query parameters, request attributes, etc.
     *
     * @param name The name of the parameter.
     * @param <T> The type of the parameter.
     * @return The parameter value.
     */
    public abstract <T> T getParameter(String name);

    /**
     * Gets the body of the request.
     * <br/>
     * Note: reading of the body maybe a one time only operation, so the implementation needs to cache the content
     * of the body so multiple calls to this method do not behave differently.
     * <br/>
     * This method should only ever be called for access and refresh token request and requests to the userinfo and
     * tokeninfo endpoints.
     *
     * @return The body of the request.
     */
    public abstract JsonValue getBody();

    /**
     * Set a Token that is in play for this request.
     * @param tokenClass The token type.
     * @param token The token instance.
     * @param <T> The type of token.
     */
    public <T extends Token> void setToken(Class<T> tokenClass, T token) {
        tokens.putInstance(tokenClass, token);
    }

    /**
     * Set a Token that is in play for this request.
     * @param tokenClass The token type.
     * @param <T> The type of token.
     * @return The token instance.
     */
    public <T extends Token> T getToken(Class<T> tokenClass) {
        return tokens.getInstance(tokenClass);
    }

    /**
     * Get the request locale.
     * @return The Locale object.
     */
    public abstract Locale getLocale();
}
