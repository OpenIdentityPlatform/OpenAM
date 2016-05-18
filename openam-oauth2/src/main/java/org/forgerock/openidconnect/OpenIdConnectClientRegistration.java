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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openidconnect;

import java.net.URI;
import java.security.PublicKey;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Models an OpenId Connect client registration in the OAuth2 provider.
 *
 * @since 12.0.0
 */
public interface OpenIdConnectClientRegistration extends ClientRegistration {

    /**
     * Gets the OpenId Token signed response algorithm.
     *
     * @return The OpenId token signed response algorithm.
     */
    String getIDTokenSignedResponseAlgorithm();

    /**
     * Determines if ID token encryption is enabled.
     *
     * @return {@code true} if ID token encryption is enabled.
     */
    boolean isIDTokenEncryptionEnabled();

    /**
     * Gets the algorithm used to encrypt OpenID Connect tokens.
     *
     * @return The OpenID Connect token encryption algorithm.
     */
    String getIDTokenEncryptionResponseAlgorithm();

    /**
     * Gets the encryption method used to encrypt OpenID Connect tokens.
     *
     * @return The OpenID Connect token encryption method.
     */
    String getIDTokenEncryptionResponseMethod();

    /**
     * Gets the encryption public key used to encrypt OpenID Connect tokens.
     *
     * @return The OpenID Connect token encryption public key.
     */
    PublicKey getIDTokenEncryptionPublicKey();

    /**
     * Gets the token_endpoint_auth_method configured for this client.
     */
    String getTokenEndpointAuthMethod();

    /**
     * Gets the subject identifier uri.
     */
    URI getSectorIdentifierUri();

    /**
     * Retrieve the sub value, appropriate for the client subject type, or null
     * if there are issues with its formation.
     */
    String getSubValue(String id, OAuth2ProviderSettings providerSettings);

    /**
     * Gets the authorization code life time in milliseconds.
     * 
     * @param providerSettings An instance of the OAuth2ProviderSettings.
     * @return The authorization code life time in milliseconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getAuthorizationCodeLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException;
         
    /**
     * Gets the access token life time in milliseconds.
     * 
     * @param providerSettings An instance of the OAuth2ProviderSettings.
     * @return The access token life time in milliseconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getAccessTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException;

    /**
     * Gets the refresh token life time in milliseconds.
     * 
     * @param providerSettings An instance of the OAuth2ProviderSettings.
     * @return The refresh token life time in milliseconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getRefreshTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException;

    /**
     * Gets the JWT token life time in milliseconds.
     * 
     * @param providerSettings An instance of the OAuth2ProviderSettings.
     * @return The JWT token life time in milliseconds.
     * @throws ServerException If any internal server error occurs.
     */
    long getJwtTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException;
}
