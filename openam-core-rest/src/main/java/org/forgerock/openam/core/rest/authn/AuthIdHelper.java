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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.authn;

import static org.forgerock.openam.core.rest.authn.RestAuthenticationConstants.SESSION_ID;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringEscapeUtils;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.CoreServicesWrapper;
import org.forgerock.openam.core.rest.authn.core.AuthenticationContext;
import org.forgerock.openam.core.rest.authn.core.LoginConfiguration;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.AMKeyProvider;

/**
 * Helper class to create and verify authentication JWTs.
 */
@Singleton
public class AuthIdHelper {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String AUTH_SERVICE_NAME = "iPlanetAMAuthService";
    private static final String SHARED_SECRET_ATTR = "iplanet-am-auth-hmac-signing-shared-secret";

    private final CoreServicesWrapper coreServicesWrapper;
    private final JwtBuilderFactory jwtBuilderFactory;
    private final SigningManager signingManager;

    /**
     * Constructs an instance of the AuthIdHelper.
     *
     * @param coreServicesWrapper An instance of the CoreServicesWrapper.
     * @param jwtBuilderFactory An instance of the JwtBuilderFactory.
     * @param signingManager An instance of the SigningManager.
     */
    @Inject
    public AuthIdHelper(CoreServicesWrapper coreServicesWrapper,
            JwtBuilderFactory jwtBuilderFactory, SigningManager signingManager) {
        this.coreServicesWrapper = coreServicesWrapper;
        this.jwtBuilderFactory = jwtBuilderFactory;
        this.signingManager = signingManager;
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
            throws SignatureException, RestAuthException {

        final SecretKey key = getSigningKey(authContext.getOrgDN());

        Map<String, Object> jwtValues = new HashMap<String, Object>();
        if (loginConfiguration.getIndexType().getIndexType() != null && loginConfiguration.getIndexValue() != null) {
            jwtValues.put("authIndexType",
                    escapeJsonString(loginConfiguration.getIndexType().getIndexType().toString()));
            jwtValues.put("authIndexValue", escapeJsonString(loginConfiguration.getIndexValue()));
        }
        jwtValues.put("realm", authContext.getOrgDN());
        jwtValues.put(SESSION_ID, authContext.getSessionID().toString());

        return generateAuthId(key, jwtValues);
    }

    /**
     * Escapes " and \ characters from a String which will be put in a JSON string, as per spec.
     *
     * @param s The String to escape
     * @return The escaped String.
     */
    private String escapeJsonString(final String s) {
        return StringEscapeUtils.escapeJava(s);
    }

    /**
     * Retrieves the secret key to use to sign and verify the JWT.
     *
     * @param orgName The organisation name for the realm being authenticated against.
     * @return The signing key.
     */
    private SecretKey getSigningKey(String orgName) throws RestAuthException {

        SSOToken token = coreServicesWrapper.getAdminToken();

        try {
            ServiceConfigManager scm = coreServicesWrapper.getServiceConfigManager(AUTH_SERVICE_NAME, token);
            ServiceConfig orgConfig = scm.getOrganizationConfig(orgName, null);
            byte[] key = Base64.decode(CollectionHelper.getMapAttr(orgConfig.getAttributes(), SHARED_SECRET_ATTR));
            return new SecretKeySpec(key, "RAW");
        } catch (SMSException | SSOException | NullPointerException e) {
            throw new RestAuthException(ResourceException.INTERNAL_ERROR, e);
        }
    }

    /**
     * Generates the authentication id JWT.
     *
     * @param jwtValues A Map of key values to include in the JWT payload. Must not be null.
     * @return The authentication id JWT.
     * @throws SignatureException If there is a problem signing the JWT.
     */
    private String generateAuthId(SecretKey key, Map<String, Object> jwtValues) throws SignatureException,
                                                                                       RestAuthException {
        String otk = new BigInteger(130, RANDOM).toString(32);

        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .claim("otk", otk)
                .claims(jwtValues)
                .build();

        final SigningHandler signingHandler = signingManager.newHmacSigningHandler(key.getEncoded());
        String jwt = jwtBuilderFactory.jws(signingHandler)
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
    public SignedJwt reconstructAuthId(String authId) throws RestAuthException {
        try {
            return jwtBuilderFactory.reconstruct(authId, SignedJwt.class);
        } catch (JwtRuntimeException e) {
            throw new RestAuthException(ResourceException.BAD_REQUEST, "Failed to parse JWT, "
                    + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Verifies the signature of the JWT, to ensure the JWT is valid.
     *
     * @param realmDN The DN for the realm being authenticated against.
     * @param authId The authentication id JWT.
     */
    public void verifyAuthId(String realmDN, String authId) throws RestAuthException {

        SecretKey key = getSigningKey(realmDN);

        try {
            final SigningHandler signingHandler = signingManager.newHmacSigningHandler(key.getEncoded());
            boolean verified = jwtBuilderFactory.reconstruct(authId, SignedJwt.class).verify(signingHandler);
            if (!verified) {
                throw new RestAuthException(ResourceException.BAD_REQUEST, "AuthId JWT Signature not valid");
            }
        } catch (JwtRuntimeException e) {
            throw new RestAuthException(ResourceException.BAD_REQUEST, "Failed to parse JWT, "
                    + e.getLocalizedMessage(), e);
        }
    }
}
