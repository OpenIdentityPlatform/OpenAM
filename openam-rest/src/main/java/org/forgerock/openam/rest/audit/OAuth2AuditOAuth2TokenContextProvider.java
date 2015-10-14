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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.rest.audit;

import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.core.CoreWrapper;

/**
 * A abstract provider giving some functionality common to OAuth2-based providers.
 *
 * @since 13.0.0
 */
public abstract class OAuth2AuditOAuth2TokenContextProvider implements OAuth2AuditContextProvider {

    protected String getTrackingIdFromToken(JsonValue accessToken) {
        return getAccessTokenProperty(OAuth2Constants.CoreTokenParams.AUDIT_TRACKING_ID, accessToken);
    }

    protected String getAccessTokenProperty(String propertyName, JsonValue accessToken) {
        if (!accessToken.isDefined(propertyName)) {
            return null;
        }

        if (accessToken.get(propertyName).isCollection()) {
            return (String) accessToken.get(propertyName).asList().get(0);
        }

        if (accessToken.get(propertyName).isString()) {
            accessToken.get(propertyName).toString(); // TODO: Return this value?
        }

        return null;
    }

    protected String getUserIdFromToken(JsonValue accessToken) {
        String username = getAccessTokenProperty(OAuth2Constants.CoreTokenParams.USERNAME, accessToken);
        String realm = getAccessTokenProperty(OAuth2Constants.CoreTokenParams.REALM, accessToken);

        return getUserIdFromUsernameAndRealm(username, realm);
    }

    protected String getUserIdFromUsernameAndRealm(String username, String realm) {
        if (username == null || realm == null) {
            return null;
        }

        CoreWrapper cw = new CoreWrapper();
        AMIdentity identity = cw.getIdentity(username, realm);
        if (identity != null) {
            return identity.getUniversalId();
        }
        return null;
    }
}
