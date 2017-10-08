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
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.session.SessionResourceUtil;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

 /**
 * Handler for 'validate' action
 */
public class ValidateActionHandler implements ActionHandler {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private SSOTokenManager ssoTokenManager;
    private SessionResourceUtil sessionResourceUtil;

    /**
     * Constructs a ValidateActionHandler instance
     *
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param sessionResourceUtil An instance of the SessionResourceUtil.
     */
    public ValidateActionHandler(SSOTokenManager ssoTokenManager,
            SessionResourceUtil sessionResourceUtil) {
        this.ssoTokenManager = ssoTokenManager;
        this.sessionResourceUtil = sessionResourceUtil;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
            ActionRequest request) {
        return newResultPromise(newActionResponse(validateSession(tokenId)));
    }

    /**
     * Will validate that the specified SSO Token Id is valid or not.
     * <br/>
     * Example response:
     * { "valid": true, "uid": "demo", "realm": "/subrealm" }
     * <br/>
     * If there is any problem getting or validating the token which causes an exception the json response will be
     * false. In addition if the token is expired then the json response will be set to true. Otherwise it will be
     * set to true.
     *
     * @param tokenId The SSO Token Id.
     * @return The json response of the validation.
     */
    private JsonValue validateSession(final String tokenId) {

        try {
            final SSOToken ssoToken = ssoTokenManager.createSSOToken(tokenId);
            return validateSession(ssoToken);
        } catch (SSOException e) {
            if (LOGGER.errorEnabled()) {
                LOGGER.error("SessionResource.validateSession() :: Unable to validate token " + tokenId, e);
            }
            return sessionResourceUtil.invalidSession();
        }
    }

    /**
     * Will validate that the specified SSOToken is valid or not.
     * <br/>
     * Example response:
     * { "valid": true, "uid": "demo", "realm": "/subrealm" }
     * <br/>
     * If there is any problem getting or validating the token which causes an exception the json response will be
     * false. In addition if the token is expired then the json response will be set to false. Otherwise it will be
     * set to true.
     *
     * @param ssoToken The SSO Token.
     * @return The json response of the validation.
     */
    private JsonValue validateSession(final SSOToken ssoToken) {
        try {
            if (!ssoTokenManager.isValidToken(ssoToken)) {
                if (LOGGER.messageEnabled()) {
                    LOGGER.message("SessionResource.validateSession() :: Session validation for token, " +
                            ssoToken.getTokenID() + ", returned false.");
                }
                return sessionResourceUtil.invalidSession();
            }

            if (LOGGER.messageEnabled()) {
                LOGGER.message("SessionResource.validateSession() :: Session validation for token, " +
                        ssoToken.getTokenID() + ", returned true.");
            }
            final AMIdentity identity = sessionResourceUtil.getIdentity(ssoToken);
            return json(object(field(sessionResourceUtil.VALID, true), field("uid", identity.getName()),
                    field("realm", sessionResourceUtil.convertDNToRealm(identity.getRealm()))));
        } catch (SSOException e) {
            if (LOGGER.errorEnabled()) {
                LOGGER.error("SessionResource.validateSession() :: Session validation for token, " +
                        ssoToken.getTokenID() + ", failed to return.", e);
            }
            return sessionResourceUtil.invalidSession();
        } catch (IdRepoException e) {
            if (LOGGER.errorEnabled()) {
                LOGGER.error("SessionResource.validateSession() :: Session validation for token, " +
                        ssoToken.getTokenID() + ", failed to return.", e);
            }
            return sessionResourceUtil.invalidSession();
        }
    }
}
