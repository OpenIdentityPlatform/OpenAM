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

package com.forgerock.openam.functionaltest.sts.frmwk.soap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import com.forgerock.openam.functionaltest.sts.frmwk.common.BasicOpenAMAuthenticator;
import com.forgerock.openam.functionaltest.sts.frmwk.common.CommonConstants;
import com.forgerock.openam.functionaltest.sts.frmwk.common.STSPublishContext;
import org.forgerock.openam.sts.soap.EndpointSpecification;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.JsonValueBuilder;

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
 * This class provides an example of how to programmatically publish soap-sts instances.
 */
public class SoapSTSInstancePublisher {
    private static final String ID = "_id";
    private static final String REV = "_rev";

    private static final String COOKIE = "Cookie";
    private static final String EQUALS = "=";

    private final String publishEndpoint;
    private final String adminPassword;
    private final String amDeploymentUrl;
    private final BasicOpenAMAuthenticator basicOpenAMAuthenticator;
    private final SoapSTSInstanceConfigFactory soapSTSInstanceConfigFactory;
    private final SoapSTSServerCryptoState soapSTSServerCryptoState;
    private final Logger logger;

    @Inject
    public SoapSTSInstancePublisher(@Named(SoapSTSIntegrationTestModule.PUBLISH_ENDPOINT_URL) String publishEndpoint,
                                    @Named(CommonConstants.ADMIN_PASSWORD) String adminPassword,
                                    @Named(CommonConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
                                    BasicOpenAMAuthenticator basicOpenAMAuthenticator,
                                    SoapSTSInstanceConfigFactory soapSTSInstanceConfigFactory,
                                    SoapSTSServerCryptoState soapSTSServerCryptoState,
                                    Logger logger) {
        this.publishEndpoint = publishEndpoint;
        this.adminPassword = adminPassword;
        this.amDeploymentUrl = amDeploymentUrl;
        this.basicOpenAMAuthenticator = basicOpenAMAuthenticator;
        this.soapSTSInstanceConfigFactory = soapSTSInstanceConfigFactory;
        this.soapSTSServerCryptoState = soapSTSServerCryptoState;
        this.logger = logger;
    }

    public String publishInstance(final String urlElement, String realm,
                                  STSPublishContext publishContext, String wsdlFile) throws IOException {
        SoapSTSInstanceConfig instanceConfig = soapSTSInstanceConfigFactory.createSoapSTSInstanceConfig(urlElement, realm,
                EndpointSpecification.getStandardEndpointSpecification(), publishContext, wsdlFile, amDeploymentUrl,
                soapSTSServerCryptoState);
        String jsonString = buildPublishInvocationJsonValue(instanceConfig).toString();
        logger.log(Level.FINE, "Publishing instance according to the following config state: " + jsonString);
        String response = invokeRestSTSInstancePublish(jsonString);
        logger.log(Level.FINE, "Publish result: " + response);
        return parseInstanceUrl(response);
    }

    /*
    The fullSTSId should be the string returned by publishInstance.
     */
    public String removeInstance(final String fullSTSId) throws IOException {
        return invokeRestSTSInstanceDeletion(getPublishRemoveInstanceUri(fullSTSId));
    }

    public List<SoapSTSInstanceConfig> getPublishedInstances() throws IOException {
        String response = getPublishedRestSTSInstancesConfigContent(publishEndpoint);
        return parsePublishResponse(response);
    }

    private List<SoapSTSInstanceConfig> parsePublishResponse(String response) {
        List<SoapSTSInstanceConfig> instanceConfigs = new ArrayList<>();
        JsonValue json;
        try {
            json = JsonValueBuilder.toJsonValue(response);
        } catch (JsonException e) {
            logger.log(Level.SEVERE, "JsonException parsing response: " + response + "\n Exception: " + e);
            throw e;
        }
        for (String key : json.asMap().keySet()) {
            if (REV.equals(key) || ID.equals(key)) {
                continue;
            }
            final JsonValue value = json.get(key);
            SoapSTSInstanceConfig instanceConfig = SoapSTSInstanceConfig.fromJson(value);
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
            throw new IOException(getErrorMessage(connection));
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
            throw new IOException(getErrorMessage(connection));
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
        return new URL(publishEndpoint + "?_action=create");
    }

    private String getPublishRemoveInstanceUri(String stsId) throws IOException {
        return new UrlConstituentCatenatorImpl().catenateUrlConstituents(publishEndpoint, stsId);
    }

    private JsonValue buildPublishInvocationJsonValue(SoapSTSInstanceConfig instanceConfig) {
        return json(object(
                field(AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT, AMSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK),
                field(AMSTSConstants.STS_PUBLISH_INSTANCE_STATE, instanceConfig.toJson())));
    }
}