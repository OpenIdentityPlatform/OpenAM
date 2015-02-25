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

package org.forgerock.openam.sts.token;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.ws.security.handler.RequestData;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;

import org.slf4j.Logger;

/**
 * This class is currently not used, due to issues described in the comments below. If these issues are resolved, it
 * may replace the ThreadLocalAMTokenCache.
 */
public class AMTokenCacheImpl implements AMTokenCache {
    private static final String HTTP_REQUEST_KEY = "HTTP.REQUEST";
    private static final String HTTP_RESPONSE_KEY = "HTTP.RESPONSE";
    private static final String AM_SESSION_ID_KEY = "AM_SESSION_ID";

    private final Logger logger;

    @Inject
    AMTokenCacheImpl(Logger logger) {
        this.logger = logger;
    }

    /*
    Called by token validation context invoked as part of the SecurityPolicy enforcement and STS token validation.
     */
    @Override
    public void cacheAMSessionId(RequestData requestData, String sessionId) throws TokenValidationException {
/*        Object messageObject = requestData.getMsgContext();
        if (!(messageObject instanceof Message)) {
            String err = "Unexpected state in AMTokenCacheImpl: " +
                    "Message cannot be obtained from RequestData. MessageContext type: " +
                    (messageObject != null ? messageObject.getClass().getCanonicalName() : null);
            logger.severe(err);
            throw new Exception(err);
        }

        Message message = (Message)messageObject;
        */

        /*
        The code above should work, but the STS UsernameTokenValidator does not set the MessageContext
        in the RequestData. The work-around below was seen on the CXF forum - submitted a bug fix.
        See http://cxf.547215.n5.nabble.com/UsernameTokenValidator-td5707938.html#a5737823
        https://issues.apache.org/jira/browse/CXF-5458
        */
        Message message = PhaseInterceptorChain.getCurrentMessage();

        Object servletRequestObject = message.get(HTTP_REQUEST_KEY);
        if (!(servletRequestObject instanceof ServletRequest)) {
            String err = "Unexpected state in AMTokenCacheImpl: " +
                    "ServletRequest cannot be obtained from Message. Type: " +
                    (servletRequestObject != null ? servletRequestObject.getClass().getCanonicalName() : null);
            logger.error(err);
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, err);
        }
        ((ServletRequest)servletRequestObject).setAttribute(AM_SESSION_ID_KEY, sessionId);
/*
        looks like filters have access to the response as well - so should be able to pull session id out of the request
        Object servletResponseObject = message.get(HTTP_RESPONSE_KEY);
        if ((servletResponseObject ==  null) || !(servletResponseObject instanceof ServletResponse)) {
            String err = "Unexpected state in AMTokenCacheImpl: " +
                    "ServletResponse cannot be obtained from Message. Type: " +
                    (servletResponseObject != null ? servletResponseObject.getClass().getCanonicalName() : null);
            logger.severe(err);
            throw new Exception(err);
        }
        ((ServletResponse)servletResponseObject).setAttribute(AM_SESSION_ID_KEY, sessionId);
*/
    }

    @Override
    public String getAMSessionId(TokenProviderParameters tokenParameters) throws TokenCreationException {
        Object servletRequestObject = tokenParameters.getWebServiceContext().getMessageContext().get(MessageContext.SERVLET_REQUEST);
        if (!(servletRequestObject instanceof ServletRequest)) {
            String message = "Unexpected state in getAMSessionId: did not find ServletRequest, but class: " +
                    (servletRequestObject != null ? servletRequestObject.getClass().getCanonicalName() : null);
            logger.error(message);
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, message);
        }
        Object amSessionIdObject = ((ServletRequest)servletRequestObject).getAttribute(AM_SESSION_ID_KEY);
        if (!(amSessionIdObject instanceof String)) {
            String message = "Unexpected state in getAMSessionId: cached AM session id not string, but class: " +
                    (amSessionIdObject != null ? amSessionIdObject.getClass().getCanonicalName() : null);
            logger.error(message);
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, message);
        }
        return (String)amSessionIdObject;
    }


}
