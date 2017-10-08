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
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.session.SessionResourceUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Handler for 'isActive' action
 */
public class IsActiveActionHandler implements ActionHandler {

    private static final String ACTIVE = "active";

    private SessionResourceUtil sessionResourceUtil;
    private SSOTokenManager ssoTokenManager;

    /**
     * Constructs a IsActiveActionHandler instance
     *
     * @param  ssoTokenManager An instance on the SSOTokenManager.
     * @param sessionResourceUtil An instance of the session resource manager
     */
    public IsActiveActionHandler(SSOTokenManager ssoTokenManager, SessionResourceUtil sessionResourceUtil) {
        this.ssoTokenManager = ssoTokenManager;
        this.sessionResourceUtil = sessionResourceUtil;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
            ActionRequest request) {
        String refresh = request.getAdditionalParameter("refresh");
        return newResultPromise(newActionResponse(isTokenIdValid(tokenId, refresh)));
    }

    /**
     * Figure whether the token id, which has been passed as an argument to the REST call
     * is valid and optionally refresh it.  This is different from validateSession because this,
     * rather inconveniently, requires you to be logged in as admin before this can be invoked.
     *
     * @param tokenId The SSO Token Id.
     * @return a jsonic "true" or "false" depending on whether the token is valid
     */
    private JsonValue isTokenIdValid(String tokenId, String refresh) {
        boolean isActive = false;
        try {
            SSOToken theToken = sessionResourceUtil.getTokenWithoutResettingIdleTime(tokenId);

            isActive = true;
            if (Boolean.valueOf(refresh)) {
                ssoTokenManager.refreshSession(theToken);
            }
        } catch (SSOException ignored) {
        }
        return json(object(field(ACTIVE, isActive)));
    }
}
