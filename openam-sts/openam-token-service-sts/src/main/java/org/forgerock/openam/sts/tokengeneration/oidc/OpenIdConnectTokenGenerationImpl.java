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

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.sso.SSOToken;
import org.apache.commons.collections.MapUtils;
import org.forgerock.guava.common.collect.Lists;
import org.forgerock.json.jose.builders.JwsHeaderBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwk.KeyUse;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.CTSTokenPersistenceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenConfig;
import org.forgerock.openam.sts.config.user.OpenIdConnectTokenPublicKeyReferenceType;
import org.forgerock.openam.sts.service.invocation.OpenIdConnectTokenGenerationState;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.tokengeneration.CTSTokenPersistence;
import org.forgerock.openam.sts.tokengeneration.SSOTokenIdentity;
import org.forgerock.openam.sts.tokengeneration.oidc.crypto.OpenIdConnectTokenPKIProvider;
import org.forgerock.openam.sts.tokengeneration.state.STSInstanceState;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @see org.forgerock.openam.sts.tokengeneration.oidc.OpenIdConnectTokenGeneration
 */
public class OpenIdConnectTokenGenerationImpl implements OpenIdConnectTokenGeneration {
    private final SSOTokenIdentity ssoTokenIdentity;
    private final JwtBuilderFactory jwtBuilderFactory;
    private final OpenIdConnectTokenClaimMapperProvider openIdConnectTokenClaimMapperProvider;
    private final CTSTokenPersistence ctsTokenPersistence;
    private final Logger logger;

    @Inject
    OpenIdConnectTokenGenerationImpl(SSOTokenIdentity ssoTokenIdentity, JwtBuilderFactory jwtBuilderFactory,
                                     OpenIdConnectTokenClaimMapperProvider openIdConnectTokenClaimMapperProvider,
                                     CTSTokenPersistence ctsTokenPersistence,
                                     Logger logger) {
        this.ssoTokenIdentity = ssoTokenIdentity;
        this.jwtBuilderFactory = jwtBuilderFactory;
        this.openIdConnectTokenClaimMapperProvider = openIdConnectTokenClaimMapperProvider;
        this.ctsTokenPersistence = ctsTokenPersistence;
        this.logger = logger;
    }

    @Override
    public String generate(SSOToken subjectToken, STSInstanceState stsInstanceState,
                           TokenGenerationServiceInvocationState invocationState) throws TokenCreationException {

        final OpenIdConnectTokenConfig tokenConfig = stsInstanceState.getConfig().getOpenIdConnectTokenConfig();
        final long issueInstant = currentTimeMillis();
        final String subject = ssoTokenIdentity.validateAndGetTokenPrincipal(subjectToken);

        STSOpenIdConnectToken openIdConnectToken = buildToken(subjectToken, tokenConfig,
                invocationState.getOpenIdConnectTokenGenerationState(), issueInstant / 1000, subject);

        final JwsAlgorithm jwsAlgorithm = tokenConfig.getSignatureAlgorithm();
        final JwsAlgorithmType jwsAlgorithmType = jwsAlgorithm.getAlgorithmType();

        String tokenString;
        if (JwsAlgorithmType.HMAC.equals(jwsAlgorithmType)) {
            final SignedJwt signedJwt = symmetricSign(openIdConnectToken, jwsAlgorithm, tokenConfig.getClientSecret());
            tokenString = signedJwt.build();
        } else if (JwsAlgorithmType.RSA.equals(jwsAlgorithmType)) {
            final SignedJwt signedJwt = asymmetricSign(openIdConnectToken, jwsAlgorithm, getKeyPair(stsInstanceState.getOpenIdConnectTokenPKIProvider(),
                    tokenConfig.getSignatureKeyAlias(), tokenConfig.getSignatureKeyPassword()), determinePublicKeyReferenceType(tokenConfig));
            tokenString = signedJwt.build();
        } else {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Unknown JwsAlgorithmType: " + jwsAlgorithmType);
        }
        if (stsInstanceState.getConfig().persistIssuedTokensInCTS()) {
            try {
                ctsTokenPersistence.persistToken(invocationState.getStsInstanceId(), TokenType.OPENIDCONNECT,
                        tokenString, subject, issueInstant, tokenConfig.getTokenLifetimeInSeconds());
            } catch (CTSTokenPersistenceException e) {
                throw new TokenCreationException(e.getCode(), e.getMessage(), e);
            }
        }
        return tokenString;
    }

    private STSOpenIdConnectToken buildToken(SSOToken subjectToken, OpenIdConnectTokenConfig tokenConfig,
                                             OpenIdConnectTokenGenerationState tokenGenerationState,
                                             long issueTimeSeconds,
                                             String subject) throws TokenCreationException {
        if (tokenConfig == null) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST,
                    "No OpenIdConnectTokenConfig associated with published sts instance state.");
        }
        if (tokenGenerationState == null) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST,
                    "No OpenIdConnectTokenGenerationState associated with the TokenGenerationServiceInvocationState.");
        }

        STSOpenIdConnectToken openIdConnectToken = new STSOpenIdConnectToken();
        final long expirationTimeInSeconds = issueTimeSeconds + tokenConfig.getTokenLifetimeInSeconds();

        openIdConnectToken
                .setAud(tokenConfig.getAudience())
                .setIss(tokenConfig.getIssuer())
                .setSub(subject)
                .setIat(issueTimeSeconds)
                .setExp(expirationTimeInSeconds)
                .setId(UUID.randomUUID().toString());
        if (!StringUtils.isEmpty(tokenConfig.getAuthorizedParty())) {
            openIdConnectToken.setAzp(tokenConfig.getAuthorizedParty());
        }
        if (!StringUtils.isEmpty(tokenGenerationState.getNonce())) {
            openIdConnectToken.setNonce(tokenGenerationState.getNonce());
        }
        if (!StringUtils.isEmpty(tokenGenerationState.getAuthenticationContextClassReference())) {
            openIdConnectToken.setAcr(tokenGenerationState.getAuthenticationContextClassReference());
        }
        if (!CollectionUtils.isEmpty(tokenGenerationState.getAuthenticationModeReferences())) {
            openIdConnectToken.setAmr(Lists.newArrayList(tokenGenerationState.getAuthenticationModeReferences()));
        }
        if (tokenGenerationState.getAuthenticationTimeInSeconds() != 0) {
            openIdConnectToken.setAuthTime(tokenGenerationState.getAuthenticationTimeInSeconds());
        }

        handleClaims(subjectToken, openIdConnectToken, tokenConfig);
        return openIdConnectToken;
    }

    private void handleClaims(SSOToken subjectToken, STSOpenIdConnectToken openIdConnectToken, OpenIdConnectTokenConfig tokenConfig) throws TokenCreationException {
        if (!MapUtils.isEmpty(tokenConfig.getClaimMap())) {
            Map<String, String> mappedClaims =
                    openIdConnectTokenClaimMapperProvider.getClaimMapper(tokenConfig).getCustomClaims(subjectToken, tokenConfig.getClaimMap());
            //processing to log a warning if any of the values corresponding to the custom clams will over-write an existing claim
            for (String key : mappedClaims.keySet()) {
                if (openIdConnectToken.isDefined(key)) {
                    logger.warn("In generating an OpenIdConnect token, the claim map for claim " + key +
                            " will over-write an existing claim.");
                }
            }
            openIdConnectToken.setClaims(mappedClaims);
        }
    }

    private KeyPair getKeyPair(OpenIdConnectTokenPKIProvider cryptoProvider, String signatureKeyAlias, byte[] signatureKeyPassword)
                                                                                    throws TokenCreationException {
        try {
            return new KeyPair(cryptoProvider.getProviderCertificateChain(signatureKeyAlias)[0].getPublicKey(),
                    cryptoProvider.getProviderPrivateKey(signatureKeyAlias, new String(signatureKeyPassword, AMSTSConstants.UTF_8_CHARSET_ID)));
        } catch (UnsupportedEncodingException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Incorrect charset encoding for signature key password: " + e);
        }
    }

    private SignedJwt symmetricSign(STSOpenIdConnectToken openIdConnectToken, JwsAlgorithm jwsAlgorithm,
                                                        byte[] clientSecret) throws TokenCreationException {
        if (!JwsAlgorithmType.HMAC.equals(jwsAlgorithm.getAlgorithmType())) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Exception in " +
                    "OpenIdConnectTokenGenerationImpl#symmetricSign: algorithm type not HMAC but "
                    + jwsAlgorithm.getAlgorithmType());
        }
        final SigningHandler signingHandler = new SigningManager().newHmacSigningHandler(clientSecret);

        JwsHeaderBuilder builder = jwtBuilderFactory.jws(signingHandler).headers().alg(jwsAlgorithm);
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims().claims(openIdConnectToken.asMap()).build();
        return builder.done().claims(claimsSet).asJwt();
    }

    private SignedJwt asymmetricSign(STSOpenIdConnectToken openIdConnectToken, JwsAlgorithm jwsAlgorithm,
                                     KeyPair keyPair, OpenIdConnectTokenPublicKeyReferenceType publicKeyReferenceType) throws TokenCreationException {
        if (!JwsAlgorithmType.RSA.equals(jwsAlgorithm.getAlgorithmType())) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Exception in " +
                    "OpenIdConnectTokenGenerationImpl#symmetricSign: algorithm type not RSA but "
                    + jwsAlgorithm.getAlgorithmType());
        }
        final SigningHandler signingHandler = new SigningManager().newRsaSigningHandler(keyPair.getPrivate());

        JwsHeaderBuilder jwsHeaderBuilder = jwtBuilderFactory.jws(signingHandler).headers().alg(jwsAlgorithm);
        JwtClaimsSet claimsSet = jwtBuilderFactory.claims().claims(openIdConnectToken.asMap()).build();
        RSAPublicKey rsaPublicKey;
        try {
            rsaPublicKey = (RSAPublicKey)keyPair.getPublic();
        } catch (ClassCastException e) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Could not sign jwt with algorithm "
                    + jwsAlgorithm + " because the PublicKey not of type RSAPublicKey but rather "
                    + (keyPair.getPublic() != null ? keyPair.getPublic().getClass().getCanonicalName() : null));
        }
        handleKeyIdentification(jwsHeaderBuilder, publicKeyReferenceType, rsaPublicKey, jwsAlgorithm);
        return jwsHeaderBuilder.done().claims(claimsSet).asJwt();
    }

    private OpenIdConnectTokenPublicKeyReferenceType determinePublicKeyReferenceType(OpenIdConnectTokenConfig tokenConfig) throws TokenCreationException {
        final OpenIdConnectTokenPublicKeyReferenceType publicKeyReferenceType = tokenConfig.getPublicKeyReferenceType();
        if (!OpenIdConnectTokenPublicKeyReferenceType.JWK.equals(publicKeyReferenceType) &&
                !OpenIdConnectTokenPublicKeyReferenceType.NONE.equals(publicKeyReferenceType)) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST,
                    "An unsupported public key reference type encountered during OpenId Connect token generation: "
                            + publicKeyReferenceType);

        }
        return publicKeyReferenceType;
    }

    private void handleKeyIdentification(JwsHeaderBuilder jwsHeaderBuilder, OpenIdConnectTokenPublicKeyReferenceType publicKeyReferenceType,
                                         RSAPublicKey rsaPublicKey, JwsAlgorithm jwsAlgorithm) throws TokenCreationException {
        switch (publicKeyReferenceType) {
            case NONE:
                return;
            case JWK:
                jwsHeaderBuilder.jwk(buildRSAJWKForPublicKey(rsaPublicKey, jwsAlgorithm));
                return;
            default:
                throw new TokenCreationException(ResourceException.BAD_REQUEST, "Unsupported public key identification " +
                        "scheme encountered during OpenIdConnect token generation: " + publicKeyReferenceType);
        }
    }

    private RsaJWK buildRSAJWKForPublicKey(RSAPublicKey rsaPublicKey, JwsAlgorithm jwsAlgorithm) {
        final String kid = null, x5u = null, x5t = null;
        final List<Base64> x5c = null;
        return new RsaJWK(rsaPublicKey, KeyUse.SIG, jwsAlgorithm.name(), kid, x5u, x5t, x5c);
    }
}
