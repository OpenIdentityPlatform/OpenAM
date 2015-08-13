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
 */

package org.forgerock.openam.sts.token.provider;

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
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
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
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
    private static final String CREATE_ACTION_PARAM = SharedSTSConstants.PUBLISH_SERVICE_CREATE_ACTION_URL_ELEMENT;
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

    private String invokeTokenCreation(String invocationString, String callerSSOTokenString) throws TokenCreationException {
        try {
            Map<String, String> headerMap = makeCommonHeaders(callerSSOTokenString);
            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(new URL(urlConstituentCatenator.catenateUrlConstituents(tokenServiceEndpoint, CREATE_ACTION_PARAM)))
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.POST)
                    .setRequestPayload(invocationString)
                    .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                return parseTokenServiceResponse(connectionResult.getResult());
            } else {
                throw new TokenCreationException(responseCode, connectionResult.getResult());
            }
        } catch (IOException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught invoking TokenService to create a token: " + e);
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

    private String parseTokenServiceResponse(String response) throws TokenCreationException {
        /*
            This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
            org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
            for details.
        */
        JsonValue responseContent;
        try {
            responseContent = JsonValueBuilder.toJsonValue(response);
        } catch (JsonException e) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "Could not map the response from the TokenService to a json object. The response: "
                            + response + "; The exception: " + e);
        }
        JsonValue assertionJson = responseContent.get(AMSTSConstants.ISSUED_TOKEN);
        if (!assertionJson.isString()) {
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR,
                    "The json response returned from the TokenService did not have " +
                            "a non-null string element for the " + AMSTSConstants.ISSUED_TOKEN + " key. The json: "
                            + responseContent.toString());
        }
        return assertionJson.asString();
    }
}
