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

package org.forgerock.openam.sts.token.validator;

import com.sun.identity.shared.xml.XMLUtils;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;

import org.w3c.dom.Element;

import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.security.Principal;
import org.slf4j.Logger;

/**
 * Validates OpenAM tokens by making a Rest call to OpenAM to obtain the principal corresponding to the session id. Part
 * of establishing this correlation in OpenAM includes determining that the session id is valid. This class is not in the
 * wss package, and is not implemented via classes in the disp and uri packages because it is not explicitly consuming
 * the OpenAM Rest authN context, but rather consumes a standard OpenAM Rest interface to correlate a session id to a
 * principal. In other words, the disp and uri packages have to do with dispatching an invocation against the authN module
 * specified in the AuthTargetMapping state of the published STS instance. Validating an OpenAM session id does not involve
 * consuming REST authN, but rather consuming a standard OpenAM REST endpoint which will take a session id, and return the
 * associated principal, and throw an exception if the session id was invalid.
 *
 */
public class AMTokenValidator implements TokenValidator {
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final Logger logger;

    /*
    The lifecycle for this class is controlled by the TokenOperationFactoryImpl, and thus needs no @Inject.
     */
    public AMTokenValidator(ThreadLocalAMTokenCache threadLocalAMTokenCache, PrincipalFromSession principalFromSession, Logger logger) {
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.logger = logger;
    }

    /*
    Because the ReceivedToken and SecurityToken objects ultimately represent a token object as a DOM Element, this
    class must process them as an Element.
     */
    @Override
    public boolean canHandleToken(ReceivedToken validateTarget) {
        Object token = validateTarget.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            return AMSTSConstants.AM_SESSION_ID_ELEMENT_NAME.equals(tokenElement.getLocalName());
        }
        return false;
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        return canHandleToken(validateTarget);
    }

    @Override
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(ReceivedToken.STATE.INVALID);
        response.setToken(validateTarget);
        try {
            String sessionId = parseSessionIdFromRequest(tokenParameters.getToken());
            threadLocalAMTokenCache.cacheAMToken(sessionId);
            Principal principal = principalFromSession.getPrincipalFromSession(sessionId);
            response.setPrincipal(principal);
            validateTarget.setState(ReceivedToken.STATE.VALID);
        } catch (Exception e) {
            logger.info("Exception caught obtaining principal from session id: " + e, e);
        }
        return response;
    }

    private String parseSessionIdFromRequest(ReceivedToken receivedToken) throws TokenCreationException {
        Object token = receivedToken.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            if (AMSTSConstants.AM_SESSION_ID_ELEMENT_NAME.equals(tokenElement.getLocalName())) {
                return ((Element)token).getFirstChild().getNodeValue();
            } else {
                try {
                    Transformer transformer = XMLUtils.getTransformerFactory().newTransformer();
                    StreamResult res =  new StreamResult(new ByteArrayOutputStream());
                    transformer.transform(new DOMSource(tokenElement), res);
                    String message = "Unexpected state: should be dealing with a DOM Element defining an AM session, but " +
                            "not the following token element: " +
                            new String(((ByteArrayOutputStream)res.getOutputStream()).toByteArray());
                    logger.error(message);
                    throw new TokenCreationException(ResourceException.INTERNAL_ERROR, message);
                } catch (Exception e) {
                    throw new TokenCreationException(ResourceException.INTERNAL_ERROR, "Unexpected state: should be dealing with a DOM Element defining an " +
                            "AM Session, but this is not the case.");
                }
            }
        } else {
            String message = "Unexpected state in AMTokenValidator: validated token of unexpected type: " +
                    (token != null ? token.getClass().getCanonicalName() : null);
            logger.error(message);
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, message);
        }
    }
}
