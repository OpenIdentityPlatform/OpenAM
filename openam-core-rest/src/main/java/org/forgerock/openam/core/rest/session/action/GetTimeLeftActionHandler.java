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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.session.action;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.session.SessionResourceUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Handler for 'getMaxTime' action - from CREST 12.0.0 onwards this means 'get remaining session time'.
 */
public class GetTimeLeftActionHandler implements ActionHandler {

    private static final String MAX_TIME = "maxtime";

    private SessionResourceUtil sessionResourceUtil;

    /**
     * Constructs a GetTimeLeftActionHandler instance
     *
     * @param sessionResourceUtil An instance of the session resource manager
     */
    public GetTimeLeftActionHandler(SessionResourceUtil sessionResourceUtil) {
        this.sessionResourceUtil = sessionResourceUtil;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
            ActionRequest request) {
        return newResultPromise(newActionResponse(getMaxTime(tokenId)));
    }

    /**
     * Using the token id specified by the invoker, find the token and if valid, return its remaining life in
     * seconds.
     *
     * @param tokenId The SSO Token Id.
     * @return jsonic representation of the number of seconds of remaining life, or a representation of -1 if
     * invalid.
     */
    private JsonValue getMaxTime(String tokenId) {

        long maxTime = -1;
        try {
            SSOToken theToken = sessionResourceUtil.getTokenWithoutResettingIdleTime(tokenId);
            maxTime = theToken.getTimeLeft();
        } catch (SSOException ignored) {
        }
        return json(object(field(MAX_TIME, maxTime)));
    }
}
