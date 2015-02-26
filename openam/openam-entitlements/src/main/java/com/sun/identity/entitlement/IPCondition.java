/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IPCondition.java,v 1.3 2009/09/05 00:24:04 veiming Exp $
 */

/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

package com.sun.identity.entitlement;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.Subject;

import org.forgerock.openam.utils.ValidateIPaddress;
import org.json.JSONObject;
import org.json.JSONException;

import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;

/**
 * Entitlement Condition to represent IP constraint
  */
public class IPCondition extends EntitlementConditionAdaptor {
    /** Key that is used to define request IP address that is passed in
     * the <code>env</code> parameter while invoking
     * <code>getConditionDecision</code> method of an <code>IPCondition</code>.
     * Value for the key should be a <code>String</code> that is a string
     * representation of IP of the client, in the form n.n.n.n where n is a
     * value between 0 and 255 inclusive.
     *
     * @see com.sun.identity.policy.interfaces.Condition#getConditionDecision(com.iplanet.sso.SSOToken, java.util.Map)
     * @see DNSNameCondition#REQUEST_DNS_NAME
     */
    public static final String REQUEST_IP = "requestIp";

    private String startIp;
    private String endIp;
    private String pConditionName;

    /**
     * Constructs an IPCondition
     */
    public IPCondition() {
    }

    /**
     * Constructs IPCondition object:w
     * @param startIp starting ip of a range for example 121.122.123.124
     * @param endIp ending ip of a range, for example 221.222.223.224
     */
    public IPCondition(String startIp, String endIp) {
        this.startIp = startIp;
        this.endIp = endIp;
    }

    /**
     * Returns state of the object
     * @return state of the object encoded as string
     */
    public String getState() {
        return toString();
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            startIp = jo.optString("startIp");
            endIp = jo.optString("endIp");
            pConditionName = jo.optString("pConditionName");
        } catch (JSONException e) {
            PrivilegeManager.debug.error("IPCondition.setState", e);
        }
    }

    /**
     * Returns <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     *
     * @param realm Realm Name.
     * @param subject EntitlementCondition who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     * @throws EntitlementException if any errors occur.
     */
    public ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        String ip = null;
        Object ipObject = environment.get(REQUEST_IP);

        // code changed to fix issue 5440
        // IPCondition evaluation is breaking
        if (ipObject != null) {
            if (ipObject instanceof String) {
                ip = (String)ipObject;
            } else if (ipObject instanceof Set) {
                Set<String> setIP= (Set<String>)ipObject;
                ip =  !setIP.isEmpty() ?
                    setIP.iterator().next() : null;
            }
        }

        if ((ip != null) && isAllowedByIp(ip)) {
            return new ConditionDecision(true, Collections.EMPTY_MAP);
        }

        Set<String> set = new HashSet<String>();
        set.add(REQUEST_IP + "=" + startIp + "-" + endIp);
        Map<String, Set<String>> advice = new HashMap<String, Set<String>>();
        advice.put(getClass().getName(), set);
        return new ConditionDecision(false, advice);
    }

    private boolean isAllowedByIp(String ip) throws EntitlementException {
        String args[] = { "ip", ip };
        if(ValidateIPaddress.isIPv4(ip) &&
                ValidateIPaddress.isIPv4(startIp) && ValidateIPaddress.isIPv4(endIp)) {
            try{
                long requestIp = stringToIp(ip);
                long startIpNum = stringToIp(startIp);
                long endIpNum = stringToIp(endIp);
                return ((requestIp >= startIpNum) && (requestIp <= endIpNum));
            } catch (Exception e){
                throw new EntitlementException(400, args);
            }
        } else if(ValidateIPaddress.isIPv6(ip) &&
                ValidateIPaddress.isIPv6(startIp) && ValidateIPaddress.isIPv6(endIp)) {
            try{
                IPv6AddressRange ipv6Range = IPv6AddressRange.fromFirstAndLast(
                        IPv6Address.fromString(startIp),IPv6Address.fromString(endIp));
                return ipv6Range.contains(IPv6Address.fromString(ip));
            } catch (Exception e){
                throw new EntitlementException(400, args);
            }
        } else {
            PrivilegeManager.debug.error("IP address invalid" + ip);
            throw new EntitlementException(400, args);
        }
    }

    private long stringToIp(String ip) throws EntitlementException {
        StringTokenizer st = new StringTokenizer(ip, ".");
        int tokenCount = st.countTokens();
        if ( tokenCount != 4 ) {
            String args[] = { "ip", ip };
            throw new EntitlementException(400, args);
        }
        long ipValue = 0L;
        while ( st.hasMoreElements()) {
            String s = st.nextToken();
            short ipElement = 0;
            try {
                ipElement = Short.parseShort(s);
            } catch(Exception e) {
                String args[] = { "ip", ip };
                throw new EntitlementException(400, args);
            }
            if ( ipElement < 0 || ipElement > 255 ) {
                String args[] = { "ipElement", s };
                throw new EntitlementException(400, args);
            }
            ipValue = ipValue * 256L + ipElement;
        }
        return ipValue;
    }

    /**
     * Return start IP.
     *
     * @return the start IP.
     */
    public String getStartIp() {
        return startIp;
    }

    /**
     * Set start IP.
     *
     * @param startIp Start IP.
     */
    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    /**
     * Returns end IP.
     *
     * @return the end IP.
     */
    public String getEndIp() {
        return endIp;
    }

    /**
     * Sets end IP.
     *
     * @param endIp the end IP.
     */
    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    /**
     * Returns OpenSSO policy subject name of the object
     * @return subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    public String getPConditionName() {
        return pConditionName;
    }

    /**
     * Sets OpenSSO policy subject name of the object
     * @param pConditionName subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    public void setPConditionName(String pConditionName) {
        this.pConditionName = pConditionName;
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping  of the object
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("startIp", startIp);
        jo.put("endIp", endIp);
        jo.put("pConditionName", pConditionName);
        return jo;
    }

    /**
     * Returns <code>true</code> if the passed in object is equal to this object
     * @param obj object to check for equality
     * @return  <code>true</code> if the passed in object is equal to this object
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        IPCondition object = (IPCondition) obj;
        if (getStartIp() == null) {
            if (object.getStartIp() != null) {
                return false;
            }
        } else {
            if (!startIp.equals(object.getStartIp())) {
                return false;
            }
        }
        if (getEndIp() == null) {
            if (object.getEndIp() != null) {
                return false;
            }
        } else {
            if (!endIp.equals(object.getEndIp())) {
                return false;
            }
        }
        if (getPConditionName() == null) {
            if (object.getPConditionName() != null) {
                return false;
            }
        } else {
            if (!pConditionName.equals(object.getPConditionName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns hash code of the object
     * @return hash code of the object
     */
    @Override
    public int hashCode() {
        int code = super.hashCode();
        
        if (startIp != null) {
            code += startIp.hashCode();
        }
        if (endIp != null) {
            code += endIp.hashCode();
        }
        if (pConditionName != null) {
            code += pConditionName.hashCode();
        }
        return code;
    }

    /**
     * Returns string representation of the object
     * @return string representation of the object
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("IPCondiiton.toString", e);
        }
        return s;
    }
}
