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

/**
 * In order to issue SAML2 assertions with SenderVouches SubjectConfirmation, a STS must be consumed in a gateway deployment,
 * in which the direct STS client (the gateway) is acting on-behalf-of the original client. To model such scenarios, WS-Trust defined the
 * OnBehalfOf and ActAs elements in the RequestSecurityToken request, which assert the original client. For more details, see
 * http://owulff.blogspot.com/2012/03/saml-sender-vouches-use-case.html
 * http://coheigea.blogspot.com/2011/08/ws-trust-14-support-in-cxf.html
 *
 * When the CXF run-time encounters these elements in a Issue Operation, it:
 * 1. runs through the TokenValidators registered with the IssueOperation to validate the ActAs/OnBehalfOf token (in 3.0.3 code-base, this is true;
 * in the 2.7.8 code-base, TokenValidators are called only for OnBehalfOf tokens, not ActAs tokens. See
 * TokenIssueOperation#handleDelegationToken (3.0.3 code-base), and AbstractOperation#performDelegationHandling (2.7.8 code-base)
 * for details). If no registered TokenValidators can handle the token, then the request is NOT failed.
 * 2. runs through the registered TokenDelegationHandler instances, to see if they approve the delegation relationship. If
 * no TokenDelegationHandlers are registered, or if none approve the delegation, then the Issue operation is failed.
 *
 * This class will be registered with the TokenIssueOperation if the STS instance is configured to support delegation relationships,
 * and the end-user did not specify a set of custom TokenDelegationHandler instances. This default TokenDelegationHandler will
 * approve of the TokenDelegation relationship ONLY if the principal set in the TokenDelegationParameters is non-null, as this
 * will be the case only when a TokenValidator was found which could successfully validate the token asserting the delegated
 * subject.
 *
 * Each Soap STS instance will be configured with a set of tokens for which it supports delegation relationships. TokenValidators
 * will be plugged-in for each of these. This default TokenDelegationHandler will insure
 * that delegation relationships are approved iff the associated token can be successfully validated. If end-users wish
 * to do additional work, then they can register their own set of TokenDelegationHandler instances.
 */
public class DefaultTokenDelegationHandler implements TokenDelegationHandler {
    @Override
    public boolean canHandleToken(ReceivedToken delegateTarget) {
        return true;
    }

    @Override
    public TokenDelegationResponse isDelegationAllowed(TokenDelegationParameters tokenParameters) {
        final TokenDelegationResponse tokenDelegationResponse = new TokenDelegationResponse();
        tokenDelegationResponse.setDelegationAllowed(tokenParameters.getTokenPrincipal() != null);
        return tokenDelegationResponse;
    }
}
