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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.sts.soap.bootstrap;

import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.soap.config.SoapSTSModule;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @see SoapSTSAccessTokenProvider
 */
public class SoapSTSAccessTokenProviderImpl implements SoapSTSAccessTokenProvider {
    private static final String QUESTION_MARK = "?";
    private static final String AMPERSAND = "&";
    private static final String AUTH_INDEX_TYPE_PARAM = "authIndexType=";
    private static final String AUTH_INDEX_VALUE_PARAM = "authIndexValue=";
    private static final String MODULE = "module";
    private static final String APPLICATION = "Application";

    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final AMTokenParser amTokenParser;
    private final String amSessionCookieName;
    private final URL authenticateUrl;
    private final URL logoutUrl;
    private final int retryNumber;
    private final int retryInterval;
    private final double retryMultiplier;
    private final String authNServiceVersion;
    private final String sessionServiceVersion;
    private final SoapSTSAgentCredentialsAccess credentialsAccess;
    private final Logger logger;

    private static AtomicReference<String> accessTokenRef = new AtomicReference<String>();

    @Inject
    SoapSTSAccessTokenProviderImpl(HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
                                          AMTokenParser amTokenParser,
                                          @Named(SoapSTSModule.AM_SESSION_COOKIE_NAME_PROPERTY_KEY) String amSessionCookieName,
                                          @Named(SoapSTSModule.OPENAM_HOME_SERVER_PROPERTY_KEY) String openamUrl,
                                          @Named(SoapSTSModule.SOAP_STS_AGENT_RETRY_NUMBER_PROPERTY_KEY) String retryNumber,
                                          @Named(SoapSTSModule.SOAP_STS_AGENT_RETRY_INITIAL_INTERVAL_PROPERTY_KEY) String retryInterval,
                                          @Named(SoapSTSModule.SOAP_STS_AGENT_RETRY_MULTIPLIER_PROPERTY_KEY) String retryMultiplier,
                                          UrlConstituentCatenator urlConstituentCatenator,
                                          @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRoot,
                                          @Named(AMSTSConstants.REST_AUTHN_URI_ELEMENT) String authNUriElement,
                                          @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE) String authNServiceVersion,
                                          @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE) String sessionServiceVersion,
                                          @Named(SoapSTSModule.AGENT_REALM) String agentRealm,
                                          @Named (AMSTSConstants.REST_LOGOUT_URI_ELEMENT) String restLogoutUriElement,
                                          SoapSTSAgentCredentialsAccess credentialsAccess,
                                          Logger logger) throws MalformedURLException {
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.amTokenParser = amTokenParser;
        this.amSessionCookieName = amSessionCookieName;
        this.authNServiceVersion = authNServiceVersion;
        this.sessionServiceVersion = sessionServiceVersion;
        this.credentialsAccess = credentialsAccess;
        this.authenticateUrl = constituteLoginUrl(urlConstituentCatenator, openamUrl, jsonRoot, agentRealm, authNUriElement);
        this.logoutUrl = constituteLogoutUrl(urlConstituentCatenator, openamUrl, jsonRoot, agentRealm, restLogoutUriElement);
        this.retryNumber = StringUtils.isEmpty(retryNumber) ? 3 : Integer.parseInt(retryNumber);
        this.retryInterval = StringUtils.isEmpty(retryInterval) ? 500 : Integer.parseInt(retryInterval);
        this.retryMultiplier = StringUtils.isEmpty(retryMultiplier) ? 1.5 : Double.parseDouble(retryMultiplier);
        this.logger = logger;
    }

    private URL constituteLoginUrl(UrlConstituentCatenator urlConstituentCatenator,
                                   String openamUrl,
                                   String jsonRoot,
                                   String agentRealm,
                                   String authNUriElement) throws MalformedURLException {
        final String urlString = urlConstituentCatenator.catenateUrlConstituents(openamUrl, jsonRoot, agentRealm,
                authNUriElement, QUESTION_MARK, AUTH_INDEX_TYPE_PARAM, MODULE, AMPERSAND, AUTH_INDEX_VALUE_PARAM, APPLICATION);
        return new URL(urlString);
    }

    private URL constituteLogoutUrl(UrlConstituentCatenator urlConstituentCatenator,
                                    String openamUrl,
                                    String jsonRoot,
                                    String agentRealm,
                                    String restLogoutUriElement) throws MalformedURLException {
        return new URL(urlConstituentCatenator.catenateUrlConstituents(openamUrl, jsonRoot, agentRealm, restLogoutUriElement));
    }

    @Override
    public String getAccessToken() throws ResourceException {
        if (StringUtils.isNotEmpty(accessTokenRef.get())) {
            return accessTokenRef.get();
        }
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, authNServiceVersion);
        headerMap.put(AMSTSConstants.AM_REST_AUTHN_USERNAME_HEADER, credentialsAccess.getAgentUsername());
        headerMap.put(AMSTSConstants.AM_REST_AUTHN_PASSWORD_HEADER, credentialsAccess.getAgentPassword());
        try {
            HttpURLConnectionWrapper.ConnectionResult connectionResult =
                    httpURLConnectionWrapperFactory
                            .httpURLConnectionWrapper(authenticateUrl)
                            .setRequestHeaders(headerMap)
                            .setRequestMethod(AMSTSConstants.POST)
                            .makeInvocation();
            final int responseCode = connectionResult.getStatusCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw ResourceException.getException(responseCode, "Non-200 response authenticating against " + authenticateUrl
                    + " : " + connectionResult.getResult());
            } else {
                try {
                    if (StringUtils.isEmpty(accessTokenRef.get())) {
                        accessTokenRef.set(amTokenParser.getSessionFromAuthNResponse(connectionResult.getResult()));
                    }
                    return accessTokenRef.get();
                } catch (TokenValidationException e) {
                    throw new InternalServerErrorException("Exception caught obtaining the soap-sts-agent token " + e,
                            e);
                }
            }
        } catch (IOException ioe) {
            throw new InternalServerErrorException("IOException caught obtaining the soap-sts-agent token: " + ioe,
                    ioe);
        }
    }

    @Override
    public void invalidateAccessToken(String sessionId){
        if (StringUtils.isEmpty(accessTokenRef.get())) {
            return;
        }
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, sessionServiceVersion);
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
                logger.error("Non-200 response invalidating the soap-sts-agent token. " +
                        "This likely means that the session was not invalidated: " + connectionResult.getResult());
            }
        } catch (IOException ioe) {
            logger.error("IOException caught invalidating the soap-sts-agent token: " + ioe, ioe);
        } finally {
            accessTokenRef.set(null);
        }
    }

    @Override
    public String getAccessTokenWithRetry() throws ResourceException {
        String accessToken;
        int currentRetryIntervalMillis = retryInterval;
        for (int i = 0; i < retryNumber; i++) {
            try {
                return getAccessToken();
            } catch (ResourceException e) {
                logger.debug("Failed to retrieve soap-sts-agent token. Retry in " + currentRetryIntervalMillis + " milliseconds.");
                try {
                 Thread.sleep(currentRetryIntervalMillis);
                 currentRetryIntervalMillis *= retryMultiplier;
                } catch (InterruptedException ignored) {}
            }
        }

        if (StringUtils.isEmpty(accessTokenRef.get())) {
            throw new InternalServerErrorException(
                    "Unable to obtain soap-sts-agent token after " + retryNumber + " retries.");
        }

        return null;
    }
}
