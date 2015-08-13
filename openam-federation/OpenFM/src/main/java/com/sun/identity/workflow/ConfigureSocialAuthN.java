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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package com.sun.identity.workflow;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.config.AuthConfigurationEntry;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.openam.utils.MapHelper;
import org.forgerock.util.Function;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.workflow.ParameterKeys.*;
import static org.forgerock.openam.utils.CollectionUtils.*;

public class ConfigureSocialAuthN extends Task {

    private static final Debug DEBUG = Debug.getInstance("workflow");

    private static final String SERVICE_NAME = "socialAuthNService";
    private static final String SERVICE_DISPLAY_NAME = "socialAuthNDisplayName";
    private static final String SERVICE_CHAIN_NAME = "socialAuthNAuthChain";
    private static final String SERVICE_ICON = "socialAuthNIcon";
    private static final String SERVICE_ENABLED = "socialAuthNEnabled";
    private static final String AUTH_MODULE_TYPE = "OAuth";
    static final String WELL_KNOWN_TOKEN_URL = "token_endpoint";
    static final String WELL_KNOWN_AUTH_URL = "authorization_endpoint";
    static final String WELL_KNOWN_PROFILE_URL = "userinfo_endpoint";
    static final String WELL_KNOWN_ISSUER = "issuer";
    static final String WELL_KNOWN_JWK = "jwks_uri";
    static final String AUTH_MODULE_AUTH_URL = "iplanet-am-auth-oauth-auth-service";
    static final String AUTH_MODULE_TOKEN_URL = "iplanet-am-auth-oauth-token-service";
    static final String AUTH_MODULE_USER_PROFILE_URL = "iplanet-am-auth-oauth-user-profile-service";
    static final String AUTH_MODULE_ISSUER = "openam-auth-openidconnect-issuer-name";
    static final String AUTH_MODULE_CLIENT_ID = "iplanet-am-auth-oauth-client-id";
    static final String AUTH_MODULE_CLIENT_SECRET = "iplanet-am-auth-oauth-client-secret";
    static final String AUTH_MODULE_PROXY_URL = "iplanet-am-auth-oauth-sso-proxy-url";
    static final String AUTH_MODULE_CRYPTO_TYPE = "openam-auth-openidconnect-crypto-context-type";
    static final String AUTH_MODULE_CRYPTO_VALUE = "openam-auth-openidconnect-crypto-context-value";
    static final String AUTH_MODULE_CREATE_PASSWORD = "org-forgerock-auth-oauth-prompt-password-flag";

    @Override
    public String execute(Locale locale, Map params) throws WorkflowException {

        String realm = getNonEmptyString(params, P_REALM, "missing-realm");
        String type = getNonEmptyString(params, P_TYPE, "missing-type");

        Map<String, Set<String>> attrs = collectAuthModuleAttributes(locale, type, params);
        String providerName = getValidatedField(type, params, attrs, P_PROVIDER_NAME, "missing-provider-name");
        String imageUrl = getValidatedField(type, params, attrs, P_IMAGE_URL, "missing-image-url");

        String authNamePrefix = providerName.replaceAll("\\W", "");
        String authModuleName = authNamePrefix + "SocialAuthentication";
        String authChainName = authNamePrefix + "SocialAuthenticationService";

        if (authModuleExists(realm, authModuleName)) {
            throw new WorkflowException("auth-module-exists", new Object[]{authModuleName});
        }
        if (authChainExists(realm, authChainName)) {
            throw new WorkflowException("auth-chain-exists", new Object[]{authChainName});
        }

        createAuthModule(realm, authModuleName, attrs);
        createSocialAuthenticationChain(realm, authModuleName, authChainName);
        createOrModifySocialService(realm, authChainName, providerName, imageUrl);

        String messageTemplate = getMessage("social.authn.configured", locale);
        return MessageFormat.format(messageTemplate, providerName, authModuleName, authChainName, SERVICE_NAME);
    }

    /**
     * Grab the value specified from the incoming map, throwing a WorkflowException with the specified error key if the
     * value is null or empty.
     *
     * @param params The incoming map.
     * @param name The specified value to retrieve.
     * @param errorKey Error key used if the retrieved value is null or empty.
     * @return
     * @throws WorkflowException thrown if the value found in the map is null or empty
     */
    private String getNonEmptyString(Map<String, ?> params, String name, String errorKey) throws WorkflowException {
        String value = getString(params, name);
        if (value == null || value.isEmpty()) {
            throw new WorkflowException(errorKey, null);
        }
        return value;
    }

    String getValidatedClientSecret(Map<String, ?> params) throws WorkflowException {
        String clientSecret = getNonEmptyString(params, P_CLIENT_SECRET, "missing-clientSecret");
        String clientSecretConfirm = getNonEmptyString(params, P_CLIENT_SECRET_CONFIRM, "missing-clientSecretConfirm");
        if (!clientSecret.equals(clientSecretConfirm)) {
            throw new WorkflowException("secrets-doesnt-match", null);
        }
        return clientSecret;
    }

    String getValidatedRedirectUrl(Map<String, ?> params) throws WorkflowException {
        String redirectUrl = getNonEmptyString(params, P_REDIRECT_URL, "missing-redirectUrl");
        try {
            URL url = new URL(redirectUrl);
        } catch (MalformedURLException murle) {
            throw new WorkflowException("invalid-redirectUrl", null);
        }
        return redirectUrl;
    }

    String getValidatedField(String type, Map<String, ?> params, Map<String, Set<String>> attrs, String field,
            String errorKey) throws WorkflowException {
        if ("other".equals(type)) {
            return getNonEmptyString(params, field, errorKey);
        } else {
            String value = CollectionHelper.getMapAttr(attrs, field);
            if (StringUtils.isEmpty(value)) {
                throw new WorkflowException(errorKey, null);
            }
            return value;
        }
    }

    private boolean authModuleExists(String realm, String authModuleName) throws WorkflowException {
        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(getAdminToken(), realm);
            for (AMAuthenticationInstance instance : mgr.getAuthenticationInstances()) {
                if (authModuleName.equals(instance.getName())) {
                    return true;
                }
            }
            return false;

        } catch (AMConfigurationException e) {
            DEBUG.error("An error occurred while creating/modifying social authentication module", e);
            throw new WorkflowException("social-service-error", null);
        }
    }

    private boolean authChainExists(String realm, String authChainName) throws WorkflowException {
        try {
            return AMAuthConfigUtils.getAllNamedConfig(realm, getAdminToken()).contains(authChainName);

        } catch (SMSException e) {
            DEBUG.error("An error occurred while creating/modifying social authentication chain", e);
            throw new WorkflowException("social-service-error", null);
        } catch (SSOException e) {
            DEBUG.warning("A session error occurred while creating/modifying social authentication chain", e);
            throw new WorkflowException("social-service-error", null);
        }
    }

    Map<String, Set<String>> collectAuthModuleAttributes(Locale locale, String type,
            Map<String, Set<String>> params) throws WorkflowException {
        Map<String, Set<String>> attrs = new CaseInsensitiveHashMap();

        attrs.putAll(readPropertiesFile(type));

        String clientId = getNonEmptyString(params, P_CLIENT_ID, "missing-clientId");
        String clientSecret = getValidatedClientSecret(params);
        String redirectUrl = getValidatedRedirectUrl(params);

        attrs.put(AUTH_MODULE_CLIENT_ID, asSet(clientId));
        attrs.put(AUTH_MODULE_CLIENT_SECRET, asSet(clientSecret));
        attrs.put(AUTH_MODULE_PROXY_URL, asSet(redirectUrl));
        attrs.put(AUTH_MODULE_CRYPTO_TYPE, asSet("client_secret"));
        attrs.put(AUTH_MODULE_CREATE_PASSWORD, asSet("false"));

        // If the wizard didn't provide a discoveryUrl, see if it was in the properties file
        String discoveryUrl = getString(params, P_OPENID_DISCOVERY_URL);
        if (discoveryUrl == null) {
            discoveryUrl = CollectionHelper.getMapAttr(attrs, P_OPENID_DISCOVERY_URL);
        }

        if (discoveryUrl != null) {
            attrs.putAll(readOpenIDWellKnownConfig(locale, discoveryUrl));
        }

        return attrs;
    }

    /**
     * Find a properties file, if it exists, and read its contents into a Map.
     * @param type The type of provider we're setting up e.g. "google", "facebook", "microsoft", or "other"
     * @return A map containing the property file contents.  The empty map if the file could not be found.
     */
    Map<String, Set<String>> readPropertiesFile(String type) {

        String propertyFileName = type + ".properties";
        try {
            return MapHelper.readMap(ConfigureSocialAuthN.class.getResourceAsStream(propertyFileName));
        } catch (IOException ioe) {
            DEBUG.warning("Caught IOException while reading properties file " + propertyFileName, ioe);
            return Collections.emptyMap();
        }
    }

    /**
     * Gets the discovery URL from the map, fetches the well known config and adds appropriate values to the map.
     * @param locale
     * @param url The discovery url.
     */
    Map<String, Set<String>> readOpenIDWellKnownConfig(Locale locale, String url)
            throws WorkflowException {

        String configurationContents = getWebContent(locale, url);
        Map<String, Object> config =  Utils.parseJson(configurationContents);

        Map<String, Set<String>> attrs = new CaseInsensitiveHashMap();
        attrs.put(AUTH_MODULE_AUTH_URL, asSet((String) config.get(WELL_KNOWN_AUTH_URL)));
        attrs.put(AUTH_MODULE_TOKEN_URL, asSet((String) config.get(WELL_KNOWN_TOKEN_URL)));
        attrs.put(AUTH_MODULE_USER_PROFILE_URL, asSet((String) config.get(WELL_KNOWN_PROFILE_URL)));
        attrs.put(AUTH_MODULE_ISSUER, asSet((String) config.get(WELL_KNOWN_ISSUER)));

        String jwkURL = (String) config.get(WELL_KNOWN_JWK);
        if (jwkURL != null && !jwkURL.isEmpty()) {
            attrs.put(AUTH_MODULE_CRYPTO_TYPE, asSet("jwk_url"));
            attrs.put(AUTH_MODULE_CRYPTO_VALUE, asSet(jwkURL));
        }

        return attrs;
    }

    /**
     * Mockable method to fetch URL content.
     * @param locale Error message locale.
     * @param url Url to fetch content from.
     * @return The content.
     * @throws WorkflowException
     */
    String getWebContent(Locale locale, String url) throws WorkflowException {
        return getWebContent(url, locale);
    }

    private void createAuthModule(String realm, String authModuleName, Map<String, Set<String>> attrs)
            throws WorkflowException {

        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(getAdminToken(), realm);
            Map<String, Set<String>> moduleAttrs = mgr.getAuthenticationSchema(AUTH_MODULE_TYPE).getAttributeValues();

            // Override default attributes using the provided attrs, but skip any the auth module doesn't expect
            for (Map.Entry<String, Set<String>> attr : attrs.entrySet()) {
                if (moduleAttrs.containsKey(attr.getKey())) {
                    moduleAttrs.put(attr.getKey(), attr.getValue());
                }
            }

            mgr.createAuthenticationInstance(authModuleName, AUTH_MODULE_TYPE, moduleAttrs);

        } catch (AMConfigurationException e) {
            DEBUG.error("An error occurred while creating/modifying social authentication module", e);
            throw new WorkflowException("social-service-error", null);
        }
    }

    private void createSocialAuthenticationChain(String realm, String authModuleName, String authChainName)
            throws WorkflowException {

        try {
            String xmlConfig = AMAuthConfigUtils.authConfigurationEntryToXMLString(
                    asList(new AuthConfigurationEntry(authModuleName, "REQUIRED", null)));
            Map<String, Set<String>> configData = new HashMap<String, Set<String>>();
            configData.put("iplanet-am-auth-configuration", asSet(xmlConfig));

            AMAuthConfigUtils.createNamedConfig(authChainName, 0, configData, realm, getAdminToken());

        } catch (SMSException e) {
            DEBUG.error("An error occurred while creating/modifying social authentication chain", e);
            throw new WorkflowException("social-service-error", null);
        } catch (SSOException e) {
            DEBUG.warning("A session error occurred while creating/modifying social authentication chain", e);
            throw new WorkflowException("social-service-error", null);
        } catch (AMConfigurationException e) {
            DEBUG.error("An error occurred while creating/modifying social authentication chain", e);
            throw new WorkflowException("social-service-error", null);
        }
    }

    /**
     * Create a social service given the attributes specified.
     * @param realm The realm/organisation
     * @param chainName
     * @param providerName
     * @param icon
     * @throws WorkflowException if there are problems modifying or assigning the service.
     */
    private void createOrModifySocialService(String realm, String chainName, String providerName, String icon)
            throws WorkflowException {

        try {
            SSOToken token = getAdminToken();
            OrganizationConfigManager ocm = new OrganizationConfigManager(token, realm);

            Map<String, Set<String>> attrs = new HashMap<String, Set<String>>(4);
            String prefix = "[" + providerName + "]=";
            attrs.put(SERVICE_DISPLAY_NAME, asSet(prefix + providerName));
            attrs.put(SERVICE_CHAIN_NAME, asSet(prefix + chainName));
            attrs.put(SERVICE_ICON, asSet(prefix + icon));
            attrs.put(SERVICE_ENABLED, asSet(providerName));
            if (ocm.getAssignedServices().contains(SERVICE_NAME)) {
                ServiceConfig serviceConfig = ocm.getServiceConfig(SERVICE_NAME);
                serviceConfig.setAttributes(mergeAttributes(serviceConfig.getAttributesWithoutDefaults(), attrs));
            } else {
                ocm.assignService(SERVICE_NAME, attrs);
            }

        } catch (SMSException smse) {
            DEBUG.error("An error occurred while creating/modifying social authentication service", smse);
            throw new WorkflowException("social-service-error", null);
        } catch (SSOException ssoe) {
            DEBUG.warning("A session error occurred while creating/modifying social authentication service", ssoe);
            throw new WorkflowException("social-service-error", null);
        }
    }

    /**
     * @return The SSO token.
     */
    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Carefully merge the values in two maps.  The existing ("base") attributes are added first, then values in
     * newAttrs.
     *
     * @param existingAttrs The "base" attributes.
     * @param newAttrs Coinciding attributes from here overwrite ones in existing attributes.
     * @return A map containing a combination of the two maps.
     */
    Map<String, Set<String>> mergeAttributes(Map<String, Set<String>> existingAttrs,
            Map<String, Set<String>> newAttrs) {
        Map<String, Set<String>> mergedAttrs = new CaseInsensitiveHashMap(existingAttrs);
        for (Map.Entry<String, Set<String>> attr : newAttrs.entrySet()) {
            Set<String> values = attr.getValue();
            Set<String> existingValues = mergedAttrs.get(attr.getKey());
            if (existingValues == null) {
                existingValues = new CaseInsensitiveHashSet();
                mergedAttrs.put(attr.getKey(), existingValues);
            }
            existingValues.addAll(values);
        }
        return mergedAttrs;
    }
}
