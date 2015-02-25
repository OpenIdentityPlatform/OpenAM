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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.provider;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
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

import static org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState.TokenGenerationServiceInvocationStateBuilder;


/**
 * @see org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer
 */
public class TokenGenerationServiceConsumerImpl implements TokenGenerationServiceConsumer {
    private static final String COOKIE = "Cookie";

    private final String tokenGenerationServiceEndpoint;
    private final String crestVersionTokenGenService;
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;

    @Inject
    TokenGenerationServiceConsumerImpl(UrlConstituentCatenator urlConstituentCatenator,
                                       @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
                                       @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT) String tokenGenServiceUriElement,
                                       @Named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE) String crestVersionTokenGenService,
                                       HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory) {
        tokenGenerationServiceEndpoint = urlConstituentCatenator.catenateUrlConstituents(amDeploymentUrl, tokenGenServiceUriElement);
        this.crestVersionTokenGenService = crestVersionTokenGenService;
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
    }

    public String getSAML2BearerAssertion(String ssoTokenString,
                                          String stsInstanceId,
                                          String realm,
                                          String authnContextClassRef,
                                          String callerSSOTokenString) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonSaml2InvocationState(
                        SAML2SubjectConfirmation.BEARER,
                        authnContextClassRef,
                        stsInstanceId,
                        realm,
                        ssoTokenString);
        return makeInvocation(invocationStateBuilder.build().toJson().toString(), callerSSOTokenString);
    }

    public String getSAML2SenderVouchesAssertion(String ssoTokenString,
                                                 String stsInstanceId,
                                                 String realm,
                                                 String authnContextClassRef,
                                                 String callerSSOTokenString) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonSaml2InvocationState(
                        SAML2SubjectConfirmation.SENDER_VOUCHES,
                        authnContextClassRef,
                        stsInstanceId,
                        realm,
                        ssoTokenString);
        return makeInvocation(invocationStateBuilder.build().toJson().toString(), callerSSOTokenString);
    }

    public String getSAML2HolderOfKeyAssertion(String ssoTokenString,
                                               String stsInstanceId,
                                               String realm,
                                               String authnContextClassRef,
                                               ProofTokenState proofTokenState,
                                               String callerSSOTokenString) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonSaml2InvocationState(
                        SAML2SubjectConfirmation.HOLDER_OF_KEY,
                        authnContextClassRef,
                        stsInstanceId,
                        realm,
                        ssoTokenString);
        invocationStateBuilder.proofTokenState(proofTokenState);
        return makeInvocation(invocationStateBuilder.build().toJson().toString(), callerSSOTokenString);
    }

    private TokenGenerationServiceInvocationStateBuilder buildCommonSaml2InvocationState(SAML2SubjectConfirmation subjectConfirmation,
                                                                                         String authnContextClassRef,
                                                                                         String stsInstanceId,
                                                                                         String realm,
                                                                                         String ssoTokenString) {
        return TokenGenerationServiceInvocationState.builder()
                .tokenType(TokenType.SAML2)
                .saml2SubjectConfirmation(subjectConfirmation)
                .authNContextClassRef(authnContextClassRef)
                .stsType(AMSTSConstants.STSType.REST)
                .stsInstanceId(stsInstanceId)
                .realm(realm)
                .ssoTokenString(ssoTokenString);
    }

    private String makeInvocation(String invocationString, String callerSSOTokenString) throws TokenCreationException {
        try {
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, crestVersionTokenGenService);
            headerMap.put(COOKIE, createAMSessionCookie(callerSSOTokenString));

            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(new URL(tokenGenerationServiceEndpoint))
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.POST)
                    .setRequestPayload(invocationString)
                    .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return parseTokenResponse(connectionResult.getResult());
            } else {
                return connectionResult.getResult();
            }
        } catch (IOException e) {
            throw new TokenCreationException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "Exception caught invoking TokenGenerationService: " + e);
        }
    }

    private String createAMSessionCookie(String callerSSOTokenString) {
        return SystemPropertiesManager.get(Constants.AM_COOKIE_NAME) + AMSTSConstants.EQUALS + callerSSOTokenString;
    }

    private String parseTokenResponse(String response) throws TokenCreationException {
        /*
            This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
            org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
            for details.
        */
        JsonValue responseContent;
        try {
            responseContent = JsonValueBuilder.toJsonValue(response);
        } catch (JsonException e) {
            throw new TokenCreationException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "Could not map the response from the TokenGenerationService to a json object. The response: "
                            + response + "; The exception: " + e);
        }
        JsonValue assertionJson = responseContent.get(AMSTSConstants.ISSUED_TOKEN);
        if (!assertionJson.isString()) {
            throw new TokenCreationException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "The json response returned from the TokenGenerationService did not have " +
                            "a non-null string element for the " + AMSTSConstants.ISSUED_TOKEN + " key. The json: "
                            + responseContent.toString());
        }
        return assertionJson.asString();
    }
}
