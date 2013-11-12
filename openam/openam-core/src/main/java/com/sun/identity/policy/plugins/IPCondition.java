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
 * $Id: IPCondition.java,v 1.5 2009/05/05 18:29:01 mrudul_uchil Exp $
 *
 */

/*
 * Portions Copyrighted [2011-13] [ForgeRock Inc]
 */
package com.sun.identity.policy.plugins;

import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.ValidateIPaddress;
import org.forgerock.openam.network.ipv4.IPv4Condition;
import org.forgerock.openam.network.ipv6.IPv6Condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.StringTokenizer;

/**
 * The class <code>IPCondition</code>  is a plugin implementation
 * of <code>Condition</code>. This lets you define the IP addresses,
 * IP address ranges and DNS name patterns for which the policy applies
 *
 */
public class IPCondition implements Condition {

    private static final Debug DEBUG
            = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    public static final String IP_VERSION = "IpVersion";
    public static final String IPV4 = "IPv4";
    public static final String IPV6 = "IPv6";
    private IPv6Condition iPv6ConditionInstance = null;
    private IPv4Condition iPv4ConditionInstance = null;
    boolean ipv4 = false;
    boolean ipv6 = false;

    private static List propertyNames = new ArrayList(4);

    static {
        propertyNames.add(IP_VERSION);
        propertyNames.add(START_IP);
        propertyNames.add(END_IP);
        propertyNames.add(DNS_NAME);
    }

    /** No argument constructor
     */
    public IPCondition() {
        if(iPv4ConditionInstance == null) {
            iPv4ConditionInstance = new IPv4Condition();
        }
        if(iPv6ConditionInstance == null) {
            iPv6ConditionInstance = new IPv6Condition();
        }
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
     * @param property property name
     * @param locale locale for which the property name must be customized
     * @return display name for the property name
     * @throws PolicyException
     */
    public String getDisplayName(String property, Locale locale)
            throws PolicyException {
        if(ipv4){
            return iPv4ConditionInstance.getDisplayName(property,locale);
        }else if(ipv6){
            return iPv6ConditionInstance.getDisplayName(property,locale);
        } else {
            return "";
        }
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
     * @param properties the properties of the condition that governs
     *       whether a policy applies. This conditions uses properties
     *       START_IP, END_IP, IP_RANGE and DNS_NAME.  
     *       The properties should have at
     *       least one of the keys START_IP, IP_RANGE and DNS_NAME.
     *       The values of the keys should be Set where each element is a
     *       String that conforms to the format dictated by IP
     *       or DNS_NAME. The parameter is not cloned
     *       before storing the reference to it.
     * @throws PolicyException if properties is null or does not contain
     *        at least one of the keys IP and DNS_NAME
     *        and/or their values do not conform to the format dictated
     *        by IP and DNS_NAME
     * @see #START_IP
     * @see #END_IP
     * @see #IP_RANGE
     * @see #DNS_NAME
     * @see #REQUEST_IP
     * @see #REQUEST_DNS_NAME
     */
    public void setProperties(Map properties) throws PolicyException {
        checkIpVersion(properties);
        if(ipv4){
            iPv4ConditionInstance.setProperties(properties);
        }else if(ipv6){
            iPv6ConditionInstance.setProperties(properties);
        }
    }

    private void checkIpVersion(Map properties) throws PolicyException{
        Set ipVersion = (Set) properties.get(IP_VERSION);
        Iterator ipVerItr = ipVersion.iterator();
        String ip = (String) ipVerItr.next();
        if(ip.equalsIgnoreCase(IPV4)){
            ipv4 = true;
        } else if(ip.equalsIgnoreCase(IPV6)){
            ipv6 = true;
        }
    }

    /** Gets the properties of the condition.
     * @return unmodifiable map view of the properties that govern 
     *         the evaluation of  the condition.
     *         Please note that properties is  not cloned before returning
     * @see #setProperties(Map)
     */
    public Map getProperties() {
        if(ipv4){
            return iPv4ConditionInstance.getProperties();
        }else if(ipv6){
            return iPv6ConditionInstance.getProperties();
        } else {
            return null;
        }
    }

    /**
     * Gets the decision computed by this condition object, based on the 
     * map of environment parameters 
     *
     * @param token single sign on token of the user
     * @param env request specific environment map of key/value
     *        pairs <code>IPCondition</code> looks for values of keys
     *        <code>REQUEST_IP</code> and <code>REQUEST_DNS_NAME</code> in the
     *        <code>env</code> map. If <code>REQUEST_IP</code> and/or 
     *        <code>REQUEST_DNS_NAME</code> could not be determined from
     *        <code>env</code>, they are obtained from single sign on token
     *        of the user.
     *
     * @return the condition decision. The condition decision encapsulates
     *         whether a policy applies for the request and advice messages
     *         generated by the condition.  
     * Policy framework continues evaluating a  policy only if it applies 
     * to the request  as indicated by the <code>CondtionDecision</code>. 
     * Otherwise, further evaluation of the policy is skipped. 
     * However, the advice messages encapsulated in the 
     * <code>ConditionDecision</code> are aggregated and passed up, encapsulated
     * in the policy  decision.
     *
     * @throws PolicyException if the condition has not been initialized
     *        with a successful call to <code>setProperties(Map)</code> and/or
     *        the value of key <code>REQUEST_IP</code> is not a String or the
     *        value of of key <code>REQUEST_DNS_NAME</code> is not a Set of
     *        strings.
     * @throws SSOException if the token is invalid
     *
     * @see #setProperties(Map)
     * @see #START_IP
     * @see #END_IP
     * @see #IP_RANGE
     * @see #DNS_NAME
     * @see #REQUEST_IP
     * @see #REQUEST_DNS_NAME
     * @see com.sun.identity.policy.
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env)
            throws PolicyException, SSOException {
        // Determine Request IP address
        setIPVersion(env);
        if(ipv4){
            return iPv4ConditionInstance.getConditionDecision(token, env);
        }else if(ipv6){
            return iPv6ConditionInstance.getConditionDecision(token, env);
        } else {
            return new ConditionDecision(false);
        }

    }

    /**
     * Helper method to extract REQUEST_IP
     *
     * @param env map containing environment description. Note that the type of the value corresponding to REQUEST_IP
     * parameter differs depending upon invocation path. It will be a String when invoked by the agents, but it will be
     * a Set<String> when invoked via the DecisionResource (GET ws/1/entitlement/entitlements).
     * @return the IP that was used
     */
    public static String getRequestIp(Map env) {
        Object requestIpObject = env.get(REQUEST_IP);
        if (requestIpObject instanceof Set) {
            Set requestIpSet = (Set)requestIpObject;
            if ((requestIpSet != null) && (!requestIpSet.isEmpty())) {
                if (requestIpSet.size() > 1) {
                    DEBUG.warning("Set cardinality in environment map corresponding to " + REQUEST_IP +
                    " key >1. Returning first value. The set: " + requestIpSet);
                }
                Object ip = requestIpSet.iterator().next();
                if (ip != null) { // Set implementations can permit null values
                    if (ip instanceof String) {
                        return (String)ip;
                    } else {
                        DEBUG.warning("ip value in environment map not String, but type " + ip.getClass().getCanonicalName() +
                            ". The value: " + ip);
                        return ip.toString();
                    }
                } else {
                    DEBUG.warning("In IPCondition, no value in Set corresponding to " + REQUEST_IP + " key contained environment map.");
                    return null;
                }
            } else {
                DEBUG.warning("In IPCondition, Set corresponding to " + REQUEST_IP + " key in environment map is null or empty.");
                return null;
            }
        } else if (requestIpObject instanceof String) {
            return (String)requestIpObject;
        } else if (requestIpObject == null) {
            DEBUG.warning("In IPCondition, no value corresponding to " + REQUEST_IP + " key in environment map.");
            return null;
        } else {
            DEBUG.error("Unexpected type of value corresponding to  " + REQUEST_IP + " key in environment map. The type: " +
                    requestIpObject.getClass().getCanonicalName() + " and the value: " + requestIpObject);
            return requestIpObject.toString();
        }
    }

    /**
     * Determine whether IPv4 or IPv6
     * @param env map containing environment description
     */
    private void setIPVersion(Map env){
        Map holdMap = env;
        String ipVer = null;
        if(holdMap.keySet().contains(REQUEST_IP)){
            try {
                // Get IP_Version
                ipVer = getRequestIp(env);
                if(ValidateIPaddress.isIPv6(ipVer)){
                    ipv6 = true;
                } else {
                    ipv4 = true; // treat as IPv4
                }
            }catch (Exception e) {
                DEBUG.error("IPCondition.setIPVersion() : Cannot set IPversion", e);
            }
        }
    }
    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        IPCondition theClone = null;
        if(ipv4){
            iPv4ConditionInstance.clone();
        }else if(ipv6){
            iPv6ConditionInstance.clone();
        }
        return theClone;
    }
}
