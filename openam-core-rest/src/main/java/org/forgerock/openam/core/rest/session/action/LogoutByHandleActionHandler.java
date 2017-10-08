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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.dpro.session.InvalidSessionIdException;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.SessionTimedOutException;
import com.sun.identity.shared.debug.Debug;

/**
 * Handler for "logoutByHandle" action.
 */
public class LogoutByHandleActionHandler implements ActionHandler {

    private static final Debug DEBUG = Debug.getInstance("CoreSystem");
    private static final String SESSION_HANDLES = "sessionHandles";

    /**
     * Destroys the sessions using the provided session handles.
     *
     * @param tokenId Not used by this implementation.
     * @param context The CREST context.
     * @param request The current ActionRequest.
     * @return The response that details which sessions were successfully destroyed.
     */
    @Override
    public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context, ActionRequest request) {
        final JsonValue json = request.getContent();
        if (!json.isDefined(SESSION_HANDLES) && !json.get(SESSION_HANDLES).isList()) {
            return new BadRequestException("The \"" + SESSION_HANDLES + "\" field is not defined in the request or it's"
                    + " not a JSON array").asPromise();
        }

        SSOTokenContext ssoTokenContext = context.asContext(SSOTokenContext.class);
        Session requester = ssoTokenContext.getCallerSession();

        final List<String> sessionHandles = json.get(SESSION_HANDLES).asList(String.class);
        Map<String, Object> map = new HashMap<>();
        for (String sessionHandle : sessionHandles) {
            try {
                requester.destroySession(new Session(new SessionID(sessionHandle)));
                map.put(sessionHandle, true);
            } catch (SessionTimedOutException | InvalidSessionIdException se) {
                map.put(sessionHandle, true);
            } catch (SessionException se) {
                DEBUG.warning("Unable to invalidate session based on session handle", se);
                map.put(sessionHandle, false);
            }
        }

        return newResultPromise(newActionResponse(json(object(field("result", map)))));
    }
}
