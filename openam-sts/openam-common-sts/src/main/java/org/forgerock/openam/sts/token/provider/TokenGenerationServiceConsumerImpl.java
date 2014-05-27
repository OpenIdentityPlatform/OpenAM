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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;

import static org.forgerock.openam.sts.service.invocation.TokenGenerationServiceInvocationState.TokenGenerationServiceInvocationStateBuilder;


/**
 * @see org.forgerock.openam.sts.token.provider.TokenGenerationServiceConsumer
 */
public class TokenGenerationServiceConsumerImpl implements TokenGenerationServiceConsumer {
    private final String tokenGenerationServiceEndpoint;

    @Inject
    TokenGenerationServiceConsumerImpl(UrlConstituentCatenator urlConstituentCatenator,
                                       @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
                                       @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT) String tokenGenServiceUriElement) {
        tokenGenerationServiceEndpoint = urlConstituentCatenator.catenateUrlConstituents(amDeploymentUrl, tokenGenServiceUriElement);
    }

    public String getSAML2BearerAssertion(String ssoTokenString,
                                          String stsInstanceId,
                                          String serviceProviderAssertionConsumerServiceUrl,
                                          String authnContextClassRef) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonSaml2Elements(
                        SAML2SubjectConfirmation.BEARER,
                        authnContextClassRef,
                        stsInstanceId,
                        ssoTokenString);
        invocationStateBuilder.serviceProviderAssertionConsumerServiceUrl(serviceProviderAssertionConsumerServiceUrl);
        return makeInvocation(invocationStateBuilder.build().toJson().toString());
    }

    public String getSAML2SenderVouchesAssertion(String ssoTokenString,
                                                 String stsInstanceId,
                                                 String authnContextClassRef) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonSaml2Elements(
                        SAML2SubjectConfirmation.SENDER_VOUCHES,
                        authnContextClassRef,
                        stsInstanceId,
                        ssoTokenString);
        return makeInvocation(invocationStateBuilder.build().toJson().toString());
    }

    public String getSAML2HolderOfKeyAssertion(String ssoTokenString,
                                               String stsInstanceId,
                                               String authnContextClassRef,
                                               ProofTokenState proofTokenState) throws TokenCreationException {
        final TokenGenerationServiceInvocationStateBuilder invocationStateBuilder =
                buildCommonSaml2Elements(
                        SAML2SubjectConfirmation.HOLDER_OF_KEY,
                        authnContextClassRef,
                        stsInstanceId,
                        ssoTokenString);
        invocationStateBuilder.proofTokenState(proofTokenState);
        return makeInvocation(invocationStateBuilder.build().toJson().toString());
    }

    private TokenGenerationServiceInvocationStateBuilder buildCommonSaml2Elements(SAML2SubjectConfirmation subjectConfirmation,
                                                                                  String authnContextClassRef,
                                                                                  String stsInstanceId,
                                                                                  String ssoTokenString) {
        return TokenGenerationServiceInvocationState.builder()
                .tokenType(TokenType.SAML2)
                .saml2SubjectConfirmation(subjectConfirmation)
                .authNContextClassRef(authnContextClassRef)
                .stsType(AMSTSConstants.STSType.REST)
                .stsInstanceId(stsInstanceId)
                .ssoTokenString(ssoTokenString);
    }

    private String makeInvocation(String invocationString) throws TokenCreationException {
        final ClientResource resource = new ClientResource(tokenGenerationServiceEndpoint);
        try {
            final Representation representation = resource.post(
                    new StringRepresentation(invocationString, MediaType.APPLICATION_JSON));
            return parseTokenResponse(representation.getText());
        } catch (ResourceException e) {
            throw new TokenCreationException(e.getStatus().getCode(), "Exception caught invoking TokenGenerationService: " + e, e);
        } catch (IOException e) {
            throw new TokenCreationException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "Exception caught getting text from string returned from TokenGenerationService: " + e, e);
        }
    }

    private String parseTokenResponse(String response) throws TokenCreationException {
        /*
            This is how the Crest HttpServletAdapter ultimately constitutes a JsonValue from a json string. See the
            org.forgerock.json.resource.servlet.HttpUtils.parseJsonBody (called from HttpServletAdapter.getJsonContent)
            for details.
        */
        Object responseContent;
        try {
            org.codehaus.jackson.JsonParser parser =
                    new org.codehaus.jackson.map.ObjectMapper().getJsonFactory().createJsonParser(response);
            responseContent = parser.readValueAs(Object.class);
        } catch (IOException e) {
            throw new AMSTSRuntimeException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "Could not map the response from the TokenGenerationService to a json object. The response: "
                            + response + "; The exception: " + e);
        }
        JsonValue assertionJson = new JsonValue(responseContent).get(AMSTSConstants.ISSUED_TOKEN);
        if (assertionJson.isNull() || !assertionJson.isString()) {
            throw new AMSTSRuntimeException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "The json response returned from the TokenGenerationService did not have " +
                            "a non-null string element for the " + AMSTSConstants.ISSUED_TOKEN + " key. The json: "
                            + responseContent.toString());
        }
        return assertionJson.asString();
    }
}
