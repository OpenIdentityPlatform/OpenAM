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

package org.forgerock.openam.sts.soap.token.canceller;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.canceller.TokenCanceller;
import org.apache.cxf.sts.token.canceller.TokenCancellerParameters;
import org.apache.cxf.sts.token.canceller.TokenCancellerResponse;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenCancellationException;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;

/**
 * Base class for TokenCanceller implementations. Implements common functionality, such as consumption of the
 * TokenService
 */
abstract class TokenCancellerBase implements TokenCanceller {
    protected final boolean verifyProofOfPossession = false;
    private final TokenServiceConsumer tokenServiceConsumer;
    private final SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
    protected final CTSTokenIdGenerator ctsTokenIdGenerator;

    protected TokenCancellerBase(TokenServiceConsumer tokenServiceConsumer, SoapSTSAccessTokenProvider soapSTSAccessTokenProvider,
                                       CTSTokenIdGenerator ctsTokenIdGenerator) {
        this.tokenServiceConsumer = tokenServiceConsumer;
        this.soapSTSAccessTokenProvider = soapSTSAccessTokenProvider;
        this.ctsTokenIdGenerator = ctsTokenIdGenerator;
    }

    @Override
    public void setVerifyProofOfPossession(boolean verifyProofOfPossession) {
        if (verifyProofOfPossession) {
            throw new IllegalArgumentException("TokenCanceller implementations do not curently support " +
                    "proof-of-possession of to-be-cancelled tokens.");
        }
    }

    @Override
    public TokenCancellerResponse cancelToken(TokenCancellerParameters tokenParameters) {
        TokenCancellerResponse response = new TokenCancellerResponse();
        ReceivedToken cancelTarget = tokenParameters.getToken();
        cancelTarget.setState(ReceivedToken.STATE.VALID);
        response.setToken(cancelTarget);
        String tokenServiceConsumptionToken = null;
        try {
            final String tokenId = generateIdFromValidateTarget(cancelTarget);
            tokenServiceConsumptionToken = getTokenServiceConsumptionToken();
            tokenServiceConsumer.cancelToken(tokenId, tokenServiceConsumptionToken);
            cancelTarget.setState(ReceivedToken.STATE.CANCELLED);
            return response;
        } catch (TokenCancellationException e) {
            throw new STSException("Exception caught validating issued token: " + e.getMessage(), e);
        } finally {
            if (tokenServiceConsumptionToken != null) {
                invalidateTokenGenerationServiceConsumptionToken(tokenServiceConsumptionToken);
            }
        }
    }

    private String getTokenServiceConsumptionToken() throws TokenCancellationException {
        try {
            return soapSTSAccessTokenProvider.getAccessToken();
        } catch (ResourceException e) {
            throw new TokenCancellationException(e.getCode(), e.getMessage(), e);
        }
    }

    private void invalidateTokenGenerationServiceConsumptionToken(String consumptionToken) {
        soapSTSAccessTokenProvider.invalidateAccessToken(consumptionToken);
    }

    protected abstract String generateIdFromValidateTarget(ReceivedToken validateTarget) throws TokenCancellationException;
}
