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

import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.soap.config.SoapSTSModule;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.soap.publish.PublishServiceConsumer
 */
public class PublishServiceConsumerImpl implements PublishServiceConsumer {
    private final String openamUrl;
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final UrlConstituentCatenator urlConstituentCatenator;
    private final String soapSTSPublishServiceVersion;
    private final SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
    private final String amSessionCookieName;
    private final String publishServiceUriElement;
    private final Logger logger;

    @Inject
    PublishServiceConsumerImpl(@Named(SoapSTSModule.OPENAM_HOME_SERVER_PROPERTY_KEY) String openamUrl,
                               HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
                               UrlConstituentCatenator urlConstituentCatenator,
                               @Named(AMSTSConstants.CREST_VERSION_SOAP_STS_PUBLISH_SERVICE) String soapSTSPublishServiceVersion,
                               SoapSTSAccessTokenProvider soapSTSAccessTokenProvider,
                               @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
                               @Named(AMSTSConstants.SOAP_STS_PUBLISH_SERVICE_URI_ELEMENT) String publishServiceUriElement,
                               Logger logger) {
        this.openamUrl = openamUrl;
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.urlConstituentCatenator = urlConstituentCatenator;
        this.soapSTSPublishServiceVersion = soapSTSPublishServiceVersion;
        this.soapSTSAccessTokenProvider = soapSTSAccessTokenProvider;
        this.amSessionCookieName = amSessionCookieName;
        this.publishServiceUriElement = publishServiceUriElement;
        this.logger = logger;
    }

    @Override
    public Set<SoapSTSInstanceConfig> getPublishedInstances() throws ResourceException {
        String sessionId = null;
        try {
            sessionId = soapSTSAccessTokenProvider.getAccessToken();
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, soapSTSPublishServiceVersion);
            headerMap.put(AMSTSConstants.COOKIE, createAMSessionCookie(sessionId));

            HttpURLConnectionWrapper.ConnectionResult connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(buildPublishServiceUrl())
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.GET)
                    .makeInvocation();

            final int responseCode = connectionResult.getStatusCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return parseResponse(connectionResult.getResult());
            } else {
                throw new STSPublishException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                        "Returning empty list from PublishServiceConsumerImpl#getPublishedInstances - non 200 " +
                                "response from sts-publish service: " + responseCode);
            }
        } catch (IOException e) {
            throw new STSPublishException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "Exception caught invoking obtaining published soap sts instance state from publish service: " + e);
        } finally {
            if (sessionId != null) {
                soapSTSAccessTokenProvider.invalidateAccessToken(sessionId);
            }
        }
    }

    private URL buildPublishServiceUrl() throws MalformedURLException {
        return new URL(urlConstituentCatenator.catenateUrlConstituents(openamUrl, publishServiceUriElement));
    }

    private String createAMSessionCookie(String sessionId) throws STSPublishException {
        return amSessionCookieName + AMSTSConstants.EQUALS + sessionId;
    }

    private Set<SoapSTSInstanceConfig> parseResponse(String response) throws STSPublishException {
        Set<SoapSTSInstanceConfig> instanceConfigs = new HashSet<SoapSTSInstanceConfig>();
        JsonValue json;
        try {
            json = JsonValueBuilder.toJsonValue(response);
        } catch (JsonException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, e.getMessage());
        }
        for (String key : json.asMap().keySet()) {
            JsonValue value = json.get(key);
            JsonValue jsonInstanceConfig;
            try {
                jsonInstanceConfig = JsonValueBuilder.toJsonValue(value.asString());
            } catch (JsonException e) {
                throw new STSPublishException(ResourceException.INTERNAL_ERROR, e.getMessage());
            }
            SoapSTSInstanceConfig instanceConfig = SoapSTSInstanceConfig.fromJson(jsonInstanceConfig);
            /*
            check for duplicates: duplicates cannot really be present because the combination of realm and deployment
            uri constitutes the identity of the soap-sts instance, and duplicate entries will result in LDAP errors
            when the instance is persisted in the SMS, but paranoia pays...
             */
            if (!instanceConfigs.add(instanceConfig)) {
                logger.error("The set of published soap-sts instances contains a duplicate!! The duplicate instance: " + instanceConfig);
            }
        }
        return instanceConfigs;
    }
}
