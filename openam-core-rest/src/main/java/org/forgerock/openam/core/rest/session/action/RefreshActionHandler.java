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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.session.SessionResourceUtil;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 /**
 * Handler for 'refresh' action
 */
public class RefreshActionHandler implements ActionHandler {

    private SSOTokenManager SSOTokenManager;
    private SessionResourceUtil sessionResourceUtil;

    /**
     * Constructs a RefreshActionHandler instance
     *
     * * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param sessionResourceUtil An instance of the SessionResourceUtil.
     */
    public RefreshActionHandler(SSOTokenManager SSOTokenManager,
            SessionResourceUtil sessionResourceUtil) {
        this.SSOTokenManager = SSOTokenManager;
        this.sessionResourceUtil = sessionResourceUtil;
    }


    @Override
    public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context, ActionRequest request) {
        JsonValue content;
        try {
            SSOToken ssoToken = SSOTokenManager.createSSOToken(tokenId);
            content = sessionResourceUtil.jsonValueOf(ssoToken);
        } catch (SSOException | IdRepoException e) {
            content = sessionResourceUtil.invalidSession();
        }
        return newResultPromise(newActionResponse(content));
    }
}
