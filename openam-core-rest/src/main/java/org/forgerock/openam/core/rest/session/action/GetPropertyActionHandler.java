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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Collections;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
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
 * Handles 'getProperty' actions. If a field is requested, return only that field. If no field is
 * specified, return the key/value of all whitelisted fields.
 *
 * REQUEST: { 'properties' : [ 'property1', 'property2'] }
 * RESPONSE: { 'property1' : 'value1', 'property2' : 'value2' }
 */
public class GetPropertyActionHandler implements ActionHandler {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);


    private final SessionPropertyWhitelist sessionPropertyWhitelist;
    private final SessionResourceUtil sessionResourceUtil;

    /**
     * Constructs a GetPropertyActionHandler instance
     *
     * @param sessionPropertyWhitelist An instance of the SessionPropertyWhitelist.
     * @param sessionResourceUtil An instance of the session resource manager.
     */
    public GetPropertyActionHandler(SessionPropertyWhitelist sessionPropertyWhitelist,
            SessionResourceUtil sessionResourceUtil) {
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
        this.sessionResourceUtil = sessionResourceUtil;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(final String tokenId, final Context context,
            final ActionRequest request) {

        final JsonValue result = json(object());
        try {
            final SSOToken caller = context.asContext(SSOTokenContext.class).getCallerSSOToken();
            final String realm = context.asContext(RealmContext.class).getResolvedRealm();
            final SSOToken target = sessionResourceUtil.getTokenWithoutResettingIdleTime(tokenId);

            if (request.getContent() == null || request.getContent().get(SessionResource.KEYWORD_PROPERTIES).isNull()) {
                for (String property : sessionPropertyWhitelist.getAllListedProperties(realm)) {
                    final String value = target.getProperty(property);
                    result.add(property, value == null ? "" : value);
                }
            } else {
                for (String requestedResult : request.getContent().get(SessionResource.KEYWORD_PROPERTIES).asSet(String.class)) {
                    if (sessionPropertyWhitelist.isPropertyListed(caller, realm, Collections.singleton(requestedResult))) {
                        final String value = target.getProperty(requestedResult);
                        result.add(requestedResult, value == null ? "" : value);
                    } else {
                        LOGGER.warning("User {} requested property {} on {} to get which was not whitelisted or "
                                + "was protected.", caller.getPrincipal(), requestedResult, target.getPrincipal());
                        return new ForbiddenException().asPromise();
                    }
                }

            }

        } catch (SSOException e) {
            LOGGER.message("Unable to read session property due to unreadable SSOToken", e);
        } catch (DelegationException e) {
            LOGGER.message("Unable to read session property due to delegation match internal error", e);
            return new InternalServerErrorException().asPromise();
        }

        return newResultPromise(newActionResponse(result));
    }
}
