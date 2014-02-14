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
import org.forgerock.openam.sts.*;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.marshal.TokenRequestMarshaller;
import org.forgerock.openam.sts.rest.marshal.TokenResponseMarshaller;
import org.forgerock.openam.sts.rest.marshal.WebServiceContextFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.WebServiceContext;
import java.io.ByteArrayOutputStream;
import java.util.*;

import org.slf4j.Logger;

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
    private final Logger logger;

    @Inject
    TokenTranslateOperationImpl(
                        TokenRequestMarshaller tokenRequestMarshaller,
                        TokenResponseMarshaller tokenResponseMarshaller,
                        @Named(AMSTSConstants.REST_SUPPORTED_TOKEN_TRANSLATIONS)
                        Set<TokenTransformConfig> supportedTranslations,
                        STSPropertiesMBean stsPropertiesMBean,
                        TokenStore tokenStore,
                        WebServiceContextFactory webServiceContextFactory,
                        TokenTransformFactory tokenTransformFactory,
                        Logger logger) throws Exception {
        this.tokenRequestMarshaller = tokenRequestMarshaller;
        this.tokenResponseMarshaller = tokenResponseMarshaller;
        this.stsPropertiesMBean = stsPropertiesMBean;
        this.tokenStore = tokenStore;
        this.webServiceContextFactory = webServiceContextFactory;
        this.logger = logger;

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

    @Override
    public String translateToken(String inputToken, String desiredStringTokenType, HttpServletRequest request)
            throws TokenValidationException, TokenCreationException {
        TokenType desiredTokenType = null;
        try {
            desiredTokenType = TokenType.valueOf(desiredStringTokenType);
        } catch (IllegalArgumentException e) {
            String message = "The specified desiredTokenType, " + desiredStringTokenType + " is an unknown token type.";
            logger.warn(message);
            throw new TokenValidationException(message);
        } catch (NullPointerException e) {
            String message = "Must specify a non-null desiredTokenType.";
            logger.warn(message);
            throw new TokenValidationException(message);
        }
        TokenType inputTokenType = null;
        try {
            inputTokenType = tokenRequestMarshaller.getTokenType(inputToken);
        } catch (TokenMarshalException e) {
            String message = "Exception caught obtaining toke type from input token json: " + e.getMessage();
            logger.warn(message);
            throw new TokenValidationException(message, e);
        }
        TokenTransform targetedTransform = null;
        for (TokenTransform transform : tokenTransforms) {
            if (transform.isTransformSupported(inputTokenType, desiredTokenType)) {
                targetedTransform = transform;
                break;
            }
        }
        if (targetedTransform == null) {
            String message = "The desired transformation, from " + inputTokenType + " to " + desiredTokenType +
                    ", is not a supported token translation.";
            logger.warn(message);
            throw new TokenValidationException(message);
        }
        ReceivedToken receivedToken = null;
        try {
            receivedToken = tokenRequestMarshaller.marshallTokenRequest(inputToken);
        } catch (TokenMarshalException e) {
            String message = "Exception caught marshalling token request: " + e.getMessage();
            logger.warn(message);
            throw new TokenValidationException(message, e);
        }
        WebServiceContext webServiceContext = webServiceContextFactory.getWebServiceContext(request);
        TokenValidatorParameters validatorParameters = buildTokenValidatorParameters(receivedToken, webServiceContext);
        TokenProviderParameters providerParameters = buildTokenProviderParameters(receivedToken, desiredTokenType, webServiceContext);

        TokenProviderResponse tokenProviderResponse = targetedTransform.transformToken(validatorParameters, providerParameters);
        try {
            return tokenResponseMarshaller.marshalTokenResponse(desiredTokenType, tokenProviderResponse);
        } catch (TokenMarshalException e) {
            String message = "Exception caught marshalling created token to string/json: " + e.getMessage();
            logger.warn(message);
            throw new TokenCreationException(message, e);
        }
    }

    private TokenValidatorParameters buildTokenValidatorParameters(ReceivedToken receivedToken, WebServiceContext webServiceContext) throws TokenValidationException {
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

    private TokenProviderParameters buildTokenProviderParameters(ReceivedToken receivedToken, TokenType desiredTokenType,
                                                                 WebServiceContext webServiceContext) throws TokenCreationException {
        TokenProviderParameters providerParameters = new TokenProviderParameters();
        providerParameters.setStsProperties(stsPropertiesMBean);
        //TODO: does setting the principal make sense here - or should the ProviderParameters principal only be set
        //using the TokenValidatorResponse following successful validation?
        providerParameters.setPrincipal(receivedToken.getPrincipal());
        providerParameters.setWebServiceContext(webServiceContext);
        providerParameters.setTokenStore(tokenStore);
        providerParameters.setEncryptionProperties(stsPropertiesMBean.getEncryptionProperties());

        /*
        TODO:
        defaults for the key and token requirements might be sufficient. Revisit in the future. Might want to have
        a json blob context and user-plug-in to pull out configurations related to key and token requirements.
         */
        KeyRequirements keyRequirements = new KeyRequirements();
        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(TokenType.getProviderParametersTokenType(desiredTokenType));
        providerParameters.setKeyRequirements(keyRequirements);
        providerParameters.setTokenRequirements(tokenRequirements);

        return providerParameters;
    }
}
