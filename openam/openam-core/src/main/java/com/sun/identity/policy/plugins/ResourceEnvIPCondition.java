/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ResourceEnvIPCondition.java,v 1.4 2009/07/21 18:33:17 mrudul_uchil Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */
package com.sun.identity.policy.plugins;

import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.security.AccessController;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.util.PolicyDecisionUtils;
import com.sun.identity.policy.ResBundleUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.openam.utils.ValidateIPaddress;


/**
 * The class <code>ResourceEnvIPCondition</code> is a plugin 
 * implementation of <code>Condition</code> interface.
 * This condition object provides the policy framework with the 
 * condition decision and advices based on the client's environment or 
 * resource such as IP address, DNS host name, location, etc.
 * For the first drop, we are only supporting IP address.
 */

public class ResourceEnvIPCondition implements Condition {

    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);
    public static final String ENV_CONDITION_VALUE = 
        "resourceEnvIPConditionValue";
    
    public static final String IP = "IP";
    public static final String THEN = "THEN";
    private ArrayList envList = new ArrayList();
    private ArrayList adviceList = new ArrayList();
    private List propertyNames;
    private Map properties;

    /** 
     * No argument constructor 
     */
    public ResourceEnvIPCondition() {
         propertyNames = new ArrayList();
         propertyNames.add(ENV_CONDITION_VALUE);
    }

     /**
      * Returns a list of property names for <code>ResourceEnvIPCondition</code>.
      *
      * @return List of property names
      */

     public List getPropertyNames()
     {
         return propertyNames;
     }
 
     /**
      * Returns the syntax for a property name
      * @see com.sun.identity.policy.Syntax
      *
      * @param property String property name
      *
      * @return <code>Syntax<code> for the property name
      */
     public Syntax getPropertySyntax(String property)
     {
         return (Syntax.LIST);
     }
      
     /**
      * Returns the display name for the property name.
      * The <code>locale</code> variable could be used by the
      * plugin to customize the display name for the given locale.
      * The <code>locale</code> variable could be <code>null</code>, in which 
      * case the plugin must use the default locale.
      *
      * @param property String property name
      * @param locale Locale for which the property name must be customized
      * @return display name for the property name
      */
     public String getDisplayName(String property, Locale locale) 
       throws PolicyException
     {
         ResourceBundle rb = AMResourceBundleCache.getInstance().getResBundle(ResBundleUtils.rbName, locale);
         return com.sun.identity.shared.locale.Locale.getString(rb, property);
     }
 
     /**
      * Returns a set of valid values given the property name. This method
      * is called if the property Syntax is either the SINGLE_CHOICE or 
      * MULTIPLE_CHOICE.
      *
      * @param property String property name
      * @return Set of valid values for the property.
      * @exception PolicyException if unable to get the Syntax.
      */
     public Set getValidValues(String property) throws PolicyException
     {
         return (Collections.EMPTY_SET);
     }


    /** 
     * Sets the properties of <code>ResourceEnvIPCondition</code>.
     * Evaluation of ConditionDecision is influenced by these properties.
     * @param properties the properties of the condition that governs
     *        whether a policy applies. The properties should
     *        define value for the key ENV_CONDITION_VALUE. The value should
     *        be a Set with multiple elements. Each element should be
     *        a String. Please note that properties is not cloned by the method.
     *
     * @throws PolicyException if properties is null or does not contain
     *         value for the key ENV_CONDITION_VALUE or the value of the key is
     *         not a Set with one String element that is parsable as
     *         an integer.
     */

    public void setProperties(Map properties) throws PolicyException {
        this.properties = properties;
        envList.clear();
        adviceList.clear();
        
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(ResBundleUtils.rbName,
                "null_properties", null, null);
        }

        // check if the value is valid
        Set envCondVal = (Set) properties.get(ENV_CONDITION_VALUE);
        if (( envCondVal == null ) || envCondVal.isEmpty() 
            || ( envCondVal.isEmpty() )) {
            throw new PolicyException(ResBundleUtils.rbName,
                "null_env_cond_value", null, null);
        }

        if ( DEBUG.messageEnabled()) {
            DEBUG.message("ResourceEnvIPCondition:setProperties envCondVal : " 
                + envCondVal);
        }
        
        Iterator envCondValIter = envCondVal.iterator();
        int i = 0;
        
        while ( envCondValIter.hasNext()) {
            String envKey = (String) envCondValIter.next();
            if ( envKey != null ) {
                int ifIndex = envKey.indexOf("IF");
                if (ifIndex == -1) {
                    ifIndex = envKey.indexOf("if");
                }
                int adviceIndex = envKey.indexOf(THEN);
                if (adviceIndex == -1) {
                    adviceIndex = envKey.indexOf("then");
                }
                String envVal = envKey.substring(ifIndex+2, adviceIndex-1);
                String adviceVal = envKey.substring(adviceIndex+5);
                envList.add(i, envVal);
                adviceList.add(i, adviceVal);
                i++;
            }
        }
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("ResourceEnvIPCondition:setProperties envList : "
                + envList);
            DEBUG.message("ResourceEnvIPCondition:setProperties adviceList : " 
                + adviceList);
        }
        
    }


    /** 
     * Returns properties of <code>ResourceEnvIPCondition</code>.
     */
    public Map getProperties() {
        return properties;
    } 


    /**
     * Returns the decision computed by <code>ResourceEnvIPCondition</code>
     * object.
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment map of key/value
     *        pairs <code>ResourceEnvIPCondition</code> looks for values of key
     *        <code>REQUEST_IP</code> in the
     *        <code>env</code> map. If <code>REQUEST_IP</code> could not be 
     *        determined from <code>env</code>, it is obtained from 
     *        single sign on token of the user.
     *
     * @return the condition decision. The condition decision encapsulates
     *         whether a policy applies for the request and advice messages
     *         generated by the condition.  
     * 
     * Policy framework continues evaluating a policy only if it applies 
     * to the request  as indicated by the <code>ConditionDecision</code>. 
     * Otherwise, further evaluation of the policy is skipped. 
     * However, the advice messages encapsulated in the 
     * <code>ConditionDecision</code> are aggregated and passed up, encapsulated
     * in the policy  decision. 
     *
     * @throws PolicyException if the condition has not been initialized
     *        with a successful call to <code>setProperties(Map)</code> and/or
     *        the value of key <code>REQUEST_IP</code> is not a String.
     * @throws SSOException if the token is invalid
     *
     * @see #setProperties(Map)
     * @see #REQUEST_IP
     * @see com.sun.identity.policy.ConditionDecision
     */

    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("ResourceEnvIPCondition:getConditionDecision - " + 
            "client environment map : " + env);
        }
	    boolean allowed = false;
        Map advices = new HashMap();
        
        String adviceStr = getAdviceStrForEnv(env,token);
 
        String adviceName = null;
        String adviceValue = null;
        if (adviceStr != null && adviceStr.contains("=")) {
            int index = adviceStr.indexOf("=");
            adviceName = adviceStr.substring(0, index);
            adviceValue = adviceStr.substring(index+1);
            
            if ( DEBUG.messageEnabled()) {
                DEBUG.message("ResourceEnvIPCondition:getConditionDecision - " +
                "adviceName : " + adviceName + " and adviceValue : "
                + adviceValue);
            }

            if ((adviceName != null) && (adviceName.length() != 0) &&
                (adviceValue != null) && (adviceValue.length() != 0)) {
                if (adviceName.equalsIgnoreCase(ISAuthConstants.MODULE_PARAM)) {
                    Set adviceMessages =
                        getAdviceMessagesforAuthScheme(adviceValue,token,env);
                    if (adviceMessages.isEmpty()) {
                        allowed = true;
                    } else {
                        advices.put(AUTH_SCHEME_CONDITION_ADVICE,
                            adviceMessages);
                    }

                } else if (adviceName.equalsIgnoreCase(
                        ISAuthConstants.SERVICE_PARAM)) {
                    Set adviceMessages =
                        getAdviceMessagesforAuthService(adviceValue,token,env);
                    if (adviceMessages.isEmpty()) {
                        allowed = true;
                    } else {
                        advices.put(AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE,
                            adviceMessages);
                    }

                } else if (adviceName.equalsIgnoreCase(
                        ISAuthConstants.AUTH_LEVEL_PARAM)) {
                    Set adviceMessages =
                        getAdviceMessagesforAuthLevel(adviceValue,token,env);
                    if (adviceMessages.isEmpty()) {
                        allowed = true;
                    } else {
                        advices.put(AUTH_LEVEL_CONDITION_ADVICE,
                            adviceMessages);
                    }
                } else if (adviceName.equalsIgnoreCase(
                        ISAuthConstants.ROLE_PARAM)) {
                    Set adviceMessages =
                        getAdviceMessagesforRole(adviceValue,token,env);
                    if (adviceMessages.isEmpty()) {
                        allowed = true;
                    } else {
                        advices.put(PolicyDecisionUtils.AUTH_ROLE_ADVICE,
                            adviceMessages);
                    }
                } else if (adviceName.equalsIgnoreCase(
                        ISAuthConstants.USER_PARAM)) {
                    Set adviceMessages =
                        getAdviceMessagesforUser(adviceValue,token,env);
                    if (adviceMessages.isEmpty()) {
                        allowed = true;
                    } else {
                        advices.put(PolicyDecisionUtils.AUTH_USER_ADVICE,
                            adviceMessages);
                    }
                } else if (adviceName.equalsIgnoreCase(
                        ISAuthConstants.REDIRECT_URL_PARAM)) {
                    Set adviceMessages =
                        getAdviceMessagesforRedirectURL(adviceValue,token,env);
                    if (adviceMessages.isEmpty()) {
                        allowed = true;
                    } else {
                        advices.put(PolicyDecisionUtils.AUTH_REDIRECTION_ADVICE,
                            adviceMessages);
                    }
                } else if ((adviceName.equalsIgnoreCase(
                        ISAuthConstants.REALM_PARAM)) ||
                        (adviceName.equalsIgnoreCase(
                        ISAuthConstants.ORG_PARAM))) {
                    Set adviceMessages =
                        getAdviceMessagesforRealm(adviceValue,token,env);
                    if (adviceMessages.isEmpty()) {
                        allowed = true;
                    } else {
                        advices.put(AUTHENTICATE_TO_REALM_CONDITION_ADVICE,
                            adviceMessages);
                    }
                } else {
                    if ( DEBUG.messageEnabled()) {
                        DEBUG.message("At ResourceEnvIPCondition."
                                + "getConditionDecision(): "
                                + "adviceName is invalid");
                    }
                }
            }

        } else if (adviceStr != null) {
            String args[] = { adviceStr };
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_property_value", args, null);
        } else {
            if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getConditionDecision(): "
                            + "Advice is NULL since there is no matching "
                            + "condition found.");
            }
        }

	return new ConditionDecision(allowed, advices);
    }

    /** 
     * Returns advice messages for Authentication Scheme condition.
     */
    private Set getAdviceMessagesforAuthScheme(String adviceValue, 
        SSOToken token, Map env) throws PolicyException, SSOException {
        Set adviceMessages = new HashSet();
        Set requestAuthSchemes = null;
        Set requestAuthSchemesIgnoreRealm = null;
        if ( (env != null) 
                    && (env.get(REQUEST_AUTH_SCHEMES) != null) ) {
            try {
                requestAuthSchemes = (Set) env.get(REQUEST_AUTH_SCHEMES);
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforAuthScheme(): "
                            + "requestAuthSchemes from env= " 
                            + requestAuthSchemes);
                }
            } catch (ClassCastException e) {
                String args[] = { REQUEST_AUTH_SCHEMES };
                throw new PolicyException(
                        ResBundleUtils.rbName, "property_is_not_a_Set", 
                        args, e);
            }
        } else {
            if (token != null) {
                requestAuthSchemes 
                        = AMAuthUtils.getRealmQualifiedAuthenticatedSchemes(
                        token);
                requestAuthSchemesIgnoreRealm =
                        AMAuthUtils.getAuthenticatedSchemes(token);
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforAuthScheme(): "
                            + "requestAuthSchemes from ssoToken= " 
                            +  requestAuthSchemes);
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforAuthScheme(): "
                            + "requestAuthSchemesIgnoreRealm from ssoToken= " 
                            +  requestAuthSchemesIgnoreRealm);
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
            String realm = AMAuthUtils.getRealmFromRealmQualifiedData(
                    authScheme); 
            if  ((realm != null) && (realm.length() != 0)) {

                adviceMessages.add(authScheme);
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforAuthScheme():"
                            + "authScheme not satisfied = "
                            + authScheme);
                }

            } else if ((realm == null) || (realm.length() == 0)) {
                if (!requestAuthSchemesIgnoreRealm.contains(authScheme)) {

                    adviceMessages.add(authScheme);
                    if ( DEBUG.messageEnabled()) {
                        DEBUG.message("At ResourceEnvIPCondition."
                                + "getAdviceMessagesforAuthScheme():"
                                + "authScheme not satisfied = "
                                + authScheme);
                    }

                }

            }
        }

        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At ResourceEnvIPCondition." + 
                    "getAdviceMessagesforAuthScheme():"
                    + "authScheme = " + authScheme + "," 
                    + " requestAuthSchemes = " + requestAuthSchemes + ", "
		    + " adviceMessages = " + adviceMessages);
        }
        return adviceMessages;
    }
    
    /** 
     * Returns advice messages for Authentication Service condition.
     */
    private Set getAdviceMessagesforAuthService(String adviceValue, 
        SSOToken token, Map env) throws PolicyException, SSOException {
        Set adviceMessages = new HashSet();
        Set requestAuthnServices = new HashSet();
        boolean allow = false;
        if ( (env != null) 
                && (env.get(REQUEST_AUTHENTICATED_TO_SERVICES) != null) ) {
            try {
                requestAuthnServices = (Set) env.get(
                    REQUEST_AUTHENTICATED_TO_SERVICES);
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforAuthService(): "
                            + "requestAuthnServices from request = " 
                            + requestAuthnServices);
                }
            } catch (ClassCastException e) {
                String args[] = { REQUEST_AUTHENTICATED_TO_SERVICES };
                throw new PolicyException(
                        ResBundleUtils.rbName, "property_is_not_a_Set", 
                        args, e);
            }
        } else {

            if (token != null) {
                Set authenticatedServices 
                        = AMAuthUtils.getRealmQualifiedAuthenticatedServices(
                        token);
                if (authenticatedServices != null) {
                    requestAuthnServices.addAll(authenticatedServices);
                }
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforAuthService(): "
                            + "requestAuthnServices from ssoToken = " 
                            + requestAuthnServices);
                }
            }
        }

        if (!requestAuthnServices.contains(adviceValue)) {
            String realm = AMAuthUtils.getRealmFromRealmQualifiedData(
                        adviceValue);
            if  ((realm != null) && (realm.length() != 0)) {

                adviceMessages.add(adviceValue);
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforAuthService():"
                            + "authService not satisfied = "
                            + adviceValue);
                }

            } else if ((realm == null) || (realm.length() == 0)) {
                for (Iterator iter = requestAuthnServices.iterator();
                    iter.hasNext(); ) {
                    String requestAuthnService = (String)iter.next();
                    String service = AMAuthUtils.getDataFromRealmQualifiedData(
                            requestAuthnService);
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
        
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At ResourceEnvIPCondition."
                +"getAdviceMessagesforAuthService():authenticateToService = " 
                + adviceValue + "," + " requestAuthnServices = " 
                + requestAuthnServices + ", " + " adviceMessages = " 
                + adviceMessages);
        }
        
        return adviceMessages;
    }
    
    /** 
     * Returns advice messages for Authentication Level condition.
     */
    private Set getAdviceMessagesforAuthLevel(String adviceValue, 
        SSOToken token, Map env) throws PolicyException, SSOException {
        Set adviceMessages = new HashSet();
        int maxRequestAuthLevel = Integer.MIN_VALUE;
        String authLevel = adviceValue;
        String authRealm = null;
        int authLevelInt = Integer.MIN_VALUE;
        try {
            authRealm = AMAuthUtils.getRealmFromRealmQualifiedData(authLevel);
            String authLevelIntString 
                    = AMAuthUtils.getDataFromRealmQualifiedData(authLevel);
            authLevelInt = Integer.parseInt(authLevelIntString);
        } catch (NumberFormatException e) {
            String args[] = { AUTH_LEVEL };
            throw new PolicyException(
                    ResBundleUtils.rbName, "property_is_not_an_Integer", 
                    args, null);
        }
        
        maxRequestAuthLevel = getMaxRequestAuthLevel(env,authRealm,authLevel);
        if ((maxRequestAuthLevel == Integer.MIN_VALUE) && (token != null)) {
            maxRequestAuthLevel = 
                getMaxRequestAuthLevel(token,authRealm,authLevel);
        }

        if (maxRequestAuthLevel < authLevelInt) {
            adviceMessages.add(authLevel);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("At ResourceEnvIPCondition." + 
                    "getAdviceMessagesforAuthLevel():"
                    + "authLevel=" + authLevel 
                    + "authRealm=" + authRealm
                    + ",maxRequestAuthLevel=" + maxRequestAuthLevel
                    + ",adviceMessages=" + adviceMessages);
        }
        
        return adviceMessages;            
    }
    
    /** 
     * Returns advice messages for Authentication Role condition.
     */
    private Set getAdviceMessagesforRole(String adviceValue, 
        SSOToken token, Map env) throws PolicyException, SSOException {
        Set adviceMessages = new HashSet();
        boolean allow = false;
        if (token != null) {
            String userAuthRoleNames = token.getProperty("Role");
            if ( DEBUG.messageEnabled()) {
                DEBUG.message("At ResourceEnvIPCondition." + 
                    "getAdviceMessagesforRole(): "
                    +"userAuthRoleNames from token =" + userAuthRoleNames);
            }
     
            if (userAuthRoleNames != null) {
                String userAuthRoleName = null;
                StringTokenizer st = 
                    new StringTokenizer(userAuthRoleNames, "|"); 
                while (st.hasMoreElements()) {
                    userAuthRoleName = (String)st.nextElement(); 
                    if ((userAuthRoleName != null) && 
                        (userAuthRoleName.equals(adviceValue))) {
                        allow = true;
                    }  
                }
            }
        }

        if (!allow) {
            adviceMessages.add(adviceValue);
        }
        
        if (DEBUG.messageEnabled()) {
            DEBUG.message("At ResourceEnvIPCondition.getAdviceMessagesforRole():"
                    + "auth role =" + adviceValue 
                    + ",adviceMessages=" + adviceMessages);
        }
        
        return adviceMessages;            
    }
    
    /** 
     * Returns advice messages for Authentication User condition.
     */
    private Set getAdviceMessagesforUser(String adviceValue, 
        SSOToken token, Map env) throws PolicyException, SSOException {
        Set adviceMessages = new HashSet();
        boolean allow = false;
        if (token != null) {
            String authUserNames = token.getProperty("UserToken");
            if ( DEBUG.messageEnabled()) {
                DEBUG.message("At ResourceEnvIPCondition." + 
                    "getAdviceMessagesforUser(): "
                    +"userAuthRoleNames from token =" + authUserNames);
            }
     
            if (authUserNames != null) {
                String authUserName = null;
                StringTokenizer st = new StringTokenizer(authUserNames, "|"); 
                while (st.hasMoreElements()) {
                    authUserName = (String)st.nextElement(); 
                    if ((authUserName != null) && 
                        (authUserName.equals(adviceValue))) {
                        allow = true;
                    }  
                }
            }
        }

        if (!allow) {
            adviceMessages.add(adviceValue);
        }
        
        if (DEBUG.messageEnabled()) {
            DEBUG.message("At ResourceEnvIPCondition.getAdviceMessagesforUser():"
                    + "auth user =" + adviceValue 
                    + ",adviceMessages=" + adviceMessages);
        }
        
        return adviceMessages;            
    }
    
    /** 
     * Returns advice messages for Authentication Realm condition.
     */
    private Set getAdviceMessagesforRealm(String adviceValue, 
        SSOToken token, Map env) throws PolicyException, SSOException {
        Set adviceMessages = new HashSet();
        Set requestAuthnRealms = new HashSet();
        if ( (env != null) 
                    && (env.get(REQUEST_AUTHENTICATED_TO_REALMS) != null) ) {
            try {
                requestAuthnRealms = (Set) env.get(
                    REQUEST_AUTHENTICATED_TO_REALMS);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforRealm(): "
                            + "requestAuthnRealms, from request / env = " 
                            + requestAuthnRealms);
                }
            } catch (ClassCastException e) {
                String args[] = { REQUEST_AUTHENTICATED_TO_REALMS };
                throw new PolicyException(
                        ResBundleUtils.rbName, "property_is_not_a_Set", 
                        args, e);
            }
        } else {

            if (token != null) {
                Set authenticatedRealms 
                        = AMAuthUtils.getAuthenticatedRealms(token);
                if (authenticatedRealms != null) {
                    requestAuthnRealms.addAll(authenticatedRealms);
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforRealm(): "
                            + "requestAuthnRealms, from ssoToken = " 
                            + requestAuthnRealms);
                }
            }
        }

        String authRealm = adviceValue;
        
        if (!requestAuthnRealms.contains(authRealm)) {
            adviceMessages.add(authRealm);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("At ResourceEnvIPCondition."
                        + "getAdviceMessagesforRealm():"
                        + "authenticateToRealm not satisfied = "
                        + authRealm);
            }
        }
        
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At ResourceEnvIPCondition." + 
                    "getAdviceMessagesforRealm():"
                    + "authRealm = " + authRealm + "," 
                    + " requestAuthnRealms = " + requestAuthnRealms + ", "
		    + " adviceMessages = " + adviceMessages);
        }
        return adviceMessages;
        
    }
    
    /** 
     * Returns advice messages for Authentication Redirect condition.
     */
    private Set getAdviceMessagesforRedirectURL(String adviceValue, 
        SSOToken token, Map env) throws PolicyException, SSOException {
        Set adviceMessages = new HashSet();
        Set requestAuthSchemes = null;
        Set requestAuthSchemesIgnoreRealm = null;
        boolean nullRealm = false;
        boolean allow = false;
        String orgName = "/";
        if ( (env != null) 
                    && (env.get(REQUEST_AUTH_SCHEMES) != null) ) {
            try {
                Map policyConfigMap = (Map) env.get("sun.am.policyConfig");
                if (policyConfigMap != null) {
                    Set orgSet = (Set) policyConfigMap.get("OrganizationName");
                    if (orgSet != null) {
                        Iterator names = orgSet.iterator();
                        orgName = (String) names.next();
                    }
                }
                requestAuthSchemes = (Set) env.get(REQUEST_AUTH_SCHEMES);
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforRedirectURL(): "
                            + "requestAuthSchemes from env= " 
                            + requestAuthSchemes
                            + " AND orgName from env= "
                            + orgName);
                }
            } catch (ClassCastException e) {
                String args[] = { REQUEST_AUTH_SCHEMES };
                throw new PolicyException(
                        ResBundleUtils.rbName, "property_is_not_a_Set", 
                        args, e);
            }
        } else {
            if (token != null) {
                orgName = token.getProperty(ISAuthConstants.ORGANIZATION);
                requestAuthSchemes 
                        = AMAuthUtils.getRealmQualifiedAuthenticatedSchemes(
                        token);
                requestAuthSchemesIgnoreRealm =
                        AMAuthUtils.getAuthenticatedSchemes(token);
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforRedirectURL(): "
                            + "orgName from ssoToken= "
                            +  orgName);
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforRedirectURL(): "
                            + "requestAuthSchemes from ssoToken= " 
                            +  requestAuthSchemes);
                    DEBUG.message("At ResourceEnvIPCondition."
                            + "getAdviceMessagesforRedirectURL(): "
                            + "requestAuthSchemesIgnoreRealm from ssoToken= " 
                            +  requestAuthSchemesIgnoreRealm);
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
            SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            for (Iterator iter = requestAuthSchemes.iterator();
                iter.hasNext(); ) {
                String requestAuthnScheme = (String)iter.next();
                schemeInstance = AMAuthUtils.getDataFromRealmQualifiedData(
                        requestAuthnScheme);
                String realm = AMAuthUtils.getRealmFromRealmQualifiedData(
                        requestAuthnScheme);
                if ((realm == null) || (realm.length() == 0)) {
                    nullRealm = true;
                    break;
                } else {
                    AMAuthenticationManager authManager = 
                        new AMAuthenticationManager(adminToken,orgName);
                    AMAuthenticationInstance authInstance = 
                        authManager.getAuthenticationInstance(schemeInstance);
                    authSchemeType = authInstance.getType();
                    if ("Federation".equals(authSchemeType)) {
                        allow = true;
                        break;
                    }
                }
            }

            if (nullRealm) {
                for (Iterator iter = requestAuthSchemesIgnoreRealm.iterator();
                    iter.hasNext(); ) {
                    schemeInstance = (String)iter.next();
                    AMAuthenticationManager authManager = 
                        new AMAuthenticationManager(adminToken,orgName);
                    AMAuthenticationInstance authInstance = 
                        authManager.getAuthenticationInstance(schemeInstance);
                    authSchemeType = authInstance.getType();
                    if ("Federation".equals(authSchemeType)) {
                        allow = true;
                        break;
                    }
                }
            }

        } catch (AMConfigurationException ace) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("ResourceEnvIPCondition." + 
                        "getAdviceMessagesforRedirectURL():"
                        + "got AMConfigurationException:"
                        + "schemeInstance=" + schemeInstance 
                        + ", authSchemeType = " + authSchemeType);
            }
            Object[] args = {schemeInstance};
            throw new PolicyException(
                    ResBundleUtils.rbName, "auth_scheme_not_found", 
                    args, ace);
        }
        if (!allow) {
            adviceMessages.add(adviceValue);
        }
        
        if (DEBUG.messageEnabled()) {
            DEBUG.message("At ResourceEnvIPCondition." + 
                    "getAdviceMessagesforRedirectURL():"
                    + "redirectURL=" + adviceValue 
                    + "schemeInstance=" + schemeInstance
                    + ",authSchemeType=" + authSchemeType
                    + ",adviceMessages=" + adviceMessages);
        }
        
        return adviceMessages;            
    }
    
    /**
     * Returns the maximum auth level specified for the REQUEST_AUTH_LEVEL
     * property in the environment Map.
     * @see #REQUEST_AUTH_LEVEL
     */
    private int getMaxRequestAuthLevel(Map env, String authRealm, 
            String authLevel) throws PolicyException {
        int maxAuthLevel = Integer.MIN_VALUE;
        int currentAuthLevel = Integer.MIN_VALUE;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("ResourceEnvIPCondition.getMaxRequestAuthLevel("
                    + "envMap,authRealm,authLevel): entering: envMap= " + env 
                    + ", authRealm= " + authRealm 
                    +  ", conditionAuthLevel= " + authLevel);
        }
        Object envAuthLevelObject = env.get(REQUEST_AUTH_LEVEL);
        if (envAuthLevelObject != null) {
            if(envAuthLevelObject instanceof Integer) {
                if ((authRealm == null) || (authRealm.length() == 0)) {
                    maxAuthLevel = ((Integer)envAuthLevelObject).intValue();
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("ResourceEnvIPCondition."
                            +"getMaxRequestAuthLevel():Integer level in env= " 
                            + maxAuthLevel);
                    }
                }
            } else if (envAuthLevelObject instanceof Set) {
                Set envAuthLevelSet = (Set)envAuthLevelObject;
                if (!envAuthLevelSet.isEmpty()) {
                    Iterator iter = envAuthLevelSet.iterator();
                    while (iter.hasNext()) { 
                        Object envAuthLevelElement = iter.next();
                        if (!(envAuthLevelElement instanceof String)) {
                            if (DEBUG.warningEnabled()) {
                                DEBUG.warning("ResourceEnvIPCondition."
                                        + "getMaxRequestAuthLevel():"
                                        + "requestAuthLevel Set element"
                                        + " not String");
                            }
                            throw new PolicyException(
                               ResBundleUtils.rbName, 
                              "request_authlevel_in_env_set_element_not_string",
                               null, null);
                        } else {
                            String qualifiedLevel = (String)envAuthLevelElement;
                            currentAuthLevel = getAuthLevel(qualifiedLevel);
                            if ((authRealm == null) 
                                        || authRealm.length() == 0) {
                                if(currentAuthLevel > maxAuthLevel) {
                                    maxAuthLevel = currentAuthLevel;
                                }
                            } else { 
                                String realmString = AMAuthUtils.
                                     getRealmFromRealmQualifiedData(
                                     qualifiedLevel);
                                if(authRealm.equals(realmString) 
                                        && (currentAuthLevel > maxAuthLevel)) {
                                    maxAuthLevel = currentAuthLevel;
                                }
                            }
                        }
                    }
                }
            } else {
                if (DEBUG.warningEnabled()) {
                    DEBUG.warning("ResourceEnvIPCondition.getMaxRequestAuthLevel():"
                            + "requestAuthLevel in env neither"
                            + " Integer nor Set");
                }
                throw new PolicyException(
                    ResBundleUtils.rbName, 
                    "request_authlevel_in_env_not_Integer_or_set", 
                    null, null);
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("ResourceEnvIPCondition.getMaxRequestAuthLevel("
                    + "): returning: maxAuthLevel=" + maxAuthLevel);
        }
        return maxAuthLevel;
    }

    /**
     * Returns the maximum auth level specified for the REQUEST_AUTH_LEVEL
     * property in the SSO token.
     * @see #REQUEST_AUTH_LEVEL
     */
    private int getMaxRequestAuthLevel(SSOToken token, String authRealm, 
            String authLevel) throws PolicyException, SSOException {
        int maxAuthLevel = Integer.MIN_VALUE;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("ResourceEnvIPCondition.getMaxRequestAuthLevel("
                    + "token,authRealm,authLevel): entering:"
                    +  " authRealm = " + authRealm
                    +  ", conditionAuthLevel= " + authLevel);
        }
        if ((authRealm == null) || authRealm.length() == 0) {
            Set levels 
                    = AMAuthUtils.getAuthenticatedLevels(token);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("ResourceEnvIPCondition.getMaxRequestAuthLevel("
                        + "): levels from token= " 
                        + levels);
            }
            if ((levels != null) && (!levels.isEmpty())) {
                Iterator iter = levels.iterator();
                while (iter.hasNext()) {
                    String levelString = (String)iter.next();
                    int level = getAuthLevel(levelString);
                    maxAuthLevel = (level > maxAuthLevel)? level : maxAuthLevel;
                }
            }
        } else {
            Set qualifiedLevels 
                    = AMAuthUtils.getRealmQualifiedAuthenticatedLevels(token);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("ResourceEnvIPCondition.getMaxRequestAuthLevel("
                        + "): qualifiedLeves from token= " 
                        + qualifiedLevels);
            }
            if ((qualifiedLevels != null) && (!qualifiedLevels.isEmpty())) {
                Iterator iter = qualifiedLevels.iterator();
                while (iter.hasNext()) {
                    String qualifiedLevel = (String)iter.next();
                    String realm = AMAuthUtils.getRealmFromRealmQualifiedData(
                            qualifiedLevel);
                    if (authRealm.equals(realm)) {
                        int level = getAuthLevel(qualifiedLevel);
                        maxAuthLevel = (level > maxAuthLevel)? level 
                                : maxAuthLevel;
                    }
                }
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("ResourceEnvIPCondition.getMaxRequestAuthLevel("
                    + "): returning:"
                    +  " maxAuthLevel= " + maxAuthLevel);
        }
        return maxAuthLevel;
    }

    /**
     * Extracts the integer auth level from  String realm qualified 
     * ( realm:level) String.
     */
    private int getAuthLevel(String qualifiedLevel) 
            throws PolicyException {
        int levelInt = 0;
        String levelString 
                = AMAuthUtils.getDataFromRealmQualifiedData(qualifiedLevel);
        try {
            levelInt = Integer.parseInt(levelString);
        } catch (NumberFormatException nfe) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("AuthLevelCondition.getAuthLevel(qualifiedLevel):"
                        + "got NumberFormatException:"
                        + "qualifiedLevel=" + qualifiedLevel 
                        + ", levelString = " + levelString);
            }
            Object[] args = {levelString};
            throw new PolicyException(
                    ResBundleUtils.rbName, "auth_level_not_integer", 
                    args, nfe);
        }
        return levelInt;
    }

    /**
     * Returns the advice string that satisfies or matches for the client
     * environment parameter, including client's IP Address.
     */
    private String getAdviceStrForEnv(Map env, SSOToken token) 
        throws PolicyException, SSOException {
        String adviceStr = null;

        //Check if all the keys are valid
        for (int i=0; i < envList.size(); i++) {
            String key = (String) envList.get(i);
            if (key != null) {
                if (key.contains("=")) {

                    StringTokenizer st = new StringTokenizer(key, "=");
                    int tokenCount = st.countTokens();
                    if ( tokenCount != 2 ) {
                        String args[] = { key };
                        throw new PolicyException(ResBundleUtils.rbName,
                        "invalid_property_value", args, null);
                    }

                    String envParamName = st.nextToken().trim();
                    String envParamValue = envParamName;
                    if ( tokenCount == 2 ) {
                        envParamValue = st.nextToken().trim();
                    }

                    Set envSet = (Set) env.get(envParamName);
                    String strEnv = null;
                    if ((envSet != null) && (!envSet.isEmpty())){
                        Iterator names = envSet.iterator();
                        while (names.hasNext()) {
                            strEnv = (String) names.next();
                            if ((strEnv != null) &&
                                (strEnv.equalsIgnoreCase(envParamValue)) ){
                                adviceStr = (String) adviceList.get(i);
                                break;
                            }
                        }
                    } else {
                        String strIP = null;
                        Object object = env.get(REQUEST_IP);
                        if (object instanceof Set) {
                            Set ipSet = (Set) object;
                            if ( (ipSet == null) || (ipSet.isEmpty()) ) {
                                if (token != null) {
                                    strIP = token.getIPAddress().getHostAddress();
                                } else {
                                    throw new PolicyException(
                                        ResBundleUtils.rbName,"client_ip_null",
                                        null, null);
                                }
                            } else {
                                Iterator names = ipSet.iterator();
                                strIP = (String) names.next();
                            }
                        } else if (object instanceof String) {
                            strIP = (String) object;
                            if (strIP == null) {
                                if (token != null) {
                                    strIP = token.getIPAddress().getHostAddress();
                                } else {
                                    throw new PolicyException(
                                        ResBundleUtils.rbName,"client_ip_null",
                                        null, null);
                                }
                            }
                        }

                        long requestIpV4 = 0;
                        IPv6Address requestIpV6 = null;
                        if(ValidateIPaddress.isIPv4(strIP)){
                            requestIpV4 = stringToIp(strIP);
                        } else if (ValidateIPaddress.isIPv6(strIP)){
                            requestIpV6 = IPv6Address.fromString(strIP);
                        } else {
                            if ( DEBUG.messageEnabled()) {
                                DEBUG.message("ResourceEnvIPCondition:getAdviceStrForEnv invalid strIP : "
                                        + strIP);
                            }
                            continue;
                        }

                        int bIndex = envParamValue.indexOf("[");
                        int lIndex = envParamValue.indexOf("]");
                        String ipVal = 
                            envParamValue.substring(bIndex+1, lIndex);
                    
                        if (ipVal.contains("-")) {                  
                            StringTokenizer stIP = 
                                new StringTokenizer(ipVal, "-");
                            int tokenCnt = stIP.countTokens();
                            if ( tokenCnt > 2 ) {
                                String args[] = { ipVal };
                                throw new PolicyException(ResBundleUtils.rbName,
                                "invalid_property_value", args, null);
                            }

                            String startIp = stIP.nextToken();
                            String endIp = startIp;
                            if ( tokenCnt == 2 ) {
                                endIp = stIP.nextToken();
                            }

                            if(ValidateIPaddress.isIPv4(strIP) &&
                                    ValidateIPaddress.isIPv4(startIp) && ValidateIPaddress.isIPv4(endIp)){
                                long lStartIP = stringToIp(startIp);
                                long lEndIP = stringToIp(endIp);
                                if ( (requestIpV4 >= lStartIP) &&
                                        ( requestIpV4 <= lEndIP) ) {
                                    adviceStr = (String) adviceList.get(i);
                                    break;
                                }
                            } else if (ValidateIPaddress.isIPv6(strIP) &&
                                    ValidateIPaddress.isIPv6(startIp) && ValidateIPaddress.isIPv6(endIp)){
                                IPv6AddressRange ipv6Range = IPv6AddressRange.fromFirstAndLast(
                                        IPv6Address.fromString(startIp),IPv6Address.fromString(endIp));
                                if(requestIpV6 != null && ipv6Range.contains(requestIpV6)) {
                                    adviceStr = (String) adviceList.get(i);
                                    break;
                                }
                            } else {
                                String args[] = { strIP };
                                throw new PolicyException(ResBundleUtils.rbName,
                                        "invalid_property_value", args, null);
                            }

                        } else if (requestIpV4 != 0 && ValidateIPaddress.isIPv4(ipVal)) {
                            long longIp = stringToIp(ipVal);
                            if (requestIpV4 == longIp) {
                                adviceStr = (String) adviceList.get(i);
                                break;
                            }
                        } else if (requestIpV6 != null && ValidateIPaddress.isIPv6(ipVal)) {
                            // treat as single ip address
                            IPv6Address iPv6AddressIpVal = IPv6Address.fromString(ipVal);
                            if(iPv6AddressIpVal.compareTo(requestIpV6) == 0){

                                adviceStr = (String) adviceList.get(i);
                                break;
                            }
                        }
                        else if (ipVal.contains("*")) {
                            adviceStr = (String) adviceList.get(i);
                            break;
                        } else {
                            String args[] = {ipVal};
                            throw new PolicyException(
                                ResBundleUtils.rbName, 
                                "resource_env_not_known",
                                args, null);
                        }
                    }

                } else {
                    String args[] = {key};
                    throw new PolicyException(
                        ResBundleUtils.rbName,
                        "resource_env_not_known",
                        args, null);
                }
            }
        }

        return adviceStr;
    }
    
    /**
     * Converts String represenration of IP address to
     * a long.
     */ 
    private long stringToIp(String ip) throws PolicyException {
        StringTokenizer st = new StringTokenizer(ip, ".");
        int tokenCount = st.countTokens();
        if ( tokenCount != 4 ) {
            String args[] = { "ip", ip };
            throw new PolicyException(ResBundleUtils.rbName,
                    "invalid_property_value", args, null);
        }
        long ipValue = 0L;
        while ( st.hasMoreElements()) {
            String s = st.nextToken();
            short ipElement = 0;
            try {
                ipElement = Short.parseShort(s);
            } catch(Exception e) {
                String args[] = { "ip", ip };
                throw new PolicyException(ResBundleUtils.rbName,
                        "invalid_property_value", args, null);
            }
            if ( ipElement < 0 || ipElement > 255 ) {
                String args[] = { "ipElement", s };
                throw new PolicyException(ResBundleUtils.rbName,
                        "invalid_property_value", args, null);
            }
            ipValue = ipValue * 256L + ipElement;
        }
        return ipValue;
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        ResourceEnvIPCondition theClone = null;
        try {
            theClone = (ResourceEnvIPCondition) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (properties != null) {
            theClone.properties = new HashMap();
            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                Object o = it.next();
                Set values = new HashSet();
                values.addAll((Set) properties.get(o));
                theClone.properties.put(o, values);
            }
        }
        return theClone;
    }

}
