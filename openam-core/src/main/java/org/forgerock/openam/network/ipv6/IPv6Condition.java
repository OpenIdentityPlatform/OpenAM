/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openam.network.ipv6;


import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.*;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.network.ValidateIPaddress;

import java.util.*;

/**
 * @author alin.brici@forgerock.com
 */
public class IPv6Condition implements Condition {
    /** Key that is used in <code>IPv6Condition</code> to define the  IP address
     * values for which a policy applies. The value corresponding to the key
     * has to be a <code>Set</code> where each element is a <code>String</code>
     * that conforms to the pattern described
     * here.
     *
     * The pattern  has 2 parts separated by "-".
     * The patterns is :
     *	  n:n:n:n:n:n:n:n[-n:n:n:n:n:n:n:n]
     * where n would take any hex value between 0-255 and a-f inclusive.
     *
     * Some sample values are
     *     FDFF:CAFE:BABE:CAB1:0:0:0:20-FDFF:CAFE:BABE:CAB1:0:0:0:230
     *	   fdff:cafe:babe:cab1:426c:8fff:fe1f:7ca7
     * @see #setProperties(java.util.Map)
     */
    public static final String IP_RANGE = "IpRange";

    private static final Debug DEBUG
            = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    public static final String IP_VERSION = "IpVersion";
    private Map properties;
    private ArrayList ipList = new ArrayList();
    private ArrayList dnsList = new ArrayList();
    private IPv6Address startIP = null;
    private IPv6Address endIP = null;

    private static List propertyNames = new ArrayList(4);

    static {
        propertyNames.add(IP_VERSION);
        propertyNames.add(START_IP);
        propertyNames.add(END_IP);
        propertyNames.add(DNS_NAME);
    }

    /** No argument constructor
     */
    public IPv6Condition() {
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
     * @throws com.sun.identity.policy.PolicyException
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
        this.properties = properties;
        ipList.clear();
        dnsList.clear();
        validateProperties();
    }

    /** Gets the properties of the condition.
     * @return unmodifiable map view of the properties that govern
     *         the evaluation of  the condition.
     *         Please note that properties is  not cloned before returning
     * @see #setProperties(Map)
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
     * @throws com.iplanet.sso.SSOException if the token is invalid
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
        boolean allowed = false;
        String ip = (String) env.get(REQUEST_IP);
        if(ValidateIPaddress.isIPv4(ip)){
            return new ConditionDecision(allowed);
        }
        if (ip == null) {
            if (token != null) {
                ip = token.getIPAddress().getHostAddress();
            }
        }
        Set reqDnsNames = (Set) env.get(REQUEST_DNS_NAME);

        if ( (ip != null) && isAllowedByIp(ip) ) {
            allowed = true;
        } else if ((reqDnsNames != null) && (!reqDnsNames.isEmpty())) {
            Iterator names = reqDnsNames.iterator();
            while (names.hasNext()) {
                String dnsName = (String) names.next();
                if (isAllowedByDns(dnsName)) {
                    allowed = true;
                    break;
                }
            }
        }
        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At IPv6Condition.getConditionDecision():requestIp, "
                    + " requestDnsName, allowed = " + ip + ", "
                    + reqDnsNames + "," + allowed );
        }
        return new ConditionDecision(allowed);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        IPv6Condition theClone = null;
        try {
            theClone = (IPv6Condition) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        theClone.dnsList = (ArrayList) dnsList.clone();
        theClone.ipList = (ArrayList) ipList.clone();
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
     * Validates the <code>properties</code> set using the
     * setProperties public method. Checks for null,
     * presence of all expected properties with valid values.
     * @see #START_IP
     * @see #END_IP
     * @see #IP_RANGE
     * @see #DNS_NAME
     */

    private boolean validateProperties() throws PolicyException {
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "properties_can_not_be_null_or_empty", null, null);
        }

        Set keySet = properties.keySet();
        // Check if the required key(s) are defined
        if ( !keySet.contains(IP_RANGE) && !keySet.contains(DNS_NAME)
                && !keySet.contains(START_IP) && !keySet.contains(IP_VERSION) ) {
            String[] args = {DNS_NAME + "," + START_IP};
            throw new PolicyException(ResBundleUtils.rbName,
                    "at_least_one_of_the_properties_should_be_defined", args,
                    null);
        }

        // Check if all the keys are valid
        Iterator keys = keySet.iterator();
        while ( keys.hasNext()) {
            String key = (String) keys.next();
            if ( !IP_RANGE.equals(key) && !DNS_NAME.equals(key)
                    && !START_IP.equals(key) && !END_IP.equals(key) && !IP_VERSION.equals(key) ) {
                String args[] = {key};
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "attempt_to_set_invalid_property ",
                        args, null);
            }
        }

        // validate IP_RANGE
        Set ipRangeSet = (Set) properties.get(IP_RANGE);
        if ( ipRangeSet != null ) {
            validateIpRangeSet(ipRangeSet);
        }

        // validate DNS_NAME
        Set dnsNameSet = (Set) properties.get(DNS_NAME);
        if ( dnsNameSet != null ) {
            validateDnsNames(dnsNameSet);
        }

        // validate START_IP and END_IP
        Set startIpSet = (Set) properties.get(START_IP);
        Set endIpSet = (Set) properties.get(END_IP);
        if ( startIpSet != null ) {
            if ( endIpSet == null ) {
                String args[] = { START_IP, END_IP};
                throw new PolicyException(ResBundleUtils.rbName,
                        "pair_property_not_defined", args, null);
            }
            validateStartIp(startIpSet);
        }

        if ( endIpSet != null ) {
            validateEndIp(endIpSet);
        }
        return true;
    }

    /**
     * validates if the value of property IP_RANGE
     * is correct and adheres to the expected
     * format
     * @see #IP_RANGE
     */
    private boolean validateIpRangeSet(Set ipSet)
            throws PolicyException {
        Iterator ipRanges = ipSet.iterator();
        while ( ipRanges.hasNext() ) {
            String ipRange = (String) ipRanges.next();
            StringTokenizer st = new StringTokenizer(ipRange, "-");
            int tokenCount = st.countTokens();
            if ( tokenCount > 2 ) {
                String args[] = { IP_RANGE, ipRange };
                throw new PolicyException(ResBundleUtils.rbName,
                        "invalid_property_value", args, null);
            }

            String startIp = st.nextToken();
            String endIp = startIp;
            if ( tokenCount == 2 ) {
                endIp = st.nextToken();
            }
            ipList.add(IPv6Address.fromString(startIp));
            ipList.add(IPv6Address.fromString(endIp));
        }
        return true;
    }

    /**
     * validates if the value of property END_IP
     * is correct and adheres to the expected
     * format
     * @see #END_IP
     */
    private boolean validateEndIp(Set ipSet)
            throws PolicyException {
        if ( ipSet.size() != 1 ) {
            String args[] = { END_IP };
            throw new PolicyException(ResBundleUtils.rbName,
                    "multiple_values_not_allowed_for", args, null);
        }
        Iterator endIpIter = ipSet.iterator();
        try {
            String endIpString = (String) endIpIter.next();
            endIP = IPv6Address.fromString(endIpString);
        } catch(ClassCastException ce) {
            String args[] = { END_IP };
            throw new PolicyException( ResBundleUtils.rbName,
                    "property_is_not_a_String", args, ce);
        }
        if (startIP.compareTo(endIP) > 0 ) {
            throw new PolicyException( ResBundleUtils.rbName,
                    "start_ip_can_not_be_greater_than_end_ip", null, null);
        }
        return true;
    }

    /**
     * validates if the value of property START_IP
     * is correct and adheres to the expected
     * format
     * @see #START_IP
     */
    private boolean validateStartIp(Set ipSet)
            throws PolicyException {
        if ( ipSet.size() != 1 ) {
            String args[] = { START_IP };
            throw new PolicyException(ResBundleUtils.rbName,
                    "multiple_values_not_allowed_for_property", args, null);
        }
        Iterator startIpIter = ipSet.iterator();
        try {
            String startIpString = (String) startIpIter.next();
            startIP = IPv6Address.fromString(startIpString);
        } catch(ClassCastException ce) {
            String args[] = { START_IP };
            throw new PolicyException( ResBundleUtils.rbName,
                    "property_is_not_a_String", args, ce);
        } catch (IllegalArgumentException iae){
            String args[] = { START_IP };
            throw new PolicyException( ResBundleUtils.rbName,
                    "property_cannot_be_parsed", args, iae);
        }
        return true;
    }

    /**
     * validates if the value of property DNS_NAME
     * is correct and adheres to the expected
     * format
     * @see #DNS_NAME
     */
    private void validateDnsNames(Set dnsNameSet)
            throws PolicyException {
        Iterator dnsNames = dnsNameSet.iterator();
        while ( dnsNames.hasNext() ) {
            String dnsName = (String) dnsNames.next();
            validateDnsName(dnsName);
            dnsList.add(dnsName.toLowerCase());
        }
    }

    /**
     * Validates a DNS name for format
     * @see #DNS_NAME
     */
    private void validateDnsName(String dnsName) throws PolicyException {
        int starIndex = dnsName.indexOf("*");
        if ((starIndex >= 0) && !dnsName.equals("*")) {
            if ((starIndex > 0) || ((starIndex == 0) &&
                    ((dnsName.indexOf("*", 1) != -1) ||
                            (dnsName.charAt(1) != '.'))))
            {
                String args[] = { DNS_NAME, dnsName };
                throw new PolicyException(ResBundleUtils.rbName,
                        "invalid_property_value", args, null);
            }
        }
    }

    /**
     *  Checks of the ip falls in the valid range between
     * start and end IP addresses.
     * @see #START_IP
     * @see #END_IP
     */

    private boolean isAllowedByIp(String ip) throws PolicyException {
        boolean allowed = false;

        IPv6AddressRange iPv6AddressRange = IPv6AddressRange.fromFirstAndLast(startIP,endIP);
        IPv6Address requestIP = IPv6Address.fromString(ip);

        if(iPv6AddressRange.contains(requestIP)){
            allowed = true;
        }
        return  allowed;
    }

    /**
     * Checks of the provided DNs name falls in the
     * list of valid DNS names.
     * @see #DNS_NAME
     */

    private boolean isAllowedByDns(String dnsName) throws PolicyException {
        boolean allowed = false;
        dnsName = dnsName.toLowerCase();
        Iterator dnsNames = dnsList.iterator();
        while ( dnsNames.hasNext() ) {
            String dnsPattern = (String)dnsNames.next();
            if (dnsPattern.equals("*")) {
                // single '*' matches everything
                allowed = true;
                break;
            }
            int starIndex = dnsPattern.indexOf("*");
            if (starIndex != -1 ) {
                // the dnsPattern is a string like *.ccc.ccc
                String dnsWildSuffix = dnsPattern.substring(1);
                if (dnsName.endsWith(dnsWildSuffix)) {
                    allowed = true;
                    break;
                }
            }
            else if (dnsPattern.equalsIgnoreCase(dnsName)) {
                allowed = true;
                break;
            }
        }
        return allowed;
    }

}
