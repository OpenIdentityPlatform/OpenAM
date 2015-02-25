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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.operation;

import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.marshal.TokenRequestMarshaller;
import org.forgerock.openam.sts.rest.marshal.TokenResponseMarshaller;
import org.forgerock.openam.sts.rest.marshal.WebServiceContextFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.ws.WebServiceContext;

import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.forgerock.openam.sts.service.invocation.ProofTokenState;
import org.forgerock.openam.sts.service.invocation.RestSTSServiceInvocationState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class defines TokenTranslateOperation implementation, the top-level operation invoked by the REST-STS
 * resource. It is responsible for taking the HttpServletRequest and the invocation parameters, creating the
 * the CXF-STS TokenValidatorParameters and TokenProviderParameters necessary to invoke the TokenValidator/TokenProvider
 * instances specified by the desired token translation operation, and marshals the result into a String.
 *
 */
public class TokenTranslateOperationImpl implements TokenTranslateOperation {
    private final TokenRequestMarshaller tokenRequestMarshaller;
    private final TokenResponseMarshaller tokenResponseMarshaller;
    private final STSPropertiesMBean stsPropertiesMBean;
    private final TokenStore tokenStore;
    private final WebServiceContextFactory webServiceContextFactory;
    private final Set<TokenTransform> tokenTransforms;

    @Inject
    TokenTranslateOperationImpl(
                        TokenRequestMarshaller tokenRequestMarshaller,
                        TokenResponseMarshaller tokenResponseMarshaller,
                        @Named(AMSTSConstants.REST_SUPPORTED_TOKEN_TRANSLATIONS)
                        Set<TokenTransformConfig> supportedTranslations,
                        STSPropertiesMBean stsPropertiesMBean,
                        TokenStore tokenStore,
                        WebServiceContextFactory webServiceContextFactory,
                        TokenTransformFactory tokenTransformFactory) throws Exception {
        this.tokenRequestMarshaller = tokenRequestMarshaller;
        this.tokenResponseMarshaller = tokenResponseMarshaller;
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.webServiceContextFactory = webServiceContextFactory;

        if (supportedTranslations.isEmpty()) {
            throw new IllegalArgumentException("No token transform operations specified.");
        }

        Set<TokenTransform> interimTransforms = new HashSet<TokenTransform>();
        Iterator<TokenTransformConfig> iter = supportedTranslations.iterator();
        while (iter.hasNext()) {
            TokenTransformConfig tokenTransformConfig = iter.next();
            interimTransforms.add(tokenTransformFactory.buildTokenTransform(tokenTransformConfig));
        }
        tokenTransforms = Collections.unmodifiableSet(interimTransforms);
    }

    public JsonValue translateToken(RestSTSServiceInvocationState invocationState, HttpContext httpContext,
                                    RestSTSServiceHttpServletContext restSTSServiceHttpServletContext)
            throws TokenMarshalException, TokenValidationException, TokenCreationException {
        TokenType inputTokenType = tokenRequestMarshaller.getTokenType(invocationState.getInputTokenState());
        TokenType outputTokenType = tokenRequestMarshaller.getTokenType(invocationState.getOutputTokenState());

        TokenTransform targetedTransform = null;
        for (TokenTransform transform : tokenTransforms) {
            if (transform.isTransformSupported(inputTokenType, outputTokenType)) {
                targetedTransform = transform;
                break;
            }
        }
        if (targetedTransform == null) {
            String message = "The desired transformation, from " + inputTokenType + " to " + outputTokenType +
                    ", is not a supported token translation.";
            throw new TokenValidationException(ResourceException.BAD_REQUEST, message);
        }
        ReceivedToken receivedToken = tokenRequestMarshaller.marshallInputToken(invocationState.getInputTokenState(), httpContext,
                restSTSServiceHttpServletContext);
        WebServiceContext webServiceContext = webServiceContextFactory.getWebServiceContext(httpContext, restSTSServiceHttpServletContext);
        TokenValidatorParameters validatorParameters = buildTokenValidatorParameters(receivedToken, webServiceContext);
        TokenProviderParameters providerParameters = buildTokenProviderParameters(inputTokenType,
                invocationState.getInputTokenState(), outputTokenType, invocationState.getOutputTokenState(), webServiceContext);

        TokenProviderResponse tokenProviderResponse = targetedTransform.transformToken(validatorParameters, providerParameters);
        return tokenResponseMarshaller.marshalTokenResponse(outputTokenType, tokenProviderResponse);
    }

    private TokenValidatorParameters buildTokenValidatorParameters(
            ReceivedToken receivedToken, WebServiceContext webServiceContext)
            throws TokenValidationException {
        TokenRequirements validateRequirements = new TokenRequirements();
        validateRequirements.setValidateTarget(receivedToken);
        TokenValidatorParameters validatorParameters = new TokenValidatorParameters();
        validatorParameters.setStsProperties(stsPropertiesMBean);
        validatorParameters.setPrincipal(receivedToken.getPrincipal());
        validatorParameters.setWebServiceContext(webServiceContext);
        /*
        TODO: A common input token type will be the UsernameToken. Currently, I am using the default UsernameTokenValidator provided
        by the STS. A WSS TokenValidator is called if the to-be-validated token is not found in the TokenStore. Currently, the hashCode
        of the UsernameToken is used to determine the key in the TokenStore. This hashCode hashes the username and password, and also any
        nonce/timestamp information. I plug-in a WSS TokenValidator to validate the <username, password> combination against OpenAM, but
        this validator only gets called if the UsernameToken is not found in the TokenStore. Yet this WSS TokenValidator is the place where the
        OpenAM session id is stored, so that it can be used to drive the TokenProviders. So if it is not called, I have no OpenAM session token
        available to the TokenProviders.
        The current, quick-and-dirty work-around is to comment-out the TokenStore, as this prevents the UsernameToken from being cached, and thus
        validation via the WSS TokenValidator being short-circuited due to finding the cached entry. It
        is not really appropriate to cache tokens in the context of a token transformation operation anyway. Going forward, I have to iron out issues
        and policy related to caching tokens.
         */
//        validatorParameters.setTokenStore(tokenStore);
        validatorParameters.setKeyRequirements(null);
        validatorParameters.setTokenRequirements(validateRequirements);
        validatorParameters.setToken(receivedToken);
        return validatorParameters;
    }

    private TokenProviderParameters buildTokenProviderParameters(TokenType inputTokenType,
                                                                 JsonValue inputToken,
                                                                 TokenType desiredTokenType,
                                                                 JsonValue desiredToken,
                                                                 WebServiceContext webServiceContext)
                                                throws TokenCreationException, TokenMarshalException {
        TokenProviderParameters providerParameters = new TokenProviderParameters();
        providerParameters.setStsProperties(stsPropertiesMBean);
        providerParameters.setWebServiceContext(webServiceContext);
        providerParameters.setTokenStore(tokenStore);
        providerParameters.setEncryptionProperties(stsPropertiesMBean.getEncryptionProperties());

        /*
        WS-Trust abstracts the actual specification of the desired token, using the TokenType and KeyType. For a TokenType of
        SAML2, a KeyType of Bearer indicates a Bearer Assertion; a KeyType of PublicKey indicates a HolderOfKey Assertion
        with public-key KeyInfo; a KeyType of SymmetricKey indicates a HolderOfKey Assertion with symmetric-key KeyInfo. The
        OnBehalfOf parameter is used to indicate a SenderVouches Assertion. As such, there is no simple way to indicate
        the SubjectConfirmation method, as it is specified in the REST invocation, and snake it through the CXF-STS engine,
        where it could be referenced by the AMSAMLTokenProvider. So I will put this information in the
        Map<String, Object> included in the TokenProviderParameters.

        Additionally, the AMSAMLTokenProvider needs to specify the AuthnContext passed to the TokenGenerationService. This
        value will be a function of the validated token type, so this value is placed in the additionalProperties as well.

        Finally, if we are dealing with HolderOfKey SubjectConfirmation, the ProofTokenState must be included in the request
        to the TokenGenerationService.
         */
        Map<String, Object> additionalProperties = new HashMap<String, Object>();
        if (TokenType.SAML2.equals(desiredTokenType)) {
            final SAML2SubjectConfirmation subjectConfirmation = tokenRequestMarshaller.getSubjectConfirmation(desiredToken);
            additionalProperties.put(AMSTSConstants.SAML2_SUBJECT_CONFIRMATION_KEY, subjectConfirmation);
            additionalProperties.put(AMSTSConstants.VALIDATED_TOKEN_TYPE_KEY, inputTokenType);
            /*
            The established CXF-STS way to include the x509Certificate specified in the ProofTokenState would be to
            include it in a ReceivedKey specified in the KeyRequirements specified in the TokenProviderParameters.
            However, this ProofTokenState ultimately must be POSTed in json format to the TokenGenerationService.
            If the established CXF-STS procedure is followed, superfluous marshalling from
            ProofTokenState->X509Certificate->ProofTokenState will be incurred. In this case, the more direct route of
            including the ProofTokenState directly in the additionalProperties is preferred, not just to avoid the
            superfluous computation, but in particular because the ProofTokenState and other classes
            in its package constitute a client-sdk of sorts, and thus encapsulate marshalling in static methods as a convenience.
            I don't want my back-end code to consume static methods, and thus aim to pass these data objects through the
            system relatively untouched, aside from being understood by the TokenRequestMarshaller.
             */
            if (SAML2SubjectConfirmation.HOLDER_OF_KEY.equals(subjectConfirmation)) {
                ProofTokenState proofTokenState = tokenRequestMarshaller.getProofTokenState(desiredToken);
                additionalProperties.put(AMSTSConstants.PROOF_TOKEN_STATE_KEY, proofTokenState);
            }
            /*
             Set the inputToken JsonValue in the additionalProperties if we are issuing a SAML2 token so that the
             AuthnContextMapper can use it to determine the appropriate AuthnContext mapping.
             */
            additionalProperties.put(AMSTSConstants.INPUT_TOKEN_STATE_KEY, inputToken);
        }
        providerParameters.setAdditionalProperties(additionalProperties);

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(desiredTokenType.name());
        KeyRequirements keyRequirements = new KeyRequirements();
        providerParameters.setKeyRequirements(keyRequirements);
        providerParameters.setTokenRequirements(tokenRequirements);

        return providerParameters;
    }
}
