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
 * $Id: DNSNameCondition.java,v 1.2 2009/09/05 00:24:04 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * EntitlementCondition to represent IP, DNS name based  constraint
  */
public class DNSNameCondition extends EntitlementConditionAdaptor {
    /** Key that is used in an <code>DNSNameCondition</code> to define the DNS
     * name values for which a policy applies. The value corresponding to the
     * key has to be a <code>Set</code> with at most one element is a
     * <code>String</code> that conforms to the patterns described here.
     *
     * The patterns is :
     * <pre>
     * ccc.ccc.ccc.ccc
     * *.ccc.ccc.ccc</pre>
     * where c is any valid character for DNS domain/host name.
     * There could be any number of <code>.ccc</code> components.
     * Some sample values are:
     * <pre>
     * www.sun.com
     * finace.yahoo.com
     * *.yahoo.com
     * </pre>
     */
    public static final String DNS_NAME = "DnsName";

    /** Key that is used to define request DNS name that is passed in
     * the <code>env</code> parameter while invoking
     * <code>getConditionDecision</code> method of an 
     * <code>DNSNameCondition</code>.
     * Value for the key should be a set of strings representing the
     * DNS names of the client, in the form <code>ccc.ccc.ccc</code>.
     * If the <code>env</code> parameter is null or does not
     * define value for <code>REQUEST_DNS_NAME</code>,  the
     * value for <code>REQUEST_DNS_NAME</code> is obtained
     * from the single sign on token of the user
     *
     * @see com.sun.identity.policy.interfaces.Condition#getConditionDecision(com.iplanet.sso.SSOToken, java.util.Map)
     */
    public static final String REQUEST_DNS_NAME = "requestDnsName";

    private String domainNameMask;
    private String pConditionName;

    /**
     * Constructs an DNSNameCondition
     */
    public DNSNameCondition() {
    }

    /**
     * Constructs DNSNameCondition object
     * 
     * @param domainNameMask domain name mask, for example *.example.com,
     * only wild card allowed is *
     */
    public DNSNameCondition(String domainNameMask) {
        this.domainNameMask = domainNameMask;
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
            domainNameMask = jo.optString("domainNameMask");
            pConditionName = jo.optString("pConditionName");
        } catch (JSONException joe) {
            PrivilegeManager.debug.error("DNSNameCondition.setState", joe);
        }
    }

    /**
     * Returns <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     *
     * @param realm Realm name.
     * @param subject EntitlementCondition who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     * @throws com.sun.identity.entitlement,  EntitlementException in case
     * of any error
     */
    public ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        if (environment != null) {
            Set<String> reqDnsNames = environment.get(REQUEST_DNS_NAME);

            if ((reqDnsNames == null) || reqDnsNames.isEmpty()) {
                return getFalseDecision();
            }

            for (String dns : reqDnsNames) {
                if (!isAllowedByDns(dns)) {
                    return getFalseDecision();
                }
            }

            return new ConditionDecision(true, Collections.EMPTY_MAP);
        }

        return getFalseDecision();
    }

    private ConditionDecision getFalseDecision() {
        Set<String> set = new HashSet<String>();
        set.add(REQUEST_DNS_NAME + "=" + domainNameMask);
        Map<String, Set<String>> advice = new HashMap<String, Set<String>>();
        advice.put(getClass().getName(), set);
        return new ConditionDecision(false, advice);
    }

    private boolean isAllowedByDns(String dnsName)
        throws EntitlementException {
        if (domainNameMask.equals("*")) {
            return true;
        }

        dnsName = dnsName.toLowerCase();
        String mask = domainNameMask.toLowerCase();
        int starIndex = mask.indexOf("*");

        if (starIndex != -1) {
            // the dnsPattern is a string like *.ccc.ccc
            String dnsWildSuffix = mask.substring(starIndex+1);
            return dnsName.endsWith(dnsWildSuffix);
        }
        
        return mask.equalsIgnoreCase(dnsName);
    }


    /**
     * Returns the Domain Name Mask
     *
     * @return the Domain Name Mask
     */
    public String getDomainNameMask() {
        return domainNameMask;
    }

    /**
     * Sets the Domain Name Mask.
     *
     * @param domainNameMask the value for Domain Name Mask.
     */
    public void setDomainNameMask(String domainNameMask) {
        this.domainNameMask = domainNameMask;
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
        jo.put("domainNameMask", domainNameMask);
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
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        DNSNameCondition object = (DNSNameCondition) obj;
        if (getDomainNameMask() == null) {
            if (object.getDomainNameMask() != null) {
                return false;
            }
        } else {
            if (!domainNameMask.equals(object.getDomainNameMask())) {
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
        if (domainNameMask != null) {
            code += domainNameMask.hashCode();
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
            PrivilegeManager.debug.error("DNSNameCondition.toString()", e);
        }
        return s;
    }
}
