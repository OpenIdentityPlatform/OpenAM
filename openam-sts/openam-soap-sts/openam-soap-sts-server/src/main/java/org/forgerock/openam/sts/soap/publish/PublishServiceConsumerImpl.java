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

import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.soap.publish.PublishServiceConsumer
 */
public class PublishServiceConsumerImpl implements PublishServiceConsumer {
    private static final String QUERY_FILTER = "?_queryFilter=";
    private static final String EQ = " eq ";
    private static final String FORWARD_SLASH="/";
    private static final String QUOTE = "\"";
    private static final String REALM = "realm";
    private static final String RESULT = "result";


    private final String openamUrl;
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final UrlConstituentCatenator urlConstituentCatenator;
    private final String soapSTSPublishServiceVersion;
    private final SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
    private final String amSessionCookieName;
    private final String publishServiceUriElement;
    private final String realm;
    private final Logger logger;

    @Inject
    PublishServiceConsumerImpl(@Named(SoapSTSModule.OPENAM_HOME_SERVER_PROPERTY_KEY) String openamUrl,
                               HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
                               UrlConstituentCatenator urlConstituentCatenator,
                               @Named(AMSTSConstants.CREST_VERSION_SOAP_STS_PUBLISH_SERVICE) String soapSTSPublishServiceVersion,
                               SoapSTSAccessTokenProvider soapSTSAccessTokenProvider,
                               @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
                               @Named(AMSTSConstants.SOAP_STS_PUBLISH_SERVICE_URI_ELEMENT) String publishServiceUriElement,
                               @Named (AMSTSConstants.REALM) String realm,
                               Logger logger) {
        this.openamUrl = openamUrl;
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.urlConstituentCatenator = urlConstituentCatenator;
        this.soapSTSPublishServiceVersion = soapSTSPublishServiceVersion;
        this.soapSTSAccessTokenProvider = soapSTSAccessTokenProvider;
        this.amSessionCookieName = amSessionCookieName;
        this.publishServiceUriElement = publishServiceUriElement;
        this.realm = realm;
        this.logger = logger;
    }

    @Override
    public Set<SoapSTSInstanceConfig> getPublishedInstances() throws ResourceException {
        String sessionId = null;
        HttpURLConnectionWrapper.ConnectionResult connectionResult;
        try {
            sessionId = soapSTSAccessTokenProvider.getAccessToken();
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
            headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, soapSTSPublishServiceVersion);
            headerMap.put(AMSTSConstants.COOKIE, createAMSessionCookie(sessionId));

            connectionResult =  httpURLConnectionWrapperFactory
                    .httpURLConnectionWrapper(buildPublishServiceUrl())
                    .setRequestHeaders(headerMap)
                    .setRequestMethod(AMSTSConstants.GET)
                    .makeInvocation();
        } catch (IOException e) {
            throw new STSPublishException(org.forgerock.json.resource.ResourceException.INTERNAL_ERROR,
                    "Exception caught invoking obtaining published soap sts instance state from publish service: " + e, e);
        } finally {
            if (sessionId != null) {
                soapSTSAccessTokenProvider.invalidateAccessToken(sessionId);
            }
        }
        final int responseCode = connectionResult.getStatusCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return parseResponse(connectionResult.getResult());
        } else {
            throw new STSPublishException(responseCode,
                    "Returning empty list from PublishServiceConsumerImpl#getPublishedInstances - non 200 " +
                            "response from sts-publish service: " + connectionResult.getResult());
        }
    }

    private URL buildPublishServiceUrl() throws MalformedURLException, UnsupportedEncodingException {
        return new URL(urlConstituentCatenator.catenateUrlConstituents(openamUrl, publishServiceUriElement, QUERY_FILTER,
                getUrlEncodedRealmQueryFilterParam()));
    }

    private String getUrlEncodedRealmQueryFilterParam() throws UnsupportedEncodingException {
        final String queryContents = FORWARD_SLASH + REALM + EQ + QUOTE + realm + QUOTE;
        return URLEncoder.encode(queryContents, StandardCharsets.UTF_8.name());
    }

    private String createAMSessionCookie(String sessionId) throws STSPublishException {
        return amSessionCookieName + AMSTSConstants.EQUALS + sessionId;
    }

    /*
    The response is created in SoapSTSPublishServiceRequestHandler#handleQuery.
     */
    private Set<SoapSTSInstanceConfig> parseResponse(String response) throws STSPublishException {
        Set<SoapSTSInstanceConfig> instanceConfigs = new HashSet<>();
        JsonValue json;
        try {
            json = JsonValueBuilder.toJsonValue(response);
        } catch (JsonException e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, e.getMessage(), e);
        }
        JsonValue queryResult = json.get(RESULT);
        if (queryResult.isCollection()) {
            int size = queryResult.asCollection().size();
            for (int ndx = 0; ndx < size; ndx++) {
                final SoapSTSInstanceConfig soapSTSInstanceConfig = SoapSTSInstanceConfig.fromJson(queryResult.get(ndx));
                /*
                check for duplicates: duplicates cannot really be present because the combination of realm and deployment
                uri constitutes the identity of the soap-sts instance, and duplicate entries will result in LDAP errors
                when the instance is persisted in the SMS, but paranoia pays...
                 */
                if (!instanceConfigs.add(soapSTSInstanceConfig)) {
                    logger.error("The set of published soap-sts instances contains a duplicate!! The duplicate instance: " + queryResult.get(ndx));
                }
            }
            return instanceConfigs;
        } else {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, "Unexpected state: the query result is not " +
                    "a collection. The query result: " + queryResult.toString());
        }
    }
}
