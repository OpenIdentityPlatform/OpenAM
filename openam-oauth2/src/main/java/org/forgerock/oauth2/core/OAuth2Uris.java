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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.oauth2.core;

import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Models all of the possible settings the OAuth2 provider can have and that can be configured.
 *
 * <p>The actual implementation is responsible for providing the method in which these settings
 * are configured. This interface only describes the API for other code to get the OAuth2 provider
 * settings.</p>
 *
 * @since 13.0.0
 */
public interface OAuth2Uris {

    /**
     * Gets the identifier of this issuer.
     *
     * @return The issuer.
     */
    String getIssuer() throws ServerException;

    /**
     * Gets the URI for the OAuth2 authorize endpoint.
     *
     * @return The OAuth2 authorize endpoint.
     */
    String getAuthorizationEndpoint();

    /**
     * Gets the URI for the OAuth2 token endpoint.
     *
     * @return The OAuth2 token endpoint.
     */
    String getTokenEndpoint();

    /**
     * Gets the URI for the OpenID Connect user info endpoint.
     *
     * @return The OpenID Connect user info endpoint.
     */
    String getUserInfoEndpoint();

    /**
     * Gets the URI for the OpenID Connect check session endpoint.
     *
     * @return The OpenID Connect check session endpoint.
     */
    String getCheckSessionEndpoint();

    /**
     * Gets the URI for the OpenID Connect end session endpoint.
     *
     * @return The OpenID Connect end session endpoint.
     */
    String getEndSessionEndpoint();

    /**
     * Gets the JSON Web Key Set URI.
     *
     * @return The JWKS URI.
     * @throws ServerException If any internal server error occurs.
     */
    String getJWKSUri() throws ServerException;

    /**
     * Gets the OpenID Connect client registration endpoint.
     *
     * @return The OpenID Connect client registration endpoint.
     */
    String getClientRegistrationEndpoint();

    /**
     * Returns the default URL for this provider's token introspection endpoint.
     * @return The URL.
     */
    String getIntrospectionEndpoint();

    /**
     * Returns the default URL for this provider's Resource Set Registration policy endpoint.
     *
     * @return The URL.
     */
    String getResourceSetRegistrationPolicyEndpoint(String resourceSetId);

    /**
     * Returns the default URL for this provider's Resource Set Registration endpoint.
     *
     * @return The URL.
     */
    String getResourceSetRegistrationEndpoint();

}
