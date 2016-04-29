/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file at legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.forgerock.openam.functionaltest.sts.frmwk.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgerock.openam.functionaltest.sts.frmwk.common.CommonConstants;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import com.forgerock.openam.functionaltest.sts.frmwk.common.BasicOpenAMAuthenticator;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import com.forgerock.openam.functionaltest.sts.frmwk.common.STSPublishContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * This class provides an example of how to programatically publish rest-sts instances. It does not provide an
 * exhaustive enumeration of configuration options, but rather provides an indication of how to consume the various
 * classes used to build the configuration state corresponding to a published rest-sts instance.
 */
public class RestSTSInstancePublisher {
    private static final String REST_STS_PUBLISH_SERVICE_VERSION = "protocol=1.0, resource=1.0";

    private static final String COOKIE = "Cookie";
    private static final String EQUALS = "=";
    private static final String ID = "_id";
    private static final String REV = "_rev";

    private final String publishEndpoint;
    private final String adminPassword;
    private final BasicOpenAMAuthenticator basicOpenAMAuthenticator;
    private final RestSTSInstanceConfigFactory restSTSInstanceConfigFactory;
    private final UrlConstituentCatenator urlConstituentCatenator;
    private final Logger logger;

    /**
     *
     * @param publishEndpoint The url of the publish service: e.g http://myhost.com:8080/openam/rest-sts-publish/publish
     * @param adminPassword The amadmin passwordRequired
     *                       as only admins can consume the rest-sts publish service.
     *
     */
    @Inject
    public RestSTSInstancePublisher(@Named(RestSTSFunctionalTestAPIModule.PUBLISH_ENDPOINT) String publishEndpoint,
                                    @Named(CommonConstants.ADMIN_PASSWORD) String adminPassword,
                                    BasicOpenAMAuthenticator basicOpenAMAuthenticator,
                                    RestSTSInstanceConfigFactory restSTSInstanceConfigFactory,
                                    UrlConstituentCatenator urlConstituentCatenator,
                                    Logger logger) {
        this.publishEndpoint = publishEndpoint;
        this.adminPassword = adminPassword;
        this.basicOpenAMAuthenticator = basicOpenAMAuthenticator;
        this.restSTSInstanceConfigFactory = restSTSInstanceConfigFactory;
        this.urlConstituentCatenator = urlConstituentCatenator;
        this.logger = logger;
    }

    /**
     An example of the json posted as part of this method invocation:

     { "invocation_context": "invocation_context_client_sdk", "instance_state": { "oidc-id-token-config": { "oidc-issuer": "oidc_issuer", "oidc-public-key-reference-type": "NONE", "oidc-token-lifetime-seconds": "600", "oidc-authorized-party": null, "oidc-audience": [ "oidc_audience" ], "oidc-signature-algorithm": "HS256", "oidc-claim-map": { "email": "mail" }, "oidc-custom-claim-mapper-class": null, "oidc-custom-authn-context-mapper-class": null, "oidc-custom-authn-method-references-mapper-class": null, "oidc-keystore-location": null, "oidc-keystore-password": null, "oidc-client-secret": "bobo", "oidc-signature-key-alias": null, "oidc-signature-key-password": null }, "supported-token-transforms": [ { "inputTokenType": "X509", "outputTokenType": "OPENIDCONNECT", "invalidateInterimOpenAMSession": true }, { "inputTokenType": "OPENIDCONNECT", "outputTokenType": "SAML2", "invalidateInterimOpenAMSession": true }, { "inputTokenType": "OPENIDCONNECT", "outputTokenType": "OPENIDCONNECT", "invalidateInterimOpenAMSession": true }, { "inputTokenType": "USERNAME", "outputTokenType": "OPENIDCONNECT", "invalidateInterimOpenAMSession": true }, { "inputTokenType": "USERNAME", "outputTokenType": "SAML2", "invalidateInterimOpenAMSession": true }, { "inputTokenType": "OPENAM", "outputTokenType": "SAML2", "invalidateInterimOpenAMSession": false }, { "inputTokenType": "X509", "outputTokenType": "SAML2", "invalidateInterimOpenAMSession": true }, { "inputTokenType": "OPENAM", "outputTokenType": "OPENIDCONNECT", "invalidateInterimOpenAMSession": false } ], "persist-issued-tokens-in-cts": "true", "saml2-config": { "issuer-name": "http://idp.com:8080/openam", "saml2-name-id-format": "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent", "saml2-token-lifetime-seconds": "600", "saml2-custom-conditions-provider-class-name": null, "saml2-custom-subject-provider-class-name": null, "saml2-custom-attribute-statements-provider-class-name": null, "saml2-custom-attribute-mapper-class-name": null, "saml2-custom-authn-context-mapper-class-name": null, "saml2-custom-authentication-statements-provider-class-name": null, "saml2-custom-authz-decision-statements-provider-class-name": null, "saml2-sign-assertion": "true", "saml2-encrypt-assertion": "false", "saml2-encrypt-attributes": "false", "saml2-encrypt-nameid": "false", "saml2-encryption-algorithm": "http://www.w3.org/2001/04/xmlenc#aes128-cbc", "saml2-encryption-algorithm-strength": "128", "saml2-attribute-map": { "email": "mail" }, "saml2-keystore-filename": "/Users/DirkHogan/openam/openam/keystore.jks", "saml2-keystore-password": "changeit", "saml2-sp-acs-url": "http://sp.com:8080/openam/acs", "saml2-sp-entity-id": "http://sp.com:8080/openam", "saml2-signature-key-alias": "test", "saml2-signature-key-password": "changeit", "saml2-encryption-key-alias": "test" }, "deployment-config": { "deployment-url-element": "rest_sts_instance1049252330", "deployment-realm": "/subrealm2021890344", "deployment-auth-target-mappings": { "X509": { "mapping-auth-index-type": "module", "mapping-auth-index-value": "cert_module", "mapping-context": { "x509_token_auth_target_header_key": "client_cert" } }, "OPENIDCONNECT": { "mapping-auth-index-type": "module", "mapping-auth-index-value": "oidc_module", "mapping-context": { "oidc_id_token_auth_target_header_key": "oidc_id_token" } }, "USERNAME": { "mapping-auth-index-type": "service", "mapping-auth-index-value": "ldapService" } }, "deployment-offloaded-two-way-tls-header-key": "also_client_cert", "deployment-tls-offload-engine-hosts": [ "10.0.0.6", "127.0.0.1" ] } } }
     */
    public String publishInstance(final String urlElement, String realm, STSPublishContext publishContext,
                                  CustomTokenOperationContext customTokenOperationContext) throws IOException {
        RestSTSInstanceConfig instanceConfig =
                restSTSInstanceConfigFactory.createRestSTSInstanceConfig(urlElement, realm, publishContext, customTokenOperationContext);
        String jsonString = buildPublishInvocationJsonValue(instanceConfig).toString();
        logger.log(Level.SEVERE, "Publishing instance according to the following config state: " + jsonString);
        String response = invokeRestSTSInstancePublish(jsonString);
        logger.log(Level.SEVERE, "Publish result: " + response);
        return parseInstanceUrl(response);
    }

    /*
    The fullSTSId should be the string returned by publishInstance.
     */
    public String removeInstance(final String fullSTSId) throws IOException {
        final String result = invokeRestSTSInstanceDeletion(getPublishRemoveInstanceUri(fullSTSId));
        logger.log(Level.FINE, "Removed rest sts instance: " + fullSTSId);
        return result;
    }

    public String updateInstance(final String fullSTSId, RestSTSInstanceConfig restSTSInstanceConfig) throws IOException {
        final String result = invokeRestSTSInstanceUpdate(getPublishUpdateInstanceUri(fullSTSId),
                buildPublishInvocationJsonValue(restSTSInstanceConfig).toString());
        logger.log(Level.FINE, "Updated rest sts instance " + fullSTSId);
        return result;
    }

    public RestSTSInstanceConfig getPublishedInstance(String stsId) throws IOException {
        final String response = getPublishedRestSTSInstancesConfigContent(
                urlConstituentCatenator.catenateUrlConstituents(publishEndpoint, stsId));
        final List<RestSTSInstanceConfig> configs = parsePublishResponse(response);
        if (configs.isEmpty()) {
            throw new IOException("No published rest sts instance corresponding to id " + stsId);
        } else if (configs.size() > 1) {
            throw new IOException("More than a single rest sts instance corresponding to id " + stsId);
        } else {
            return configs.get(0);
        }
    }

    public List<RestSTSInstanceConfig> getPublishedInstances() throws IOException {
        final String response = getPublishedRestSTSInstancesConfigContent(publishEndpoint);
        return parsePublishResponse(response);
    }

    private List<RestSTSInstanceConfig> parsePublishResponse(String response) {
        List<RestSTSInstanceConfig> instanceConfigs = new ArrayList<>();
        JsonValue json;
        try {
            json = JsonValueBuilder.toJsonValue(response);
        } catch (JsonException e) {
            logger.log(Level.FINE, "JsonException parsing response: " + response + "\n Exception: " + e);
            throw e;
        }
        for (String key : json.asMap().keySet()) {
            if (REV.equals(key) || ID.equals(key)) {
                continue;
            }
            final JsonValue value = json.get(key);
            RestSTSInstanceConfig instanceConfig = RestSTSInstanceConfig.fromJson(value);
            instanceConfigs.add(instanceConfig);
        }
        return instanceConfigs;

    }

    private String getPublishedRestSTSInstancesConfigContent(String publishEndpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)new URL(publishEndpoint).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        connection.connect();
        return readInputStream(connection.getInputStream());
    }

    private String invokeRestSTSInstancePublish(String invocationPayload) throws IOException {

        HttpURLConnection connection = (HttpURLConnection)getPublishAddInstanceUri().openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
            return getSuccessMessage(connection);
        } else {
            throw new IOException("Could not publish sts instance with content: " + invocationPayload + ": " +
                    getErrorMessage(connection));
        }
    }

    private String invokeRestSTSInstanceDeletion(String deletionUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)new URL(deletionUrl).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        connection.connect();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return getSuccessMessage(connection);
        } else {
            throw new IOException("Failed to perform DELETE on url: " + deletionUrl + ": " + getErrorMessage(connection));
        }
    }

    private String invokeRestSTSInstanceUpdate(String updateUrl, String invocationPayload) throws IOException{
        HttpURLConnection connection = (HttpURLConnection)new URL(updateUrl).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        /*
        Don't remove this version specification - it is necessary to avoid 'upsert' semantics on a PUT. See
        https://bugster.forgerock.org/jira/browse/CREST-100
        for details.
         */
        connection.setRequestProperty(SharedSTSConstants.CREST_VERSION_HEADER_KEY, REST_STS_PUBLISH_SERVICE_VERSION);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return getSuccessMessage(connection);
        } else {
            throw new IOException("Failed to perform PUT on url: " + updateUrl + ": " + getErrorMessage(connection) +
                    ". The connection request method: " + connection.getRequestMethod());
        }
    }

    private String getSuccessMessage(HttpURLConnection connection) throws IOException {
        return readInputStream(connection.getInputStream());
    }

    private String getErrorMessage(HttpURLConnection connection) throws IOException {
        if (connection.getErrorStream() != null) {
            return readInputStream(connection.getErrorStream());
        } else {
            return readInputStream(connection.getInputStream());
        }
    }

    private String getAdminSessionTokenCookie() throws IOException {
        return "iPlanetDirectoryPro" + EQUALS + getAdminSession();
    }

    private String getAdminSession() throws IOException {
        return basicOpenAMAuthenticator.getAdminSSOTokenString("amadmin", adminPassword);
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "Empty error stream";
        } else {
            return IOUtils.readStream(inputStream);
        }
    }

    private String parseInstanceUrl(String publishResponse) throws IOException {
        Object responseContent;
        try {
            JsonParser parser =
                    new ObjectMapper().getFactory().createParser(publishResponse);
            responseContent = parser.readValueAs(Object.class);
        } catch (IOException e) {
            throw new IOException("Could not map the response from the PublishService to a json object. The response: "
                    + publishResponse + "; The exception: " + e);
        }
        return new JsonValue(responseContent).get("url_element").asString();
    }

    private URL getPublishAddInstanceUri() throws MalformedURLException {
        return new URL(publishEndpoint + RestSTSFunctionalTestAPIModule.CREATE_ACTION);
    }

    private String getPublishRemoveInstanceUri(String stsId) {
        return new UrlConstituentCatenatorImpl().catenateUrlConstituents(publishEndpoint, stsId);
    }

    private String getPublishUpdateInstanceUri(String stsId) {
        return new UrlConstituentCatenatorImpl().catenateUrlConstituents(publishEndpoint, stsId);
    }

    private JsonValue buildPublishInvocationJsonValue(RestSTSInstanceConfig instanceConfig) {
        return json(object(
                field(AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT, AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK),
                field(AMSTSConstants.STS_PUBLISH_INSTANCE_STATE, instanceConfig.toJson())));
    }
}