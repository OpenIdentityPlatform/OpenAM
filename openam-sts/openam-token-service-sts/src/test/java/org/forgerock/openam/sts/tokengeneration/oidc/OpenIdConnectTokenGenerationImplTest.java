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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.oidc;

import com.iplanet.sso.SSOToken;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolverFactory;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.service.invocation.OpenIdConnectTokenGenerationState;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.CTSTokenPersistence;
import org.forgerock.openam.sts.tokengeneration.SSOTokenIdentity;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProviderImpl;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceState;
import org.slf4j.Logger;
import org.testng.annotations.Test;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import static org.forgerock.openam.utils.Time.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class OpenIdConnectTokenGenerationImplTest {
    private static final int READ_TIMEOUT = 10;
    private static final int CONNECT_TIMEOUT = 10;
    private static final String SUBJECT_NAME = "demo";
    private static final long TOKEN_LIFETIME = 60 * 10;
    private static final String ISSUER = "da_issuer";
    private static final String AUDIENCE = "foo_audience";
    private static final JwsAlgorithm HMAC_SIGNING_ALGORITHM = JwsAlgorithm.HS256;
    private static final JwsAlgorithm RSA_SIGNING_ALGORITHM = JwsAlgorithm.RS256;
    private static final byte[] CLIENT_SECRET = "super_bobo".getBytes();
    private static final String AUTHN_CLASS_REFERENCE = "urn:whatever:foobar";
    private static final String AUTHORIZED_PARTY = "foo_azp";
    private static final String EMAIL_CLAIM_KEY = "email";
    private static final String EMAIL_CLAIM_VALUE = "cornholio@dingus.com";
    private static final String EMAIL_CLAIM_ATTRIBUTE = "mail";
    private final Map<String, String> mappedClaimConfig;
    private final Map<String, Object> mappedClaimAttributes;

    public OpenIdConnectTokenGenerationImplTest() {
        mappedClaimConfig = new HashMap<>();
        mappedClaimConfig.put(EMAIL_CLAIM_KEY, EMAIL_CLAIM_ATTRIBUTE);
        mappedClaimAttributes = new HashMap<>();
        mappedClaimAttributes.put(EMAIL_CLAIM_KEY, EMAIL_CLAIM_VALUE);
    }

    @Test
    public void testHMACOpenIdConnectTokenGeneration() throws TokenCreationException {
        SSOTokenIdentity mockSSOTokenIdentity = mock(SSOTokenIdentity.class);
        when(mockSSOTokenIdentity.validateAndGetTokenPrincipal(any(SSOToken.class))).thenReturn(SUBJECT_NAME);
        SSOToken mockSSOToken = mock(SSOToken.class);
        STSInstanceState mockSTSInstanceState = mock(STSInstanceState.class);
        STSInstanceConfig mockSTSInstanceConfig = mock(STSInstanceConfig.class);
        when(mockSTSInstanceState.getConfig()).thenReturn(mockSTSInstanceConfig);
        OpenIdConnectTokenConfig openIdConnectTokenConfig = buildHMACOpenIdConnectTokenConfig();
        when(mockSTSInstanceConfig.getOpenIdConnectTokenConfig()).thenReturn(openIdConnectTokenConfig);
        TokenGenerationServiceInvocationState mockTokenGenerationInvocationState = mock(TokenGenerationServiceInvocationState.class);
        OpenIdConnectTokenClaimMapperProvider mockClaimMapperProvider = mock(OpenIdConnectTokenClaimMapperProvider.class);
        OpenIdConnectTokenClaimMapper mockClaimMapper = mock(OpenIdConnectTokenClaimMapper.class);
        when(mockClaimMapperProvider.getClaimMapper(any(OpenIdConnectTokenConfig.class))).thenReturn(mockClaimMapper);
        when(mockClaimMapper.getCustomClaims(mockSSOToken, mappedClaimConfig)).thenReturn(mappedClaimAttributes);
        long authTime = currentTimeMillis() / 1000;
        OpenIdConnectTokenGenerationState openIdConnectTokenGenerationState = buildOpenIdConnectTokenGenerationState(authTime);
        when(mockTokenGenerationInvocationState.getOpenIdConnectTokenGenerationState()).thenReturn(openIdConnectTokenGenerationState);
        String oidcToken = new OpenIdConnectTokenGenerationImpl(mockSSOTokenIdentity, new JwtBuilderFactory(),
                mockClaimMapperProvider, mock(CTSTokenPersistence.class), mock(Logger.class))
                .generate(mockSSOToken, mockSTSInstanceState, mockTokenGenerationInvocationState);
        SignedJwt signedJwt = reconstructSignedJwt(oidcToken);
        JwtClaimsSet jwtClaimsSet = signedJwt.getClaimsSet();
        assertEquals(SUBJECT_NAME, jwtClaimsSet.getSubject());
        assertEquals(AUDIENCE, jwtClaimsSet.getAudience().get(0));
        assertEquals(AUTHN_CLASS_REFERENCE, jwtClaimsSet.getClaim("acr", String.class));
        assertEquals(ISSUER, jwtClaimsSet.getIssuer());
        assertEquals(EMAIL_CLAIM_VALUE, jwtClaimsSet.get(EMAIL_CLAIM_KEY).asString());
        assertTrue(verifyHMACSignature(signedJwt));
    }

    @Test
    public void testRSAOpenIdConnectTokenGeneration() throws TokenCreationException {
        SSOTokenIdentity mockSSOTokenIdentity = mock(SSOTokenIdentity.class);
        when(mockSSOTokenIdentity.validateAndGetTokenPrincipal(any(SSOToken.class))).thenReturn(SUBJECT_NAME);
        SSOToken mockSSOToken = mock(SSOToken.class);
        STSInstanceState mockSTSInstanceState = mock(STSInstanceState.class);
        STSInstanceConfig mockSTSInstanceConfig = mock(STSInstanceConfig.class);
        when(mockSTSInstanceState.getConfig()).thenReturn(mockSTSInstanceConfig);
        OpenIdConnectTokenConfig openIdConnectTokenConfig = buildRSAOpenIdConnectTokenConfig();
        when(mockSTSInstanceConfig.getOpenIdConnectTokenConfig()).thenReturn(openIdConnectTokenConfig);
        OpenIdConnectTokenPKIProviderImpl tokenCryptoProvider = new OpenIdConnectTokenPKIProviderImpl(openIdConnectTokenConfig);
        when(mockSTSInstanceState.getOpenIdConnectTokenPKIProvider()).thenReturn(tokenCryptoProvider);
        TokenGenerationServiceInvocationState mockTokenGenerationInvocationState = mock(TokenGenerationServiceInvocationState.class);
        OpenIdConnectTokenClaimMapperProvider mockClaimMapperProvider = mock(OpenIdConnectTokenClaimMapperProvider.class);
        OpenIdConnectTokenClaimMapper mockClaimMapper = mock(OpenIdConnectTokenClaimMapper.class);
        when(mockClaimMapperProvider.getClaimMapper(any(OpenIdConnectTokenConfig.class))).thenReturn(mockClaimMapper);
        when(mockClaimMapper.getCustomClaims(mockSSOToken, mappedClaimConfig)).thenReturn(mappedClaimAttributes);
        long authTime = currentTimeMillis() / 1000;
        OpenIdConnectTokenGenerationState openIdConnectTokenGenerationState = buildOpenIdConnectTokenGenerationState(authTime);
        when(mockTokenGenerationInvocationState.getOpenIdConnectTokenGenerationState()).thenReturn(openIdConnectTokenGenerationState);
        String oidcToken = new OpenIdConnectTokenGenerationImpl(mockSSOTokenIdentity, new JwtBuilderFactory(),
                mockClaimMapperProvider, mock(CTSTokenPersistence.class), mock(Logger.class))
                .generate(mockSSOToken, mockSTSInstanceState, mockTokenGenerationInvocationState);
        SignedJwt signedJwt = reconstructSignedJwt(oidcToken);
        JwtClaimsSet jwtClaimsSet = signedJwt.getClaimsSet();
        assertEquals(SUBJECT_NAME, jwtClaimsSet.getSubject());
        assertEquals(AUDIENCE, jwtClaimsSet.getAudience().get(0));
        assertEquals(AUTHN_CLASS_REFERENCE, jwtClaimsSet.getClaim("acr", String.class));
        assertEquals(ISSUER, jwtClaimsSet.getIssuer());
        assertEquals(EMAIL_CLAIM_VALUE, jwtClaimsSet.get(EMAIL_CLAIM_KEY).asString());

        assertTrue(verifyRSASignature(signedJwt, openIdConnectTokenConfig));
    }

    private boolean verifyHMACSignature(SignedJwt signedJwt) {
        try {
            new OpenIdResolverFactory(READ_TIMEOUT, CONNECT_TIMEOUT)
                    .createSharedSecretResolver(ISSUER, new String(CLIENT_SECRET))
                    .validateIdentity(signedJwt);
            return true;
        } catch (OpenIdConnectVerificationException e) {
            return false;
        }
    }

    private boolean verifyRSASignature(SignedJwt signedJwt, OpenIdConnectTokenConfig tokenConfig) throws TokenCreationException {
        try {
            new OpenIdResolverFactory(READ_TIMEOUT, CONNECT_TIMEOUT)
                    .createPublicKeyResolver(ISSUER, getPublicKey(tokenConfig))
                    .validateIdentity(signedJwt);
            return true;
        } catch (OpenIdConnectVerificationException e) {
            return false;
        }
    }

    private PublicKey getPublicKey(OpenIdConnectTokenConfig tokenConfig) throws TokenCreationException {
        return new OpenIdConnectTokenPKIProviderImpl(tokenConfig).getProviderCertificateChain("test")[0].getPublicKey();
    }

    private SignedJwt reconstructSignedJwt(String oidcToken) {
            return new JwtReconstruction().reconstructJwt(oidcToken, SignedJwt.class);
    }

    private OpenIdConnectTokenConfig buildHMACOpenIdConnectTokenConfig() {
        return OpenIdConnectTokenConfig.builder()
                .tokenLifetimeInSeconds(TOKEN_LIFETIME)
                .signatureAlgorithm(HMAC_SIGNING_ALGORITHM)
                .issuer(ISSUER)
                .addAudience(AUDIENCE)
                .authorizedParty(AUTHORIZED_PARTY)
                .clientSecret(CLIENT_SECRET)
                .claimMap(mappedClaimConfig)
                .build();
    }

    private OpenIdConnectTokenConfig buildRSAOpenIdConnectTokenConfig() {
        return OpenIdConnectTokenConfig.builder()
                .tokenLifetimeInSeconds(TOKEN_LIFETIME)
                .signatureAlgorithm(RSA_SIGNING_ALGORITHM)
                .issuer(ISSUER)
                .addAudience(AUDIENCE)
                .authorizedParty(AUTHORIZED_PARTY)
                //once CREST-273 is fixed, this line can be uncommented.
                //.publicKeyReferenceType("jwk")
                .keystoreLocation("keystore.jks")
                .keystorePassword("changeit".getBytes())
                .signatureKeyPassword("changeit".getBytes())
                .signatureKeyAlias("test")
                .claimMap(mappedClaimConfig)
                .build();
    }

    private OpenIdConnectTokenGenerationState buildOpenIdConnectTokenGenerationState(long authTime) {
        return OpenIdConnectTokenGenerationState.builder()
                .authenticationTimeInSeconds(authTime)
                .authenticationContextClassReference(AUTHN_CLASS_REFERENCE)
                .nonce("dfdee")
                .build();
    }
}
