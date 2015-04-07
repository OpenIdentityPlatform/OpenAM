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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.token.delegation;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.apache.cxf.sts.token.delegation.TokenDelegationParameters;
import org.apache.cxf.sts.token.delegation.TokenDelegationResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.slf4j.Logger;

import java.util.Map;

/**
 * If a published soap-sts instance supports OnBehalfOf/ActAs tokens (i.e. delegation relationships), then the associated
 * soap-sts configuration supports the ability to specify a custom org.apache.cxf.sts.token.delegation.TokenDelegationHandler
 * implementation(s) which are responsible for approving/validating the ActAs/OnBehalfOf token. If no TokenValidator implementations
 * are specified to perform the validation, the custom TokenDelegationHandler for each type of delegated token must validate
 * the ActAs/OnBehalfOf token, and return a reference to the OpenAM session id corresponding to this ActAs/OnBehalfOf token,
 * so that it can be passed to the TokenGenerationService. This wrapper class takes this session id, and places it in the
 * ThreadLocalAMTokenCache, where it can be referenced by the TokenGenerationService client.
 *
 * This class is not Injected by guice, but rather instantiated by the TokenDelegationHandlersProvider class.
 */
public class CustomDelegationHandlerWrapper implements TokenDelegationHandler {
    private final TokenDelegationHandler customHandler;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final boolean delegationValidatorsSpecified;
    private final Logger logger;

    public CustomDelegationHandlerWrapper(TokenDelegationHandler customHandler, ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                   boolean delegationValidatorsSpecified, Logger logger) {
        this.customHandler = customHandler;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.delegationValidatorsSpecified = delegationValidatorsSpecified;
        this.logger = logger;
    }

    @Override
    public boolean canHandleToken(ReceivedToken delegateTarget) {
        return customHandler.canHandleToken(delegateTarget);
    }

    @Override
    public TokenDelegationResponse isDelegationAllowed(TokenDelegationParameters tokenParameters) {
        final TokenDelegationResponse tokenDelegationResponse = customHandler.isDelegationAllowed(tokenParameters);
        if (tokenDelegationResponse.isDelegationAllowed()) {
            final Map<String, Object> additionalProperties = tokenDelegationResponse.getAdditionalProperties();
            if ((additionalProperties != null) &&
                    additionalProperties.get(AMSTSConstants.CUSTOM_DELEGATION_HANDLER_AM_SESSION_ID) instanceof String) {
                boolean invalidateInterimSession = true;
                Object invalidateSessionObject = additionalProperties.get(AMSTSConstants.CUSTOM_DELEGATION_HANDLER_INVALIDATE_AM_SESSION);
                if (invalidateSessionObject instanceof Boolean) {
                    invalidateInterimSession = (Boolean)invalidateSessionObject;
                }
                threadLocalAMTokenCache.cacheDelegatedAMSessionId(
                        (String)additionalProperties.get(AMSTSConstants.CUSTOM_DELEGATION_HANDLER_AM_SESSION_ID),
                        invalidateInterimSession);
            } else {
                if (!delegationValidatorsSpecified) {
                    String message = "In a custom TokenDelegationHandler, the delegated token is allowed, no delegation " +
                            "validators have been specified, and the AM Session Id was not specified in the " +
                            "DelegationHandlerResponse#getAdditionalProperties keyed by "
                            + AMSTSConstants.CUSTOM_DELEGATION_HANDLER_AM_SESSION_ID + ". This means the " +
                            "TokenGenerationService cannot issue an assertion corresponding to the delegated token.";
                    logger.error(message);
                    throw new AMSTSRuntimeException(ResourceException.UNAVAILABLE, message);
                }
            }
        }
        return tokenDelegationResponse;
    }
}
