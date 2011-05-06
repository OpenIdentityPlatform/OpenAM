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
 * $Id: SessionCondition.java,v 1.4 2008/06/25 05:43:52 qcheng Exp $
 *
 */




package com.sun.identity.policy.plugins;


import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.interfaces.Condition;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;

/**
 * The class <code>SessionCondition</code> is a plugin implementation
 * of <code>Condition</code>. This lets you define the maximum
 * user session time during which a policy applies. There is an option
 * to terminate the user session if the session time exceeds the
 * maximum allowed.
 *
 */
public class SessionCondition implements Condition {

    /**
     * Key that is used to define the user session creation time
     * of the request. This is passed in to the <code>env</code> parameter while
     * invoking <code>getConditionDecision</code> method of the
     * <code>SessionCondition</code>. Value for the key should be a
     * <code>Long</code> whose value is time in milliseconds since epoch.
     */
    public static final String REQUEST_SESSION_CREATION_TIME =
	"requestSessionCreationTime";

    /**
     * Key that is used to identify the advice messages from this
     * condition.
     */
    public static final String SESSION_CONDITION_ADVICE =
        "SessionConditionAdvice";

    /**
     * Key that is used in the <code>Advice</code> to identify the session was
     * terminated.
     */
    public static final String ADVICE_TERMINATE_SESSION = "terminateSession";

    /**
     * Key that is used in the <code>Advice</code> to identify the condition 
     * decision is <code>deny</code>.
     */
    public static final String ADVICE_DENY = "deny";

    private static final String SSOTOKEN_PROPERTY_AUTHINSTANT = "authInstant";

    private static final String SESSION_CONDITION_TRUE_VALUE =
        "session_condition_true_value";

    private static final String SESSION_CONDITION_FALSE_VALUE =
        "session_condition_false_value";

    private Map properties;
    private long maxSessionTime;
    private boolean terminateSession;

    private static Debug debug
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private static List propertyNames = new ArrayList(2);

    private static AMResourceBundleCache amCache =
            AMResourceBundleCache.getInstance();



    static {
        propertyNames.add(MAX_SESSION_TIME);
        propertyNames.add(TERMINATE_SESSION);
    }

    /**
     * No argument constructor.
     */
    public SessionCondition() {
    }

    /**
     * Gets a list of property names for the condition.
     *
     * @return list of property names
     */
    public List getPropertyNames() {
        return (new ArrayList(propertyNames));
    }

    /**
     * Gets the <code>Syntax</code> for a property name.
     *
     * @param property property name
     * @return <code>Syntax<code> for the property name
     *
     * @see com.sun.identity.policy.Syntax
     */
    public Syntax getPropertySyntax(String property) {
        if (property.equals(TERMINATE_SESSION)) {
            return Syntax.SINGLE_CHOICE;
        }
        return Syntax.ANY;
    }

    /**
     * Gets the display name for the property name.
     * The locale variable could be used by the plugin to
     * customize the display name for the given locale.
     * The locale variable could be null, in which
     * case the plugin must use the default locale.
     *
     * @param property property name
     * @param locale locale for which the property name must be customized
     * @return display name for the property name
     * @exception PolicyException if unable to get the diplay name.
     */
    public String getDisplayName(String property, java.util.Locale locale)
        throws PolicyException {
        ResourceBundle rb = amCache.getResBundle(ResBundleUtils.rbName, locale);
        return com.sun.identity.shared.locale.Locale.getString(rb, property);
    }

    /**
     * Gets a set of valid values given the property name. This method
     * is called if the property <code>Syntax</code> is either the
     * <code>SINGLE_CHOICE</code> or <code>MULTIPLE_CHOICE</code>.
     * 
     * @param property property name
     * @return set of valid values for the property
     *
     * @exception PolicyException if unable to get the <code>Syntax</code>
     */
    public Set getValidValues(String property) throws PolicyException {
        if (property.equals(TERMINATE_SESSION)) {
            // use OrderedSet to ensure the default is "false"
            Set values = new OrderedSet();
            values.add(SESSION_CONDITION_FALSE_VALUE);
            values.add(SESSION_CONDITION_TRUE_VALUE);
            return values;
        }
        return Collections.EMPTY_SET;
    }

    /**
     * Sets the properties of the condition.
     * Evaluation of <code>ConditionDecision</code> is influenced
     * by these properties.
     *
     * @param properties the properties of the condition that governs
     *        whether a policy applies. The properties should define
     *        value for <code>MAX_SESSION_TIME</code> and optionally
     *        <code>TERMINATE_SESSION</code>.
     *        The value should be a Set of string values. The value for
     *        <code>MAX_SESSION_TIME</code> should be parse-able as
     *        an <code>Integer</code>
     *
     * @throws PolicyException if properties is null or does not contain
     *         valid value for <code>MAX_SESSION_TIME</code>
     */
    public void setProperties(Map properties) throws PolicyException {
        this.properties = properties;
        validateProperties();
    }

    /**
     * Gets the properties of the condition.
     * @return unmodifiable <code>Map</code> view of the properties that govern 
     *         the evaluation of the condition.
     *         Please note that properties is not cloned before returning
     */
    public Map getProperties() {
        return (properties == null)
            ? null : Collections.unmodifiableMap(properties);
    }

    /**
     * Gets the decision computed by this condition object, based on the 
     * map of environment parameters or the user token. If the value of
     * <code>TERMINATE_SESSION</code> is true and the condition
     * evaluation is false, it terminates the user session.
     *
     * @param token single-sign-on token of the user
     * @param env request specific environment map of key/value pair. This
     *        condition looks for value of key
     *        <code>REQUEST_SESSION_CREATION_TIME</code> in the map. And the
     *        value should be a <code>Long</code>. If the <code>env</code> is
     *        null of does not define value for
     *        <code>REQUEST_SESSION_CREATION_TIME</code>, the
     *        value will be obtained from SSO token of the user
     * @return The condition decision. The condition decision encapsulates
     *         whether a policy applies for the request and advice messages 
     *         generated by the condition.
     *         Policy framework continues evaluating a policy only if it
     *         applies to the request as indicated by the condition decision.
     *         Otherwise, further evaluation of the policy is skipped.
     *         However, the advice messages encapsulated in the
     *         condition decision are aggregated and passed up, encapsulated in
     *         the policy decision
     *
     * @throws PolicyException if the condition has not been initialized
     * @throws SSOException if the SSO token is invalid or there is error when
               trying to destroy the SSO token
     *
     * @see com.sun.identity.policy.ConditionDecision
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
        throws PolicyException, SSOException {
        boolean allowed = false;
        Long requestSessionCreationTime = null;

        if (token == null) {
            return new ConditionDecision(true, Long.MAX_VALUE);
        }

        if (env != null) {
            try {
                requestSessionCreationTime =
                    (Long) env.get(REQUEST_SESSION_CREATION_TIME);
            } catch (ClassCastException e) {
                String args[] = {REQUEST_SESSION_CREATION_TIME};
                throw new PolicyException(
                    ResBundleUtils.rbName,
                    "property_is_not_a_Long", args, null);
            }
        }
        
        long tokenCreationTime;
        if (requestSessionCreationTime != null) {
            tokenCreationTime = requestSessionCreationTime.longValue();
        } else {
            try {
                tokenCreationTime = (DateUtils.stringToDate(token.getProperty(
                    SSOTOKEN_PROPERTY_AUTHINSTANT))).getTime();
            } catch (ParseException e) {
                throw new PolicyException(
                    ResBundleUtils.rbName,
                    "unable_to_parse_ssotoken_authinstant", null, e);
            }
        }

        long currentTime = System.currentTimeMillis();
        long timeToLive = Long.MAX_VALUE;
        long expiredTime = tokenCreationTime + maxSessionTime;
        if (debug.messageEnabled()) {
            debug.message(
                new StringBuffer("SessionCondition.getConditionDecision():")
                .append("\n  currentTime: ").append(currentTime)
                .append("\n  expiredTime: ").append(expiredTime).toString());
        }

        ConditionDecision conditionDecision = null;
        if (currentTime < expiredTime) {
            allowed = true;
            timeToLive = expiredTime;
            conditionDecision = new ConditionDecision(allowed, timeToLive);
        } else {
            Map advices = new HashMap(1);
            Set adviceMessages = null;
            if (terminateSession) {
                // set advice message
                adviceMessages = new HashSet(2);
                adviceMessages.add(ADVICE_DENY);
                adviceMessages.add(ADVICE_TERMINATE_SESSION);
                // terminate token session
                try {
                    SSOTokenManager.getInstance().destroyToken(token);
                    if (debug.messageEnabled()) {
                        debug.message(
                            "SessionCondition.getConditionDecision(): " +
                            "successfully terminated user session!");
                    }
                } catch (SSOException ssoEx) {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "SessionCondition.getConditionDecision(): " +
                            "failed to terminate user session!", ssoEx);
                    }
                }
            } else {
                // set advice message
                adviceMessages = new HashSet(1);
                adviceMessages.add(ADVICE_DENY);
            }
            advices.put(SESSION_CONDITION_ADVICE, adviceMessages);
            conditionDecision =
                new ConditionDecision(allowed, timeToLive, advices);
        }

        return conditionDecision;
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        SessionCondition theClone = null;
        try {
            theClone = (SessionCondition) super.clone();
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
     * This method validates the properties set using the <code>setProperties
     * </code> method. It checks for the presence of the required key 
     * <code>MAX_SESSION_TIME</code>, validates it and also makes sure no other 
     * invalid key is being set. It also looks for optional key 
     * TERMINATE_SESSION and ensures its value is valid.
     * @see #MAX_SESSION_TIME
     * @see #TERMINATE_SESSION
     */ 
    private boolean validateProperties() throws PolicyException {
        if ((properties == null) || (properties.keySet() == null)) {
            throw new PolicyException(
                ResBundleUtils.rbName,
                "properties_can_not_be_null_or_empty", null, null);
        }

        if (debug.messageEnabled()) {
            debug.message("SessionCondition.validateProperties(): " +
                "properties: " + properties);
        }

        // validate and get max session time
        String value = getPropertyStringValue(MAX_SESSION_TIME, true);
        try {
            int i = Integer.parseInt(value);
            if (i > 0) {
                maxSessionTime = i * 60000;
            } else {
                String args[] = {MAX_SESSION_TIME, value};
                throw new PolicyException(
                    ResBundleUtils.rbName,
                    "invalid_property_value", args, null);
            }
        } catch (NumberFormatException e) {
            String args[] = {MAX_SESSION_TIME};
            throw new PolicyException(
                ResBundleUtils.rbName,
                "property_is_not_an_Integer", args, null);
        }

        // get value for terminate session
        value = getPropertyStringValue(TERMINATE_SESSION, false);
        if (value != null &&
            value.equals(SESSION_CONDITION_TRUE_VALUE))
        {
            terminateSession = true;
        }

        return true;
    }

    /**
     * Utility method to return the <code>propertyName</code> value
     * from the properties map.
     */
    private String getPropertyStringValue(
        String propertyName, boolean required) throws PolicyException {
        Set values = (Set) properties.get(propertyName);
        if (values == null || values.isEmpty()) {
            if (required) {
                String args[] = {propertyName};
                throw new PolicyException(
                    ResBundleUtils.rbName,"property_value_not_defined", 
                    args, null);
            } else {
                return null;
            }
        }
        return ((String) values.iterator().next());
    }

}
