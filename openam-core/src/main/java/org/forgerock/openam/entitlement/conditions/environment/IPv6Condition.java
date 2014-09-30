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
 * Copyright 2013-2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.ValidateIPaddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static com.sun.identity.entitlement.EntitlementException.*;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.*;

public class IPv6Condition extends EntitlementConditionAdaptor {

    private final Debug debug;
    private final CoreWrapper coreWrapper;

    private List<String> ipRange = new ArrayList<String>();
    private List<IPv6Address> ipList = new ArrayList<IPv6Address>();
    private List<String> dnsName = new ArrayList<String>();
    private String startIp;
    private String endIp;
    private IPv6Address startIpLong;
    private IPv6Address endIpLong;

    /**
     * Constructs a new IPv6Condition instance.
     */
    public IPv6Condition() {
        this(PrivilegeManager.debug, new CoreWrapper());
    }

    /**
     * Constructs a new IPv6Condition instance.
     *
     * @param debug A Debug instance.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    IPv6Condition(Debug debug, CoreWrapper coreWrapper) {
        this.debug = debug;
        this.coreWrapper = coreWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            ipRange.clear();
            ipList.clear();
            dnsName.clear();
            JSONObject jo = new JSONObject(state);
            setState(jo);

            setIpRanges(jo);
            setDnsNames(jo);
            setStartIp(jo);
            setEndIp(jo);

        } catch (Exception e) {
            debug.message("IPv6Condition: Failed to set state", e);
        }
    }

    private void setIpRanges(JSONObject jo) throws JSONException, EntitlementException {

        JSONArray ipRanges = jo.getJSONArray(IP_RANGE);
        List<String> ipRange = new ArrayList<String>();
        for (int i = 0; i < ipRanges.length(); i++) {
            ipRange.add(ipRanges.getString(i));
        }

        setIpRange(ipRange);
    }

    private void setDnsNames(JSONObject jo) throws JSONException {
        JSONArray dnsNames = jo.getJSONArray(DNS_NAME);
        for (int i = 0; i < dnsNames.length(); i++) {
            String dnsName = dnsNames.getString(i);
            this.dnsName.add(dnsName.toLowerCase());
        }
    }

    private void setStartIp(JSONObject jo) throws JSONException, EntitlementException {
        startIp = jo.getString(START_IP);
        startIpLong = IPv6Address.fromString(startIp);
    }

    private void setEndIp(JSONObject jo) throws JSONException, EntitlementException {
        endIp = jo.getString(END_IP);
        endIpLong = IPv6Address.fromString(endIp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return toString();
    }

    private void validateProperties() throws EntitlementException {

        //Check if the required key(s) are defined
        if (!ipList.isEmpty() && !dnsName.isEmpty()) {
            if (debug.errorEnabled()) {
                debug.error("IPv6Condition.validateProperties(): at least one time property MUST be defined, " +
                IP_RANGE + ", " + DNS_NAME + ", " + START_IP);
            }
            throw new EntitlementException(AT_LEAST_ONE_OF_TIME_PROPS_SHOULD_BE_DEFINED,
                    new String[]{IP_RANGE + "," + DNS_NAME + "," + START_IP});
        }

        if (startIpLong.compareTo(endIpLong) > 0) {
            debug.error("IPv6Condition.validateProperties(): END IP is before START IP");
            throw new EntitlementException(END_IP_BEFORE_START_IP);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        validateProperties();

        String ip = IPCondition.getRequestIp(env);
        if(ValidateIPaddress.isIPv4(ip)){
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

        if ( (ip != null) && isAllowedByIp(ip) ) {
            allowed = true;
        } else if ((reqDnsNames != null) && (!reqDnsNames.isEmpty())) {
            for (String dnsName : reqDnsNames) {
                if (isAllowedByDns(dnsName)) {
                    allowed = true;
                    break;
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("At IPv6Condition.getConditionDecision():requestIp,  requestDnsName, allowed = " + ip + ", "
                    + reqDnsNames + "," + allowed);
        }
        return new ConditionDecision(allowed, Collections.<String, Set<String>>emptyMap());
    }

    private <T> T getValue(Set<T> values) {
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

        IPv6AddressRange iPv6AddressRange = IPv6AddressRange.fromFirstAndLast(startIpLong, endIpLong);
        IPv6Address requestIP = IPv6Address.fromString(ip);

        if(iPv6AddressRange.contains(requestIP)){
            allowed = true;
        }
        return  allowed;
    }

    /**
     * Checks of the provided DNs name falls in the list of valid DNS names.
     *
     * @see ConditionConstants#DNS_NAME
     */
    private boolean isAllowedByDns(String dnsName) throws EntitlementException {
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
        jo.put(START_IP, startIp);
        jo.put(END_IP, endIp);
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
            PrivilegeManager.debug.error("IPv6Condition.toString()", e);
        }
        return s;
    }

    public List<String> getIpRange() {
        return ipRange;
    }

    public void setIpRange(List<String> ipRanges) throws EntitlementException {
        for (String ipRange : ipRanges) {
            StringTokenizer st = new StringTokenizer(ipRange, "-");
            int tokenCount = st.countTokens();
            if (tokenCount > 2) {
                throw new EntitlementException(INVALID_PROPERTY_VALUE, new String[]{IP_RANGE, ipRange});
            }

            String startIp = st.nextToken();
            String endIp = startIp;
            if (tokenCount == 2) {
                endIp = st.nextToken();
            }
            this.ipRange.add(startIp);
            this.ipRange.add(endIp);
            ipList.add(IPv6Address.fromString(startIp));
            ipList.add(IPv6Address.fromString(endIp));
        }
    }

    public List<String> getDnsName() {
        return dnsName;
    }

    public void setDnsName(List<String> dnsName) {
        this.dnsName = dnsName;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }
}
