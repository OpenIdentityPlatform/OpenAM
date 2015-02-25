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
 * Copyright 2009 Sun Microsystems Inc
 */
/*
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 * Portions Copyright 2011-2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import static com.sun.identity.entitlement.EntitlementException.AUTH_LEVEL_NOT_INTEGER;
import static com.sun.identity.entitlement.EntitlementException.AUTH_LEVEL_NOT_INT_OR_SET;
import static com.sun.identity.entitlement.EntitlementException.AUTH_SCHEME_NOT_FOUND;
import static com.sun.identity.entitlement.EntitlementException.CLIENT_IP_EMPTY;
import static com.sun.identity.entitlement.EntitlementException.INVALID_PROPERTY_VALUE;
import static com.sun.identity.entitlement.EntitlementException.PROPERTY_IS_NOT_AN_INTEGER;
import static com.sun.identity.entitlement.EntitlementException.PROPERTY_IS_NOT_A_SET;
import static com.sun.identity.entitlement.EntitlementException.PROPERTY_VALUE_NOT_DEFINED;
import static com.sun.identity.entitlement.EntitlementException.RESOURCE_ENV_NOT_KNOWN;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.util.PolicyDecisionUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import javax.security.auth.Subject;
import org.forgerock.oauth2.core.Utils;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.AUTHENTICATE_TO_REALM_CONDITION_ADVICE;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.AUTH_LEVEL;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.AUTH_LEVEL_CONDITION_ADVICE;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.AUTH_SCHEME_CONDITION_ADVICE;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_AUTHENTICATED_TO_REALMS;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_AUTHENTICATED_TO_SERVICES;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_AUTH_LEVEL;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_AUTH_SCHEMES;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_IP;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.ValidateIPaddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This condition provides the policy framework with the condition decision and advices based on the client's
 * environment or resource such as IP address, DNS host name, location, etc.
 */
public class ResourceEnvIPCondition extends EntitlementConditionAdaptor {

    public static final String ENV_CONDITION_VALUE = "resourceEnvIPConditionValue";

    private static final String KEY_VALUE = "\\s*(\\w+)\\s*=\\s*(\\S+)\\s*";
    private static final Pattern CONDITION_PATTERN = compile("\\s*IF" + KEY_VALUE + "THEN" + KEY_VALUE,
            CASE_INSENSITIVE);

    private final Debug debug;
    private final String debugName = "ResourceEnvIPCondition";
    private String localDebugName = "";

    private Set<String> resourceEnvIPConditionValue = new HashSet<String>();

    /**
     * No argument constructor
     */
    public ResourceEnvIPCondition() {
        this(PrivilegeManager.debug);
    }

    public ResourceEnvIPCondition(Debug debug) {
        this.debug = debug;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String,
            Set<String>> env) throws EntitlementException {

        if (debug.messageEnabled()) {
            localDebugName = debugName + ".evaluate(): ";
            debug.message(localDebugName + "client environment map: " + env);
        }

        boolean allowed = false;
        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        SSOToken token = (SSOToken) subject.getPrivateCredentials().iterator().next();
        try {
            EnvironmentCondition condition = matchEnvironment(env, token);

            if (condition != null) {
                String adviceName = condition.adviceName;
                String adviceValue = condition.adviceValue;

                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "adviceName : " + adviceName + " and adviceValue : " + adviceValue);
                }

                if (!Utils.isEmpty(adviceName) && !Utils.isEmpty(adviceValue)) {
                    if (adviceName.equalsIgnoreCase(ISAuthConstants.MODULE_PARAM)) {
                        Set<String> adviceMessages = getAdviceMessagesforAuthScheme(adviceValue, token, env);
                        if (adviceMessages.isEmpty()) {
                            allowed = true;
                        } else {
                            advices.put(AUTH_SCHEME_CONDITION_ADVICE, adviceMessages);
                        }

                    } else if (adviceName.equalsIgnoreCase(ISAuthConstants.SERVICE_PARAM)) {
                        Set<String> adviceMessages = getAdviceMessagesforAuthService(adviceValue, token, env);
                        if (adviceMessages.isEmpty()) {
                            allowed = true;
                        } else {
                            advices.put(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE, adviceMessages);
                        }

                    } else if (adviceName.equalsIgnoreCase(ISAuthConstants.AUTH_LEVEL_PARAM)) {
                        Set<String> adviceMessages = getAdviceMessagesforAuthLevel(adviceValue, token, env);
                        if (adviceMessages.isEmpty()) {
                            allowed = true;
                        } else {
                            advices.put(AUTH_LEVEL_CONDITION_ADVICE, adviceMessages);
                        }
                    } else if (adviceName.equalsIgnoreCase(ISAuthConstants.ROLE_PARAM)) {
                        Set<String> adviceMessages = getAdviceMessagesforRole(adviceValue, token, env);
                        if (adviceMessages.isEmpty()) {
                            allowed = true;
                        } else {
                            advices.put(PolicyDecisionUtils.AUTH_ROLE_ADVICE, adviceMessages);
                        }
                    } else if (adviceName.equalsIgnoreCase(ISAuthConstants.USER_PARAM)) {
                        Set<String> adviceMessages = getAdviceMessagesforUser(adviceValue, token, env);
                        if (adviceMessages.isEmpty()) {
                            allowed = true;
                        } else {
                            advices.put(PolicyDecisionUtils.AUTH_USER_ADVICE, adviceMessages);
                        }
                    } else if (adviceName.equalsIgnoreCase(ISAuthConstants.REDIRECT_URL_PARAM)) {
                        Set<String> adviceMessages = getAdviceMessagesforRedirectURL(adviceValue, token, env);
                        if (adviceMessages.isEmpty()) {
                            allowed = true;
                        } else {
                            advices.put(PolicyDecisionUtils.AUTH_REDIRECTION_ADVICE, adviceMessages);
                        }
                    } else if ((adviceName.equalsIgnoreCase(ISAuthConstants.REALM_PARAM)) || (adviceName
                            .equalsIgnoreCase(ISAuthConstants.ORG_PARAM))) {
                        Set<String> adviceMessages = getAdviceMessagesforRealm(adviceValue, token, env);
                        if (adviceMessages.isEmpty()) {
                            allowed = true;
                        } else {
                            advices.put(AUTHENTICATE_TO_REALM_CONDITION_ADVICE, adviceMessages);
                        }
                    } else if (debug.messageEnabled()) {
                        debug.message(localDebugName + "adviceName is invalid");
                    }
                }

            } else if (debug.messageEnabled()) {
                debug.message(localDebugName + "Advice is NULL since there is no matching condition found.");
            }
        } catch (SSOException e) {
            debug.error(debugName + ".evaluate(): Condition evaluation failed", e);
        }

        return new ConditionDecision(allowed, advices);
    }

    /**
     * Returns advice messages for Authentication Scheme condition.
     */
    private Set<String> getAdviceMessagesforAuthScheme(String adviceValue, SSOToken token,
                                                       Map<String, Set<String>> env) throws EntitlementException, SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getAdviceMessagesforAuthScheme(): ";
        }
        Set<String> adviceMessages = new HashSet<String>();
        Set requestAuthSchemes = null;
        Set requestAuthSchemesIgnoreRealm = null;
        if ((env != null) && (env.get(REQUEST_AUTH_SCHEMES) != null)) {
            try {
                requestAuthSchemes = env.get(REQUEST_AUTH_SCHEMES);
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "requestAuthSchemes from env=" + requestAuthSchemes);
                }
            } catch (ClassCastException e) {
                throw new EntitlementException(PROPERTY_VALUE_NOT_DEFINED, new String[]{REQUEST_AUTH_SCHEMES}, e);
            }
        } else {
            if (token != null) {
                requestAuthSchemes = AMAuthUtils.getRealmQualifiedAuthenticatedSchemes(token);
                requestAuthSchemesIgnoreRealm = AMAuthUtils.getAuthenticatedSchemes(token);
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "requestAuthSchemes from ssoToken=" + requestAuthSchemes);
                    debug.message(localDebugName + "requestAuthSchemesIgnoreRealm from ssoToken= " +
                            requestAuthSchemesIgnoreRealm);
                }
            }
        }

        if (requestAuthSchemes == null) {
            requestAuthSchemes = Collections.EMPTY_SET;
        }

        if (requestAuthSchemesIgnoreRealm == null) {
            requestAuthSchemesIgnoreRealm = Collections.EMPTY_SET;
        }

        String authScheme = adviceValue;

        if (!requestAuthSchemes.contains(authScheme)) {
            String realm = AMAuthUtils.getRealmFromRealmQualifiedData(authScheme);
            if ((realm != null) && (realm.length() != 0)) {

                adviceMessages.add(authScheme);
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "authScheme not satisfied = " + authScheme);
                }

            } else if ((realm == null) || (realm.length() == 0)) {
                if (!requestAuthSchemesIgnoreRealm.contains(authScheme)) {

                    adviceMessages.add(authScheme);
                    if (debug.messageEnabled()) {
                        debug.message(localDebugName + "authScheme not satisfied = " + authScheme);
                    }
                }
            }
        }

        if (debug.messageEnabled()) {
            debug.message(localDebugName + "authScheme = " + authScheme + ", " +
                    "requestAuthSchemes = " + requestAuthSchemes + ", " + " adviceMessages = " + adviceMessages);
        }
        return adviceMessages;
    }

    /**
     * Returns advice messages for Authentication Service condition.
     */
    private Set<String> getAdviceMessagesforAuthService(String adviceValue, SSOToken token,
                                                        Map<String, Set<String>> env) throws EntitlementException, SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getAdviceMessagesforAuthService(): ";
        }
        Set<String> adviceMessages = new HashSet<String>();
        Set<String> requestAuthnServices = new HashSet<String>();
        boolean allow = false;
        if ((env != null) && (env.get(REQUEST_AUTHENTICATED_TO_SERVICES) != null)) {
            try {
                requestAuthnServices = env.get(REQUEST_AUTHENTICATED_TO_SERVICES);
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "requestAuthnServices from request = " + requestAuthnServices);
                }
            } catch (ClassCastException e) {
                throw new EntitlementException(PROPERTY_VALUE_NOT_DEFINED,
                        new String[]{REQUEST_AUTHENTICATED_TO_SERVICES}, e);
            }
        } else {

            if (token != null) {
                Set authenticatedServices = AMAuthUtils.getRealmQualifiedAuthenticatedServices(token);
                if (authenticatedServices != null) {
                    requestAuthnServices.addAll(authenticatedServices);
                }
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "requestAuthnServices from ssoToken = " + requestAuthnServices);
                }
            }
        }

        if (!requestAuthnServices.contains(adviceValue)) {
            String realm = AMAuthUtils.getRealmFromRealmQualifiedData(adviceValue);
            if ((realm != null) && (realm.length() != 0)) {

                adviceMessages.add(adviceValue);
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "authService not satisfied = " + adviceValue);
                }

            } else if ((realm == null) || (realm.length() == 0)) {
                for (String requestAuthnService : requestAuthnServices) {
                    String service = AMAuthUtils.getDataFromRealmQualifiedData(requestAuthnService);
                    if (adviceValue.equals(service)) {
                        allow = true;
                        break;
                    }
                }
            }
        }

        if (!allow) {
            adviceMessages.add(adviceValue);
        }

        if (debug.messageEnabled()) {
            debug.message(localDebugName + "authenticateToService = " + adviceValue + ", " +
                    "requestAuthnServices = " + requestAuthnServices + ", adviceMessages = " + adviceMessages);
        }

        return adviceMessages;
    }

    /**
     * Returns advice messages for Authentication Level condition.
     */
    private Set<String> getAdviceMessagesforAuthLevel(String authLevel, SSOToken token, Map<String, Set<String>> env)
            throws EntitlementException, SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getAdviceMessagesforAuthLevel(): ";
        }
        Set<String> adviceMessages = new HashSet<String>();
        int maxRequestAuthLevel;
        String authRealm;
        int authLevelInt;
        try {
            authRealm = AMAuthUtils.getRealmFromRealmQualifiedData(authLevel);
            String authLevelIntString = AMAuthUtils.getDataFromRealmQualifiedData(authLevel);
            authLevelInt = Integer.parseInt(authLevelIntString);
        } catch (NumberFormatException e) {
            throw new EntitlementException(PROPERTY_IS_NOT_AN_INTEGER, new String[]{AUTH_LEVEL});
        }

        maxRequestAuthLevel = getMaxRequestAuthLevel(env, authRealm, authLevel);
        if ((maxRequestAuthLevel == Integer.MIN_VALUE) && (token != null)) {
            maxRequestAuthLevel = getMaxRequestAuthLevel(token, authRealm, authLevel);
        }

        if (maxRequestAuthLevel < authLevelInt) {
            adviceMessages.add(authLevel);
        }

        if (debug.messageEnabled()) {
            debug.message(localDebugName + "authLevel=" + authLevel + "authRealm=" + authRealm + ", " +
                    "maxRequestAuthLevel=" + maxRequestAuthLevel + ",adviceMessages=" + adviceMessages);
        }

        return adviceMessages;
    }

    /**
     * Returns advice messages for Authentication Role condition.
     */
    private Set<String> getAdviceMessagesforRole(String adviceValue, SSOToken token, Map<String, Set<String>> env) throws SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getAdviceMessagesforRole(): ";
        }
        Set<String> adviceMessages = new HashSet<String>();
        boolean allow = false;
        if (token != null) {
            String userAuthRoleNames = token.getProperty("Role");
            if (debug.messageEnabled()) {
                debug.message(localDebugName + "userAuthRoleNames from token =" + userAuthRoleNames);
            }

            if (userAuthRoleNames != null) {
                String userAuthRoleName = null;
                StringTokenizer st = new StringTokenizer(userAuthRoleNames, "|");
                while (st.hasMoreElements()) {
                    userAuthRoleName = (String) st.nextElement();
                    if ((userAuthRoleName != null) && (userAuthRoleName.equals(adviceValue))) {
                        allow = true;
                    }
                }
            }
        }

        if (!allow) {
            adviceMessages.add(adviceValue);
        }

        if (debug.messageEnabled()) {
            debug.message(localDebugName + "auth role =" + adviceValue + ", adviceMessages=" + adviceMessages);
        }

        return adviceMessages;
    }

    /**
     * Returns advice messages for Authentication User condition.
     */
    private Set<String> getAdviceMessagesforUser(String adviceValue, SSOToken token, Map<String, Set<String>> env) throws SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getAdviceMessagesforUser(): ";
        }
        Set<String> adviceMessages = new HashSet<String>();
        boolean allow = false;
        if (token != null) {
            String authUserNames = token.getProperty("UserToken");
            if (debug.messageEnabled()) {
                debug.message(localDebugName + "userAuthRoleNames from token =" + authUserNames);
            }

            if (authUserNames != null) {
                String authUserName = null;
                StringTokenizer st = new StringTokenizer(authUserNames, "|");
                while (st.hasMoreElements()) {
                    authUserName = (String) st.nextElement();
                    if ((authUserName != null) && (authUserName.equals(adviceValue))) {
                        allow = true;
                    }
                }
            }
        }

        if (!allow) {
            adviceMessages.add(adviceValue);
        }

        if (debug.messageEnabled()) {
            debug.message(localDebugName + "auth user =" + adviceValue + ", adviceMessages=" + adviceMessages);
        }

        return adviceMessages;
    }

    /**
     * Returns advice messages for Authentication Realm condition.
     */
    private Set<String> getAdviceMessagesforRealm(String adviceValue, SSOToken token,
                                                  Map<String, Set<String>> env) throws EntitlementException, SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getAdviceMessagesforRealm(): ";
        }
        Set<String> adviceMessages = new HashSet<String>();
        Set<String> requestAuthnRealms = new HashSet<String>();
        if ((env != null) && (env.get(REQUEST_AUTHENTICATED_TO_REALMS) != null)) {
            try {
                requestAuthnRealms = env.get(REQUEST_AUTHENTICATED_TO_REALMS);
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "requestAuthnRealms, from request / env = " + requestAuthnRealms);
                }
            } catch (ClassCastException e) {
                throw new EntitlementException(PROPERTY_IS_NOT_A_SET, new String[]{REQUEST_AUTHENTICATED_TO_REALMS}, e);
            }
        } else {

            if (token != null) {
                Set authenticatedRealms = AMAuthUtils.getAuthenticatedRealms(token);
                if (authenticatedRealms != null) {
                    requestAuthnRealms.addAll(authenticatedRealms);
                }
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "requestAuthnRealms, from ssoToken = " + requestAuthnRealms);
                }
            }
        }

        String authRealm = adviceValue;

        if (!requestAuthnRealms.contains(authRealm)) {
            adviceMessages.add(authRealm);
            if (debug.messageEnabled()) {
                debug.message(localDebugName + "authenticateToRealm not satisfied = " + authRealm);
            }
        }

        if (debug.messageEnabled()) {
            debug.message(localDebugName + "authRealm = " + authRealm + "," + " requestAuthnRealms = " +
                    requestAuthnRealms + ", adviceMessages = " + adviceMessages);
        }
        return adviceMessages;

    }

    /**
     * Returns advice messages for Authentication Redirect condition.
     */
    private Set<String> getAdviceMessagesforRedirectURL(String adviceValue, SSOToken token,
                                                        Map env) throws EntitlementException, SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getAdviceMessagesforRedirectURL(): ";
        }
        Set<String> adviceMessages = new HashSet<String>();
        Set requestAuthSchemes = null;
        Set requestAuthSchemesIgnoreRealm = null;
        boolean nullRealm = false;
        boolean allow = false;
        String orgName = "/";
        if ((env != null) && (env.get(REQUEST_AUTH_SCHEMES) != null)) {
            try {
                orgName = CollectionHelper.getMapAttr(env, PolicyEvaluator.REALM_DN, orgName);
                requestAuthSchemes = (Set) env.get(REQUEST_AUTH_SCHEMES);
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "requestAuthSchemes from env= " + requestAuthSchemes + " AND " +
                            "orgName from env= " + orgName);
                }
            } catch (ClassCastException e) {
                throw new EntitlementException(PROPERTY_IS_NOT_A_SET, new String[]{REQUEST_AUTH_SCHEMES}, e);
            }
        } else {
            if (token != null) {
                orgName = token.getProperty(ISAuthConstants.ORGANIZATION);
                requestAuthSchemes = AMAuthUtils.getRealmQualifiedAuthenticatedSchemes(token);
                requestAuthSchemesIgnoreRealm = AMAuthUtils.getAuthenticatedSchemes(token);
                if (debug.messageEnabled()) {
                    debug.message(localDebugName + "orgName " + "from ssoToken= " + orgName);
                    debug.message(localDebugName + "requestAuthSchemes from ssoToken= " + requestAuthSchemes);
                    debug.message(localDebugName + "requestAuthSchemesIgnoreRealm from ssoToken= " +
                            requestAuthSchemesIgnoreRealm);
                }
            }
        }

        if (requestAuthSchemes == null) {
            requestAuthSchemes = Collections.EMPTY_SET;
        }

        if (requestAuthSchemesIgnoreRealm == null) {
            requestAuthSchemesIgnoreRealm = Collections.EMPTY_SET;
        }

        String schemeInstance = null;
        String authSchemeType = null;
        try {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            for (Iterator iter = requestAuthSchemes.iterator(); iter.hasNext(); ) {
                String requestAuthnScheme = (String) iter.next();
                schemeInstance = AMAuthUtils.getDataFromRealmQualifiedData(requestAuthnScheme);
                String realm = AMAuthUtils.getRealmFromRealmQualifiedData(requestAuthnScheme);
                if ((realm == null) || (realm.length() == 0)) {
                    nullRealm = true;
                    break;
                } else {
                    AMAuthenticationManager authManager = new AMAuthenticationManager(adminToken, orgName);
                    AMAuthenticationInstance authInstance = authManager.getAuthenticationInstance(schemeInstance);
                    authSchemeType = authInstance.getType();
                    if ("Federation".equals(authSchemeType)) {
                        allow = true;
                        break;
                    }
                }
            }

            if (nullRealm) {
                for (Iterator iter = requestAuthSchemesIgnoreRealm.iterator(); iter.hasNext(); ) {
                    schemeInstance = (String) iter.next();
                    AMAuthenticationManager authManager = new AMAuthenticationManager(adminToken, orgName);
                    AMAuthenticationInstance authInstance = authManager.getAuthenticationInstance(schemeInstance);
                    authSchemeType = authInstance.getType();
                    if ("Federation".equals(authSchemeType)) {
                        allow = true;
                        break;
                    }
                }
            }

        } catch (AMConfigurationException ace) {
            if (debug.warningEnabled()) {
                debug.warning(localDebugName + "got AMConfigurationException: schemeInstance=" + schemeInstance + ", " +
                        "authSchemeType = " + authSchemeType);
            }
            throw new EntitlementException(AUTH_SCHEME_NOT_FOUND, new String[]{schemeInstance}, ace);
        }
        if (!allow) {
            adviceMessages.add(adviceValue);
        }

        if (debug.messageEnabled()) {
            debug.message(localDebugName + "redirectURL=" + adviceValue + "schemeInstance=" + schemeInstance + "," +
                    "authSchemeType=" + authSchemeType + ",adviceMessages=" + adviceMessages);
        }

        return adviceMessages;
    }

    /**
     * Returns the maximum auth level specified for the REQUEST_AUTH_LEVEL
     * property in the environment Map.
     */
    private int getMaxRequestAuthLevel(Map<String, Set<String>> env, String authRealm, String authLevel) throws EntitlementException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getMaxRequestAuthLevel(): ";
        }
        int maxAuthLevel = Integer.MIN_VALUE;
        int currentAuthLevel = Integer.MIN_VALUE;
        if (debug.messageEnabled()) {
            debug.message(localDebugName + "entering: envMap= " + env + ", authRealm= " + authRealm + ", " +
                    "conditionAuthLevel= " + authLevel);
        }
        Object envAuthLevelObject = env.get(REQUEST_AUTH_LEVEL);
        if (envAuthLevelObject != null) {
            if (envAuthLevelObject instanceof Integer) {
                if ((authRealm == null) || (authRealm.length() == 0)) {
                    maxAuthLevel = ((Integer) envAuthLevelObject).intValue();
                    if (debug.messageEnabled()) {
                        debug.message(localDebugName + "Integer level in env= " + maxAuthLevel);
                    }
                }
            } else if (envAuthLevelObject instanceof Set) {
                Set envAuthLevelSet = (Set) envAuthLevelObject;
                if (!envAuthLevelSet.isEmpty()) {
                    Iterator iter = envAuthLevelSet.iterator();
                    while (iter.hasNext()) {
                        Object envAuthLevelElement = iter.next();
                        if (!(envAuthLevelElement instanceof String)) {
                            if (debug.warningEnabled()) {
                                debug.warning(localDebugName + "requestAuthLevel Set element" + " not String");
                            }
                            throw new EntitlementException(AUTH_LEVEL_NOT_INT_OR_SET);
                        } else {
                            String qualifiedLevel = (String) envAuthLevelElement;
                            currentAuthLevel = getAuthLevel(qualifiedLevel);
                            if ((authRealm == null) || authRealm.length() == 0) {
                                if (currentAuthLevel > maxAuthLevel) {
                                    maxAuthLevel = currentAuthLevel;
                                }
                            } else {
                                String realmString = AMAuthUtils.
                                        getRealmFromRealmQualifiedData(qualifiedLevel);
                                if (authRealm.equals(realmString) && (currentAuthLevel > maxAuthLevel)) {
                                    maxAuthLevel = currentAuthLevel;
                                }
                            }
                        }
                    }
                }
            } else {
                if (debug.warningEnabled()) {
                    debug.warning(localDebugName + "requestAuthLevel in env neither Integer nor Set");
                }
                throw new EntitlementException(AUTH_LEVEL_NOT_INT_OR_SET);
            }
        }
        if (debug.messageEnabled()) {
            debug.message(localDebugName + "returning: maxAuthLevel=" + maxAuthLevel);
        }
        return maxAuthLevel;
    }

    /**
     * Returns the maximum auth level specified for the REQUEST_AUTH_LEVEL
     * property in the SSO token.
     */
    private int getMaxRequestAuthLevel(SSOToken token, String authRealm,
                                       String authLevel) throws EntitlementException, SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getMaxRequestAuthLevel(): ";
        }
        int maxAuthLevel = Integer.MIN_VALUE;
        if (debug.messageEnabled()) {
            debug.message(localDebugName + "entering:" + " authRealm = " + authRealm + ", " +
                    "conditionAuthLevel= " + authLevel);
        }
        if ((authRealm == null) || authRealm.length() == 0) {
            Set levels = AMAuthUtils.getAuthenticatedLevels(token);
            if (debug.messageEnabled()) {
                debug.message(localDebugName + "levels from token= " + levels);
            }
            if ((levels != null) && (!levels.isEmpty())) {
                for (final Object level1 : levels) {
                    String levelString = (String) level1;
                    int level = getAuthLevel(levelString);
                    maxAuthLevel = (level > maxAuthLevel) ? level : maxAuthLevel;
                }
            }
        } else {
            Set qualifiedLevels = AMAuthUtils.getRealmQualifiedAuthenticatedLevels(token);
            if (debug.messageEnabled()) {
                debug.message(localDebugName + "qualifiedLeves from token= " +
                        qualifiedLevels);
            }
            if ((qualifiedLevels != null) && (!qualifiedLevels.isEmpty())) {
                Iterator iter = qualifiedLevels.iterator();
                while (iter.hasNext()) {
                    String qualifiedLevel = (String) iter.next();
                    String realm = AMAuthUtils.getRealmFromRealmQualifiedData(qualifiedLevel);
                    if (authRealm.equals(realm)) {
                        int level = getAuthLevel(qualifiedLevel);
                        maxAuthLevel = (level > maxAuthLevel) ? level : maxAuthLevel;
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message(localDebugName + "returning:" + " maxAuthLevel= " +
                    maxAuthLevel);
        }
        return maxAuthLevel;
    }

    /**
     * Extracts the integer auth level from  String realm qualified
     * ( realm:level) String.
     */
    private int getAuthLevel(String qualifiedLevel) throws EntitlementException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".getAuthLevel(): ";
        }
        int levelInt = 0;
        String levelString = AMAuthUtils.getDataFromRealmQualifiedData(qualifiedLevel);
        try {
            levelInt = Integer.parseInt(levelString);
        } catch (NumberFormatException nfe) {
            if (debug.warningEnabled()) {
                debug.warning(localDebugName + "got NumberFormatException: qualifiedLevel=" + qualifiedLevel + ", " +
                        "levelString = " + levelString);
            }
            throw new EntitlementException(AUTH_LEVEL_NOT_INTEGER, new String[]{levelString}, nfe);
        }
        return levelInt;
    }

    /**
     * Returns the environment condition that satisfies or matches for the client
     * environment parameter, including client's IP Address.
     */
    @SuppressWarnings("unchecked")
    private EnvironmentCondition matchEnvironment(Map env, SSOToken token)
            throws EntitlementException,
            SSOException {
        if (debug.messageEnabled()) {
            localDebugName = debugName + ".matchEnvironment(): ";
        }

        EnvironmentCondition matchingCondition = null;
        final List<EnvironmentCondition> conditions = parseConditions(resourceEnvIPConditionValue);

        //Check if all the keys are valid
        for (EnvironmentCondition condition : conditions) {
            final String envParamName = condition.paramName;
            final String envParamValue = condition.paramValue;

            Set<String> envSet = (Set<String>) env.get(envParamName);
            if (!Utils.isEmpty(envSet)) {
                for (String strEnv : envSet) {
                    if ((strEnv != null) && (strEnv.equalsIgnoreCase(envParamValue))) {
                        matchingCondition = condition;
                        break;
                    }
                }
            } else {

                String strIP = null;
                Object object = env.get(REQUEST_IP);
                if (object instanceof Set) {
                    Set ipSet = (Set) object;
                    if ( ipSet.isEmpty() ) {
                        if (token != null) {
                            strIP = token.getIPAddress().getHostAddress();
                        } else {
                            throw new EntitlementException(CLIENT_IP_EMPTY);
                        }
                    } else {
                        Iterator names = ipSet.iterator();
                        strIP = (String) names.next();
                    }
                } else if (object instanceof String) {
                    strIP = (String) object;
                    if (StringUtils.isBlank(strIP)) {
                        if (token != null) {
                            strIP = token.getIPAddress().getHostAddress();
                        } else {
                            throw new EntitlementException(CLIENT_IP_EMPTY);
                        }
                    }
                }

                long requestIpV4 = 0;
                IPv6Address requestIpV6 = null;
                if (ValidateIPaddress.isIPv4(strIP)) {
                    requestIpV4 = stringToIp(strIP);
                } else if (ValidateIPaddress.isIPv6(strIP)) {
                    requestIpV6 = IPv6Address.fromString(strIP);
                } else {
                    if (debug.messageEnabled()) {
                        debug.message(localDebugName + "invalid strIP : " + strIP);
                    }
                    continue;
                }

                int bIndex = envParamValue.indexOf("[");
                int lIndex = envParamValue.indexOf("]");
                String ipVal = envParamValue.substring(bIndex + 1, lIndex);

                if (ipVal.contains("-")) {
                    StringTokenizer stIP = new StringTokenizer(ipVal, "-");
                    int tokenCnt = stIP.countTokens();
                    if (tokenCnt > 2) {
                        throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{ipVal});
                    }

                    String startIp = stIP.nextToken();
                    String endIp = startIp;
                    if (tokenCnt == 2) {
                        endIp = stIP.nextToken();
                    }

                    if (ValidateIPaddress.isIPv4(strIP) &&
                            ValidateIPaddress.isIPv4(startIp) && ValidateIPaddress.isIPv4(endIp)) {
                        long lStartIP = stringToIp(startIp);
                        long lEndIP = stringToIp(endIp);
                        if ((requestIpV4 >= lStartIP) && (requestIpV4 <= lEndIP)) {
                            matchingCondition = condition;
                            break;
                        }
                    } else if (ValidateIPaddress.isIPv6(strIP) &&
                            ValidateIPaddress.isIPv6(startIp) && ValidateIPaddress.isIPv6(endIp)) {
                        IPv6AddressRange ipv6Range = IPv6AddressRange.fromFirstAndLast(IPv6Address.fromString
                                (startIp), IPv6Address.fromString(endIp));
                        if (requestIpV6 != null && ipv6Range.contains(requestIpV6)) {
                            matchingCondition = condition;
                            break;
                        }
                    } else {
                        if (debug.errorEnabled()) {
                            debug.error(debugName + ".matchEnvironment(): invalid property value, " + strIP);
                        }
                        throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{strIP});
                    }

                } else if (requestIpV4 != 0 && ValidateIPaddress.isIPv4(ipVal)) {
                    long longIp = stringToIp(ipVal);
                    if (requestIpV4 == longIp) {
                        matchingCondition = condition;
                        break;
                    }
                } else if (requestIpV6 != null && ValidateIPaddress.isIPv6(ipVal)) {
                    // treat as single ip address
                    IPv6Address iPv6AddressIpVal = IPv6Address.fromString(ipVal);
                    if (iPv6AddressIpVal.compareTo(requestIpV6) == 0) {
                        matchingCondition = condition;
                        break;
                    }
                } else if (ipVal.contains("*")) {
                    matchingCondition = condition;
                    break;
                } else {
                    throw new EntitlementException(RESOURCE_ENV_NOT_KNOWN, new String[]{ipVal});
                }
            }

        }

        return matchingCondition;
    }

    /**
     * Converts String representation of IP address to a long.
     * No nee for error checking as IP has already been validated.
     */
    private long stringToIp(String ip) {
        StringTokenizer st = new StringTokenizer(ip, ".");
        long ipValue = 0L;
        while (st.hasMoreElements()) {
            ipValue = ipValue * 256L + Short.parseShort(st.nextToken());
        }
        return ipValue;
    }


    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            JSONArray conditionList = jo.getJSONArray(ENV_CONDITION_VALUE);
            for (int i = 0; i < conditionList.length(); i++) {
                resourceEnvIPConditionValue.add(conditionList.getString(i));
            }
        } catch (JSONException joe) {
            debug.error(debugName + ".setState(): State invalid: " + state, joe);
        }
    }

    @Override
    public String getState() {
        return toString();
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put(ENV_CONDITION_VALUE, resourceEnvIPConditionValue);
        return jo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            debug.error(debugName + ".toString(): ", e);
        }
        return s;
    }

    public Set<String> getResourceEnvIPConditionValue() {
        return resourceEnvIPConditionValue;
    }

    public void setResourceEnvIPConditionValue(Set<String> resourceEnvIPConditionValue) {
        this.resourceEnvIPConditionValue = resourceEnvIPConditionValue;
    }

    /**
     * Parse condition strings of the form {@code IF paramName=paramValue THEN adviceName=adviceValue} into condition
     * objects. The syntax of the paramValue and adviceValue parts may be further constrained during evaluation.
     *
     * @param conditionStrings the set of condition strings passed from the front end.
     * @return the parsed condition objects.
     * @throws EntitlementException if any of the conditions is in an invalid format.
     */
    static List<EnvironmentCondition> parseConditions(final Set<String> conditionStrings)
            throws EntitlementException {
        final List<EnvironmentCondition> conditions =
                new ArrayList<EnvironmentCondition>(conditionStrings.size());

        for (final String conditionString : conditionStrings) {
            final Matcher matcher = CONDITION_PATTERN.matcher(conditionString);
            if (!matcher.matches()) {
                throw new EntitlementException(EntitlementException.INVALID_PROPERTY_VALUE, ENV_CONDITION_VALUE,
                        conditionString);
            }

            conditions.add(new EnvironmentCondition(matcher.group(1), matcher.group(2), matcher.group(3),
                    matcher.group(4)));
        }

        return conditions;
    }

    @Override
    public void validate() throws EntitlementException {
        if (resourceEnvIPConditionValue == null || resourceEnvIPConditionValue.isEmpty()) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, ENV_CONDITION_VALUE);
        }

        parseConditions(resourceEnvIPConditionValue);
    }

    /**
     * Represents a parsed resource environment condition consisting of a parameter name and value to test from the
     * environment, and an advice name and value to return if the condition matches.
     */
    static final class EnvironmentCondition {
        final String paramName;
        final String paramValue;
        final String adviceName;
        final String adviceValue;

        EnvironmentCondition(String paramName, String paramValue, String adviceName, String adviceValue) {
            this.paramName = paramName;
            this.paramValue = paramValue;
            this.adviceName = adviceName;
            this.adviceValue = adviceValue;
        }

        @Override
        public boolean equals(final Object that) {
            return (this == that) ||
                    (that instanceof EnvironmentCondition) && this.isEqualTo((EnvironmentCondition) that);
        }

        public boolean isEqualTo(final EnvironmentCondition that) {
            // Names are case-sensitive, values are not
            return this.adviceName.equals(that.adviceName)
                    && this.adviceValue.equalsIgnoreCase(that.adviceValue)
                    && this.paramName.equals(that.paramName)
                    && this.paramValue.equalsIgnoreCase(that.paramValue);
        }

        @Override
        public int hashCode() {
            int result = paramName.hashCode();
            result = 31 * result + paramValue.toLowerCase().hashCode();
            result = 31 * result + adviceName.hashCode();
            result = 31 * result + adviceValue.toLowerCase().hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "IF " + paramName + "=" + paramValue + " THEN " + adviceName + "=" + adviceValue;
        }
    }
}
