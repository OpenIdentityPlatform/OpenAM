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

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.slf4j.Logger;

/**
 * AMTokenParser implementation. Responsible for parsing out the OpenAM session id from all successful authentication
 * requests.
 */
public class AMTokenParserImpl implements AMTokenParser {
    private static final String TOKEN_ID = "tokenId";
    private final Logger logger;

    @Inject
    AMTokenParserImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String getSessionFromAuthNResponse(String authNResponse) throws TokenValidationException {
        JsonValue responseJson;
        try {
            responseJson = JsonValueBuilder.toJsonValue(authNResponse);
        } catch (JsonException e) {
            String message = "Exception caught getting the text of the json authN response: " + e;
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, message, e);
        }
        JsonValue sessionIdJsonValue = responseJson.get(TOKEN_ID);
        if (!sessionIdJsonValue.isString()) {
            String message = "REST authN response does not contain " + TOKEN_ID + " string entry. The obtained entry: "
                    + sessionIdJsonValue.toString() + "; The response: " + responseJson.toString();
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, message);
        }
        return sessionIdJsonValue.asString();
    }
}
