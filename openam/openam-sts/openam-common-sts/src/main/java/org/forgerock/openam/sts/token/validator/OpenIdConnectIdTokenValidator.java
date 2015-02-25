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

package org.forgerock.openam.sts.token.validator;

import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.ws.security.handler.RequestData;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.XmlMarshaller;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.validator.wss.AuthenticationHandler;
import org.slf4j.Logger;
import org.w3c.dom.Element;

import java.security.Principal;

/**
 * The TokenValidator implementation responsible for dispatching OpenID Connect ID Tokens to the OpenAM Rest authN
 * context.
 */
public class OpenIdConnectIdTokenValidator implements TokenValidator {
    private final AuthenticationHandler<OpenIdConnectIdToken> authenticationHandler;
    private final XmlMarshaller<OpenIdConnectIdToken> idTokenXmlMarshaller;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final Logger logger;

    public OpenIdConnectIdTokenValidator(AuthenticationHandler<OpenIdConnectIdToken> authenticationHandler,
                                         XmlMarshaller<OpenIdConnectIdToken> idTokenXmlMarshaller,
                                         ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                         PrincipalFromSession principalFromSession,
                                         Logger logger) {
        this.authenticationHandler = authenticationHandler;
        this.idTokenXmlMarshaller = idTokenXmlMarshaller;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.logger = logger;
    }
    public boolean canHandleToken(ReceivedToken validateTarget) {
        Object token = validateTarget.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            return AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_KEY.equals(tokenElement.getLocalName());
        }
        return false;
    }

    /*
    The OIDC TokenValidator is not CXF-STS realm-aware, so just delegate non-realm method.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        return canHandleToken(validateTarget);
    }

    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(ReceivedToken.STATE.INVALID);
        response.setToken(validateTarget);
        OpenIdConnectIdToken idToken;
        /*
        We will be dealing with the XML representation of the OpenIdConnectIdToken, as the Rest STS facade, or the
        SOAP client, will have created an XML representation of the OpenIdConnectIdToken class, so we just have to
        marshall it back out of its XML format to consume the authenticationHandler.
         */
        if (validateTarget.isDOMElement()) {
            try {
                idToken = idTokenXmlMarshaller.fromXml((Element)validateTarget.getToken());
            } catch (TokenMarshalException e) {
                throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                        "Exception caught marshalling OIDC ID token from XML: " + e, e);
            }
        } else {
            //No toString in ReceivedToken, so I can't log what I really have
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "Token passed to OpenIdConnectIdTokenValidator not DOM Element, as expected.");
        }
        try {
            authenticationHandler.authenticate(makeRequestData(tokenParameters), idToken);
            /*
            a successful call to the authenticationHandler will put the sessionId in the tokenCache. Pull it
            out and use it to obtain the principal corresponding to the Session.
             */
            Principal principal = principalFromSession.getPrincipalFromSession(threadLocalAMTokenCache.getAMToken());
            response.setPrincipal(principal);
            validateTarget.setState(ReceivedToken.STATE.VALID);
        } catch (TokenValidationException e) {
            throw new AMSTSRuntimeException(e.getCode(),
                    "Exception caught validating OIDC token with authentication handler: " + e, e);
        } catch (TokenCreationException e) {
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR,
                    "No OpenAM Session token cached: " + e, e);
        }
        return response;
    }

    /*
     * Creates the RequestData object in a manner similar to the org.apache.cxf.sts.token.validator.UsernameTokenValidator.
     * As the name implies, the RequestData provides request context. For the AuthenticationHandler<OpenIDConnectToken>
     * encapsulated in this class, the entity which will dispatch the request to the OpenAM Rest authN context, this
     * data is not used, though it is provided simply to fulfill the contract.
     */
    private RequestData makeRequestData(TokenValidatorParameters parameters) {
        STSPropertiesMBean stsProperties = parameters.getStsProperties();
        RequestData requestData = new RequestData();
        requestData.setSigCrypto(stsProperties.getSignatureCrypto());
        requestData.setCallbackHandler(stsProperties.getCallbackHandler());
        return requestData;
    }
}
