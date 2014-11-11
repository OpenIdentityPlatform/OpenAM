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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static com.sun.identity.entitlement.EntitlementException.*;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

/**
 * Abstract Base Class for {@link IPv4Condition} and {@link IPv6Condition}.
 *
 * @param <T> The type used for IP address values.
 */
abstract class IPvXCondition<T extends Comparable<T>> extends EntitlementConditionAdaptor {

    protected final Debug debug;
    private final CoreWrapper coreWrapper;

    private List<String> ipRange = new ArrayList<String>();
    private List<T> ipList = new ArrayList<T>();
    private List<String> dnsName = new ArrayList<String>();
    private String startIpString;
    private String endIpString;
    private final T initialStartIp;
    private final T initialEndIp;
    private T startIp;
    private T endIp;
    private final IPVersion version;

    /**
     * Constructs a new IPvXCondition instance.
     */
    protected IPvXCondition(Debug debug, CoreWrapper coreWrapper, T initialStartIp, T initialEndIp, IPVersion version) {
        this.debug = debug;
        this.coreWrapper = coreWrapper;
        this.startIp = initialStartIp;
        this.initialStartIp = initialStartIp;
        this.initialEndIp = initialEndIp;
        this.endIp = initialEndIp;
        this.version = version;
    }

    /**
     * Factory method for constructing an IP value from its String representation.
     *
     * @param ip A String representation of an IP value.
     * @return An IP value.
     * @throws EntitlementException If argument is not a string representing an IP value understood by this object.
     */
    protected abstract T stringToIp(String ip) throws EntitlementException;

    /**
     * Checks if string represents an IP value understood by this object.
     *
     * @param ip A String representation of an IP value.
     * @return <code>True</code> If argument is not a string representing an IP value understood by this object.
     */
    protected abstract boolean validateIpAddress(String ip);

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);

            setIpRangesFromJson(jo);
            setDnsNamesFromJson(jo);
            setStartIpFromJson(jo);
            setEndIpFromJson(jo);
        } catch (Exception e) {
            debug.message(getClass().getSimpleName() + ": Failed to set state", e);
        }
    }

    private void setStartIpFromJson(JSONObject jo) throws JSONException, EntitlementException {
        setStartIp(jo.getString(START_IP));
    }

    private void setEndIpFromJson(JSONObject jo) throws JSONException, EntitlementException {
        if (!isStartIpSet()) {
            throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, new String[]{END_IP, START_IP});
        }
        setEndIp(jo.getString(END_IP));
    }

    private void setIpRangesFromJson(JSONObject jo) throws JSONException, EntitlementException {

        JSONArray ipRanges = jo.getJSONArray(IP_RANGE);
        List<String> ipRange = new ArrayList<String>();
        for (int i = 0; i < ipRanges.length(); i++) {
            ipRange.add(ipRanges.getString(i));
        }

        setIpRange(ipRange);
    }

    private void setDnsNamesFromJson(JSONObject jo) throws JSONException {
        dnsName.clear();
        JSONArray dnsNames = jo.getJSONArray(DNS_NAME);
        for (int i = 0; i < dnsNames.length(); i++) {
            String dnsName = dnsNames.getString(i);
            this.dnsName.add(dnsName.toLowerCase());
        }
    }

    public String getStartIp() {
        return startIpString;
    }

    public void setStartIp(String startIp) throws EntitlementException {
        this.startIpString = startIp;
        this.startIp = startIp == null ? initialStartIp : stringToIp(startIp);
    }

    public String getEndIp() {
        return endIpString;
    }

    public void setEndIp(String endIp) throws EntitlementException {
        this.endIpString = endIp;
        this.endIp = endIp == null ? initialEndIp : stringToIp(endIp);
    }

    public List<String> getDnsName() {
        return dnsName;
    }

    public void setDnsName(List<String> dnsName) {
        this.dnsName = dnsName;
    }

    public List<String> getIpRange() {
        return ipRange;
    }

    public void setIpRange(List<String> ipRanges) throws EntitlementException {
        ipRange.clear();
        ipList.clear();
        if (ipRanges != null) {
            for (String ipRange : ipRanges) {
                StringTokenizer st = new StringTokenizer(ipRange, "-");
                int tokenCount = st.countTokens();
                if (tokenCount > 2) {
                    throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{IP_RANGE, ipRange});
                }

                String startIp = st.nextToken();
                this.ipRange.add(startIp);
                ipList.add(stringToIp(startIp));
                if (tokenCount == 2) {
                    String endIp = st.nextToken();
                    this.ipRange.add(endIp);
                    ipList.add(stringToIp(endIp));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return toString();
    }

    private void validateProperties() throws EntitlementException {

        if (ipList.isEmpty() && dnsName.isEmpty() && (!isStartIpSet() || !isEndIpSet())) {
            if (debug.errorEnabled()) {
                debug.error(getClass().getSimpleName() + ".validateProperties(): " +
                        "at least one IP range or DNS name MUST be defined");
            }
            throw new EntitlementException(IP_RANGE_OR_DNS_NAME_REQUIRED);
        }

        if (startIp.compareTo(endIp) > 0) {
            if (debug.errorEnabled()) {
                debug.error(getClass().getSimpleName() + ".validateProperties(): END IP is before START IP");
            }
            throw new EntitlementException(END_IP_BEFORE_START_IP);
        }
    }

    private boolean isStartIpSet() {
        return startIp != initialStartIp;
    }

    private boolean isEndIpSet() {
        return endIp != initialEndIp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        validateProperties();

        String ip = getRequestIp(env);
        if (!validateIpAddress(ip)) {
            return new ConditionDecision(false, Collections.<String, Set<String>>emptyMap());
        }

        boolean allowed = false;
        SSOToken token = (SSOToken) getValue(subject.getPrivateCredentials());
        if (ip == null) {
            if (token != null) {
                try {
                    ip = token.getIPAddress().getHostAddress();
                } catch (SSOException e) {
                    throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
                }
            }
        }
        Set<String> reqDnsNames = env.get(REQUEST_DNS_NAME);

        if (ip != null && isAllowedByIp(ip)) {
            allowed = true;
        } else if (reqDnsNames != null && !reqDnsNames.isEmpty()) {
            for (String dnsName : reqDnsNames) {
                if (isAllowedByDns(dnsName)) {
                    allowed = true;
                    break;
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message(getClass().getSimpleName() + ".getConditionDecision(): requestIp, requestDnsName, " +
                    "allowed = " + ip + ", " + reqDnsNames + "," + allowed );
        }
        return new ConditionDecision(allowed, Collections.<String, Set<String>>emptyMap());
    }

    /**
     * Helper method to extract {@code REQUEST_IP}.
     *
     * @param env The map containing environment description. Note that the type of the value corresponding to
     *            {@code REQUEST_IP} parameter differs depending upon invocation path. It will be a {@code String} when
     *            invoked by the agents, but it will be a {@code Set<String>} when invoked via the DecisionResource
     *            (GET ws/1/entitlement/entitlements).
     * @return The IP that was used.
     */
    public String getRequestIp(Map<String, Set<String>> env) {
        Set<String> requestIpSet = env.get(REQUEST_IP);
        if (requestIpSet != null && !requestIpSet.isEmpty()) {
            if (requestIpSet.size() > 1) {
                debug.warning("Set cardinality in environment map corresponding to " + REQUEST_IP +
                        " key >1. Returning first value. The set: " + requestIpSet);
            }
            Object ip = requestIpSet.iterator().next();
            if (ip != null) { // Set implementations can permit null values
                return (String) ip;
            } else {
                debug.warning("In IPCondition, no value in Set corresponding to " + REQUEST_IP
                        + " key contained environment map.");
                return null;
            }
        } else {
            debug.warning("In IPCondition, Set corresponding to " + REQUEST_IP
                    + " key in environment map is null or empty.");
            return null;
        }
    }

    /**
     * Return first value from Set or null if Set is empty.
     */
    private <V> V getValue(Set<V> values) {
        if (values != null && values.iterator().hasNext()) {
            return values.iterator().next();
        }
        return null;
    }

    /**
     * Checks of the ip falls in the valid range between start and end IP addresses.
     *
     * @see ConditionConstants#START_IP
     * @see ConditionConstants#END_IP
     */
    private boolean isAllowedByIp(String ip) throws EntitlementException {
        boolean allowed = false;
        T requestIp = stringToIp(ip);
        Iterator<T> ipValues = ipList.iterator();
        while ( ipValues.hasNext() ) {
            T startIp = ipValues.next();
            if ( ipValues.hasNext() ) {
                T endIp = ipValues.next();
                if (requestIp.compareTo(startIp) >= 0 && requestIp.compareTo(endIp) <= 0) {
                    allowed = true;
                    break;
                }
            }
        }
        if (requestIp.compareTo(startIp) >= 0 && requestIp.compareTo(endIp) <= 0) {
            allowed = true;
        }
        return allowed;
    }

    /**
     * Checks of the provided DNs name falls in the list of valid DNS names.
     *
     * @see ConditionConstants#DNS_NAME
     */
    private boolean isAllowedByDns(String dnsName) {
        boolean allowed = false;
        dnsName = dnsName.toLowerCase();
        for (String dnsPattern : this.dnsName) {
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

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);

        jo.put(ConditionConstants.IP_VERSION, version.toString());
        JSONArray ipRangeJson = new JSONArray();
        for (String ip : ipRange) {
            ipRangeJson.put(ip);
        }
        jo.put(IP_RANGE, ipRangeJson);
        JSONArray dnsJson = new JSONArray();
        for (String dns : dnsName) {
            dnsJson.put(dns);
        }
        jo.put(DNS_NAME, dnsJson);
        jo.put(START_IP, startIpString);
        jo.put(END_IP, endIpString);
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
            PrivilegeManager.debug.error(getClass().getSimpleName() + ".toString()", e);
        }
        return s;
    }

    /**
     * Validates a DNS name for format
     */
    static boolean isValidDnsName(String dnsName) {
        int starIndex = dnsName.indexOf("*");
        if ((starIndex >= 0) && !dnsName.equals("*")) {
            if ((starIndex > 0) || ((starIndex == 0) && ((dnsName.indexOf("*", 1) != -1) || (dnsName.charAt(1) != '.')))) {
                return false;
            }
        }
        return true;
    }

//    /**
//     * JSON deserialization constructor used to ensure fields are set in an order
//     * that allows inter-field validation to pass.
//     *
//     * @param startIp
//     * @param endIp
//     * @param ipRange
//     * @param dnsName
//     * @throws EntitlementException
//     */
//    @JsonCreator
//    public IPCondition(@JsonProperty(START_IP) String startIp,
//                       @JsonProperty(END_IP) String endIp,
//                       @JsonProperty(IP_RANGE) List<String> ipRange,
//                       @JsonProperty(DNS_NAME) List<String> dnsName) throws EntitlementException {
//        this();
//        if (startIp != null || endIp != null) {
//            setStartIpAndEndIp(startIp, endIp);
//        }
//        if (ipRange != null) {
//            setIpRange(ipRange);
//        }
//        if (dnsName != null) {
//            setDnsName(dnsName);
//        }
//    }
}
