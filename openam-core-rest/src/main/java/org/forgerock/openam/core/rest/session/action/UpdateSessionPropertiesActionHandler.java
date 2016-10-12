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

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.util.Map;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.session.SessionResourceUtil;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Handler for 'updateSessionProperties' action.
 */
public class UpdateSessionPropertiesActionHandler extends AbstractSessionPropertiesActionHandler {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private final SessionPropertyWhitelist sessionPropertyWhitelist;

    /**
     * Constructs a UpdateSessionPropertiesActionHandler instance
     *
     * @param sessionPropertyWhitelist An instance of the sessionPropertyWhitelist.
     * @param sessionResourceUtil An instance of SessionResourceUtil.
     */
    public UpdateSessionPropertiesActionHandler(SessionPropertyWhitelist sessionPropertyWhitelist,
            SessionResourceUtil sessionResourceUtil) {
        super(sessionPropertyWhitelist, sessionResourceUtil);
        this.sessionPropertyWhitelist = sessionPropertyWhitelist;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context, ActionRequest request) {
        JsonValue result;
        try {
            SSOToken target = getToken(tokenId);
            JsonValue content = request.getContent();
            ensureUpdatePermitted(context, content, target);

            for (Map.Entry<String, String> entry : content.asMap(String.class).entrySet()) {
                target.setProperty(entry.getKey(), entry.getValue());
            }
            result = getSessionProperties(tokenId);
        } catch (BadRequestException | ForbiddenException e) {
            return e.asPromise();
        } catch (SSOException | IdRepoException e) {
            LOGGER.message("Unable to set session property due to unreadable SSOToken", e);
            return new BadRequestException().asPromise();
        } catch (DelegationException e) {
            LOGGER.message("Unable to read session property due to delegation match internal error", e);
            return new InternalServerErrorException().asPromise();
        }
        return newResultPromise(newActionResponse(result));
    }

    /**
     * Ensures the update is permitted by checking the request has valid contents.
     * Update is permitted only if the request contains all the white listed properties
     *
     * @param context The context,
     * @param content The request content.
     * @param target  The target session SSOToken
     * @throws SSOException When the SSOToken in invalid
     * @throws BadRequestException When the request has not content
     * @throws DelegationException When is whitelisted check fails
     * @throws ForbiddenException When the content in the request does not match whitelisted properties
     */
    private void ensureUpdatePermitted(Context context, JsonValue content, SSOToken target) throws SSOException,
            BadRequestException, DelegationException, ForbiddenException, IdRepoException {

        SSOToken caller = context.asContext(SSOTokenContext.class).getCallerSSOToken();
        String realm = getTargetRealm(target);

        try {
            if (content == null || content.isNull() || content.asMap(String.class).size() == 0) {
                LOGGER.warning("User {} requested with an empty values.", caller.getPrincipal());
                throw new BadRequestException();
            }
        } catch (JsonValueException e) {
            LOGGER.warning("User {} requested with no property value pairs", caller.getPrincipal());
            throw new BadRequestException();
        }

        Map<String, String> entrySet = content.asMap(String.class);
        if (!sessionPropertyWhitelist.getAllListedProperties(realm).equals(entrySet.keySet())
                || !sessionPropertyWhitelist.isPropertyMapSettable(caller, entrySet)) {
            LOGGER.warning("User {} requested property/ies {} to set on {} which was not whitelisted.",
                    caller.getPrincipal(), target.getPrincipal(), entrySet.toString());
            throw new ForbiddenException();
        }
    }
}
