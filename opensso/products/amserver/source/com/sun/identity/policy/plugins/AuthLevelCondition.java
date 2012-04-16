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
 * $Id: AuthLevelCondition.java,v 1.9 2009/05/26 08:06:23 kiran_gonipati Exp $
 *
 */



package com.sun.identity.policy.plugins;

import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class <code>AuthLevelCondition</code>  is a plugin implementation
 * of <code>Condition</code> interface.  This condition would imply policy 
 * applies if the <code>requestAuthLevel</code> is greater than or equal to the
 * <code>AuthLevel</code> set in the Condition. <code>requestAuthLevel</code>
 * is looked up from <code>env </code> map passed in the
 * <code>getConditionDecision()</code> call. If it is not found in the
 * <code>env</code> map, <code>AuthLevel</code> is looked up from single sign on
 * token.
 *
 */
public class AuthLevelCondition implements Condition {

    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME); 

    private Map properties;
    private String authLevel; //realmQualifiedLevel
    private String authRealm;
    private int authLevelInt;

    private static List propertyNames = new ArrayList(1);

    static {
        propertyNames.add(AUTH_LEVEL);
    }

    /** No argument constructor 
     */
    public AuthLevelCondition() {
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
     * @param property property name.
     * @param locale locale for which the property name must be customized.
     * @return display name for the property name.
     * @throws PolicyException if unable to get display name
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
     * @return <code>Set</code> of valid values for the property.
     * @exception PolicyException if unable to get the Syntax.
     */
    public Set getValidValues(String property) throws PolicyException {
        return Collections.EMPTY_SET;
    }

    /** Sets the properties of the condition.  
     *  Evaluation of <code>ConditionDecision</code> is influenced by these
     *  properties.
     *
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The properties should 
     *         define value for the key <code>AUTH_LEVEL</code>. The value
     *         should be a Set with only one element. The element should be
     *         a String, parseable as an integer or an integer qaulified with 
     *         realm name. Please note that properties is not cloned by 
     *         the method. 
     *
     *  @throws PolicyException if properties is null or does not contain
     *          value for the key <code>AUTH_LEVEL</code> or the value of the
     *          key is not a Set with one String element that is parse-able as
     *          an integer  
     *
     *  @see #REQUEST_AUTH_LEVEL
     *  @see #getConditionDecision(SSOToken, Map)
     */
    public void setProperties(Map properties) throws PolicyException {
        this.properties = (Map)((HashMap) properties);
        validateProperties();
    }

    /** Gets the properties of the condition.  
     *  @return  unmodifiable map view of properties that govern the 
     *           evaluation of  the condition decision
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
     * @param token single-sign-on token of the user
     *
     * @param env request specific environment map of key/value pairs
     *        <code>AuthLevelCondition</code> looks for value of key
     *        <code>REQUEST_AUTH_LEVEL</code> in the map.  The value should be
     *        an Integer or a set of <code>String</code>s. 
     *        If it is a <code>Set</code> of <code>String</code>s, each element 
     *        of the set has to be parseable as integer or should be a realm 
     *        qualified integer. If the <code>env</code> parameter is null or 
     *        does not define value for <code>REQUEST_AUTH_LEVEL</code>,  
     *        the value for <code>REQUEST_AUTH_LEVEL</code> is obtained from 
     *        the single sign on token of the user.
     *
     * @return the condition decision. The condition decision encapsulates
     *         whether a policy applies for the request and advice messages
     *         generated by the condition. 
     *
     *         The decision would imply policy is
     *         applicable if <code>AUTH_LEVEL</code> is greater than or equal to
     *         <code>REQUES_AUTH_LEVEL</code>. If <code>AUTH_LEVEL</code> is 
     *         qualified with a realm name, <code>REQUEST_AUTH_LEVEL</code> 
     *         values only with the matching realm name are compared. If the 
     *         policy is not applicable as determined by the 
     *         <code>Condition</code>, an <code>Advice</code> would be 
     *         included in the <code>ConditionDecision</code> with key 
     *         <code>AUTH_LEVEL_ADVICE</code> and value corresponding to 
     *         <code>AUTH_LEVEL</code>
     *
     * Policy framework continues evaluating a  policy only if it applies 
     * to the request  as indicated by the <code>ConditionDecision</code>. 
     * Otherwise, further evaluation of the policy is skipped. 
     * However, the <code>Advice</code>s encapsulated in the 
     * <code>ConditionDecision</code> are aggregated and passed up, encapsulated
     * in the <code>PolicyDecision</code>.
     *
     * @throws PolicyException if the condition has not been initialized
     *         with a successful call to <code>setProperties(Map)</code>
     *         and/or the value of <code>REQUEST_AUTH_LEVEL</code> could not
     *         be determined.
     * @throws SSOException if the token is invalid
     *
     * @see #setProperties(Map)
     * @see #AUTH_LEVEL
     * @see #REQUEST_AUTH_LEVEL
     * @see com.sun.identity.policy.ConditionDecision
     * @see com.sun.identity.authentication.util.AMAuthUtils
     *      #getAuthenticatedLevels(SSOToken)
     * @see com.sun.identity.authentication.util.AMAuthUtils
     *      #getRealmQualifiedAuthenticatedLevels(SSOToken)
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        boolean allowed = false;
        int maxRequestAuthLevel = Integer.MIN_VALUE;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthLevelCondition.getConditionDecision():"
                    + "entering"); 
        }

        maxRequestAuthLevel = getMaxRequestAuthLevel(env);
        if ((maxRequestAuthLevel == Integer.MIN_VALUE) && (token != null)) {
            maxRequestAuthLevel = getMaxRequestAuthLevel(token);
        }

        if (maxRequestAuthLevel >= authLevelInt) {
            allowed = true;
        }

        Map advices = new HashMap();
        if (!allowed) {
            Set adviceMessages = new HashSet(1);
            adviceMessages.add(authLevel);
            advices.put(AUTH_LEVEL_CONDITION_ADVICE, adviceMessages);
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("At AuthLevelCondition.getConditionDecision():"
                    + "authLevel=" + authLevel 
                    + ",maxRequestAuthLevel=" + maxRequestAuthLevel
                    + ",allowed = "  + allowed
                    + ",advices=" + advices);
        }
        return new ConditionDecision(allowed, advices);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AuthLevelCondition theClone = null;
        try {
            theClone = (AuthLevelCondition) super.clone();
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
     * <code>AUTH_LEVEL</code>, validates it and also makes sure no other 
     * invalid key is being set.
     * @see #AUTH_LEVEL
     */ 

    private boolean validateProperties() throws PolicyException {
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "properties_can_not_be_null_or_empty", null, null);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthLevelCondition.setProperties(),"
                    + "properties=" + properties);
        }

        Set keySet = properties.keySet();
        //Check if the required key(s) are defined
        if ( !keySet.contains(AUTH_LEVEL) ) {
            String args[] = { AUTH_LEVEL };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_value_not_defined", args,
                    null);
        }

        //Check if all the keys are valid 
        Iterator keys = keySet.iterator();
        while ( keys.hasNext()) {
            String key = (String) keys.next();
            if ( !AUTH_LEVEL.equals(key) ) {
                String args[] = {key};
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "attempt_to_set_invalid_property ",
                        args, null);
            }
        }

        //validate AUTH_LEVEL
        Set authLevelSet = (Set) properties.get(AUTH_LEVEL);
        if ( authLevelSet != null ) {
            validateAuthLevels(authLevelSet);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthLevelCondition.setProperties(),"
                    + "authLevel=" + authLevel
                    + ",authRealm=" + authRealm
                    + ",authLevelInt=" + authLevelInt);
        }
        return true;

    }

    /**
     * This method validates the auth levels set using the <code>setProperties
     * </code> method. It is called from validateProperties() method. 
     * It validates <code>AUTH_LEVEL</code>.
     * @see #AUTH_LEVEL
     */ 
    private boolean validateAuthLevels(Set authLevelSet) 
            throws PolicyException {
        if ( authLevelSet.isEmpty() || ( authLevelSet.size() > 1 ) ) {
            String args[] = { AUTH_LEVEL };
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "property_does_not_allow_empty_or_multiple_values", 
                    args, null);
        }
        Iterator authLevels = authLevelSet.iterator();
        authLevel = (String) authLevels.next();
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
        return true;
    }

    /**
     * gets the maximum auth level specified for the REQUEST_AUTH_LEVEL
     * property in the environment Map.
     * @see #REQUEST_AUTH_LEVEL
     */
    private int getMaxRequestAuthLevel(Map env) 
            throws PolicyException {
        int maxAuthLevel = Integer.MIN_VALUE;
        int currentAuthLevel = Integer.MIN_VALUE;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthLevelCondition.getMaxRequestAuthLevel("
                    + "envMap,realm): entering: envMap= " + env 
                    + ", authRealm= " + authRealm 
                    +  ", conditionAuthLevel= " + authLevel);
        }
        Object envAuthLevelObject = env.get(REQUEST_AUTH_LEVEL);
        if (envAuthLevelObject != null) {
            if(envAuthLevelObject instanceof Integer) {
                if ((authRealm == null) || (authRealm.length() == 0)) {
                    maxAuthLevel = ((Integer)envAuthLevelObject).intValue();
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("AuthLevelCondition."
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
                                DEBUG.warning("AuthLevelCondition."
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
                    DEBUG.warning("AuthLevelCondition.getMaxRequestAuthLevel():"
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
            DEBUG.message("AuthLevelCondition.getMaxRequestAuthLevel("
                    + "): returning: maxAuthLevel=" + maxAuthLevel);
        }
        return maxAuthLevel;
    }


    /**
     * gets the maximum auth level specified for the REQUEST_AUTH_LEVEL
     * property in the SSO token.
     * @see #REQUEST_AUTH_LEVEL
     */
    private int getMaxRequestAuthLevel(SSOToken token) 
            throws PolicyException, SSOException {
        int maxAuthLevel = Integer.MIN_VALUE;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthLevelCondition.getMaxRequestAuthLevel("
                    + "token,authRealm): entering:"
                    +  " authRealm = " + authRealm
                    +  ", conditionAuthLevel= " + authLevel);
        }
        if ((authRealm == null) || authRealm.length() == 0) {
            Set levels 
                    = AMAuthUtils.getAuthenticatedLevels(token);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("AuthLevelCondition.getMaxRequestAuthLevel("
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
            Set qualifiedLevels = null;
            if (token != null) {
                qualifiedLevels =
                    AMAuthUtils.getRealmQualifiedAuthenticatedLevels(token);
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("AuthLevelCondition.getMaxRequestAuthLevel("
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
            DEBUG.message("AuthLevelCondition.getMaxRequestAuthLevel("
                    + "): returning:"
                    +  " maxAuthLevel= " + maxAuthLevel);
        }
        return maxAuthLevel;
    }

    /**
     * Extract the integer auth level from  String realm qualified 
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

}
