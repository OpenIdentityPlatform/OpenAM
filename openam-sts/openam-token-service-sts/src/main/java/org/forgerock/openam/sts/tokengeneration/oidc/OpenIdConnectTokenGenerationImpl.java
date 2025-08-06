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
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.sts.tokengeneration.oidc;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.sm.DNMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.forgerock.json.jose.builders.JwsHeaderBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
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
import org.slf4j.Logger;

import jakarta.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    String stsInstanceId=null;
    
    @SuppressWarnings("rawtypes")
	@Override
    public String generate(SSOToken subjectToken, STSInstanceState stsInstanceState,
                           TokenGenerationServiceInvocationState invocationState) throws TokenCreationException {

        final OpenIdConnectTokenConfig tokenConfig = stsInstanceState.getConfig().getOpenIdConnectTokenConfig();
        final long issueInstant = currentTimeMillis();
        final String subject = ssoTokenIdentity.validateAndGetTokenPrincipal(subjectToken);
        stsInstanceId=invocationState.getStsInstanceId();
        
        STSOpenIdConnectToken openIdConnectToken = buildToken(subjectToken, tokenConfig,
                invocationState.getOpenIdConnectTokenGenerationState(), issueInstant / 1000, subject);

        final JwsAlgorithm jwsAlgorithm = tokenConfig.getSignatureAlgorithm();
        final JwsAlgorithmType jwsAlgorithmType = jwsAlgorithm.getAlgorithmType();

        String tokenString;
        if (JwsAlgorithmType.HMAC.equals(jwsAlgorithmType)) {
            final SignedJwt signedJwt = symmetricSign(openIdConnectToken, jwsAlgorithm, tokenConfig.getClientSecret());
            tokenString = signedJwt.build();
        } else if (JwsAlgorithmType.RSA.equals(jwsAlgorithmType)||JwsAlgorithmType.ECDSA.equals(jwsAlgorithmType)) {
            final SignedJwt signedJwt = asymmetricSign(openIdConnectToken, jwsAlgorithm,tokenConfig.getSignatureKeyAlias(), getKeyPair(stsInstanceState.getOpenIdConnectTokenPKIProvider(),
                    tokenConfig.getSignatureKeyAlias(), tokenConfig.getSignatureKeyPassword()), determinePublicKeyReferenceType(tokenConfig));
            tokenString = signedJwt.build();
        } else {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Unknown JwsAlgorithmType: " + jwsAlgorithmType);
        }
        if (stsInstanceState.getConfig().persistIssuedTokensInCTS()) {
            try {
                ctsTokenPersistence.persistToken(invocationState.getStsInstanceId(), TokenType.OPENIDCONNECT,
                        tokenString, subject,openIdConnectToken.get("jti").asString(), issueInstant, tokenConfig.getTokenLifetimeInSeconds());
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
        //auth claims
        if (subjectToken!=null)
	        try {
		        openIdConnectToken.put("ip", subjectToken.getProperty("Host", true));
		        try {
		        	openIdConnectToken.put("realm", DNMapper.orgNameToRealmName(subjectToken.getProperty("Organization", true)));
		        }catch (Throwable e) {
		        	openIdConnectToken.put("realm", subjectToken.getProperty("Organization", true));
				}
		        try {
					openIdConnectToken.put("auth:time",DateUtils.stringToDate(subjectToken.getProperty("authInstant", true)).getTime()/1000);
				} catch (Throwable e) {}
		        openIdConnectToken.put("auth:ctxid", subjectToken.getProperty("AMCtxId", true));
		        openIdConnectToken.put("auth:service", subjectToken.getProperty("Service", true));
		        openIdConnectToken.put("auth:module", subjectToken.getProperty("AuthType", true));
		        openIdConnectToken.put("auth:level", subjectToken.getProperty("AuthLevel", true));
		        openIdConnectToken.put("auth:time:max:idle", currentTimeMillis()/1000+subjectToken.getMaxIdleTime()*60);
		        openIdConnectToken.put("auth:time:max", currentTimeMillis()/1000+subjectToken.getTimeLeft());
		        openIdConnectToken.put("auth:token:encrypt", Crypt.encryptLocal(subjectToken.getTokenID().toString()));
	        }catch (SSOException e) {
				throw new TokenCreationException(1,"token expired",e);
			}
        
        handleClaims(subjectToken, openIdConnectToken, tokenConfig);
        return openIdConnectToken;
    }

    static Cache<String, Map<String, String>> token2claims=CacheBuilder.newBuilder()
    		.maximumSize(SystemProperties.getAsInt("org.openidentityplatform.sts.cache.maxSize", 64000))
    		.expireAfterWrite(SystemProperties.getAsInt("org.openidentityplatform.sts.cache.maxTime", 1), TimeUnit.SECONDS).build();
    
    private void handleClaims(SSOToken subjectToken, STSOpenIdConnectToken openIdConnectToken, OpenIdConnectTokenConfig tokenConfig) throws TokenCreationException {
    	try {
			openIdConnectToken.setClaims(token2claims.get(stsInstanceId+":"+subjectToken.getTokenID().toString(), new Callable<Map<String,String>>() {
				@Override
				public Map<String, String> call() throws Exception {
					final Map<String, String> mappedClaims = openIdConnectTokenClaimMapperProvider.getClaimMapper(tokenConfig).getCustomClaims(subjectToken, tokenConfig.getClaimMap());
			        //processing to log a warning if any of the values corresponding to the custom clams will over-write an existing claim
			        if (logger.isDebugEnabled()) {
			            for (String key : mappedClaims.keySet()) {
			                if (openIdConnectToken.isDefined(key)) {
			                    logger.debug("In generating an OpenIdConnect token, the claim map for claim " + key +" will over-write an existing claim.");
			                }
			            }
			        }
				    return mappedClaims;
				}
			}));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
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

    private SignedJwt asymmetricSign(STSOpenIdConnectToken openIdConnectToken, JwsAlgorithm jwsAlgorithm,String kid,
                                     KeyPair keyPair, OpenIdConnectTokenPublicKeyReferenceType publicKeyReferenceType) throws TokenCreationException {
        if (!JwsAlgorithmType.RSA.equals(jwsAlgorithm.getAlgorithmType())&!JwsAlgorithmType.ECDSA.equals(jwsAlgorithm.getAlgorithmType())) {
            throw new TokenCreationException(ResourceException.BAD_REQUEST, "Exception in " +
                    "OpenIdConnectTokenGenerationImpl#symmetricSign: algorithm type not RSA but "
                    + jwsAlgorithm.getAlgorithmType());
        }
        final SigningHandler signingHandler = JwsAlgorithmType.RSA.equals(jwsAlgorithm.getAlgorithmType())?new SigningManager().newRsaSigningHandler(keyPair.getPrivate()):new SigningManager().newEcdsaSigningHandler((ECPrivateKey)keyPair.getPrivate());

        final JwsHeaderBuilder jwsHeaderBuilder = jwtBuilderFactory.jws(signingHandler).headers().alg(jwsAlgorithm).kid(kid);
        final JwtClaimsSet claimsSet = jwtBuilderFactory.claims().claims(openIdConnectToken.asMap()).build();

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

}
