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

package org.forgerock.openam.sts.soap.bootstrap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.soap.config.SoapSTSModule;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.utils.JsonValueBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @see org.forgerock.openam.sts.soap.bootstrap.SoapSTSAgentConfigAccess
 */
public class SoapSTSAgentConfigAccessImpl implements SoapSTSAgentConfigAccess {
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final String agentsProfileServiceVersion;
    private final SoapSTSAccessTokenProvider accessTokenProvider;
    private final String amSessionCookieName;
    private final URL agentProfileUrl;

    @Inject
    SoapSTSAgentConfigAccessImpl(@Named(SoapSTSModule.OPENAM_HOME_SERVER_PROPERTY_KEY) String openamUrl,
                                 HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
                                 UrlConstituentCatenator urlConstituentCatenator,
                                 @Named(AMSTSConstants.CREST_VERSION_AGENTS_PROFILE_SERVICE) String agentsProfileServiceVersion,
                                 SoapSTSAccessTokenProvider accessTokenProvider,
                                 @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
                                 @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRoot,
                                 @Named(AMSTSConstants.AGENTS_PROFILE_SERVICE_URI_ELEMENT) String agentProfileServiceUriElement,
                                 @Named(SoapSTSModule.AGENT_REALM) String agentRealm,
                                 @Named(SoapSTSModule.SOAP_STS_AGENT_USERNAME_PROPERTY_KEY) String agentName) throws MalformedURLException {
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.agentsProfileServiceVersion = agentsProfileServiceVersion;
        this.accessTokenProvider = accessTokenProvider;
        this.amSessionCookieName = amSessionCookieName;
        this.agentProfileUrl = buildAgentProfileUrl(urlConstituentCatenator, openamUrl, jsonRoot, agentRealm,
                agentProfileServiceUriElement, agentName);
    }

    @Override
    public JsonValue getConfigurationState() throws ResourceException {
        String sessionId = null;
        try {
            sessionId = accessTokenProvider.getAccessToken();
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, agentsProfileServiceVersion);
            headerMap.put(AMSTSConstants.COOKIE, createAMSessionCookie(sessionId));

            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(agentProfileUrl)
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.GET)
                    .makeInvocation();

            final int responseCode = connectionResult.getStatusCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return JsonValueBuilder.toJsonValue(connectionResult.getResult());
            } else {
                throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                        "non 200 response from agent config service at: " + agentProfileUrl +
                                "; response code: " + responseCode);
            }
        } catch (IOException e) {
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR,
                    "Exception caught obtaining agent config state from: " + agentProfileUrl + "; Exception: " + e);
        } finally {
            if (sessionId != null) {
                accessTokenProvider.invalidateAccessToken(sessionId);
            }
        }
    }

    private URL buildAgentProfileUrl(UrlConstituentCatenator urlConstituentCatenator,
                                     String openamUrl,
                                     String jsonRoot,
                                     String agentRealm,
                                     String agentsProfileServiceUriElement,
                                     String agentName) throws MalformedURLException {
        return new URL(urlConstituentCatenator.catenateUrlConstituents(
                openamUrl, jsonRoot, agentRealm, agentsProfileServiceUriElement, agentName));
    }

    private String createAMSessionCookie(String sessionId) throws STSPublishException {
        return amSessionCookieName + AMSTSConstants.EQUALS + sessionId;
    }
}
