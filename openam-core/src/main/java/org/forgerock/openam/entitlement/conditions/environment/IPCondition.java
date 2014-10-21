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
 * Copyright 2006 Sun Microsystems Inc
 */
/*
 * Portions Copyright 2011-2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.ValidateIPaddress;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_IP;

public class IPCondition extends EntitlementConditionAdaptor {

    private static final Debug debug = PrivilegeManager.debug;
    private final CoreWrapper coreWrapper;

    public static final String IP_VERSION = "ipVersion";
    private IPv6Condition iPv6ConditionInstance = null;
    private IPv4Condition iPv4ConditionInstance = null;
    private IPVersion ipVersion = IPVersion.IPV4;
    private String endIp;
    private String startIp;
    private List<String> dnsName;
    private List<String> ipRanges;

    /**
     * Constructs a new IPCondition instance.
     */
    public IPCondition() {
        this(new CoreWrapper());
    }

    /**
     * Constructs a new IPCondition instance.
     *
     * @param coreWrapper An instance of the CoreWrapper.
     */
    IPCondition(CoreWrapper coreWrapper) {
        this.coreWrapper = coreWrapper;
        this.iPv4ConditionInstance = new IPv4Condition();
        this.iPv6ConditionInstance = new IPv6Condition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);

            String ipVersion = jo.optString(IP_VERSION);
            if (ipVersion.isEmpty()) {
                this.ipVersion = IPVersion.IPV4;
            } else {
                this.ipVersion = IPVersion.valueOf(ipVersion.toUpperCase());
            }
            if (this.ipVersion == IPVersion.IPV4) {
                iPv4ConditionInstance.setState(state);
            } else {
                iPv6ConditionInstance.setState(state);
            }
        } catch (JSONException e) {
            debug.message("IPCondition: Failed to set state", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        // Determine Request IP address
        IPVersion requestVersion = getRequestedIPVersion(env);
        if (requestVersion != this.ipVersion) {
            return new ConditionDecision(false, Collections.<String, Set<String>>emptyMap());
        } else if (this.ipVersion == IPVersion.IPV4) {
            return iPv4ConditionInstance.evaluate(realm, subject, resourceName, env);
        } else {
            return iPv6ConditionInstance.evaluate(realm, subject, resourceName, env);
        }
    }

    /**
     * Determine whether IPv4 or IPv6.
     *
     * @param env map containing environment description
     */
    private IPVersion getRequestedIPVersion(Map<String, Set<String>> env) {
        IPVersion version = null;
        if (env.keySet().contains(REQUEST_IP)) {
            // Get IP_Version
            if (ValidateIPaddress.isIPv6(getRequestIp(env))) {
                version = IPVersion.IPV6;
            } else {
                version = IPVersion.IPV4;
            }
        }
        return version;
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
    public static String getRequestIp(Map<String, Set<String>> env) {
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (this.ipVersion == IPVersion.IPV4) {
            return iPv4ConditionInstance.toString();
        } else {
            return iPv6ConditionInstance.toString();
        }
    }

    public List<String> getIpRange() {
        if (this.ipVersion == IPVersion.IPV4) {
            return iPv4ConditionInstance.getIpRange();
        } else {
            return iPv6ConditionInstance.getIpRange();
        }
    }

    public void setIpRange(List<String> ipRanges) throws EntitlementException {
        this.ipRanges = ipRanges;
        if (ipRanges.size() > 0) {
            String firstIP = ipRanges.get(0).split("-")[0];
            if (ValidateIPaddress.isIPv4(firstIP)) {
                iPv4ConditionInstance.setIpRange(ipRanges);
            } else if (this.ipVersion == IPVersion.IPV6) {
                iPv6ConditionInstance.setIpRange(ipRanges);
            }
        } else {
            iPv4ConditionInstance.setIpRange(ipRanges);
            iPv6ConditionInstance.setIpRange(ipRanges);
        }
    }

    public List<String> getDnsName() {
        if (this.ipVersion == IPVersion.IPV4) {
            return iPv4ConditionInstance.getDnsName();
        } else {
            return iPv6ConditionInstance.getDnsName();
        }
    }

    public void setDnsName(List<String> dnsName) {
        this.dnsName = dnsName;
        iPv4ConditionInstance.setDnsName(dnsName);
        iPv6ConditionInstance.setDnsName(dnsName);
    }

    public String getStartIp() {
        if (this.ipVersion == IPVersion.IPV4) {
            return iPv4ConditionInstance.getStartIp();
        } else {
            return iPv6ConditionInstance.getStartIp();
        }
    }

    public void setStartIp(String startIp) throws EntitlementException {
        this.startIp = startIp;
        if (ValidateIPaddress.isIPv4(startIp)) {
            iPv4ConditionInstance.setStartIp(startIp);
        } else {
            iPv6ConditionInstance.setStartIp(startIp);
        }
    }

    public String getEndIp() {
        if (this.ipVersion == IPVersion.IPV4) {
            return iPv4ConditionInstance.getEndIp();
        } else {
            return iPv6ConditionInstance.getEndIp();
        }
    }

    public void setEndIp(String endIp) throws EntitlementException {
        this.endIp = endIp;
        if (ValidateIPaddress.isIPv4(endIp)) {
            iPv4ConditionInstance.setEndIp(endIp);
        } else {
            iPv6ConditionInstance.setEndIp(endIp);
        }
    }

    public String getIpVersion() {
        return ipVersion.toString();
    }

    public void setIpVersion(String ipVersion) throws EntitlementException {
        if (ipVersion != null && !ipVersion.isEmpty()) {
            this.ipVersion = IPVersion.valueOf(ipVersion.toUpperCase());
        } else {
            this.ipVersion = IPVersion.IPV4;
        }
        setStartIp(startIp);
        setEndIp(endIp);
        setDnsName(dnsName == null ? Collections.<String>emptyList() : dnsName);
        setIpRange(ipRanges == null ? Collections.<String>emptyList() : ipRanges);
    }
}
