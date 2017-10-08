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
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.rest.session.SessionResourceUtil;
import org.forgerock.openam.dpro.session.PartialSessionFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Handler for 'getSessionInfo' action.
 */
public class GetSessionInfoActionHandler implements ActionHandler {

    private final SessionResourceUtil sessionResourceUtil;
    private final PartialSessionFactory partialSessionFactory;

    /**
     * Constructs a GetSessionInfoActionHandler instance.
     *
     * @param sessionResourceUtil An instance of SessionResourceUtil.
     * @param partialSessionFactory An instance of PartialSessionFactory.
     */
    public GetSessionInfoActionHandler(SessionResourceUtil sessionResourceUtil,
            PartialSessionFactory partialSessionFactory) {
        this.sessionResourceUtil = sessionResourceUtil;
        this.partialSessionFactory = partialSessionFactory;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context, ActionRequest request) {
        JsonValue content;
        try {
            SSOToken ssoToken = sessionResourceUtil.getTokenWithoutResettingIdleTime(tokenId);
            content = partialSessionFactory.fromSSOToken(ssoToken).asJson();
        } catch (SSOException e) {
            content = sessionResourceUtil.invalidSession();
        }
        return newResultPromise(newActionResponse(content));
    }
}
