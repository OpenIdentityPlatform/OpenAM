/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]".
 */
package com.sun.identity.workflow;

import static java.text.MessageFormat.format;
import static java.util.Collections.*;
import static org.forgerock.oauth2.core.OAuth2Constants.OAuth2ProviderService.*;
import static org.forgerock.oauth2.core.OAuth2Constants.AuthorizationEndpoint.*;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.guava.common.collect.ImmutableSet;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.core.AuthorizationCodeResponseTypeHandler;
import org.forgerock.oauth2.core.TokenResponseTypeHandler;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.forgerock.openam.entitlement.rest.PolicyStore;
import org.forgerock.openam.entitlement.rest.PolicyStoreProvider;
import org.forgerock.openam.entitlement.rest.PrivilegePolicyStoreProvider;
import org.forgerock.openam.entitlement.rest.query.QueryAttribute;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.IdTokenResponseTypeHandler;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSUtils;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

public class ConfigureOAuth2 extends Task {

    private static final Map<String, String> OIDC_SCOPES = new ImmutableMap.Builder<String, String>()
            .put("openid", "")
            .put("email", "openidconnect.scopes.email")
            .put("address", "openidconnect.scopes.address")
            .put("phone", "openidconnect.scopes.phone")
            .put("profile", "openidconnect.scopes.profile")
            .build();

    private static final Map<String, String> OIDC_CLAIMS = new ImmutableMap.Builder<String, String>()
            .put("email", "openidconnect.claims.email")
            .put("address", "openidconnect.claims.address")
            .put("phone_number", "openidconnect.claims.phonenumber")
            .put("given_name", "openidconnect.claims.givenname")
            .put("zoneinfo", "openidconnect.claims.zoneinfo")
            .put("family_name", "openidconnect.claims.familyname")
            .put("locale", "openidconnect.claims.locale")
            .put("name", "openidconnect.claims.name")
            .put("profile", "openidconnect.scopes.profile")
            .build();

    private static final Set<String> OIDC_DEFAULT_SCOPES = asSet("openid", "profile", "email", "address", "phone");

    private static final Map<String, Set<String>> COMMON_OIDC_UMA_ATTRIBUTES =
            new ImmutableMap.Builder<String, Set<String>>()
                    .put(SUBJECT_TYPES_SUPPORTED, singleton("public"))
                    .put(ID_TOKEN_SIGNING_ALGORITHMS, asSet("HS256", "HS384", "HS512", "RS256"))
                    .put(RESPONSE_TYPE_LIST, asSet(
                            TOKEN + "|" + TokenResponseTypeHandler.class.getName(),
                            CODE + "|" + AuthorizationCodeResponseTypeHandler.class.getName(),
                            ID_TOKEN + "|" + IdTokenResponseTypeHandler.class.getName()
                    ))
                    .build();

    private static final Map<String, Set<String>> OIDC_ATTRIBUTES = new ImmutableMap.Builder<String, Set<String>>()
            .putAll(COMMON_OIDC_UMA_ATTRIBUTES)
            .put(DEFAULT_SCOPES, OIDC_DEFAULT_SCOPES)
            .build();

    private static final Map<String, Set<String>> MOBILE_CONNECT_ATTRIBUTES =
            new ImmutableMap.Builder<String, Set<String>>()
                    .putAll(OIDC_ATTRIBUTES)
                    .put(CREATED_TIMESTAMP_ATTRIBUTE_NAME, singleton("createTimestamp"))
                    .put(MODIFIED_TIMESTAMP_ATTRIBUTE_NAME, singleton("modifyTimestamp"))
                    .build();

    private static final Map<String, Set<String>> UMA_ATTRIBUTES =
            new ImmutableMap.Builder<String, Set<String>>()
                    .putAll(COMMON_OIDC_UMA_ATTRIBUTES)
                    .put(DEFAULT_SCOPES, new ImmutableSet.Builder<String>()
                            .addAll(OIDC_DEFAULT_SCOPES).add("uma_protection").add("uma_authorization").build())
                    .build();

    private static final Map<String, Map<String, Set<String>>> PROFILE_SETTINGS =
            new ImmutableMap.Builder<String, Map<String, Set<String>>>()
                    .put("oauth2", Collections.<String, Set<String>>emptyMap())
                    .put("oidc", OIDC_ATTRIBUTES)
                    .put("mobileconnect", MOBILE_CONNECT_ATTRIBUTES)
                    .put("uma", UMA_ATTRIBUTES)
                    .build();

    private static final Map<String, Map<String, String>> SUPPORTED_SCOPE_KEYS =
            new ImmutableMap.Builder<String, Map<String, String>>()
                    .put("oauth2", Collections.<String, String>emptyMap())
                    .put("oidc", OIDC_SCOPES)
                    .put("mobileconnect", OIDC_SCOPES)
                    .put("uma", new ImmutableMap.Builder<String, String>()
                            .putAll(OIDC_SCOPES)
                            .put("uma_protection", "uma.scopes.umaprotection")
                            .put("uma_authorization", "uma.scopes.umaauthorization")
                            .build())
                    .build();

    private static final Map<String, Map<String, String>> SUPPORTED_CLAIM_KEYS =
            new ImmutableMap.Builder<String, Map<String, String>>()
                    .put("oauth2", Collections.<String, String>emptyMap())
                    .put("oidc", OIDC_CLAIMS)
                    .put("mobileconnect", OIDC_CLAIMS)
                    .put("uma", OIDC_CLAIMS)
                    .build();

    private static final Debug DEBUG = Debug.getInstance("workflow");

    private static final String OAUTH2_SERVICE_NAME = "OAuth2Provider";
    private static final String UMA_SERVICE_NAME = "UmaProvider";

    //params
    private static final String TYPE = "type";
    private static final String REALM = "realm";
    private static final String ROOT = "/";

    //service params
    private static final String RTL = "rtl";
    private static final String ACL = "acl";
    private static final String ATL = "atl";
    private static final String IRT = "irt";
    private static final String IRTR = "irtr";
    private static final String SIC = "sic";

    //policy params
    private static final String POLICY_NAME = "OAuth2ProviderPolicy";
    private static final String OAUTH2_AUTHORIZE_ENDPOINT = "/oauth2/authorize?*";
    public static final String MESSAGE = "oauth2.provider.configured";
    public static final String POLICY_CREATED = "oauth2.provider.policy.created";
    public static final String POLICY_EXISTS = "oauth2.provider.policy.exists";
    public static final String UMA_SERVICE_CREATED = "oauth2.provider.created.uma";

    private final PolicyStoreProvider storeProvider;

    public ConfigureOAuth2(){
        storeProvider = new PrivilegePolicyStoreProvider(Collections.<String, QueryAttribute>emptyMap());
    }

    public String execute(Locale locale, Map params) throws WorkflowException {
        final String type = getString(params, TYPE);
        final String realm = getString(params, REALM);
        final SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());

        if (StringUtils.isEmpty(type)) {
            throw new WorkflowException("type parameter is required");
        }


        //replace service attributes
        final Map<String, Set<String>> attrValues = getDefaultOAuth2ProviderAttributes(token);
        attrValues.putAll(PROFILE_SETTINGS.get(type));

        attrValues.put(SUPPORTED_SCOPES, translate(realm, SUPPORTED_SCOPE_KEYS.get(type)));
        attrValues.put(SUPPORTED_CLAIMS, translate(realm, SUPPORTED_CLAIM_KEYS.get(type)));

        attrValues.put(REFRESH_TOKEN_LIFETIME_NAME, singleton(getString(params, RTL)));
        attrValues.put(AUTHZ_CODE_LIFETIME_NAME, singleton(getString(params, ACL)));
        attrValues.put(ACCESS_TOKEN_LIFETIME_NAME, singleton(getString(params, ATL)));
        attrValues.put(ISSUE_REFRESH_TOKEN, singleton(getString(params, IRT)));
        attrValues.put(ISSUE_REFRESH_TOKEN_ON_REFRESHING_TOKEN, singleton(getString(params, IRTR)));
        attrValues.put(SCOPE_PLUGIN_CLASS, singleton(getString(params, SIC)));

        createProvider(OAUTH2_SERVICE_NAME, token, realm, attrValues);

        final boolean createUmaService = "uma".equals(type);
        if (createUmaService) {
            createProvider(UMA_SERVICE_NAME, token, realm, Collections.<String, Set<String>>emptyMap());
        }

        String policyURL = getRequestURL(params) + OAUTH2_AUTHORIZE_ENDPOINT;

        //check if policy exists
        PolicyManager mgr;
        boolean createPolicy = false;
        try {
            mgr = new PolicyManager(token, ROOT);
            if (mgr.getPolicy(POLICY_NAME) == null) {
                createPolicy = true;
            }
        } catch (Exception e){
            createPolicy = true;
        }

        if (createPolicy){

            try {
                Privilege toStore = Privilege.getNewInstance();

                Map<String, Boolean> actions = new HashMap<>();
                actions.put("POST", true);
                actions.put("GET", true);

                Entitlement entitlement = new Entitlement();
                entitlement.setActionValues(actions);
                entitlement.setResourceName(policyURL);

                Subject adminSubject = SubjectUtils.createSuperAdminSubject();
                toStore.setResourceTypeUuid(getUrlResourceTypeId(entitlement, adminSubject, realm));
                toStore.setSubject(new AuthenticatedUsers());
                toStore.setName(POLICY_NAME);
                toStore.setEntitlement(entitlement);

                PolicyStore policyStore = storeProvider.getPolicyStore(adminSubject, ROOT);
                policyStore.create(toStore);

            } catch (EntitlementException e) {
                DEBUG.error("ConfigureOAuth2.execute() : Unable to create policy", e);
                throw new WorkflowException("oauth2.provider.policy.failed");
            }

        }

        String messageTemplate = getMessage(MESSAGE, locale);

        return format(messageTemplate, createUmaService ? getMessage(UMA_SERVICE_CREATED, locale) : "", realm,
                format(getMessage(createPolicy ? POLICY_CREATED : POLICY_EXISTS, locale), POLICY_NAME));
    }

    private Set<String> translate(String realm, Map<String, String> values) {
        Set<String> result = new HashSet<>();
        ISLocaleContext localeContext = new ISLocaleContext(realm);
        ResourceBundle bundle = ResourceBundle.getBundle("oauth2-default-user-descriptions", localeContext.getLocale());
        for (Map.Entry<String, String> value : values.entrySet()) {
            if (StringUtils.isNotEmpty(value.getValue())) {
                result.add(value.getKey() + "|" + bundle.getString(value.getValue()));
            } else {
                result.add(value.getKey() + "|");
            }
        }
        return result;
    }

    private String getUrlResourceTypeId(Entitlement entitlement, Subject adminSubject, String realm)
            throws EntitlementException, WorkflowException {

        ResourceTypeService resourceTypeService = InjectorHolder.getInstance(ResourceTypeService.class);
        Application application = entitlement.getApplication(adminSubject, realm);
        Set<String> resourceTypeIds = application.getResourceTypeUuids();
        for (String id : resourceTypeIds) {
            ResourceType resourceType = resourceTypeService.getResourceType(adminSubject, realm, id);
            if ("URL".equalsIgnoreCase(resourceType.getName())) {
                return id;
            }
        }
        DEBUG.error("Could not find URL resource type on {} application. Found: {}", entitlement.getApplicationName(),
                resourceTypeIds.toString());
        throw new WorkflowException("oauth2.provider.resourceType.error", entitlement.getApplicationName());
    }

    private Map<String, Set<String>> getDefaultOAuth2ProviderAttributes(SSOToken token) throws WorkflowException {
        try {
            final ServiceSchema serviceSchema = new ServiceSchemaManager(OAUTH2_SERVICE_NAME, token).getOrganizationSchema();
            return SMSUtils.removeValidators(serviceSchema.getReadOnlyAttributeDefaults(), serviceSchema);
        } catch (SMSException e) {
            DEBUG.error("An error occurred while trying to read the default OAuth2 Provider settings.", e);
            throw new WorkflowException("oauth2.provider.read.error", null);
        } catch (SSOException e) {
            DEBUG.error("An error occurred while trying to read the default OAuth2 Provider settings.", e);
            throw new WorkflowException("oauth2.provider.read.error", null);
        }
    }

    private void createProvider(String serviceName, SSOToken token, String realm, Map<String, Set<String>> attrValues)
            throws WorkflowException {
        try {
            new ServiceConfigManager(serviceName, token).createOrganizationConfig(realm, attrValues);
        } catch (SMSException e) {
            DEBUG.error("An error occurred while trying to create the OAuth2 Provider.", e);
            throw new WorkflowException("oauth2.provider.create.error", null);
        } catch (SSOException e) {
            DEBUG.error("An error occurred while trying to create the OAuth2 Provider.", e);
            throw new WorkflowException("oauth2.provider.create.error", null);
        }
    }
}
