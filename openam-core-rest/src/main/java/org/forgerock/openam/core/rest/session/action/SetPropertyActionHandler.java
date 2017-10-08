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

import java.util.Map;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.session.SessionResource;
import org.forgerock.openam.core.rest.session.SessionResourceUtil;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Handles 'setProperty' actions.
 *
 * REQUEST: { 'property1' : 'value1', 'property2' : 'value2' }
 * RESPONSE: { 'success' : true }
 */
public class SetPropertyActionHandler implements ActionHandler {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private final SessionPropertyWhitelist sessionPropertyWhitelist;
    private final SessionResourceUtil sessionResourceUtil;

    /**
     * Constructs a SetPropertyActionHandler instance
     *
     * @param sessionPropertyWhitelist An instance of SessionPropertyWhitelist.
     * @param sessionResourceUtil An instance of the session resource manager
     */
    public SetPropertyActionHandler(SessionPropertyWhitelist sessionPropertyWhitelist,
            SessionResourceUtil sessionResourceUtil) {
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
        this.sessionResourceUtil = sessionResourceUtil;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(final String tokenId, final Context context,
            final ActionRequest request) {
        try {
            final SSOToken caller = context.asContext(SSOTokenContext.class).getCallerSSOToken();
            final String realm = context.asContext(RealmContext.class).getRealm().asPath();
            final SSOToken target = sessionResourceUtil.getTokenWithoutResettingIdleTime(tokenId);

            if (request.getContent() == null || request.getContent().isNull() ||
                    request.getContent().asMap(String.class).size() == 0) {
                return new BadRequestException().asPromise();
            }

            final Map<String, String> entrySet = request.getContent().asMap(String.class);

            if (sessionPropertyWhitelist.isPropertyListed(caller, realm, entrySet.keySet()) &&
                    sessionPropertyWhitelist.isPropertyMapSettable(caller, entrySet)) {
                for (Map.Entry<String, String> entry : request.getContent().asMap(String.class).entrySet()) {
                    target.setProperty(entry.getKey(), entry.getValue());
                }
            } else {
                LOGGER.warning("User {} requested property/ies {} to set on {} which was not whitelisted.",
                        caller.getPrincipal(), target.getPrincipal(), entrySet.toString());
                return new ForbiddenException().asPromise();
            }
        } catch (SSOException e) {
            LOGGER.message("Unable to set session property due to unreadable SSOToken", e);
            return newResultPromise(newActionResponse(json(object(field(SessionResource.KEYWORD_SUCCESS, false)))));
        } catch (DelegationException e) {
            LOGGER.message("Unable to read session property due to delegation match internal error", e);
            return new InternalServerErrorException().asPromise();
        }

        return newResultPromise(newActionResponse(json(object(field(SessionResource.KEYWORD_SUCCESS, true)))));
    }
}
