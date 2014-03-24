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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.reslet;

import org.forgerock.oauth2.core.ClientAuthentication;
import org.forgerock.oauth2.core.InvalidClientException;
import org.forgerock.oauth2.core.InvalidRequestException;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;

import static org.forgerock.oauth2.reslet.RestletUtils.getAttribute;

/**
 * @since 12.0.0
 */
public abstract class ClientCredentialsExtractor {

    public ClientAuthentication extract(final Request request) throws InvalidClientException, InvalidRequestException {

        String clientId = getAttribute(request, "client_id");
        String clientSecret = getAttribute(request, "client_secret");

        if (request.getChallengeResponse() != null && clientId != null) {
            //TODO log
            throw new InvalidRequestException("Client authentication failed");
        }

        if (request.getChallengeResponse() != null) {
            final ChallengeResponse challengeResponse = request.getChallengeResponse();

            clientId = challengeResponse.getIdentifier();
            clientSecret = "";
            if (challengeResponse.getSecret() != null && challengeResponse.getSecret().length > 0) {
                clientSecret = String.valueOf(request.getChallengeResponse().getSecret());
            }

        }

        if (clientId == null || clientId.isEmpty()) {
            //TODO log
            throw new InvalidClientException("Client authentication failed");
        }

        return createClientAuthentication(request, clientId, clientSecret);
    }

    protected abstract ClientAuthentication createClientAuthentication(final Request request, final String clientId,
            final String clientSecret);
}
