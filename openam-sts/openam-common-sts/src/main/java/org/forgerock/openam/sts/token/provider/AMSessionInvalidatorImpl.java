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
 * Copyright 2013-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.provider;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@link org.forgerock.openam.sts.token.provider.AMSessionInvalidator}
 */
public class AMSessionInvalidatorImpl implements AMSessionInvalidator {
    private final URL logoutUrl;
    private final String amSessionCookieName;
    private final String crestVersionSessionService;
    private final HttpURLConnectionWrapperFactory connectionWrapperFactory;
    private final Logger logger;

    @Inject
    public AMSessionInvalidatorImpl(@Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
                                    @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRestRoot,
                                    @Named (AMSTSConstants.REALM) String realm,
                                    @Named(AMSTSConstants.REST_LOGOUT_URI_ELEMENT) String restLogoutUriElement,
                                    @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
                                    UrlConstituentCatenator urlConstituentCatenator,
                                    @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE) String crestVersionSessionService,
                                    HttpURLConnectionWrapperFactory connectionWrapperFactory,
                                    Logger logger) throws MalformedURLException {
        this.logoutUrl = constituteLogoutUrl(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement, urlConstituentCatenator);
        this.amSessionCookieName = amSessionCookieName;
        this.crestVersionSessionService = crestVersionSessionService;
        this.connectionWrapperFactory = connectionWrapperFactory;
        this.logger = logger;
    }

    private URL constituteLogoutUrl(String amDeploymentUrl,
                                    String jsonRestRoot,
                                    String realm,
                                    String restLogoutUriElement,
                                    UrlConstituentCatenator urlConstituentCatenator) throws MalformedURLException {

        return new URL(urlConstituentCatenator.catenateUrlConstituents(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement));
    }

    @Override
    public void invalidateAMSession(String sessionId) throws TokenCreationException {
        try {
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, crestVersionSessionService);
            headerMap.put(amSessionCookieName, sessionId);
            HttpURLConnectionWrapper.ConnectionResult connectionResult =
                    connectionWrapperFactory
                    .httpURLConnectionWrapper(logoutUrl)
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.POST)
                    .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new TokenCreationException(responseCode, "Non-200 response from invalidating session " + sessionId +
                        "against url " + logoutUrl);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalidated session " + sessionId);
                }
            }
        } catch (IOException e) {
            throw new TokenCreationException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "Exception caught invalidating session: " + sessionId + " against Url " + logoutUrl
                            + ". Exception: " + e, e);
        }

    }
}
