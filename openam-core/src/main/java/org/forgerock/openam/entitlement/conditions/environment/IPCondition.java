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
import org.json.JSONArray;
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
    public static final String IPV4 = "IPv4";
    public static final String IPV6 = "IPv6";
    private IPv6Condition iPv6ConditionInstance = null;
    private IPv4Condition iPv4ConditionInstance = null;
    private boolean ipv4 = false;
    private boolean ipv6 = false;

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

            checkIpVersion(jo);
            if (ipv4) {
                iPv4ConditionInstance.setState(state);
            } else if (ipv6) {
                iPv6ConditionInstance.setState(state);
            }
        } catch (JSONException e) {
            debug.message("IPCondition: Failed to set state", e);
        }
    }

    private void checkIpVersion(JSONObject jo) throws JSONException {
        JSONArray ipVersionJson = jo.getJSONArray(IP_VERSION);

        if (ipVersionJson.length() > 0) {
            String ipVersion = ipVersionJson.getString(0);
            if (ipVersion.equalsIgnoreCase(IPV4)) {
                ipv4 = true;
            } else if (ipVersion.equalsIgnoreCase(IPV6)) {
                ipv6 = true;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        // Determine Request IP address
        setIPVersion(env);
        if (ipv4) {
            return iPv4ConditionInstance.evaluate(realm, subject, resourceName, env);
        } else if(ipv6) {
            return iPv6ConditionInstance.evaluate(realm, subject, resourceName, env);
        } else {
            return new ConditionDecision(false, Collections.<String, Set<String>>emptyMap());
        }
    }

    /**
     * Determine whether IPv4 or IPv6.
     *
     * @param env map containing environment description
     */
    private void setIPVersion(Map<String, Set<String>> env) {
        String ipVer;
        if (env.keySet().contains(REQUEST_IP)) {
            try {
                // Get IP_Version
                ipVer = getRequestIp(env);
                if (ValidateIPaddress.isIPv6(ipVer)) {
                    ipv6 = true;
                } else {
                    ipv4 = true; // treat as IPv4
                }
            } catch (Exception e) {
                debug.error("IPCondition.setIPVersion() : Cannot set IPversion", e);
            }
        }
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

        if (ipv4) {
            return iPv4ConditionInstance.toString();
        } else if (ipv6) {
            return iPv6ConditionInstance.toString();
        } else {
            PrivilegeManager.debug.error("IPCondition.toString()");
            return super.toString();
        }
    }

    public List<String> getIpRange() {
        if (ipv4) {
            return iPv4ConditionInstance.getIpRange();
        } else if (ipv6) {
            return iPv6ConditionInstance.getIpRange();
        }
        return null;
    }

    public void setIpRange(List<String> ipRanges) throws EntitlementException {
        if (ipv4) {
            iPv4ConditionInstance.setIpRange(ipRanges);
        } else if (ipv6) {
            iPv6ConditionInstance.setIpRange(ipRanges);
        }
    }

    public List<String> getDnsName() {
        if (ipv4) {
            return iPv4ConditionInstance.getDnsName();
        } else if (ipv6) {
            return iPv6ConditionInstance.getDnsName();
        }
        return null;
    }

    public void setDnsName(List<String> dnsName) {
        if (ipv4) {
            iPv4ConditionInstance.setDnsName(dnsName);
        } else if (ipv6) {
            iPv6ConditionInstance.setDnsName(dnsName);
        }
    }

    public String getStartIp() {
        if (ipv4) {
            return iPv4ConditionInstance.getStartIp();
        } else if (ipv6) {
            return iPv6ConditionInstance.getStartIp();
        }
        return null;
    }

    public void setStartIp(String startIp) {
        if (ipv4) {
            iPv4ConditionInstance.setStartIp(startIp);
        } else if (ipv6) {
            iPv6ConditionInstance.setStartIp(startIp);
        }
    }

    public String getEndIp() {
        if (ipv4) {
            return iPv4ConditionInstance.getEndIp();
        } else if (ipv6) {
            return iPv6ConditionInstance.getEndIp();
        }
        return null;
    }

    public void setEndIp(String endIp) {
        if (ipv4) {
            iPv4ConditionInstance.setEndIp(endIp);
        } else if (ipv6) {
            iPv6ConditionInstance.setEndIp(endIp);
        }
    }
}
