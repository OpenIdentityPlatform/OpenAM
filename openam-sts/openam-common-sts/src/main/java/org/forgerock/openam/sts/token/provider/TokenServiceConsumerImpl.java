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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2019-2025 3A Systems, LLC
 */

package org.forgerock.openam.sts.token.provider;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.TokenCancellationException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.service.invocation.OpenIdConnectTokenGenerationState;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.service.invocation.SAML2TokenGenerationState;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState.TokenGenerationServiceInvocationStateBuilder;


/**
 * @see TokenServiceConsumer
 */
public class TokenServiceConsumerImpl implements TokenServiceConsumer {
    private static final String COOKIE = "Cookie";
    private static final ProofTokenState NULL_PROOF_TOKEN_STATE = null;
    private static final String DELETE = "DELETE";

    private final AMSTSConstants.STSType stsType;
    private final String tokenServiceEndpoint;
    private final String crestVersionTokenGenService;
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final String amSessionCookieName;
    private final UrlConstituentCatenator urlConstituentCatenator;

    @Inject
    TokenServiceConsumerImpl(AMSTSConstants.STSType stsType,
                             UrlConstituentCatenator urlConstituentCatenator,
                             @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
                             @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT) String tokenGenServiceUriElement,
                             @Named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE) String crestVersionTokenGenService,
                             HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
                             @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName) {
        this.stsType = stsType;
        this.urlConstituentCatenator = urlConstituentCatenator;
        tokenServiceEndpoint = urlConstituentCatenator.catenateUrlConstituents(amDeploymentUrl, tokenGenServiceUriElement);
        this.crestVersionTokenGenService = crestVersionTokenGenService;
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.amSessionCookieName = amSessionCookieName;
    }

    @Override
    public String getSAML2BearerAssertion(String ssoTokenString,
                                          String stsInstanceId,
                                          String realm,
                                          String authnContextClassRef,
                                          String callerSSOTokenString) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonTokenGenerationInvocationState(TokenType.SAML2, stsInstanceId, realm, ssoTokenString);
        invocationStateBuilder.saml2GenerationState(buildSaml2TokenGenerationState(authnContextClassRef,
                SAML2SubjectConfirmation.BEARER, NULL_PROOF_TOKEN_STATE));
        return invokeTokenCreation(invocationStateBuilder.build().toJson().toString(), callerSSOTokenString);
    }

    @Override
    public String getSAML2SenderVouchesAssertion(String ssoTokenString,
                                                 String stsInstanceId,
                                                 String realm,
                                                 String authnContextClassRef,
                                                 String callerSSOTokenString) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonTokenGenerationInvocationState(TokenType.SAML2, stsInstanceId, realm, ssoTokenString);
        invocationStateBuilder.saml2GenerationState(buildSaml2TokenGenerationState(authnContextClassRef,
                SAML2SubjectConfirmation.SENDER_VOUCHES, NULL_PROOF_TOKEN_STATE));
        return invokeTokenCreation(invocationStateBuilder.build().toJson().toString(), callerSSOTokenString);
    }

    @Override
    public String getSAML2HolderOfKeyAssertion(String ssoTokenString,
                                               String stsInstanceId,
                                               String realm,
                                               String authnContextClassRef,
                                               ProofTokenState proofTokenState,
                                               String callerSSOTokenString) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonTokenGenerationInvocationState(TokenType.SAML2, stsInstanceId, realm, ssoTokenString);
        invocationStateBuilder.saml2GenerationState(buildSaml2TokenGenerationState(authnContextClassRef,
                SAML2SubjectConfirmation.HOLDER_OF_KEY, proofTokenState));
        return invokeTokenCreation(invocationStateBuilder.build().toJson().toString(), callerSSOTokenString);
    }

    @Override
    public String getOpenIdConnectToken(String ssoTokenString, String stsInstanceId, String realm,
                                 String authnContextClassRef, Set<String> authnMethodReferences,
                                 long authnTimeInSeconds, String nonce,
                                 String callerSSOTokenString) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonTokenGenerationInvocationState(TokenType.OPENIDCONNECT, stsInstanceId, realm, ssoTokenString);
        invocationStateBuilder.openIdConnectTokenGenerationState(buildOpenIdConectTokenGenerationState(authnContextClassRef,
                authnMethodReferences, authnTimeInSeconds, nonce));
        return invokeTokenCreation(invocationStateBuilder.build().toJson().toString(), callerSSOTokenString);
    }

    @Override
    public boolean validateToken(String tokenId, String callerSSOTokenString) throws TokenValidationException {
        return isTokenPresent(tokenId, callerSSOTokenString);
    }

    @Override
    public void cancelToken(String tokenId, String callerSSOTokenString) throws TokenCancellationException {
        invokeTokenCancellation(tokenId, callerSSOTokenString);
    }

    private TokenGenerationServiceInvocationStateBuilder buildCommonTokenGenerationInvocationState(TokenType tokenType,
                                                                                                   String stsInstanceId,
                                                                                                   String realm,
                                                                                                   String ssoTokenString) {
        return TokenGenerationServiceInvocationState.builder()
                .tokenType(tokenType)
                .stsType(stsType)
                .stsInstanceId(stsInstanceId)
                .realm(realm)
                .ssoTokenString(ssoTokenString);
    }

    private SAML2TokenGenerationState buildSaml2TokenGenerationState(String authnContextClassRef,
                                                                     SAML2SubjectConfirmation subjectConfirmation,
                                                                     ProofTokenState proofTokenState) {
        return SAML2TokenGenerationState.builder()
                .authenticationContextClassReference(authnContextClassRef)
                .proofTokenState(proofTokenState)
                .subjectConfirmation(subjectConfirmation)
                .build();
    }

    private OpenIdConnectTokenGenerationState buildOpenIdConectTokenGenerationState(String authenticationContextClassReference,
                                                                                    Set<String> authenticationMethodReferences,
                                                                                    long authenticationTimeInSeconds,
                                                                                    String nonce) {
        return OpenIdConnectTokenGenerationState.builder()
                .authenticationMethodReferences(authenticationMethodReferences)
                .authenticationContextClassReference(authenticationContextClassReference)
                .authenticationTimeInSeconds(authenticationTimeInSeconds)
                .nonce(nonce)
                .build();
    }

    private boolean isTokenPresent(String tokenId, String callerSSOTokenString) throws TokenValidationException {
        try {
            Map<String, String> headerMap = makeCommonHeaders(callerSSOTokenString);
            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(new URL(urlConstituentCatenator.catenateUrlConstituents(tokenServiceEndpoint, tokenId)))
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.GET)
                    .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return false;
            } else {
                throw new TokenValidationException(responseCode, connectionResult.getResult());
            }
        } catch (IOException e) {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught invoking TokenService to verify token: " + e);
        }
    }

    private void invokeTokenCancellation(String tokenId, String callerSSOTokenString) throws TokenCancellationException {
        try {
            Map<String, String> headerMap = makeCommonHeaders(callerSSOTokenString);
            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(new URL(urlConstituentCatenator.catenateUrlConstituents(tokenServiceEndpoint, tokenId)))
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(DELETE)
                    .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new TokenCancellationException(responseCode, connectionResult.getResult());
            }
        } catch (IOException e) {
            throw new TokenCancellationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught invoking TokenService to cancel a token: " + e);
        }
    }

    static Method  createInstance=null;;
    
    static {
    	try {
			createInstance=Class.forName("org.forgerock.openam.sts.tokengeneration.service.TokenGenerationService").getMethod("createInstance", String.class);
		} catch (Exception e) {}
    }
    
    private String invokeTokenCreation(final String invocationString, String callerSSOTokenString) throws TokenCreationException {
    	if (createInstance==null)
    		throw new TokenCreationException(500,"org.forgerock.openam.sts.tokengeneration.service.TokenGenerationService create error");
    	try {
    		return (String)createInstance.invoke(null, invocationString);
    	}catch (InvocationTargetException e) {
    		throw (TokenCreationException)e.getTargetException();
		}catch (IllegalAccessException e) {
    		throw new TokenCreationException(500,"org.forgerock.openam.sts.tokengeneration.service.TokenGenerationService",e);
		}
    }

    private Map<String, String> makeCommonHeaders(String callerSSOTokenString) {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, crestVersionTokenGenService);
        headerMap.put(COOKIE, createAMSessionCookie(callerSSOTokenString));
        return headerMap;
    }

    private String createAMSessionCookie(String callerSSOTokenString) {
        return amSessionCookieName + AMSTSConstants.EQUALS + callerSSOTokenString;
    }

}
