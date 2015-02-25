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
import static com.sun.identity.entitlement.EntitlementException.CONDITION_EVALUTATION_FAILED;
import static com.sun.identity.entitlement.EntitlementException.END_IP_BEFORE_START_IP;
import static com.sun.identity.entitlement.EntitlementException.INVALID_PROPERTY_VALUE;
import static com.sun.identity.entitlement.EntitlementException.IP_CONDITION_CONFIGURATION_REQUIRED;
import static com.sun.identity.entitlement.EntitlementException.PAIR_PROPERTY_NOT_DEFINED;
import com.sun.identity.shared.debug.Debug;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.Subject;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.DNS_NAME;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.END_IP;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.IP_RANGE;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_DNS_NAME;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_IP;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.START_IP;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Abstract Base Class for {@link IPv4Condition} and {@link IPv6Condition}.
 *
 * @param <T> The type used for IP address values.
 */
abstract class IPvXCondition<T extends Comparable<T>> extends EntitlementConditionAdaptor {

    protected final Debug debug;

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
    protected IPvXCondition(Debug debug, T initialStartIp, T initialEndIp, IPVersion version) {
        this.debug = debug;
        this.startIp = initialStartIp;
        this.initialStartIp = initialStartIp;
        this.initialEndIp = initialEndIp;
        this.endIp = initialEndIp;
        this.version = version;
    }

    /**
     * Constructs a new IPvXCondition instance.
     */
    protected IPvXCondition(Debug debug, T initialStartIp, T initialEndIp, IPVersion version,
                            String startIp, String endIp, List<String> ipRange, List<String> dnsName)
            throws EntitlementException {

        this(debug, initialStartIp, initialEndIp, version);

        if (startIp != null || endIp != null) {
            setStartIpAndEndIp(startIp, endIp);
        }
        if (ipRange != null) {
            setIpRange(ipRange);
        }
        if (dnsName != null) {
            setDnsName(dnsName);
        }

        validate();
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
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {

            JSONObject jo = new JSONObject(state);
            setState(jo);
            setIpRangesFromJson(jo);
            setDnsNamesFromJson(jo);
            setStartIpAndEndIpFromJson(jo);
            validate();

        } catch (Exception e) {
            debugMessage(e, "Failed to set state");
        }
    }

    private void setStartIpAndEndIpFromJson(JSONObject jo) throws JSONException, EntitlementException {
        String ipStart = jo.has(START_IP) ? jo.getString(START_IP) : null;
        String ipEnd = jo.has(END_IP) ? jo.getString(END_IP) : null;
        setStartIpAndEndIp(ipStart, ipEnd);
    }

    private void setIpRangesFromJson(JSONObject jo) throws JSONException, EntitlementException {

        List<String> ipRange = new ArrayList<String>();

        if (jo.has(IP_RANGE)) {
            JSONArray ipRanges = jo.getJSONArray(IP_RANGE);
            for (int i = 0; i < ipRanges.length(); i++) {
                ipRange.add(ipRanges.getString(i));
            }
        }

        setIpRange(ipRange);
    }

    private void setDnsNamesFromJson(JSONObject jo) throws JSONException, EntitlementException {

        List<String> dnsName = new ArrayList<String>();

        if (jo.has(DNS_NAME)) {
            JSONArray dnsNames = jo.getJSONArray(DNS_NAME);
            for (int i = 0; i < dnsNames.length(); i++) {
                dnsName.add(dnsNames.getString(i).toLowerCase());
            }
        }

        setDnsName(dnsName);
    }

    public String getStartIp() {
        return startIpString;
    }

    public String getEndIp() {
        return endIpString;
    }

    public void setStartIpAndEndIp(String startIp, String endIp) throws EntitlementException {

        T startIpValue = startIp == null ? initialStartIp : stringToIp(startIp);
        T endIpValue = endIp == null ? initialEndIp : stringToIp(endIp);

        if (isDefinedStartIp(startIpValue) && isDefinedEndIp(endIpValue)) {

            if (startIp.compareTo(endIp) > 0) {
                debugWarning("Validation: {0} is before {1}", END_IP, START_IP);
                throw new EntitlementException(END_IP_BEFORE_START_IP);
            }

        } else {

            if (isDefinedStartIp(startIpValue)) {
                debugWarning("Validation: Should define value for {1}, as value is defined for {0}", START_IP, END_IP);
                throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, new String[]{START_IP, END_IP});
            }
            if (isDefinedEndIp(endIpValue)) {
                debugWarning("Validation: Should define value for {1}, as value is defined for {0}", END_IP, START_IP);
                throw new EntitlementException(PAIR_PROPERTY_NOT_DEFINED, new String[]{END_IP, START_IP});
            }
        }

        this.startIpString = startIp;
        this.startIp = startIpValue;
        this.endIpString = endIp;
        this.endIp = endIpValue;
    }

    public List<String> getDnsName() {
        return dnsName;
    }

    public void setDnsName(List<String> dnsName) throws EntitlementException {
        if (dnsName != null) {
            for (String dnsNameEntry : dnsName) {
                if (!isValidDnsName(dnsNameEntry)) {
                    throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{DNS_NAME, dnsNameEntry});
                }
            }
        }
        this.dnsName = dnsName;
    }

    @Deprecated
    public List<String> getIpRange() {
        return ipRange;
    }

    @Deprecated
    public void setIpRange(List<String> ipRanges) throws EntitlementException {
        ipRange.clear();
        ipList.clear();
        if (ipRanges != null) {
            for (String ipRange : ipRanges) {
                StringTokenizer st = new StringTokenizer(ipRange, "-");
                int tokenCount = st.countTokens();
                if (tokenCount == 0) {
                    return;
                }
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

    @Override
    public void validate() throws EntitlementException {

        if (ipList.isEmpty() && dnsName.isEmpty() && (!isDefinedStartIp(startIp) || !isDefinedEndIp(endIp))) {
            debugWarning("Validation: ipRange, dnsName, or startIp-endIp pair MUST be defined");
            throw new EntitlementException(IP_CONDITION_CONFIGURATION_REQUIRED,
                    new String[]{IP_RANGE, DNS_NAME, START_IP, END_IP});
        }

    }

    /**
     * Return true if the provided IP value is neither null nor equal to the {@link #initialStartIp} value.
     */
    private boolean isDefinedStartIp(T ip) {
        if (ip == null) {
            return false;
        } else if (initialStartIp == null) {
            return true;
        } else {
            return !initialStartIp.equals(ip);
        }
    }

    /**
     * Return true if the provided IP value is neither null nor equal to the {@link #initialEndIp} value.
     */
    private boolean isDefinedEndIp(T ip) {
        if (ip == null) {
            return false;
        } else if (initialEndIp == null) {
            return true;
        } else {
            return !initialEndIp.equals(ip);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        boolean allowed = false;

        // If DNS matches we can ignore incorrect IP version - So, check DNS first

        Set<String> reqDnsNames = env.get(REQUEST_DNS_NAME);
        if (reqDnsNames != null) {
            for (String dnsName : reqDnsNames) {
                if (isAllowedByDns(dnsName)) {
                    allowed = true;
                    break;
                }
            }
        }

        // If DNS didn't match, then we'll need to check IP

        String ip = null;
        if (!allowed) {
            ip = getRequestIp(env);
            if (ip == null) {
                debugMessage("ConditionDecision: IP not provided in request, using session IP");
                ip = getSessionIp(subject);
            }
            if (ip != null && isAllowedByIp(ip)) {
                allowed = true;
            }
        }

        // Log and return result

        String ipDesc = ip != null ? ip : "not checked";
        debugMessage("ConditionDecision: requestIp={0}, requestDnsName={1}, allowed={2}", ipDesc, reqDnsNames, allowed);
        return new ConditionDecision(allowed, Collections.<String, Set<String>>emptyMap());
    }

    /**
     * Helper method to extract {@code REQUEST_IP}.
     *
     * @param env The map containing environment description. Note that the type of the value corresponding to
     *            {@code REQUEST_IP} parameter differs depending upon invocation path. It will be a {@code String} when
     *            invoked by the agents, but it will be a {@code Set<String>} when invoked via the DecisionResource
     *            (GET ws/1/entitlement/entitlements).
     * @return The IP that was used, can return null if no IP found.
     */
    @SuppressWarnings("unchecked")
    public String getRequestIp(Map env) {
        String ip = null;
        final Object requestIp = env.get(REQUEST_IP);

        if (requestIp instanceof Set) {
            Set<String> requestIpSet =  (Set<String>) requestIp;
            if (!requestIpSet.isEmpty()) {
                if (requestIpSet.size() > 1) {
                    debugWarning("Environment map {0} cardinality > 1. Using first from: {1}",
                            REQUEST_IP, requestIpSet);
                }
                ip = requestIpSet.iterator().next();
            }
        } else if (requestIp instanceof String) {
            ip = (String) requestIp;
        }

        if (StringUtils.isBlank(ip)) {
            debugWarning("Environment map {0} is null or empty", REQUEST_IP);
        }
        return ip;
    }

    /**
     * Helper method to retrieve IP from subject's {@link SSOToken}.
     *
     * @param subject Subject who is under evaluation.
     * @return IP address from subject's {@link SSOToken} or null if no SSOToken is found.
     * @throws EntitlementException If any exception occurs when accessing the subject's {@link SSOToken}.
     */
    public String getSessionIp(Subject subject) throws EntitlementException {

        SSOToken token = (SSOToken) CollectionUtils.getFirstItem(subject.getPrivateCredentials(), null);
        if (token != null) {
            try {
                InetAddress ipAddress = token.getIPAddress();
                if (ipAddress != null) {
                    return ipAddress.getHostAddress();
                }
            } catch (SSOException e) {
                throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
            }
        }
        return null;
    }

    /**
     * Checks if the IP falls in the valid range between start and end IP addresses.
     *
     * @see ConditionConstants#START_IP
     * @see ConditionConstants#END_IP
     * @see ConditionConstants#IP_RANGE
     */
    private boolean isAllowedByIp(String ip) throws EntitlementException {

        T requestIp;
        try {
            requestIp = stringToIp(ip);
        } catch (EntitlementException ex) {
            return false;
        }

        Iterator<T> ipValues = ipList.iterator();
        while (ipValues.hasNext()) {
            T startIp = ipValues.next();
            if (ipValues.hasNext()) {
                T endIp = ipValues.next();
                if (requestIp.compareTo(startIp) >= 0 && requestIp.compareTo(endIp) <= 0) {
                    return true;
                }
            }
        }

        if (isDefinedStartIp(startIp) && isDefinedEndIp(endIp)) {
            return (requestIp.compareTo(startIp) >= 0 && requestIp.compareTo(endIp) <= 0);
        }

        return false;
    }

    /**
     * Checks of the provided DNS name falls in the list of valid DNS names.
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
            debugError(e, "toString()");
        }
        return s;
    }

    /**
     * Validates a DNS name for format
     */
    private boolean isValidDnsName(String dnsName) {
        int starIndex = dnsName.indexOf("*");

        if ((starIndex >= 0) && !dnsName.equals("*")) {

            if ((starIndex > 0) || (dnsName.indexOf("*", 1) != -1)) {
                // star wildcard can only appear as first character
                return false;

            } else if (dnsName.charAt(1) != '.') {
                // and, if star wildcard is used, it must be immediately followed by '.'
                return false;
            }
        }

        return true;
    }

    private void debugMessage(String format, Object... args) {
        debugMessage(null, format, args);
    }

    private void debugMessage(Exception e, String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(formattedWithHeader(format, args), e);
        }
    }

    private void debugWarning(String format, Object... args) {
        if (debug.warningEnabled()) {
            debug.warning(formattedWithHeader(format, args));
        }
    }

    private void debugError(String format, Object... args) {
        debugError(null, format, args);
    }

    private void debugError(Exception e, String format, Object... args) {
        if (debug.errorEnabled()) {
            debug.error(formattedWithHeader(format, args), e);
        }
    }

    private String formattedWithHeader(String format, Object... args) {
        return getClass().getSimpleName() + ": " + MessageFormat.format(format, args);
    }

}
