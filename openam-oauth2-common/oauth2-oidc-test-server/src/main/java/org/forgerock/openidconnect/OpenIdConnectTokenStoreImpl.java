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

package org.forgerock.openidconnect;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.oauth2.TokenStoreImpl;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.util.encode.Base64url;
import org.restlet.Request;

/**
 * @since 12.0.0
 */
@Singleton
public class OpenIdConnectTokenStoreImpl extends TokenStoreImpl implements OpenIdConnectTokenStore {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;

    @Inject
    public OpenIdConnectTokenStoreImpl(final OAuth2ProviderSettingsFactory providerSettingsFactory,
            final OpenIdConnectClientRegistrationStore clientRegistrationStore) {
        super(providerSettingsFactory);
        this.providerSettingsFactory = providerSettingsFactory;
        this.clientRegistrationStore = clientRegistrationStore;
    }

    public OpenIdConnectToken createOpenIDToken(String resourceOwnerId, String clientId,
                                                String authorizationParty, String nonce, String ops,
                                                OAuth2Request request) throws ServerException, InvalidClientException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final OpenIdConnectClientRegistration clientRegistration = clientRegistrationStore.get(clientId, request);
        final String algorithm = clientRegistration.getIDTokenSignedResponseAlgorithm();
        final byte[] clientSecret = clientRegistration.getClientSecret().getBytes(Utils.CHARSET);
        final KeyPair keyPair = providerSettings.getServerKeyPair();
        final long timeInSeconds = System.currentTimeMillis()/1000;
        final long tokenLifetime = providerSettings.getOpenIdTokenLifetime();
        final long exp = timeInSeconds + tokenLifetime;

        final Request req = request.getRequest();
        final String iss = req.getHostRef().toString() + "/" + req.getResourceRef().getSegments().get(0);

        final String atHash = generateAtHash(algorithm, request, providerSettings);
        final String cHash = generateCHash(algorithm, request, providerSettings);

        //todo - if necessary, match test impl. with OpenAMTokenStore.java
        final String acr = null;
        final List<String> amr = null;
        String kid = null;
        JsonValue jwks = providerSettings.getJWKSet().get("keys");
        if (!jwks.isNull() && !jwks.asList().isEmpty()) {
            kid = jwks.get(0).get("kid").asString();
        }

        return new OpenIdConnectToken(kid, clientSecret, keyPair, algorithm, iss, resourceOwnerId, clientId,
                authorizationParty, exp, timeInSeconds, timeInSeconds, nonce, ops, atHash, cHash, acr, amr);
    }

    /**
     * For at_hash values, used when token and id_token exist in scope.
     */
    private String generateAtHash(String algorithm, OAuth2Request request,
                                  OAuth2ProviderSettings providerSettings) throws ServerException {

        final AccessToken accessToken = request.getToken(AccessToken.class);

        if (accessToken == null) {
            return null;
        }

        final String accessTokenValue = ((String) accessToken.getTokenInfo().get("access_token"));

        return generateHash(algorithm, accessTokenValue, providerSettings);

    }

    /**
     * For c_hash, used when code and id_token exist in scope.
     */
    private String generateCHash(String algorithm, OAuth2Request request,
                                 OAuth2ProviderSettings providerSettings) throws ServerException {

        final AuthorizationCode authorizationCode = request.getToken(AuthorizationCode.class);

        if (authorizationCode == null) {
            return null;
        }

        final String codeValue = authorizationCode.getTokenId();

        return generateHash(algorithm, codeValue, providerSettings);
    }

    /**
     * Generates hash values, by hashing the valueToEncode using the requests's "alg"
     * parameter, then returning the base64url encoding of the
     * leftmost half of the returned bytes. Used for both at_hash and c_hash claims.
     */
    private String generateHash(String algorithm, String valueToEncode, OAuth2ProviderSettings providerSettings)
            throws ServerException {

        if (!providerSettings.getSupportedIDTokenSigningAlgorithms().contains(algorithm)) {
            return null;
        }

        final JwsAlgorithm alg = JwsAlgorithm.valueOf(algorithm);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(alg.getMdAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new ServerException("Algorithm not supported.");
        }

        final byte[] result = digest.digest(valueToEncode.getBytes(Utils.CHARSET));
        final byte[] toEncode = Arrays.copyOfRange(result, 0, result.length / 2);

        return Base64url.encode(toEncode);
    }


}
