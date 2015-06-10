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
 * Copyright 2014-2015 ForgeRock AS. All rights reserved.
 */

package com.sun.identity.console.reststs.model;

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
import org.forgerock.json.fluent.JsonValue;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * @see com.sun.identity.console.reststs.model.RestSTSModel
 * This class extends the AMServiceProfileModelImpl because this class provides functionality for reading values corresponding
 * to propertySheets.
 *
 */
public class RestSTSModelImpl extends AMServiceProfileModelImpl implements RestSTSModel {
    private static final String COOKIE = "Cookie";
    /*
    A string matching a regular expression which will match the '|' character (u007c), which needs to be escaped to
    escape its regular-expression semantics. Equivalent to "\|", which is not a valid string.
     */
    private static final String REGEX_PIPE = "\\u007c";
    private static final String EQUALS = "=";
    private static final String REST_STS_PUBLISH_SERVICE_VERSION = "protocol=1.0, resource=1.0";

    public RestSTSModelImpl(HttpServletRequest req, Map map) throws AMConsoleException {
        super(req, AMAdminConstants.REST_STS_SERVICE, map);
    }

    public Set<String> getPublishedInstances(String realm) throws AMConsoleException {
        try {
            ServiceConfig baseService = new ServiceConfigManager(AMAdminConstants.REST_STS_SERVICE,
                    getUserSSOToken()).getOrganizationConfig(realm, null);
            if (baseService != null) {
                return baseService.getSubConfigNames();
            } else {
                return Collections.EMPTY_SET;
            }
        } catch (SMSException e) {
            throw new AMConsoleException(e);
        } catch (SSOException e) {
            throw new AMConsoleException(e);
        }
    }

    public void deleteInstances(Set<String> instanceNames) throws AMConsoleException {
        for (String instanceName : instanceNames) {
            try {
                RestSTSModelResponse response = deleteInstance(instanceName);
                if (!response.isSuccessful()) {
                    throw new AMConsoleException(response.getMessage());
                }
            } catch (IOException e) {
                throw new AMConsoleException(e);
            }
        }
    }

    public RestSTSModelResponse createInstance(Map<String, Set<String>> configurationState, String realm) throws AMConsoleException {
        addProgrammaticConfigurationState(configurationState, realm);
        JsonValue invocationJson = createInstanceInvocationState(configurationState);
        try {
            return invokeRestSTSInstancePublish(invocationJson.toString());
        } catch (IOException e) {
            throw new AMConsoleException(e);
        }
    }

    public RestSTSModelResponse updateInstance(Map<String, Set<String>> configurationState, String realm, String instanceName) throws AMConsoleException {
        addProgrammaticConfigurationState(configurationState, realm);
        JsonValue invocationJson = createInstanceInvocationState(configurationState);
        try {
            return invokeRestSTSInstanceUpdate(invocationJson.toString(), instanceName);
        } catch (IOException e) {
            throw new AMConsoleException(e);
        }
    }

    public Map<String, Set<String>> getInstanceState(String realm, String instanceName) throws AMConsoleException {
        try {
            ServiceConfig baseService = new ServiceConfigManager(AMAdminConstants.REST_STS_SERVICE,
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
        } catch (SMSException e) {
            throw new AMConsoleException(e);
        } catch (SSOException e) {
            throw new AMConsoleException(e);
        }
    }

    public RestSTSModelResponse validateConfigurationState(Map<String, Set<String>> configurationState) {
        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.DEPLOYMENT_URL_ELEMENT))) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.deployment.url.message"));
        } else {
            String urlElement = configurationState.get(SharedSTSConstants.DEPLOYMENT_URL_ELEMENT).iterator().next();
            if (urlElement.contains(SharedSTSConstants.FORWARD_SLASH)) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.deployment.url.content.message"));
            }
        }

        final Set<String> supportedTokenTransforms = configurationState.get(SharedSTSConstants.SUPPORTED_TOKEN_TRANSFORMS);
        if (CollectionUtils.isEmpty(supportedTokenTransforms)) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.tokentransforms.message"));
        }
        /*
        Need to check if selected transforms include both the validate_interim_session and !invalidate_interim_session
        flavors. If the token transformation set includes two entries for a specific input token type, then this is the
        case, and the configuration must be rejected.
         */
        if (duplicateTransformsSpecified(supportedTokenTransforms)) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.tokentransforms.duplicate.message"));
        }

        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.ISSUER_NAME)) &&
                StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_ISSUER))) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.output.token.configuration.message"));
        }

        //block above will insure that either the SAML2 issuer or the OIDC issuer has been set if we reach here, so null will
        //not be returned
        RestSTSModelResponse response = null;
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

    private RestSTSModelResponse validateSAML2ConfigurationState(Map<String, Set<String>> configurationState) {
        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_TOKEN_LIFETIME))) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.saml2.token.lifetime.message"));
        }

        if (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_SIGN_ASSERTION, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ASSERTION, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ATTRIBUTES, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_NAME_ID, false)) {

            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_KEYSTORE_FILE_NAME))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.saml2.keystore.filename.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_KEYSTORE_PASSWORD))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.saml2.keystore.password.message"));
            }
        }
        if (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_SIGN_ASSERTION, false)) {
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_SIGNATURE_KEY_ALIAS))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.saml2.keystore.signature.keyalias.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_SIGNATURE_KEY_PASSWORD))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.saml2.keystore.signature.keypassword.message"));
            }
        }
        if (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ASSERTION, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ATTRIBUTES, false)
                || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_NAME_ID, false)) {

            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPTION_KEY_ALIAS))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.saml2.keystore.encryption.keyalias.message"));
            }
        }
        if (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ASSERTION, false)
                && (CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_ATTRIBUTES, false)
                    || CollectionHelper.getBooleanMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPT_NAME_ID, false))) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.saml2.encryptioncombinations.message"));
        }

        if (!CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.SAML2_ATTRIBUTE_MAP))) {
            if (!attributeMappingCorrectFormat(configurationState.get(SharedSTSConstants.SAML2_ATTRIBUTE_MAP))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.saml2.claim.map.incorrect.format.message"));
            }
        }

        return RestSTSModelResponse.success();
    }

    private RestSTSModelResponse validateOIDCConfigurationState(Map<String, Set<String>> configurationState) {
        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_TOKEN_LIFETIME))) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.token.lifetime.message"));
        }

        boolean rsaSignature = false;
        if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_SIGNATURE_ALGORITHM))) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.signature.algorithm.message"));
        } else {
            rsaSignature = rsaSignatureForOIDC(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_SIGNATURE_ALGORITHM));
        }

        if (rsaSignature) {
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_KEYSTORE_LOCATION))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.keystore.location.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_KEYSTORE_PASSWORD))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.keystore.password.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_SIGNATURE_KEY_ALIAS))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.keystore.signature.keyalias.message"));
            }
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_SIGNATURE_KEY_PASSWORD))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.keystore.signature.keypassword.message"));
            }
        } else {
            if (StringUtils.isEmpty(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.OIDC_CLIENT_SECRET))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.client.secret.missing.message"));
            }
        }

        if (CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.OIDC_AUDIENCE))) {
            return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.audience.not.specified.message"));
        }

        if (!CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.OIDC_CLAIM_MAP))) {
            if (!attributeMappingCorrectFormat(configurationState.get(SharedSTSConstants.OIDC_CLAIM_MAP))) {
                return RestSTSModelResponse.failure(getLocalizedString("rest.sts.validation.oidc.claim.map.incorrect.format.message"));
            }
        }
        return RestSTSModelResponse.success();
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

    /**
     * The set of possible token transformation definition selections, as defined in the supported-token-transforms property
     * in propertyRestSecurityTokenService.xml, is as follow:
     *      USERNAME|SAML2|true
     *      USERNAME|SAML2|false
     *      OPENIDCONNECT|SAML2|true
     *      OPENIDCONNECT|SAML2|false
     *      OPENAM|SAML2|true
     *      OPENAM|SAML2|false
     *      X509|SAML2|true
     *      X509|SAML2|false
     *      USERNAME|OPENIDCONNECT|true
     *      USERNAME|OPENIDCONNECT|false
     *      OPENIDCONNECT|OPENIDCONNECT|true
     *      OPENIDCONNECT|OPENIDCONNECT|false
     *      OPENAM|OPENIDCONNECT|true
     *      OPENAM|OPENIDCONNECT|false
     *      X509|OPENIDCONNECT|true
     *      X509|OPENIDCONNECT|false
     * This method will return true if the supportedTokenTransforms method specified by the user contains more than a single
     * entry for a given input token type per given output token type.
     * @param supportedTokenTransforms The set of supported token transformations specified by the user
     * @return true if duplicate transformations are specified - i.e. the user cannot specify token transformations with
     * USERNAME input which specify that interim OpenAM sessions should be, and should not be, invalidated.
     */
    private boolean duplicateTransformsSpecified(Set<String> supportedTokenTransforms) {
        Set<String> inputOutputComboSet = new HashSet<>(supportedTokenTransforms.size());
        for (String transform : supportedTokenTransforms) {
            String[] breakdown = transform.split(REGEX_PIPE);
            String entry = breakdown[0] + breakdown[1];
            if (inputOutputComboSet.contains(entry)) {
                return true;
            } else {
                inputOutputComboSet.add(entry);
            }
        }
        return false;
    }

    /*
    Add the realm, as this information does not have to be solicited from the user. Also add the encryption strength
    parameter, as this value is hard-coded based upon the encryption algorithm type, and necessary only if the
    FMEncProvider is over-ridden. See comment in SAML2Config.SAML2ConfigBuilder#encryptionAlgorithmStrength for details.
     */
    private void addProgrammaticConfigurationState(Map<String, Set<String>> configurationState, String realm) {
        configurationState.put(SharedSTSConstants.DEPLOYMENT_REALM, CollectionUtils.asSet(realm));
        final String encryptionAlgorithmStrength =
                getEncryptionStrengthFromEncryptionAlgorithm(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.SAML2_ENCRYPTION_ALGORITHM));
        configurationState.put(SharedSTSConstants.SAML2_ENCRYPTION_ALGORITHM_STRENGTH,
                CollectionUtils.asSet(encryptionAlgorithmStrength));
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

    private String getRestSTSInstanceUpdateUrl(String instanceId) {
        return getRestSTSInstanceDeletionUrl(instanceId);
    }

    private String getRestSTSInstanceCreationUrl() {
        return getRestSTSPublishEndpointUrl() + SharedSTSConstants.REST_PUBLISH_SERVICE_CREATE_ACTION_URL_ELEMENT;
    }

    private String getRestSTSPublishEndpointUrl() {
        return getAMDeploymentUrl() + SharedSTSConstants.REST_PUBLISH_SERVICE_URL_ELEMENT;
    }

    private String getAMDeploymentUrl() {
        return AMSystemConfig.serverURL + AMSystemConfig.serverDeploymentURI;
    }

    private RestSTSModelResponse deleteInstance(String instanceId) throws IOException {
        return invokeRestSTSInstanceDeletion(getRestSTSInstanceDeletionUrl(instanceId));
    }

    private RestSTSModelResponse invokeRestSTSInstancePublish(String invocationPayload) throws IOException {
        URL url = new URL(getRestSTSInstanceCreationUrl());
        HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        connection.setRequestProperty(SharedSTSConstants.CREST_VERSION_HEADER_KEY, REST_STS_PUBLISH_SERVICE_VERSION);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
            return RestSTSModelResponse.success(getSuccessMessage(connection));
        } else {
            return RestSTSModelResponse.failure(getErrorMessage(connection));
        }
    }

    private RestSTSModelResponse invokeRestSTSInstanceDeletion(String deletionUrl) throws IOException {
        URL url = new URL(deletionUrl);
        HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        connection.setRequestProperty(SharedSTSConstants.CREST_VERSION_HEADER_KEY, REST_STS_PUBLISH_SERVICE_VERSION);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        connection.connect();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return RestSTSModelResponse.success(getSuccessMessage(connection));
        } else {
            return RestSTSModelResponse.failure(getErrorMessage(connection));
        }
    }

    private RestSTSModelResponse invokeRestSTSInstanceUpdate(String invocationPayload, String instanceId) throws IOException {
        URL url = new URL(getRestSTSInstanceUpdateUrl(instanceId));
        HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
        connection.setRequestProperty(SharedSTSConstants.CREST_VERSION_HEADER_KEY, REST_STS_PUBLISH_SERVICE_VERSION);
        connection.setRequestProperty(COOKIE, getAdminSessionTokenCookie());
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return RestSTSModelResponse.success(getSuccessMessage(connection));
        } else {
            return RestSTSModelResponse.failure(getErrorMessage(connection));
        }
    }

    private String getAdminSessionTokenCookie() {
        return SystemPropertiesManager.get(Constants.AM_COOKIE_NAME) + EQUALS + getUserSSOToken().getTokenID().toString();
    }
}
