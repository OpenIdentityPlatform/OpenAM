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
import static org.forgerock.openam.utils.CollectionUtils.newList;
import static org.forgerock.util.promise.Promises.newResultPromise;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.session.SessionResource;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Handles 'getPropertyNames' actions.
 *
 * REQUEST:
 * RESPONSE: { 'properties' : [ 'property1', 'property2' ] }
 */
public class GetPropertyNamesActionHandler implements ActionHandler {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private final SessionPropertyWhitelist sessionPropertyWhitelist;

    /**
     * Constructs a GetPropertyNamesActionHandler instance
     *
     * @param sessionPropertyWhitelist An instance of the SessionPropertyWhitelist.
     */
    public GetPropertyNamesActionHandler(SessionPropertyWhitelist sessionPropertyWhitelist) {
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(final String tokenId, final Context context,
            final ActionRequest request) {
        try {
            final SSOToken caller = context.asContext(SSOTokenContext.class).getCallerSSOToken();
            final String realm = context.asContext(RealmContext.class).getRealm().asPath();
            return newResultPromise(newActionResponse(json(object(field(SessionResource.KEYWORD_PROPERTIES,
                    newList(sessionPropertyWhitelist.getAllListedProperties(realm)))))));
        } catch (Exception e) {
            LOGGER.message("Unable to read all whitelisted session properties.", e);
        }
        return new InternalServerErrorException().asPromise();
    }
}
