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
 * $Id: FormatUtils.java,v 1.6 2008/06/25 05:42:08 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SchemaType;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility to format CLI output.
 */
public class FormatUtils {
    public static final String SPACE = "    ";
    public static final String MASKED_PWD = "********";
    private FormatUtils() {
    }

    public static String printServiceNames(
        Set serviceNames,
        String template,
        SSOToken ssoToken
    ) throws SMSException, SSOException {
        StringBuilder buff = new StringBuilder();
        String[] arg = new String[1];
        if (serviceNames != null) {
            for (Iterator i = serviceNames.iterator(); i.hasNext(); ) {
                String serviceName = (String)i.next();
                ServiceSchemaManager mgr = new ServiceSchemaManager(
                    serviceName, ssoToken);
                Set types = mgr.getSchemaTypes();
                if (!types.isEmpty()) {
                    SchemaType type = (SchemaType)types.iterator().next();
                    ServiceSchema schema = mgr.getSchema(type);
                    if (schema != null) {
                        String i18nKey = schema.getI18NKey();
                        if ((i18nKey != null) && (i18nKey.length() > 0)) {
                            arg[0] = serviceName;
                            buff.append(MessageFormat.format(template, 
                                    (Object[])arg))
                                .append("\n");
                        }
                    }
                }
            }
        }
        return buff.toString();
    }
    
    public static String printAttributeValues(
        String template,
        Map attributeValues,
        Set passwords
    ) {
        Map map = new HashMap(attributeValues.size() *2);
        Set setPwd = new HashSet(2);
        setPwd.add(MASKED_PWD);
        for (Iterator i = attributeValues.entrySet().iterator(); i.hasNext(); ){
            Map.Entry entry = (Map.Entry)i.next();
            Object key = entry.getKey();
            if (passwords.contains(key)) {
                map.put(key, setPwd);
            } else {
                map.put(key, entry.getValue());
            }
        }
        return printAttributeValues(template, map);
    }
    
    public static String printAttributeValues(
        String template,
        Map attributeValues
    ) {
        StringBuilder buff = new StringBuilder();
        if (attributeValues != null) {
            String[] args = new String[2];
            for (Iterator i = attributeValues.keySet().iterator(); i.hasNext();
            ) {
                String name = (String)i.next();
                args[0] = name;
                Set values = (Set)attributeValues.get(name);
                if (values.isEmpty()) {
                    args[1] = "";
                    buff.append(
                            MessageFormat.format(template, (Object[])args))
                        .append("\n");
                } else {
                    for (Iterator j = values.iterator(); j.hasNext(); ) {
                        args[1] = (String)j.next();
                        buff.append(MessageFormat.format(
                                template, (Object[])args))
                            .append("\n");
                    }
                }
            }
        }
        return buff.toString();
    }

    public static String formatProperties(Map prop) {
        StringBuilder buff = new StringBuilder();
        if (prop != null) {
            Set sorted = new TreeSet();
            for (Iterator i = prop.keySet().iterator(); i.hasNext(); ) {
                sorted.add((String)i.next());
            }

            for (Iterator i = sorted.iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                String value = (String)prop.get(key);
                buff.append(key).append("=").append(value).append("\n");
            }
        }
        return buff.toString();
    }

    /**
     * Returns a formatted string. Given a map, a key and value label.
     * Tabulates the keys and values in the map in this manner.
     * <pre>
     * keyLabel       propLabel
     * -------------- --------------
     * key1           value1
     * key2           value2
     * keyN           valueN
     * </pre>
     *
     * @param keyLabel Label for the key column.
     * @param propLabel Label for the value column.
     * @param map Map that contains the information.
     * @return a formatted string of a map.
     */
    public static String formatMap(String keyLabel, String propLabel, Map map) {
        StringBuilder buff = new StringBuilder();
        int szColKey = keyLabel.length();
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            szColKey = Math.max(szColKey, key.length());
        }

        int szColProp = propLabel.length();
        for (Iterator i = map.values().iterator(); i.hasNext(); ) {
            String val = (String)i.next();
            szColProp = Math.max(szColProp, val.length());
        }

        buff.append(padString(keyLabel, szColKey))
            .append(" ")
            .append(propLabel)
            .append("\n");
        buff.append(padString("-", "-", szColKey))
            .append(" ")
            .append(padString("-", "-", szColProp))
            .append("\n");

        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            String prop = (String)map.get(key);
            buff.append(padString(key, szColKey))
                .append(" ")
                .append(padString(prop, szColProp))
                .append("\n");
        }
        return buff.toString();
    }

    private static String padString(String str, int pad) {
        return padString(str, " ", pad);
    }

    private static String padString(String str, String padStr, int pad) {
        for (int i = str.length(); i < pad; i++) {
            str += padStr;
        }
        return str;
    }
}
