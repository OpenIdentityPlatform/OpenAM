/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */
package com.sun.identity.workflow;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.*;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.sm.ServiceConfigManager;

import java.security.AccessController;
import java.util.*;

public class ConfigureOAuth2 extends Task {
    private static final String SERVICE_NAME = "OAuth2Provider";

    private static final String AUTHZ_CODE_LIFETIME_NAME = "forgerock-oauth2-provider-authorization-code-lifetime";
    private static final String REFRESH_TOKEN_LIFETIME_NAME = "forgerock-oauth2-provider-refresh-token-lifetime";
    private static final String ACCESS_TOKEN_LIFETIME_NAME = "forgerock-oauth2-provider-access-token-lifetime";
    private static final String ISSUE_REFRESH_TOKEN = "forgerock-oauth2-provider-issue-refresh-token";
    private static final String SCOPE_PLUGIN_CLASS= "forgerock-oauth2-provider-scope-implementation-class";

    //params
    private static final String REALM = "realm";

    //service params
    private static final String RTL = "rtl";
    private static final String ACL = "acl";
    private static final String ATL = "atl";
    private static final String IRT = "irt";
    private static final String SIC = "sic";

    //policy params
    private static final String POLICY_NAME = "OAuth2ProviderPolicy";
    private static final String RULE_NAME = "OAuth2ProviderRule";
    private static final String SUBJECT_NAME = "OAuth2ProviderSubject";
    private static final String OAUTH2_AUTHORIZE_ENDPOINT = "/oauth2/authorize?*";
    private static final String ROOT_REALM = "/";

    public ConfigureOAuth2(){

    }

    public String execute(Locale locale, Map params)
            throws WorkflowException {
        String realm = getString(params, REALM);

        //get the service params
        String refreshTokenLifetime = getString(params, RTL);
        String accessCodeLifetime = getString(params, ACL);
        String accessTokenLifetime = getString(params, ATL);
        String issueRefreshToken = getString(params, IRT);
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
        temp.add(scopeImplementationClass);
        attrValues.put(SCOPE_PLUGIN_CLASS, temp);

        //create service
        SSOToken token = null;
        try {
            token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager sm = new ServiceConfigManager(SERVICE_NAME,token);
            sm.createOrganizationConfig(realm,attrValues);
        } catch (Exception e){
            throw new WorkflowException("ConfigureOAuth2.execute() : Unable to create Service");
        }

        //get policy url
        String policyURL = getRequestURL(params) + OAUTH2_AUTHORIZE_ENDPOINT;

        //check if policy exists
        PolicyManager mgr = null;
        boolean createPolicy = false;
        try {
            mgr = new PolicyManager(token, ROOT_REALM);
            if (mgr.getPolicy(POLICY_NAME) == null){
                createPolicy = true;
            }
        } catch (Exception e){
            createPolicy = true;
        }

        if (createPolicy){
            //build the policy
            Policy policy = null;
            try {
                policy = new Policy(POLICY_NAME);
            } catch (Exception e){
                throw new WorkflowException("ConfigureOAuth2.execute() : Unable create policy");
            }
            Map<String,Set<String>> actions = new HashMap<String,Set<String>>();
            temp = new HashSet<String>();
            temp.add("allow");
            actions.put("POST", temp);
            temp = new HashSet<String>();
            temp.add("allow");
            actions.put("GET", temp);

            Rule policyURLRule = null;
            Subject sub = null;

            try {
                policyURLRule = new Rule(RULE_NAME,
                        "iPlanetAMWebAgentService",
                        policyURL,
                        actions);
                PolicyManager pm = new PolicyManager(token, ROOT_REALM);
                SubjectTypeManager stm = pm.getSubjectTypeManager();
                sub = stm.getSubject("AuthenticatedUsers");
            } catch (Exception e){
                throw new WorkflowException("ConfigureOAuth2.execute() : Unable to get Subject");
            }
            try {
                policy.addSubject(SUBJECT_NAME, sub);
                policy.addRule(policyURLRule);
            } catch (Exception e){
                throw new WorkflowException("ConfigureOAuth2.execute() : Unable add subject and rule to policy");
            }
            mgr = null;
            try {
                mgr = new PolicyManager(token, ROOT_REALM);
                mgr.addPolicy(policy);
            } catch (Exception e){
                throw new WorkflowException("ConfigureOAuth2.execute() : Unable to add policy");
            }
        }
        return "Sucessfully configured OAuth2 for realm: " + realm +
                "<br> A policy was created for the authorization end point. The policy" +
                " name is: " + POLICY_NAME;
    }
}
