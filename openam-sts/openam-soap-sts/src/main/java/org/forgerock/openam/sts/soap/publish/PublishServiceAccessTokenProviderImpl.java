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

package org.forgerock.openam.sts.soap.publish;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.soap.config.SoapSTSModule;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @see org.forgerock.openam.sts.soap.publish.PublishServiceAccessTokenProvider
 *
 * This is currently using admin credentials - will probably use application credentials soon, when the sts-publish
 * service is protected by something other than admin credentials (or perhaps only the soap version).TODO
 *
 */
public class PublishServiceAccessTokenProviderImpl implements PublishServiceAccessTokenProvider {
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final AMTokenParser amTokenParser;
    private final String amSessionCookieName;
    private final URL authenticateUrl;
    private final URL logoutUrl;
    private final String authNServiceVersion;
    private final String publishServiceConsumerUsername;
    private final String publishServiceConsumerPassword;
    private final Logger logger;

    @Inject
    PublishServiceAccessTokenProviderImpl(HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
                                          AMTokenParser amTokenParser,
                                          @Named(SoapSTSModule.AM_SESSION_COOKIE_NAME_PROPERTY_KEY) String amSessionCookieName,
                                          @Named(SoapSTSModule.OPENAM_HOME_SERVER_PROPERTY_KEY) String openamUrl,
                                          UrlConstituentCatenator urlConstituentCatenator,
                                          @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRoot,
                                          @Named(AMSTSConstants.REST_AUTHN_URI_ELEMENT) String authNUriElement,
                                          @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE) String authNServiceVersion,
                                          @Named(SoapSTSModule.PUBLISH_SERVICE_CONSUMER_USERNAME_PROPERTY_KEY) String publishServiceConsumerUsername,
                                          @Named(SoapSTSModule.PUBLISH_SERVICE_CONSUMER_PASSWORD_PROPERTY_KEY) String publishServiceConsumerPassword,
                                          @Named (AMSTSConstants.REST_LOGOUT_URI_ELEMENT) String restLogoutUriElement,
                                          Logger logger)
                                            throws MalformedURLException {
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.amTokenParser = amTokenParser;
        this.amSessionCookieName = amSessionCookieName;
        this.authNServiceVersion = authNServiceVersion;
        this.publishServiceConsumerUsername = publishServiceConsumerUsername;
        this.publishServiceConsumerPassword = publishServiceConsumerPassword;
        this.authenticateUrl = constituteLoginUrl(urlConstituentCatenator, openamUrl, jsonRoot, authNUriElement);
        this.logoutUrl = constituteLogoutUrl(urlConstituentCatenator, openamUrl, jsonRoot, restLogoutUriElement);
        this.logger = logger;
    }

    private URL constituteLoginUrl(UrlConstituentCatenator urlConstituentCatenator,
                                   String openamUrl,
                                   String jsonRoot,
                                   String authNUriElement) throws MalformedURLException{
        StringBuilder stringBuilder =
                new StringBuilder(urlConstituentCatenator.catenateUrlConstituents(openamUrl, jsonRoot));
        return new URL(urlConstituentCatenator.catentateUrlConstituent(stringBuilder, authNUriElement).toString());
    }

    private URL constituteLogoutUrl(UrlConstituentCatenator urlConstituentCatenator,
                                    String openamUrl,
                                    String jsonRoot,
                                    String restLogoutUriElement) throws MalformedURLException {
        StringBuilder sb = new StringBuilder(urlConstituentCatenator.catenateUrlConstituents(openamUrl, jsonRoot));
        sb = urlConstituentCatenator.catentateUrlConstituent(sb, restLogoutUriElement);
        return new URL(sb.toString());
    }

    @Override
    public String getPublishServiceAccessToken() throws STSPublishException {
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, authNServiceVersion);
        headerMap.put(AMSTSConstants.AM_REST_AUTHN_USERNAME_HEADER, publishServiceConsumerUsername);
        headerMap.put(AMSTSConstants.AM_REST_AUTHN_PASSWORD_HEADER, publishServiceConsumerPassword);
        try {
            HttpURLConnectionWrapper.ConnectionResult connectionResult =
                    httpURLConnectionWrapperFactory
                            .httpURLConnectionWrapper(authenticateUrl)
                            .setRequestHeaders(headerMap)
                            .setRequestMethod(AMSTSConstants.POST)
                            .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new STSPublishException(responseCode, "Non-200 response authenticating against " + authenticateUrl);
            } else {
                try {
                    return amTokenParser.getSessionFromAuthNResponse(connectionResult.getResult());
                } catch (TokenValidationException e) {
                    throw new STSPublishException(ResourceException.INTERNAL_ERROR, "Exception caught obtaining the session " +
                            "token necessary to consume the sts-publish service: " + e, e);
                }
            }
        } catch (IOException ioe) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR,
                    "IOException caught obtaining the token used to consume the sts-publish service: " + ioe, ioe);
        }
    }

    @Override
    public void invalidatePublishServiceAccessToken(String sessionId){
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, "protocol=1.0, resource=1.0");
        headerMap.put(amSessionCookieName, sessionId);
        try {
            HttpURLConnectionWrapper.ConnectionResult connectionResult =
                    httpURLConnectionWrapperFactory
                            .httpURLConnectionWrapper(logoutUrl)
                            .setRequestHeaders(headerMap)
                            .setRequestMethod(AMSTSConstants.POST)
                            .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.error("Non-200 response invalidating the session token necessary to consume the sts-publish service. " +
                        "This likely means that the session was not invalidated. The return code: " + responseCode);
            }
        } catch (IOException ioe) {
            logger.error("IOException caught invalidating the session token necessary to consume the sts-publish service: "
                    + ioe, ioe);
        }
    }
}
