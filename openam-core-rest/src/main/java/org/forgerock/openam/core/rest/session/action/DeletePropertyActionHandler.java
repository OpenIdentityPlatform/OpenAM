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

import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
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
 * Handles 'deleteProperty' actions.
 *
 * REQUEST: { 'properties' : [ 'property1', 'property2'] }
 * RESPONSE: { 'success' : true }
 */
public class DeletePropertyActionHandler implements ActionHandler {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private final SessionPropertyWhitelist sessionPropertyWhitelist;
    private final SessionResourceUtil sessionResourceUtil;

    /**
     * Constructs a DeletePropertyActionHandler instance
     *
     * @param sessionPropertyWhitelist An instance of the sessionPropertyWhitelist.
     * @param  sessionResourceUtil An instance of SessionResourceUtil.
     */
    public DeletePropertyActionHandler(SessionPropertyWhitelist sessionPropertyWhitelist,
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

            JsonValue content = request.getContent().get(SessionResource.KEYWORD_PROPERTIES);

            if (content == null || content.isNull()) {
                return new BadRequestException().asPromise(); //no properties = bad request
            }

            final Set<String> propSet = request.getContent().get(SessionResource.KEYWORD_PROPERTIES).asSet(String.class);

            if (sessionPropertyWhitelist.isPropertyListed(caller, realm, propSet) &&
                    sessionPropertyWhitelist.isPropertySetSettable(caller, propSet)) {
                for (String entry : propSet) {
                    //there is no "delete" function - we can't store null in the property map so blank it
                    target.setProperty(entry, "");
                }
            } else {
                LOGGER.message("User {} requested property/ies {} on {} to delete which was not whitelisted.",
                        caller.getPrincipal(), propSet.toString(), target.getPrincipal());
                return new ForbiddenException().asPromise();
            }

        } catch (SSOException e) {
            LOGGER.message("Unable to delete session property due to unreadable SSOToken", e);
            return newResultPromise(newActionResponse(json(object(field(SessionResource.KEYWORD_SUCCESS, false)))));
        } catch (DelegationException e) {
            LOGGER.message("Unable to read session property due to delegation match internal error", e);
            return new InternalServerErrorException().asPromise();
        }

        return newResultPromise(newActionResponse(json(object(field(SessionResource.KEYWORD_SUCCESS, true)))));
    }
}
