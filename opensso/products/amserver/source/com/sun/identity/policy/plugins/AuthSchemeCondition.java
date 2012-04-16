/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthSchemeCondition.java,v 1.6 2009/05/05 18:29:01 mrudul_uchil Exp $
 *
 */




package com.sun.identity.policy.plugins;

import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Collections;

/**
 * The class <code>AuthSchemeCondition</code>  is a plugin implementation
 * of <code>Condition</code> that lets you define authentication module
 * instances for which a <code>Policy</code> applies.
 *
 */
public class AuthSchemeCondition implements Condition {

    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    /** 
     * Prefix for key that is used to set session property
     * to track application based session idle timeout
     */
    public static final String APPLICATION_IDLE_TIMESOUT_AT_PREFIX
            = "am.protected.policy.AppIdleTimesoutAt.";

    /** 
     * Key name that is used to communicate ForceAuth advice.
     * This indicates to authentication service that user needs to 
     * authenticate again even if he has already authenticated to 
     * required module instance, chain, level or realm.
     */
    public static final String FORCE_AUTH_ADVICE = "ForceAuth";

    /** 
     * Constant for representing <code>true</value> for ForceAuth
     */
    public static final String TRUE = "true";

    /** 
     * Constant for representing authn type of module_intance
     */
    public static final String MODULE_INSTANCE = "module_instance";

    private Map properties;
    private Set authSchemes = new HashSet(); //Set of String(s)

    private int appIdleTimeout = Integer.MAX_VALUE; //millis
    private String appName = null;
    private String appIdleTimesoutAtSessionKey = null;
    private boolean appIdleTimeoutEnabled = false;

    private static List propertyNames = new ArrayList(1);

    static {
        propertyNames.add(AUTH_SCHEME);
        propertyNames.add(APPLICATION_NAME);
        propertyNames.add(APPLICATION_IDLE_TIMEOUT);
    }

    /** No argument constructor 
     */
    public AuthSchemeCondition() {
    }

    /**
     * Returns a list of property names for the condition.
     *
     * @return list of property names
     */
    public List getPropertyNames() {
        return (new ArrayList(propertyNames));
    }

    /**
     * Returns the syntax for a property name
     * @see com.sun.identity.policy.Syntax
     *
     * @param property property name
     *
     * @return <code>Syntax<code> for the property name
     */
    public Syntax getPropertySyntax(String property) {
        return Syntax.NONE;
    }

    /**
     * Gets the display name for the property name.
     * The <code>locale</code> variable could be used by the plugin to
     * customize the display name for the given locale.
     * The <code>locale</code> variable could be <code>null</code>, in which
     * case the plugin must use the default locale.
     *
     * @param property property name
     * @param locale locale for which the property name must be customized
     * @return display name for the property name
     * @throws PolicyException
     */
    public String getDisplayName(String property, Locale locale)
       throws PolicyException {
        return "";
    }

    /**
     * Returns a set of valid values given the property name. This method
     * is called if the property Syntax is either the SINGLE_CHOICE or
     * MULTIPLE_CHOICE.
     *
     * @param property property name
     * @return Set of valid values for the property.
     * @exception PolicyException if unable to get the Syntax.
     */
    public Set getValidValues(String property) throws PolicyException {
        return Collections.EMPTY_SET;
    }

    /** Sets the properties of the condition.  
     *  Evaluation of <code>ConditionDecision</code> is influenced by these
     *  properties.
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The properties should 
     *         define value for the key <code>AUTH_SCHEME</code>. The value
     *         should be a Set with only one element. The element should be 
     *         a String, the authentication module instance name. 
     *         Please note that  properties is not cloned by the method. 
     *
     *  @throws PolicyException if properties is null or does not contain
     *          value for the key <code>AUTH_SCHEME</code> or the value of the
     *          key is not a Set with one String element 
     *
     *  @see #REQUEST_AUTH_SCHEMES
     */
    public void setProperties(Map properties) throws PolicyException {
        this.properties = properties;
        validateProperties();
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At AuthSchemeCondition."
                    + "setProperties():"
                    + "authSchemes=" + authSchemes
                    + ", appName=" + appName
                    + ", appIdleTimeout millis=" + appIdleTimeout
                    + ", appIdleTimeoutEnabled=" + appIdleTimeoutEnabled
                    + ", appIdleTimesoutAtSessionKey"
                    + appIdleTimesoutAtSessionKey);
        }
    }

    /** Gets the properties of the condition.  
     *  @return  unmodifiable map view of properties that govern the 
     *           evaluation of  the condition.
     *           Please note that properties is  not cloned before returning
     *  @see #setProperties(Map)
     */
    public Map getProperties() {
        return (properties == null)
                ? null : Collections.unmodifiableMap(properties);
    }

    /**
     * Gets the decision computed by this condition object, based on the 
     * map of environment parameters 
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment map of key/value pairs
     *        <code>AuthSchemeCondition</code> looks for value of key
     *        <code>REQUEST_AUTH_SCHEHMES</code> in the map.  The value should
     *        be a String. If the <code>env</code> parameter is null or does not
     *       define the value for <code.REQUEST_AUTH_SCHEMES</code>, value for
     *        <code>REQUEST_AUTH_SCHEMES</code> is computed using
     *        <code>AuthMethod</code> obtained from single sign on token of
     *        the user.
     *
     * @return the condition decision. The condition decision encapsulates
     *         whether a policy applies for the request and advice messages
     *         generated by the condition.  
     * Policy framework continues evaluating a  policy only if it applies 
     * to the request  as indicated by the <code>ConditionDecision</code>. 
     * Otherwise, further evaluation of the policy is skipped. 
     * However, the advice messages encapsulated in the 
     * <code>ConditionDecision</code> are aggregated and passed up, encapsulated
     * in the policy  decision.
     *
     * @throws PolicyException if the condition has not been initialized with a
     *         successful call to <code>setProperties(Map)</code> and/or the
     *         value of <code>REQUEST_AUTH_SCHEMES</code> could not be
     *         determined.
     * @throws SSOException if the token is invalid
     *
     * @see #setProperties(Map)
     * @see #AUTH_SCHEME
     * @see #REQUEST_AUTH_SCHEMES
     * @see com.sun.identity.policy.ConditionDecision
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At AuthSchemeCondition."
                    + "getConditionDecision():entering:"
                    + "authSchemes=" + authSchemes
                    + ", appName=" + appName
                    + ", appIdleTimeout=" + appIdleTimeout);
        }
        boolean allowed = false;
        Set requestAuthSchemes = null;
        Set requestAuthSchemesIgnoreRealm = null;
        if ( (env != null) 
                    && (env.get(REQUEST_AUTH_SCHEMES) != null) ) {
            try {
                requestAuthSchemes = (Set) env.get(REQUEST_AUTH_SCHEMES);
                    if ( DEBUG.messageEnabled()) {
                        DEBUG.message("At AuthSchemeCondition."
                                + "getConditionDecision(): "
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
                    DEBUG.message("At AuthSchemeCondition."
                            + "getConditionDecision(): "
                            + "requestAuthSchemes from ssoToken= " 
                            +  requestAuthSchemes);
                    DEBUG.message("At AuthSchemeCondition."
                            + "getConditionDecision(): "
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

        Iterator authSchemesIter = authSchemes.iterator();
        String authScheme = null;
        allowed = true;
        Set adviceMessages = new HashSet(authSchemes.size());
        while (authSchemesIter.hasNext()) {
            authScheme = (String)authSchemesIter.next();
            if (!requestAuthSchemes.contains(authScheme)) {
                String realm = AMAuthUtils.getRealmFromRealmQualifiedData(
                        authScheme); 
                if  ((realm != null) && (realm.length() != 0)) {
                    allowed = false;
                    adviceMessages.add(authScheme);
                    if ( DEBUG.messageEnabled()) {
                        DEBUG.message("At AuthSchemeCondition."
                                + "getConditionDecision():"
                                + "authScheme not satisfied = "
                                + authScheme);
                    }
                    break;
                } else if ((realm == null) || (realm.length() == 0)) {
                    if (!requestAuthSchemesIgnoreRealm.contains(authScheme)) {
                        allowed = false;
                        adviceMessages.add(authScheme);
                        if ( DEBUG.messageEnabled()) {
                            DEBUG.message("At AuthSchemeCondition."
                                    + "getConditionDecision():"
                                    + "authScheme not satisfied = "
                                    + authScheme);
                        }
                        break;
                    }

                }
            }
        }

        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At AuthSchemeCondition.getConditionDecision():"
                    + "authSchemes = " + authSchemes + "," 
                    + " requestAuthSchemes = " + requestAuthSchemes + ", "
		    + " allowed before appIdleTimeout check = " + allowed);
        }
        Map advices = new HashMap();
        if (!allowed) {
            advices.put(AUTH_SCHEME_CONDITION_ADVICE, adviceMessages);
        }
        long timeToLive = Long.MAX_VALUE;

        //following additions are to support application idle timeout
        long currentTimeMillis = System.currentTimeMillis(); 
        Set expiredAuthSchemes = new HashSet(); //a collector
        if (appIdleTimeoutEnabled) {
            if (allowed) { //condition satisfied pending idletimeout check
                //do idletimeout check
                long idleTimesOutAtMillis = getApplicationIdleTimesoutAt(token,
                        expiredAuthSchemes, currentTimeMillis);
                if (idleTimesOutAtMillis <= currentTimeMillis) {
                    allowed = false;
                }
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At AuthSchemeCondition."
                            + "getConditionDecision():"
                            + "currentTimeMillis = " + currentTimeMillis 
                            + ", idleTimesOutAtMillis = " 
                            + idleTimesOutAtMillis 
                            + ", expiredAuthSchemes = " + expiredAuthSchemes 
                            + ", allowed after appIdleTimeout check = " 
                            + allowed);
                }
            }

            if (allowed) { //condition satisfied
                long appIdleTimesoutAt = currentTimeMillis + appIdleTimeout;
                token.setProperty(appIdleTimesoutAtSessionKey, 
                        Long.toString(appIdleTimesoutAt));
                timeToLive = appIdleTimesoutAt;
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At AuthSchemeCondition."
                            + "getConditionDecision():"
                            + "app access allowed, revised appIdleTimesOutAt="
                            + appIdleTimesoutAt
                            + ", currentTimeMillis=" + currentTimeMillis);
                }
            } else { //condiiton not satisifed
                adviceMessages.addAll(expiredAuthSchemes);
                advices.put(AUTH_SCHEME_CONDITION_ADVICE, adviceMessages);
                Set forceAuthAdvices = new HashSet();
                forceAuthAdvices.add(TRUE);
                advices.put(FORCE_AUTH_ADVICE, forceAuthAdvices);
            }

        }

        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At AuthSchemeCondition.getConditionDecision():"
                    + "just before return:"
                    + "allowed = " + allowed 
                    + ", timeToLive = " + timeToLive 
                    + ", advices = " + advices );
        }

        return new ConditionDecision(allowed, timeToLive, advices);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AuthSchemeCondition theClone = null;
        try {
            theClone = (AuthSchemeCondition) super.clone();
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

    /**
     * Checks the properties set using setProperties() method for
     * validity like, not null, presence of AUTH_SCHEME property,
     * and no other invalid property.
     */

    private boolean validateProperties() throws PolicyException {
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "properties_can_not_be_null_or_empty", null, null);
        }

        Set keySet = properties.keySet();
        //Check if the required key(s) are defined
        if ( !keySet.contains(AUTH_SCHEME) ) {
            String args[] = { AUTH_SCHEME };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_value_not_defined", 
                    args, null);
        }

        //Check if all the keys are valid 
        Iterator keys = keySet.iterator();
        while ( keys.hasNext()) {
            String key = (String) keys.next();
            if ( !AUTH_SCHEME.equals(key) 
                        && !APPLICATION_NAME.equals(key)
                        && !APPLICATION_IDLE_TIMEOUT.equals(key)) {
                String args[] = {key};
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "attempt_to_set_invalid_property ",
                        args, null);
            }
        }

        //validate AUTH_SCHEME
        Set authSchemeSet = (Set) properties.get(AUTH_SCHEME);
        if ( authSchemeSet != null ) {
            validateAuthSchemes(authSchemeSet);
        }

        //appIdleTimeoutEnabled
        appIdleTimeoutEnabled = false;

        //cache app name
        appName = null;
        appIdleTimesoutAtSessionKey = null;
        Set appNameSet = (Set) properties.get(APPLICATION_NAME);
        if ( (appNameSet != null) && !appNameSet.isEmpty() ) {
            appName = (String)(appNameSet.iterator().next());
            appName = appName.trim();
            if (appName.length() == 0) {
                appName = null;
            } else {
                appIdleTimesoutAtSessionKey 
                        = APPLICATION_IDLE_TIMESOUT_AT_PREFIX 
                        + appName;
            }
        }

        //cache appIdleTimeout
        Set appIdleTimeoutSet = (Set) properties.get(APPLICATION_IDLE_TIMEOUT);
        if ( (appIdleTimeoutSet != null) && !appIdleTimeoutSet.isEmpty() ) {
            String appIdleTimeoutString 
                    = (String)(appIdleTimeoutSet.iterator().next());
            appIdleTimeoutString = appIdleTimeoutString.trim();
            if (appIdleTimeoutString.length() == 0) {
                appIdleTimeoutString = null;
            } else {
                try {
                    appIdleTimeout = Integer.parseInt(appIdleTimeoutString);

                    //convert timeout in minutes to milliseconds
                    appIdleTimeout = appIdleTimeout * 60 * 1000; 
                } catch (NumberFormatException nfe) {
                    //debug warning
                    if ( DEBUG.warningEnabled()) {
                        DEBUG.warning("At AuthSchemeCondition."
                                + "validateProperties():"
                                + "can not parse appIdleTeimout"
                                + "defaulting to " + Integer.MAX_VALUE);
                    }
                    appIdleTimeout = Integer.MAX_VALUE;
                }
            }
        }

        if ((appName != null) && (appIdleTimeout != Integer.MAX_VALUE)) {
            appIdleTimeoutEnabled = true;
        }

        return true;

    }
    /**
     * Validates the module instance names provided to the setProperties()
     * call for the AUTH_SCHEME key. Checks for null and throws
     * Exception if null or not a String.
     */

    private boolean validateAuthSchemes(Set authSchemeSet) 
            throws PolicyException {
        if ( authSchemeSet.isEmpty() ) {
            String args[] = { AUTH_SCHEME };
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "property_does_not_allow_empty_values", 
                    args, null);
        }
        Iterator authSchemeSetIter = authSchemeSet.iterator();
        authSchemes.clear();
        while (authSchemeSetIter.hasNext()) {
            try {
                String authScheme = (String) authSchemeSetIter.next();
                authSchemes.add(authScheme);
            } catch (ClassCastException e) {
                String args[] = { AUTH_SCHEME };
                throw new PolicyException(
                        ResBundleUtils.rbName,"property_is_not_a_String", 
                        args, null);
            }
        }
        return true;
    }

    /**
     * Returns the time at which the application would idle time out
     * @param ssoToken <code>SSOToken</code> of the user
     * @param expiredAuthSchemes <code>Set</code> that would be filled
     *       with the authn module instance names that require 
     *       reauthentication. This <code>Set</code> acts as a collector.
     * @param currentTimeMillis current time in milli seconds
     * @throws SSOException if <code>SSOToken</code> is invalid
     * @throws PolicyException if there is any other policy error
     */
    private long getApplicationIdleTimesoutAt(SSOToken ssoToken, 
            Set expiredAuthSchemes, long currentTimeMillis) 
            throws SSOException, PolicyException {
        long idleTimesoutAtMillis = 0;
        String idleTimesoutAtString = ssoToken.getProperty(
                appIdleTimesoutAtSessionKey);
        if (idleTimesoutAtString != null) {
            try {
                idleTimesoutAtMillis = Long.parseLong(idleTimesoutAtString);
            } catch (NumberFormatException nfe) {
                //this should not happen 
                if ( DEBUG.warningEnabled()) {
                    DEBUG.warning("At AuthSchemeCondition."
                            + "getApplicationIdleTimesoutAt():"
                            + "can not parse idleTimeoutAtMillis, "
                            + "defaulting to 0");
                }
            }
            DEBUG.message("At AuthSchemeCondition."
                    + "getApplicationIdleTimesoutAt():"
                    + ",idleTimeoutAtMillis based on last access=" 
                    + idleTimesoutAtMillis
                    + ", currentTimeMillis=" + currentTimeMillis);
        } else { //first visit to application 
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At AuthSchemeCondition."
                            + "getApplicationIdleTimesoutAt():"
                            + appIdleTimesoutAtSessionKey + " not set, "
                            + "first visit to application");
                }
        }
        if (idleTimesoutAtMillis <= currentTimeMillis) {
            Iterator authSchemesIter  = authSchemes.iterator();
            while (authSchemesIter.hasNext()) {
                String authScheme = (String)authSchemesIter.next();
                long authInstant= AMAuthUtils.getAuthInstant(ssoToken, 
                        MODULE_INSTANCE, authScheme);
                idleTimesoutAtMillis = authInstant + appIdleTimeout;
                if ( DEBUG.messageEnabled()) {
                    DEBUG.message("At AuthSchemeCondition."
                            + "getApplicationIdleTimesoutAt():"
                            + "authScheme=" + authScheme
                            + ",authInstant=" + authInstant
                            + ",idleTimesoutAtMillis=" + idleTimesoutAtMillis
                            + ",currentTimeMillis=" + currentTimeMillis);
                }
                if (idleTimesoutAtMillis <= currentTimeMillis) {
                    expiredAuthSchemes.add(authScheme);
                    if ( DEBUG.messageEnabled()) {
                        DEBUG.message("At AuthSchemeCondition."
                                + "getApplicationIdleTimesoutAt():"
                                + "expired authScheme=" + authScheme);
                    }
                    break;
                }
            }
        }
        return idleTimesoutAtMillis;
    }

}
