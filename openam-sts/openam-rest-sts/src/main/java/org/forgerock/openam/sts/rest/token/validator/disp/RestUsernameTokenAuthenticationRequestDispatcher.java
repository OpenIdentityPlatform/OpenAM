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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.token.validator.disp;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.sts.token.validator.disp.TokenAuthenticationRequestDispatcher;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for dispatching authentication requests for RestUsernameToken instances.
 */
public class RestUsernameTokenAuthenticationRequestDispatcher implements TokenAuthenticationRequestDispatcher<RestUsernameToken> {
    private final String crestVersionAuthNService;
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;

    @Inject
    RestUsernameTokenAuthenticationRequestDispatcher(@Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE) String crestVersionAuthNService,
                                                     HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory) {
        this.crestVersionAuthNService = crestVersionAuthNService;
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
    }

    @Override
    public String dispatch(URL url, AuthTargetMapping.AuthTarget target, RestUsernameToken token) throws TokenValidationException {
        try {
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, crestVersionAuthNService);
            headerMap.put(AMSTSConstants.AM_REST_AUTHN_USERNAME_HEADER, new String(token.getUsername(), AMSTSConstants.UTF_8_CHARSET_ID));
            headerMap.put(AMSTSConstants.AM_REST_AUTHN_PASSWORD_HEADER, new String(token.getPassword(), AMSTSConstants.UTF_8_CHARSET_ID));
            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(url)
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.POST)
                    .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new TokenValidationException(responseCode, "Non-200 response from posting Username token " +
                        "to rest authN.");
            } else {
                return connectionResult.getResult();
            }
        } catch (IOException e) {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                    "Exception caught posting UsernameToken to rest authN: " + e, e);
        }
    }
}
