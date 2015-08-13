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

package org.forgerock.openam.sts.rest.operation.cancel;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenCancellationException;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.rest.operation.TokenRequestMarshaller;
import org.forgerock.openam.sts.rest.token.canceller.RestIssuedTokenCanceller;
import org.forgerock.openam.sts.rest.token.canceller.RestIssuedTokenCancellerParameters;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenCancellationInvocationState;

import javax.inject.Named;
import java.util.HashSet;
import java.util.Set;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * @see IssuedTokenCancelOperation
 */
public class IssuedTokenCancelOperationImpl implements IssuedTokenCancelOperation {
    private static final String RESULT = "result";
    private final TokenRequestMarshaller tokenRequestMarshaller;
    private final Set<RestIssuedTokenCanceller> tokenCancellers;

    /*
    No @Inject as instance created by @Provider - see RestSTSInstanceModule#getIssuedTokenCancelOperation
     */
    public IssuedTokenCancelOperationImpl(IssuedTokenCancellerFactory issuedTokenCancellerFactory,
                                            TokenRequestMarshaller tokenRequestMarshaller,
                                            @Named(AMSTSConstants.ISSUED_TOKEN_TYPES) Set<TokenTypeId> cancelledTokenTypes)
            throws STSInitializationException {
        this.tokenRequestMarshaller = tokenRequestMarshaller;

        if (cancelledTokenTypes.isEmpty()) {
            throw new IllegalArgumentException("No supported cancelled token types specified.");
        }
        tokenCancellers = new HashSet<>();
        for (TokenTypeId tokenType : cancelledTokenTypes) {
            tokenCancellers.add(issuedTokenCancellerFactory.getTokenCanceller(tokenType));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonValue cancelToken(RestSTSTokenCancellationInvocationState invocationState) throws TokenCancellationException, TokenMarshalException {
        TokenTypeId tokenTypeId = tokenRequestMarshaller.getTokenType(invocationState.getCancelledTokenState());
        RestIssuedTokenCancellerParameters<?> cancellerParameters =
                tokenRequestMarshaller.buildIssuedTokenCancellerParameters(invocationState.getCancelledTokenState());

        for (RestIssuedTokenCanceller tokenCanceller : tokenCancellers) {
            if (tokenCanceller.canCancelToken(tokenTypeId)) {
                tokenCanceller.cancelToken(cancellerParameters);
                return json(object(field(RESULT, tokenTypeId.getId() + " token cancelled successfully.")));

            }
        }
        throw new TokenCancellationException(ResourceException.BAD_REQUEST, "No IssuedTokenCancellers available for " +
                "token type: " + tokenTypeId.getId() + ". Does this sts issue tokens of the specified type?");
    }
}
