/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2014 ForgeRock AS.
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
 *
 * Portions Copyrighted 2012-2014 ForgeRock AS
 */
package com.sun.identity.workflow;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.forgerock.openam.forgerockrest.entitlements.PolicyStore;
import org.forgerock.openam.forgerockrest.entitlements.PolicyStoreProvider;
import org.forgerock.openam.forgerockrest.entitlements.PrivilegePolicyStoreProvider;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryAttribute;

public class ConfigureOAuth2 extends Task {
    private static final String SERVICE_NAME = "OAuth2Provider";

    private static final String AUTHZ_CODE_LIFETIME_NAME = "forgerock-oauth2-provider-authorization-code-lifetime";
    private static final String REFRESH_TOKEN_LIFETIME_NAME = "forgerock-oauth2-provider-refresh-token-lifetime";
    private static final String ACCESS_TOKEN_LIFETIME_NAME = "forgerock-oauth2-provider-access-token-lifetime";
    private static final String ISSUE_REFRESH_TOKEN = "forgerock-oauth2-provider-issue-refresh-token";
    private static final String ISSUE_REFRESH_TOKEN_ON_REFRESHING_TOKEN = "forgerock-oauth2-provider-issue-refresh-token-on-refreshing-token";
    private static final String SCOPE_PLUGIN_CLASS= "forgerock-oauth2-provider-scope-implementation-class";

    //params
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

    private final PolicyStoreProvider storeProvider;

    public ConfigureOAuth2(){
        storeProvider = new PrivilegePolicyStoreProvider(Collections.<String, QueryAttribute>emptyMap());
    }

    public String execute(Locale locale, Map params) throws WorkflowException {
        String realm = getString(params, REALM);

        //get the service params
        String refreshTokenLifetime = getString(params, RTL);
        String accessCodeLifetime = getString(params, ACL);
        String accessTokenLifetime = getString(params, ATL);
        String issueRefreshToken = getString(params, IRT);
        String issueRefreshTokenOnRefreshing = getString(params, IRTR);
        String scopeImplementationClass = getString(params, SIC);

        //create service attrs
        Map<String,Set<String>> attrValues = new HashMap<String, Set<String>>();
        Set<String> temp = new HashSet<String>();
        temp.add(refreshTokenLifetime);
        attrValues.put(REFRESH_TOKEN_LIFETIME_NAME, temp);
        temp = new HashSet<String>();
        temp.add(accessCodeLifetime);
        attrValues.put(AUTHZ_CODE_LIFETIME_NAME, temp);
        temp = new HashSet<String>();
        temp.add(accessTokenLifetime);
        attrValues.put(ACCESS_TOKEN_LIFETIME_NAME, temp);
        temp = new HashSet<String>();
        temp.add(issueRefreshToken);
        attrValues.put(ISSUE_REFRESH_TOKEN, temp);
        temp = new HashSet<String>();
        temp.add(issueRefreshTokenOnRefreshing);
        attrValues.put(ISSUE_REFRESH_TOKEN_ON_REFRESHING_TOKEN, temp);
        temp = new HashSet<String>();
        temp.add(scopeImplementationClass);
        attrValues.put(SCOPE_PLUGIN_CLASS, temp);

        //create service
        SSOToken token;
        try {
            token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager sm = new ServiceConfigManager(SERVICE_NAME, token);
            sm.createOrganizationConfig(realm,attrValues);
        } catch (Exception e){
            throw new WorkflowException("ConfigureOAuth2.execute() : Unable to create Service");
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

                Map<String, Boolean> actions = new HashMap<String, Boolean>();
                actions.put("POST", true);
                actions.put("GET", true);

                Entitlement entitlement = new Entitlement();
                entitlement.setActionValues(actions);
                entitlement.setResourceName(policyURL);

                toStore.setSubject(new AuthenticatedUsers());
                toStore.setName(POLICY_NAME);
                toStore.setEntitlement(entitlement);

                PolicyStore policyStore = storeProvider.getPolicyStore(SubjectUtils.createSuperAdminSubject(), ROOT);
                policyStore.create(toStore);

            } catch (EntitlementException e) {
                throw new WorkflowException("ConfigureOAuth2.execute() : Unable to create policy");
            }

        }

        String messageTemplate = getMessage(MESSAGE, locale);

        return MessageFormat.format(messageTemplate, realm,
                MessageFormat.format(getMessage(createPolicy ? POLICY_CREATED : POLICY_EXISTS, locale), POLICY_NAME));
    }
}
