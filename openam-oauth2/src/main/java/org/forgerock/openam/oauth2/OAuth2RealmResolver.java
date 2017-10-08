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
 *  Copyright 2016 ForgeRock AS.
 *
 */

package org.forgerock.openam.oauth2;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;

/**
 * Resolves the realm from OAuth2Request or Context
 *
 * This is a helper class to help the CREST and non CREST endpoints to resolve the
 * realm which is used in TokenStore to check if the token is stateless or not.
 *
 *
 * @since 13.5.0
 */
public class OAuth2RealmResolver {


    /**
     * Resolve realm from the request
     *
     * @param request The Request
     * @return The realm
     */
    public String resolveFrom(OAuth2Request request) {
        AccessToken accessToken = request.getToken(AccessToken.class);
        String realm;
        if (accessToken != null) {
            realm = accessToken.getRealm();
        } else {
            realm = request.getParameter(RestletRealmRouter.REALM);
        }
        return realm;
    }

    /**
     * Resolve Realm from the context
     *
     * @param context The context
     * @return The realm
     */
    public String resolveFrom(Context context) {
        Reject.ifFalse(context.containsContext(RealmContext.class), "Must contain a RealmContext cannot be null");
        Reject.ifNull(context, "Context cannot be null");
        return RealmContext.getRealm(context).asPath();
    }

}
