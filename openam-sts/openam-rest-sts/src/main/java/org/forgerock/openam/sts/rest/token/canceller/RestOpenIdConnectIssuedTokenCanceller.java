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

package org.forgerock.openam.sts.rest.token.canceller;

import com.sun.identity.security.AdminTokenAction;
import org.forgerock.openam.sts.TokenCancellationException;
import org.forgerock.openam.sts.TokenIdGenerationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;

import java.security.AccessController;

/**
 * Implements token cancellation for rest-sts-issued OpenIdConnect tokens.
 */
public class RestOpenIdConnectIssuedTokenCanceller implements RestIssuedTokenCanceller<OpenIdConnectIdToken> {
    protected final CTSTokenIdGenerator ctsTokenIdGenerator;
    protected final TokenServiceConsumer tokenServiceConsumer;

    /**
     * No @Inject as the ctor is called by the IssuedtokenValidatorFactoryImpl
     * @param ctsTokenIdGenerator encapsulation of logic to generate a CTS token id, given a token of a specific type
     * @param tokenServiceConsumer encapsulation of consumption of the TokenService, which ultimately performs token validation
     */
    public RestOpenIdConnectIssuedTokenCanceller(CTSTokenIdGenerator ctsTokenIdGenerator, TokenServiceConsumer tokenServiceConsumer) {
        this.ctsTokenIdGenerator = ctsTokenIdGenerator;
        this.tokenServiceConsumer = tokenServiceConsumer;
    }

    @Override
    public boolean canCancelToken(TokenTypeId tokenType) {
        return TokenType.OPENIDCONNECT.getId().equals(tokenType.getId());
    }

    @Override
    public void cancelToken(RestIssuedTokenCancellerParameters<OpenIdConnectIdToken> cancellerParameters) throws TokenCancellationException {
        tokenServiceConsumer.cancelToken(generateIdFromValidateTarget(cancellerParameters.getInputToken()), getAdminToken());
    }

    private String generateIdFromValidateTarget(OpenIdConnectIdToken validateTarget) throws TokenCancellationException {
        try {
            return ctsTokenIdGenerator.generateTokenId(TokenType.OPENIDCONNECT, validateTarget.getTokenValue());
        } catch (TokenIdGenerationException e) {
            throw new TokenCancellationException(e.getCode(), e.getMessage(), e);
        }
    }

    protected String getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance()).getTokenID().toString();
    }
}
