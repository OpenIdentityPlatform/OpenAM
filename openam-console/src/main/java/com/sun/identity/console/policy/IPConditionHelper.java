/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IPConditionHelper.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011-2013] [ForgeRock Inc]
 */
package com.sun.identity.console.policy;

import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.policy.model.PolicyModel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class IPConditionHelper {

    private enum IPVersion {
        IPv4, IPv6, INVALID
    }

    private static final String ATTR_IP_VERSION = "IpVersion";
    private static final String ATTR_DNS_NAME = "DnsName";
    private static final String ATTR_START_IP = "StartIp";
    private static final String ATTR_END_IP = "EndIp";
    private static final String NOTATION = "Notation";
    private static IPConditionHelper instance = new IPConditionHelper();

    private IPConditionHelper() {
    }

    public static IPConditionHelper getInstance() {
        return instance;
    }

    public String getConditionXML(boolean bCreate, boolean readonly) {
        String xml = null;

        if (bCreate) {
            xml = "com/sun/identity/console/propertyPMConditionIP.xml";
        } else {
            xml = (readonly) ?
                    "com/sun/identity/console/propertyPMConditionIP_Readonly.xml" :
                    "com/sun/identity/console/propertyPMConditionIP.xml";
        }
        return AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xml));
    }

    public String getMissingValuesMessage() {
        return "policy.condition.missing.ip.dns.message";
    }

    public void setPropertiesValues(
            Map values,
            AMPropertySheetModel propertySheetModel,
            AMModel model
    ) {
        String ipVer = null;
        if ((values != null) && !values.isEmpty()) {
            // Check which IP version we are working with

            Map<String, Set<String>> holdMap = values;
            if(holdMap.keySet().contains(ATTR_IP_VERSION)){
                // Get IP_Version
                ipVer = (String)holdMap.get(ATTR_IP_VERSION).toArray()[0];
                if(ipVer != null && !ipVer.isEmpty()){
                    setIPVersion(ATTR_IP_VERSION, ipVer, propertySheetModel);
                } else {
                    ipVer = "IPv4";
                    setIPVersion(ATTR_IP_VERSION, ipVer, propertySheetModel);
                }
            }

            Iterator i = values.keySet().iterator();
            for (i = values.keySet().iterator(); i.hasNext(); ) {
                String propName = (String)i.next();
                Set val = (Set)values.get(propName);
                // add IPversion to propertySheetModel
                if ((val != null) && !val.isEmpty()) {
                    if (propName.equals(ATTR_START_IP) || propName.equals(ATTR_END_IP)) {
                        if(IPVersion.valueOf(ipVer) == IPVersion.IPv4){
                            setIPAddress(propName, (String)val.iterator().next(), propertySheetModel);
                        } else if(IPVersion.valueOf(ipVer) == IPVersion.IPv6){
                            setIPv6Address(propName, (String)val.iterator().next(), propertySheetModel);
                        }
                    } else {
                        propertySheetModel.setValues(
                                propName, val.toArray(), model);
                    }
                }
            }
        }
    }

    public Map getConditionValues(
            PolicyModel model,
            AMPropertySheetModel propertySheetModel
    ) {
        Map map = new HashMap(6);
        String dnsName = (String)propertySheetModel.getValue(ATTR_DNS_NAME);
        if (dnsName.trim().length() > 0) {
            HashSet set = new HashSet(2);
            set.add(dnsName);
            map.put(ATTR_DNS_NAME, set);
        }
        String ipVersion =  (String)propertySheetModel.getValue(ATTR_IP_VERSION); // add to returning map
        if(ipVersion != null && !ipVersion.isEmpty()){
            HashSet set = new HashSet(2);
            set.add(ipVersion);
            map.put(ATTR_IP_VERSION, set);
        }

        String startIP = null;
        String endIP = null;
        Map<String,String> ipAddresses = new HashMap<String, String>(2);
        if(IPVersion.valueOf(ipVersion) == IPVersion.IPv4){
            startIP = getIPv4Address(ATTR_START_IP, propertySheetModel);
            endIP = getIPv4Address(ATTR_END_IP, propertySheetModel);
            ipAddresses.put(ATTR_START_IP, startIP);
            ipAddresses.put(ATTR_END_IP, endIP);
            map = ipV4NotSet(ipAddresses, map);
        } else { // must be IPv6
            startIP = getIPv6Address(ATTR_START_IP, propertySheetModel);
            endIP = getIPv6Address(ATTR_END_IP, propertySheetModel);
            ipAddresses.put(ATTR_START_IP, startIP);
            ipAddresses.put(ATTR_END_IP, endIP);
            map = ipV6NotSet(ipAddresses, map);
        }

        return map;
    }

    private Map ipV4NotSet(Map<String,String> ipAddresses, Map map) {
        String startIP = ipAddresses.get(ATTR_START_IP);
        String endIP = ipAddresses.get(ATTR_END_IP);
        if (!startIP.equals("0.0.0.0")) {
            HashSet set = new HashSet(2);
            set.add(startIP);
            map.put(ATTR_START_IP, set);
        } else if (!endIP.equals("0.0.0.0")) {
            HashSet set = new HashSet(2);
            set.add(endIP);
            map.put(ATTR_START_IP, set);
        }

        if (!endIP.equals("0.0.0.0")) {
            HashSet set = new HashSet(2);
            set.add(endIP);
            map.put(ATTR_END_IP, set);
        } else if (!startIP.equals("0.0.0.0")) {
            HashSet set = new HashSet(2);
            set.add(startIP);
            map.put(ATTR_END_IP, set);
        }

        return map;

    }

    private Map ipV6NotSet(Map<String,String> ipAddresses, Map map) {
        String startIP = ipAddresses.get(ATTR_START_IP);
        String endIP = ipAddresses.get(ATTR_END_IP);
        if (!startIP.equals("0:0:0:0:0:0:0:0")) {
            HashSet set = new HashSet(2);
            set.add(startIP);
            map.put(ATTR_START_IP, set);
        } else if (!endIP.equals("0:0:0:0:0:0:0:0")) {
            HashSet set = new HashSet(2);
            set.add(endIP);
            map.put(ATTR_START_IP, set);
        }

        if (!endIP.equals("0:0:0:0:0:0:0:0")) {
            HashSet set = new HashSet(2);
            set.add(endIP);
            map.put(ATTR_END_IP, set);
        } else if (!startIP.equals("0:0:0:0:0:0:0:0")) {
            HashSet set = new HashSet(2);
            set.add(startIP);
            map.put(ATTR_END_IP, set);
        }

        return map;

    }

    private String getIPv6Address( String propName, AMPropertySheetModel propertySheetModel) {
        StringBuilder buff = new StringBuilder(36);
        for (int i = 1; i < 9; i++) {
            if (i > 1 && i <= 5) {
                buff.append(":");
            } else if(i > 5 ){
                buff.append((String)propertySheetModel.getValue(NOTATION + (i-1))); // :: or . depending if IPv6 hybrid notation
            }
            String node = (String)propertySheetModel.getValue(propName +i);
            node = node.trim();
            if (node.length() == 0) {
                node = "0";
            }
            buff.append(node);
        }
        return buff.toString();
    }

    private String getIPv4Address(
            String propName,
            AMPropertySheetModel propertySheetModel
    ) {
        StringBuilder buff = new StringBuilder(20);
        for (int i = 1; i < 5; i++) {
            if (i > 1) {
                buff.append(".");
            }
            String node = (String)propertySheetModel.getValue(propName +i);
            node = node.trim();
            if (node.length() == 0) {
                node = "0";
            }
            buff.append(node);
        }
        return buff.toString();
    }

    private void setIPVersion(String propName, String ipVersion, AMPropertySheetModel propertySheetModel) {
        propertySheetModel.setValue(propName, ipVersion);
    }

    private void setIPv6Address(String propName, String value, AMPropertySheetModel propertySheetModel) {
        // TODO add difference between hybrid and normal ipv6 notation for display
        StringTokenizer st = new StringTokenizer(value, ".:");
        if (st.countTokens() == 8) {
            int i = 1;
            while (st.hasMoreTokens()) {
                propertySheetModel.setValue(propName + i, st.nextToken());
                i++;
            }
        }
    }

    private void setIPAddress(
            String propName,
            String value,
            AMPropertySheetModel propertySheetModel
    ) {
        StringTokenizer st = new StringTokenizer(value, ".");
        if (st.countTokens() == 4) {
            int i = 1;
            while (st.hasMoreTokens()) {
                propertySheetModel.setValue(propName + i, st.nextToken());
                i++;
            }
        }
    }
}
