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

import com.google.inject.Singleton;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.forgerockrest.authn.core.AuthenticationContext;
import org.forgerock.openam.forgerockrest.authn.core.LoginConfiguration;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.AMKeyProvider;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to create and verify authentication JWTs.
 */
@Singleton
public class AuthIdHelper {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String AUTH_SERVICE_NAME = "iPlanetAMAuthService";
    private static final String KEY_ALIAS_KEY = "iplanet-am-auth-key-alias";

    private final CoreServicesWrapper coreServicesWrapper;
    private final AMKeyProvider amKeyProvider;
    private final JwtBuilderFactory jwtBuilderFactory;

    /**
     * Constructs an instance of the AuthIdHelper.
     *
     * @param coreServicesWrapper An instance of the CoreServicesWrapper.
     * @param amKeyProvider An instance of the AMKeyProvider.
     * @param jwtBuilderFactory An instance of the JwtBuilderFactory.
     */
    @Inject
    public AuthIdHelper(CoreServicesWrapper coreServicesWrapper, AMKeyProvider amKeyProvider,
            JwtBuilderFactory jwtBuilderFactory) {
        this.coreServicesWrapper = coreServicesWrapper;
        this.amKeyProvider = amKeyProvider;
        this.jwtBuilderFactory = jwtBuilderFactory;
    }

    /**
     * Creates a JWT authentication id.
     *
     * @param loginConfiguration The Login Configuration used in the login process.
     * @param authContext The underlying AuthContextLocal object.
     * @return The authentication id JWT.
     * @throws SignatureException If there is a problem signing or verifying the JWT.
     */
    public String createAuthId(LoginConfiguration loginConfiguration, AuthenticationContext authContext)
            throws SignatureException {

        String keyAlias = getKeyAlias(authContext.getOrgDN());

        Map<String, Object> jwtValues = new HashMap<String, Object>();
        if (loginConfiguration.getIndexType().getIndexType() != null && loginConfiguration.getIndexValue() != null) {
            jwtValues.put("authIndexType", loginConfiguration.getIndexType().getIndexType().toString());
            jwtValues.put("authIndexValue", loginConfiguration.getIndexValue());
        }
        jwtValues.put("realm", authContext.getOrgDN());
        jwtValues.put("sessionId", authContext.getSessionID().toString());

        String authId = generateAuthId(keyAlias, jwtValues);
        return authId;
    }

    /**
     * Retrieves the alias of the key to use to sign the JWT.
     *
     * @param orgName The organisation name for the realm being authenticated against.
     * @return The key alias.
     */
    private String getKeyAlias(String orgName) {

        SSOToken token = coreServicesWrapper.getAdminToken();

        String keyAlias = null;
        try {
            ServiceConfigManager scm = coreServicesWrapper.getServiceConfigManager(AUTH_SERVICE_NAME, token);

            ServiceConfig orgConfig = scm.getOrganizationConfig(orgName, null);
            Set<String> values = (Set<String>) orgConfig.getAttributes().get(KEY_ALIAS_KEY);
            for (String value : values) {
                if (value != null && !"".equals(value)) {
                    keyAlias = value;
                    break;
                }
            }
        } catch (SMSException e) {
            throw new RestAuthException(Response.Status.INTERNAL_SERVER_ERROR, e);
        } catch (SSOException e) {
            throw new RestAuthException(Response.Status.INTERNAL_SERVER_ERROR, e);
        }

        return keyAlias;
    }

    /**
     * Generates the authentication id JWT.
     *
     * @param keyAlias The key alias.
     * @param jwtValues A Map of key values to include in the JWT payload. Must not be null.
     * @return The authentication id JWT.
     * @throws SignatureException If there is a problem signing the JWT.
     */
    private String generateAuthId(String keyAlias, Map<String, Object> jwtValues) throws SignatureException {

        String keyStoreAlias = keyAlias;

        if (keyStoreAlias == null) {
            throw new RestAuthException(Response.Status.INTERNAL_SERVER_ERROR,
                    "Could not find Key Store with alias, " + keyStoreAlias);
        }

        PrivateKey privateKey = amKeyProvider.getPrivateKey(keyStoreAlias);

        String otk = new BigInteger(130, RANDOM).toString(32);

        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .claim("otk", otk)
                .claims(jwtValues)
                .build();

        String jwt = jwtBuilderFactory.jws(privateKey)
                .headers()
                .alg(JwsAlgorithm.HS256)
                .done()
                .claims(claimsSet)
                .build();

        return jwt;
    }

    /**
     * Reconstructs the Auth Id from a String to a JWT.
     *
     * @param authId The Auth Id jwt string
     * @return The JWT object.
     */
    public SignedJwt reconstructAuthId(String authId) {
        try {
            return jwtBuilderFactory.reconstruct(authId, SignedJwt.class);
        } catch (JwtRuntimeException e) {
            throw new RestAuthException(Response.Status.BAD_REQUEST, "Failed to parse JWT, " + e.getLocalizedMessage(),
                    e);
        }
    }

    /**
     * Verifies the signature of the JWT, to ensure the JWT is valid.
     *
     * @param realmDN The DN for the realm being authenticated against.
     * @param authId The authentication id JWT.
     */
    public void verifyAuthId(String realmDN, String authId) {

        String keyAlias = getKeyAlias(realmDN);

        PrivateKey privateKey = amKeyProvider.getPrivateKey(keyAlias);

        try {
            boolean verified = jwtBuilderFactory.reconstruct(authId, SignedJwt.class).verify(privateKey);
            if (!verified) {
                throw new RestAuthException(Response.Status.BAD_REQUEST, "AuthId JWT Signature not valid");
            }
        } catch (JwtRuntimeException e) {
            throw new RestAuthException(Response.Status.BAD_REQUEST, "Failed to parse JWT, " + e.getLocalizedMessage(),
                    e);
        }
    }
}
