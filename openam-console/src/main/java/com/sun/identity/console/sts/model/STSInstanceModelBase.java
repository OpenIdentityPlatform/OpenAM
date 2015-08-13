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
 * Copyright 2014-2015 ForgeRock AS.
 */

package com.sun.identity.console.sts.model;

import com.iplanet.sso.SSOException;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * @see STSInstanceModel
 * This class extends the AMServiceProfileModelImpl because this class provides functionality for reading values corresponding
 * to propertySheets.
 *
 */
public abstract class STSInstanceModelBase extends AMServiceProfileModelImpl implements STSInstanceModel {
    private static final String COOKIE = "Cookie";
    private static final String EQUALS = "=";
    private static final String REST_STS_PUBLISH_SERVICE_VERSION = "protocol=1.0, resource=1.0";
    private static final String SOAP_STS_PUBLISH_SERVICE_VERSION = "protocol=1.0, resource=1.0";

    public STSInstanceModelBase(HttpServletRequest req, String serviceName, Map map) throws AMConsoleException {
        super(req, serviceName, map);
    }

    /*
    Called during validateConfigurationState - allows the RestSTSInstanceModel or SoapSTSInstanceModel subclasses to
    perform sts-type-specific input validation.
     */
    abstract STSInstanceModelResponse stsTypeSpecificValidation(Map<String, Set<String>> configurationState);

    /*
     Called during the createInstance and updateInstance invocations. Allows subclasses to add rest- or soap-specific
     state corresponding to selections made in the ViewBean.
     */
    abstract STSInstanceModelResponse addStsTypeSpecificConfigurationState(Map<String, Set<String>> configurationState);

    @Override
    public Set<String> getPublishedInstances(STSType stsType, String realm) throws AMConsoleException {
        try {
            ServiceConfig baseService = new ServiceConfigManager(getServiceNameForSTSType(stsType),
                    getUserSSOToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                return baseService.getSubConfigNames();
            } else {
                return Collections.EMPTY_SET;
            }
        } catch (SMSException | SSOException e) {
            throw new AMConsoleException(e);
        }
    }

    @Override
    public void deleteInstances(STSType stsType, Set<String> instanceNames) throws AMConsoleException {
        for (String instanceName : instanceNames) {
            try {
                STSInstanceModelResponse response = invokeSTSInstanceDeletion(stsType, instanceName);
                if (!response.isSuccessful()) {
                    throw new AMConsoleException(response.getMessage());
                }
            } catch (IOException e) {
                throw new AMConsoleException(e);
            }
        }
    }

    @Override
    public STSInstanceModelResponse createInstance(STSType stsType, Map<String, Set<String>> configurationState, String realm) throws AMConsoleException {
        addCommonProgrammaticConfigurationState(configurationState, realm);
        STSInstanceModelResponse additionResponse = addStsTypeSpecificConfigurationState(configurationState);
        if (!additionResponse.isSuccessful()) {
            return additionResponse;
        }
        JsonValue invocationJson = createInstanceInvocationState(configurationState);
        try {
            return invokeSTSInstancePublish(stsType, invocationJson.toString());
        } catch (IOException e) {
            throw new AMConsoleException(e);
        }
    }

    @Override
    public STSInstanceModelResponse updateInstance(STSType stsType, Map<String, Set<String>> configurationState, String realm, String instanceName) throws AMConsoleException {
        addCommonProgrammaticConfigurationState(configurationState, realm);
        STSInstanceModelResponse additionResponse = addStsTypeSpecificConfigurationState(configurationState);
        if (!additionResponse.isSuccessful()) {
            return additionResponse;
        }
        JsonValue invocationJson = createInstanceInvocationState(configurationState);
        try {
            return invokeSTSInstanceUpdate(stsType, invocationJson.toString(), instanceName);
        } catch (IOException e) {
            throw new AMConsoleException(e);
        }
    }

    @Override
    public Map<String, Set<String>> getInstanceState(STSType stsType, String realm, String instanceName) throws AMConsoleException {
        try {
            ServiceConfig baseService = new ServiceConfigManager(getServiceNameForSTSType(stsType),
                    getUserSSOToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                ServiceConfig serviceConfig = baseService.getSubConfig(instanceName);
                if (serviceConfig != null) {
                    return serviceConfig.getAttributes();
                } else {
                    return Collections.EMPTY_MAP;
                }
            } else {
                return Collections.EMPTY_MAP;
            }
        } catch (SMSException | SSOException e) {
            throw new AMConsoleException(e);
        }
    }

    private String getServiceNameForSTSType(STSType stsType) {
        if (stsType.isRestSTS()) {
            return AMAdminConstants.REST_STS_SERVICE;
        }
        return AMAdminConstants.SOAP_STS_SERVICE;
    }

    @Override
    public STSInstanceModelResponse validateConfigurationState(STSType stsType, Map<String, Set<String>> configurationState) {
        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.DEPLOYMENT_URL_ELEMENT))) {
            return STSInstanceModelResponse.failure(getLocalizedString("rest.sts.validation.deployment.url.message"));
        } else {
            String urlElement = configurationState.get(SharedSTSConstants.DEPLOYMENT_URL_ELEMENT).iterator().next();
            if (urlElement.contains(SharedSTSConstants.FORWARD_SLASH)) {
                return STSInstanceModelResponse.failure(getLocalizedString("rest.sts.validation.deployment.url.content.message"));
            }
        }

        STSInstanceModelResponse specificTypeValidations = stsTypeSpecificValidation(configurationState);
        if (!specificTypeValidations.isSuccessful()) {
            return specificTypeValidations;
        }

        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.ISSUER_NAME)) &&
                StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_ISSUER))) {
            return STSInstanceModelResponse.failure(getLocalizedString("rest.sts.validation.output.token.configuration.message"));
        }
        //block above will insure that either the SAML2 issuer or the OIDC issuer has been set if we reach here, so null will
        //not be returned
        STSInstanceModelResponse response = null;
        if (!StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.ISSUER_NAME))) {
            response = validateSAML2ConfigurationState(configurationState);
            if (!response.isSuccessful()) {
                return response;
            }
        }
        if (!StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_ISSUER))) {
            response = validateOIDCConfigurationState(configurationState);
        }
        return response;
    }

    private STSInstanceModelResponse validateSAML2ConfigurationState(Map<String, Set<String>> configurationState) {
        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_TOKEN_LIFETIME))) {
            return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.saml2.token.lifetime.message"));
        }

        if (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_SIGN_ASSERTION, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ASSERTION, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ATTRIBUTES, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_NAME_ID, false)) {

            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_KEYSTORE_FILE_NAME))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.saml2.keystore.filename.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_KEYSTORE_PASSWORD))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.saml2.keystore.password.message"));
            }
        }
        if (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_SIGN_ASSERTION, false)) {
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_SIGNATURE_KEY_ALIAS))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.saml2.keystore.signature.keyalias.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_SIGNATURE_KEY_PASSWORD))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.saml2.keystore.signature.keypassword.message"));
            }
        }
        if (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ASSERTION, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ATTRIBUTES, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_NAME_ID, false)) {

            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPTION_KEY_ALIAS))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.saml2.keystore.encryption.keyalias.message"));
            }
        }
        if (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ASSERTION, false)
                && (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ATTRIBUTES, false)
                    || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_NAME_ID, false))) {
            return STSInstanceModelResponse.failure(getLocalizedString("sts.saml2.encryptioncombinations.message"));
        }

        if (!CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.SAML2_ATTRIBUTE_MAP))) {
            if (!attributeMappingCorrectFormat(configurationState.get(SharedSTSConstants.SAML2_ATTRIBUTE_MAP))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.saml2.claim.map.incorrect.format.message"));
            }
        }

        return STSInstanceModelResponse.success();
    }

    private STSInstanceModelResponse validateOIDCConfigurationState(Map<String, Set<String>> configurationState) {
        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_TOKEN_LIFETIME))) {
            return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.token.lifetime.message"));
        }

        boolean rsaSignature = false;
        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_SIGNATURE_ALGORITHM))) {
            return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.signature.algorithm.message"));
        } else {
            rsaSignature = rsaSignatureForOIDC(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_SIGNATURE_ALGORITHM));
        }

        if (rsaSignature) {
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_KEYSTORE_LOCATION))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.keystore.location.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_KEYSTORE_PASSWORD))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.keystore.password.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_SIGNATURE_KEY_ALIAS))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.keystore.signature.keyalias.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_SIGNATURE_KEY_PASSWORD))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.keystore.signature.keypassword.message"));
            }
        } else {
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_CLIENT_SECRET))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.client.secret.missing.message"));
            }
        }

        if (CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.OIDC_AUDIENCE))) {
            return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.audience.not.specified.message"));
        }

        if (!CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.OIDC_CLAIM_MAP))) {
            if (!attributeMappingCorrectFormat(configurationState.get(SharedSTSConstants.OIDC_CLAIM_MAP))) {
                return STSInstanceModelResponse.failure(getLocalizedString("sts.validation.oidc.claim.map.incorrect.format.message"));
            }
        }
        return STSInstanceModelResponse.success();
    }

    /*
    Method to insure that the attribute/claim mappings are of format x=y
     */
    private boolean attributeMappingCorrectFormat(Set<String> attributeMapping) {
        for (String mapping : attributeMapping) {
            if (mapping.split(EQUALS).length != 2) {
                return false;
            }
        }
        return true;
    }

    private boolean rsaSignatureForOIDC(String algorithm) {
        return algorithm.startsWith("RS");
    }

    /*
    Add the realm, as this information does not have to be solicited from the user. And if SAML2 tokens are to be issued,
    also add the encryption strength parameter, as this value is hard-coded based upon the encryption algorithm type, and necessary only if the
    FMEncProvider is over-ridden. See comment in SAML2Config.SAML2ConfigBuilder#encryptionAlgorithmStrength for details.
     */
    private void addCommonProgrammaticConfigurationState(Map<String, Set<String>> configurationState, String realm) {
        configurationState.put(SharedSTSConstants.DEPLOYMENT_REALM, CollectionUtils.asSet(realm));
        if (!CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.ISSUER_NAME))) {
            final String encryptionAlgorithmStrength =
                    getEncryptionStrengthFromEncryptionAlgorithm(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPTION_ALGORITHM));
            configurationState.put(SharedSTSConstants.SAML2_ENCRYPTION_ALGORITHM_STRENGTH,
                    CollectionUtils.asSet(encryptionAlgorithmStrength));
        }
    }

    private String getEncryptionStrengthFromEncryptionAlgorithm(String encryptionAlgorithm) {
        if ("http://www.w3.org/2001/04/xmlenc#aes128-cbc".equals(encryptionAlgorithm)) {
            return "128";
        } else if ("http://www.w3.org/2001/04/xmlenc#aes192-cbc".equals(encryptionAlgorithm)) {
            return "192";
        } else if ("http://www.w3.org/2001/04/xmlenc#aes256-cbc".equals(encryptionAlgorithm)) {
            return "256";
        }
        //safety case, should not be triggered because possible values specified in properties file
        return "128";
    }

    private JsonValue createInstanceInvocationState(Map<String, Set<String>> configurationState) {
        JsonValue propertiesMap = new JsonValue(configurationState);
        return json(object(
                field(SharedSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT, SharedSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN),
                field(SharedSTSConstants.STS_PUBLISH_INSTANCE_STATE, propertiesMap)));
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

    private String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "Empty error stream";
        } else {
            return IOUtils.readStream(inputStream);
        }
    }

    private String getRestSTSInstanceDeletionUrl(String instanceId) {
        String processedInstanceId = instanceId;
        if (processedInstanceId.startsWith(SharedSTSConstants.FORWARD_SLASH)) {
            processedInstanceId = processedInstanceId.substring(1);
        }
        return getRestSTSPublishEndpointUrl() + SharedSTSConstants.FORWARD_SLASH + processedInstanceId;
    }

    private String getSoapSTSInstanceDeletionUrl(String instanceId) {
        String processedInstanceId = instanceId;
        if (processedInstanceId.startsWith(SharedSTSConstants.FORWARD_SLASH)) {
            processedInstanceId = processedInstanceId.substring(1);
        }
        return getSoapSTSPublishEndpointUrl() + SharedSTSConstants.FORWARD_SLASH + processedInstanceId;
    }

    private String getRestSTSInstanceUpdateUrl(String instanceId) {
        return getRestSTSInstanceDeletionUrl(instanceId);
    }

    private String getSoapSTSInstanceUpdateUrl(String instanceId) {
        return getSoapSTSInstanceDeletionUrl(instanceId);
    }

    private String getRestSTSInstanceCreationUrl() {
        return getRestSTSPublishEndpointUrl() + SharedSTSConstants.PUBLISH_SERVICE_CREATE_ACTION_URL_ELEMENT;
    }

    private String getSoapSTSInstanceCreationUrl() {
        return getSoapSTSPublishEndpointUrl() + SharedSTSConstants.PUBLISH_SERVICE_CREATE_ACTION_URL_ELEMENT;
    }

    private String getRestSTSPublishEndpointUrl() {
        return getAMDeploymentUrl() + SharedSTSConstants.REST_PUBLISH_SERVICE_URL_ELEMENT;
    }

    private String getSoapSTSPublishEndpointUrl() {
        return getAMDeploymentUrl() + SharedSTSConstants.SOAP_PUBLISH_SERVICE_URL_ELEMENT;
    }

    private String getAMDeploymentUrl() {
        return AMSystemConfig.serverURL + AMSystemConfig.serverDeploymentURI;
    }

    private STSInstanceModelResponse invokeSTSInstancePublish(STSType stsType, String invocationPayload) throws IOException {
        URL url;
        if (stsType.isRestSTS()) {
            url = new URL(getRestSTSInstanceCreationUrl());
        } else {
            url = new URL(getSoapSTSInstanceCreationUrl());
        }
        HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        connection.setRequestProperty(SharedSTSConstants.CREST_VERSION_HEADER_KEY,
                stsType.isRestSTS() ? REST_STS_PUBLISH_SERVICE_VERSION : SOAP_STS_PUBLISH_SERVICE_VERSION);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
            return STSInstanceModelResponse.success(getSuccessMessage(connection));
        } else {
            return STSInstanceModelResponse.failure(getErrorMessage(connection));
        }
    }

    private STSInstanceModelResponse invokeSTSInstanceDeletion(STSType stsType, String instanceId) throws IOException {
        URL url;
        if (stsType.isRestSTS()) {
            url = new URL(getRestSTSInstanceDeletionUrl(instanceId));
        } else {
            url = new URL(getSoapSTSInstanceDeletionUrl(instanceId));
        }
        HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        connection.setRequestProperty(SharedSTSConstants.CREST_VERSION_HEADER_KEY,
                stsType.isRestSTS() ? REST_STS_PUBLISH_SERVICE_VERSION : SOAP_STS_PUBLISH_SERVICE_VERSION);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        connection.connect();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return STSInstanceModelResponse.success(getSuccessMessage(connection));
        } else {
            return STSInstanceModelResponse.failure(getErrorMessage(connection));
        }
    }

    private STSInstanceModelResponse invokeSTSInstanceUpdate(STSType stsType, String invocationPayload, String instanceId) throws IOException {
        URL url;
        if (stsType.isRestSTS()) {
            url = new URL(getRestSTSInstanceUpdateUrl(instanceId));
        } else {
            url = new URL(getSoapSTSInstanceUpdateUrl(instanceId));
        }
        HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        connection.setRequestProperty(SharedSTSConstants.CREST_VERSION_HEADER_KEY,
                stsType.isRestSTS() ? REST_STS_PUBLISH_SERVICE_VERSION : SOAP_STS_PUBLISH_SERVICE_VERSION);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return STSInstanceModelResponse.success(getSuccessMessage(connection));
        } else {
            return STSInstanceModelResponse.failure(getErrorMessage(connection));
        }
    }

    private String getAdminSessionTokenCookie() {
        return SystemPropertiesManager.get(Constants.AM_COOKIE_NAME) + EQUALS + getUserSSOToken().getTokenID().toString();
    }
}
