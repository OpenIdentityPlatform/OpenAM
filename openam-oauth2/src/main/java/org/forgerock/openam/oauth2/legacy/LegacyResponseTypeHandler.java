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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2.legacy;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResponseTypeHandler;
import org.forgerock.openam.oauth2.CookieExtractor;
import org.forgerock.openam.oauth2.provider.ResponseType;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 * Adapter between old {@link ResponseType} and the new {@link ResponseTypeHandler}.
 *
 * @since 12.0.0
*/
@Deprecated
public class LegacyResponseTypeHandler implements ResponseTypeHandler {

    private final ResponseType responseType;
    private final String realm;
    private final String ssoCookieName;
    private final CookieExtractor cookieExtractor;

    public LegacyResponseTypeHandler(ResponseType responseType, String realm, String ssoCookieName,
            CookieExtractor cookieExtractor) {
        this.responseType = responseType;
        this.realm = realm;
        this.ssoCookieName = ssoCookieName;
        this.cookieExtractor = cookieExtractor;
    }

    public Map.Entry<String, org.forgerock.oauth2.core.Token> handle(
            String tokenType, Set<String> scope, String resourceOwnerId, String clientId, String redirectUri,
            String nonce, OAuth2Request request) {

        final Map<String, Object> data = new HashMap<String, Object>();
        data.put(OAuth2Constants.CoreTokenParams.TOKEN_TYPE, tokenType);
        data.put(OAuth2Constants.CoreTokenParams.SCOPE, scope);
        data.put(OAuth2Constants.CoreTokenParams.USERNAME, resourceOwnerId);
        data.put(OAuth2Constants.CoreTokenParams.CLIENT_ID, clientId);
        data.put(OAuth2Constants.CoreTokenParams.REDIRECT_URI, redirectUri);
        data.put(OAuth2Constants.Custom.NONCE, nonce);

        data.put(OAuth2Constants.CoreTokenParams.REALM, realm);

        final HttpServletRequest req = ServletUtils.getRequest(request.<Request>getRequest());
        data.put(OAuth2Constants.Custom.SSO_TOKEN_ID, cookieExtractor.extract(req, ssoCookieName));

        final CoreToken token = responseType.createToken(request.getToken(AccessToken.class), data);
        return new AbstractMap.SimpleEntry<String, org.forgerock.oauth2.core.Token>(responseType.URIParamValue(),
                new LegacyToken(token));
    }

    public OAuth2Constants.UrlLocation getReturnLocation() {
        return OAuth2Constants.UrlLocation.valueOf(responseType.getReturnLocation().toUpperCase());
    }
}
