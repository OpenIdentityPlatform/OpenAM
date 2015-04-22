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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.token.validator;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;

import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;

import org.forgerock.openam.sts.token.validator.PrincipalFromSession;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import java.security.Principal;
import org.slf4j.Logger;

/**
 * Instances of this class will be deployed in a published soap-sts instance when the soap-sts instance is configured to
 * support delegated token relationships (ActAs/OnBehalfOf) and OpenAM token types are configured as one of the validated
 * delegated token types.
 *
 * This class validates OpenAM tokens by making a Rest call to OpenAM to obtain the principal corresponding to the session id. Part
 * of establishing this correlation in OpenAM includes determining that the session id is valid. This class is not in the
 * wss package, and is not implemented via classes in the disp and uri packages because it is not explicitly consuming
 * the OpenAM Rest authN context, but rather consumes a standard OpenAM Rest interface to correlate a session id to a
 * principal. In other words, the disp and uri packages have to do with dispatching an invocation against the authN module
 * specified in the AuthTargetMapping state of the published STS instance. Validating an OpenAM session id does not involve
 * consuming REST authN, but rather consuming a standard OpenAM REST endpoint which will take a session id, and return the
 * associated principal, and throw an exception if the session id was invalid.
 *
 * And note that the structure of the OpenAM token is the same as produced by the OpenAMSessionAssertion. In other words,
 * when an OpenAM session id is used to traverse a SecurityPolicy binding specifying an OpenAMSessionAssertion, the OpenAM
 * session id will be encapsulated in a BinarySecurityToken as produced by the OpenAMSessionAssertion class. Likewise, when
 * an OpenAM session id is used in the ActAs/OnBehalfOf in the issue operation (necessary to produce SAML2 SV assertions),
 * then the client will have to use this same format to encapsulate the OpenAM session id. This format is produced by
 * TokenSpecification#openAMSessionTokenSaml2SenderVouches method in the openam-soap-sts-client module.
 *
 */
public class SoapAMTokenValidator implements TokenValidator {
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final PrincipalFromSession principalFromSession;
    private final ValidationInvocationContext validationInvocationContext;
    private final boolean invalidateAMSession;
    private final Logger logger;

    /*
    The lifecycle for this class is controlled by the TokenOperationFactoryImpl, and thus needs no @Inject.
     */
    public SoapAMTokenValidator(ThreadLocalAMTokenCache threadLocalAMTokenCache, PrincipalFromSession principalFromSession,
                                ValidationInvocationContext validationInvocationContext,
                                boolean invalidateAMSession, Logger logger) {
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.principalFromSession = principalFromSession;
        this.validationInvocationContext = validationInvocationContext;
        this.invalidateAMSession = invalidateAMSession;
        this.logger = logger;
    }

    /**
     *
     * @param validateTarget the to-be-validated token
     * @return whether this TokenValidator instance can validate this type of token
     */
    @Override
    public boolean canHandleToken(ReceivedToken validateTarget) {
        Object token = validateTarget.getToken();
        return (token instanceof BinarySecurityTokenType &&
                AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_BST_VALUE_TYPE.equals(((BinarySecurityTokenType)token).getValueType()));
    }

    /**
     *
     * @param validateTarget the to-be-validated token
     * @param realm CXF-STS construct to segment TokenValidator instances. Unused.
     * @return whether this TokenValidator instance can validate this type of token
     */
    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        return canHandleToken(validateTarget);
    }

    /**
     *
     * @param tokenParameters the state necessary for token validation
     * @return an instance of the TokenValidatorResponse class which indicates whether the token was successfully
     * validated.
     */
    @Override
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(ReceivedToken.STATE.INVALID);
        response.setToken(validateTarget);
        try {
            String sessionId = parseSessionIdFromRequest(tokenParameters.getToken());
            Principal principal = principalFromSession.getPrincipalFromSession(sessionId);
            threadLocalAMTokenCache.cacheSessionIdForContext(validationInvocationContext, sessionId, invalidateAMSession);
            response.setPrincipal(principal);
            validateTarget.setState(ReceivedToken.STATE.VALID);
        } catch (Exception e) {
            logger.info("Exception caught obtaining principal from session id: " + e, e);
        }
        return response;
    }

    private String parseSessionIdFromRequest(ReceivedToken receivedToken) throws TokenCreationException {
        Object token = receivedToken.getToken();
        if (token instanceof BinarySecurityTokenType) {
            return ((BinarySecurityTokenType)token).getValue();
        } else {
            String message = "Unexpected state in AMTokenValidator: validated token of unexpected type: " +
                    (token != null ? token.getClass().getCanonicalName() : null);
            logger.error(message);
            throw new TokenCreationException(ResourceException.INTERNAL_ERROR, message);
        }
    }
}
