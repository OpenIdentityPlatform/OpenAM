/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMSUtils.java,v 1.5 2008/07/11 01:46:21 arviranga Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */
package com.sun.identity.sm;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.identity.common.CaseInsensitiveHashMap;

public class SMSUtils {

    public static final String SERVICE = "Service";

    public static final String NAME = "name";

    public static final String VERSION = "version";

    public static final String SCHEMA = "Schema";

    public static final String SUB_SCHEMA = "SubSchema";

    public static final String SUB_CONFIG = "SubConfiguration";

    public static final String GLOBAL_SCHEMA = "Global";

    public static final String GLOBAL_CONFIG = "GlobalConfiguration";

    public static final String ORG_SCHEMA = "Organization";

    public static final String ORG_ATTRIBUTE_SCHEMA =
        "OrganizationAttributeSchema";

    public static final String ORG_ATTRIBUTE_VALUE_PAIR =
        "OrganizationAttributeValuePair";

    public static final String ORG_CONFIG = "OrganizationConfiguration";

    public static final String DYNAMIC_SCHEMA = "Dynamic";

    public static final String USER_SCHEMA = "User";

    public static final String POLICY_SCHEMA = "Policy";

    public static final String GROUP_SCHEMA = "Group";

    public static final String DOMAIN_SCHEMA = "Domain";

    public static final String GENERIC_SCHEMA = "Generic";

    public static final String CONFIGURATION = "Configuration";

    public static final String INSTANCE = "Instance";

    public static final String GROUP = "group";

    protected static final String URI = "uri";

    protected static final String SERVICE_ID = "id";

    protected static final String PRIORITY = "priority";

    protected static final String COSPRIORITY = "cospriority";

    protected static final String DEFAULT = "default";

    protected static final String RESOURCE_BUNDLE_URL = "i18nJarURL";

    protected static final String PROPERTIES_FILENAME = "i18nFileName";

    protected static final String SERVICE_HIERARCHY = "serviceHierarchy";

    protected static final String PROPERTIES_VIEW_BEAN_URL =
        "propertiesViewBeanURL";

    protected static final String I18N_KEY = "i18nKey";

    protected static final String REVISION_NUMBER = "revisionNumber";

    protected static final String STATUS_ATTRIBUTE = "statusAttribute";

    protected static final String VALIDATE = "validate";

    protected static final String INHERITANCE = "inheritance";

    protected static final String ISSEARCHABLE = "isSearchable";

    protected static final String PLUGIN_INTERFACE = "PluginInterface";

    protected static final String PLUGIN_INTERFACE_CLASS = "interface";

    protected static final String PLUGIN_SCHEMA = "PluginSchema";

    protected static final String PLUGIN_SCHEMA_INT_NAME = "interfaceName";

    protected static final String PLUGIN_SCHEMA_CLASS_NAME = "className";

    protected static final String PLUGIN_SCHEMA_JAR_URL = "jarURL";

    protected static final String PLUGIN_SCHEMA_ORG_NAME = "organizationName";

    protected static final String PLUGIN_CONFIG = "PluginConfiguration";

    protected static final String PLUGIN_CONFIG_SCHEMA_NAME =
        "pluginSchemaName";

    protected static final String PLUGIN_CONFIG_INT_NAME = "interfaceName";

    protected static final String PLUGIN_CONFIG_ORG_NAME = "organizationName";

    protected static final String SCHEMA_ATTRIBUTE = "AttributeSchema";

    protected static final String ATTRIBUTE_VALUE_PAIR = "AttributeValuePair";

    protected static final String ATTRIBUTE = "Attribute";

    protected static final String ATTRIBUTE_TYPE = "type";

    protected static final String ATTRIBUTE_UITYPE = "uitype";

    protected static final String ATTRIBUTE_LIST_ORDER = "listOrder";

    protected static final String ATTRIBUTE_SYNTAX = "syntax";

    protected static final String ATTRIBUTE_DEFAULT = "default";

    protected static final String ATTRIBUTE_RANGE_START = "rangeStart";

    protected static final String ATTRIBUTE_RANGE_END = "rangeEnd";

    protected static final String ATTRIBUTE_MIN_VALUE = "minValue";

    protected static final String ATTRIBUTE_MAX_VALUE = "maxValue";

    protected static final String ATTRIBUTE_VALIDATOR = "validator";

    protected static final String ATTRIBUTE_OPTIONAL = "IsOptional";

    protected static final String ATTRIBUTE_SERVICE_ID = "IsServiceIdentifier";

    protected static final String ATTRIBUTE_RESOURCE_NAME =
        "IsResourceNameAllowed";

    protected static final String ATTRIBUTE_STATUS_ATTR = "IsStatusAttribute";

    protected static final String HAS_SERVICE_URLS = "HasServiceURLs";

    protected static final String ATTRIBUTE_ANY = "any";

    protected static final String ATTRIBUTE_VIEW_BEAN_URL =
        "propertiesViewBeanURL";

    protected static final String ATTRIBUTE_VALUE = "Value";

    protected static final String ATTRIBUTE_DEFAULT_ELEMENT = "DefaultValues";

    protected static final String ATTRIBUTE_DEFAULT_CLASS =
        "DefaultValuesClassName";

    protected static final String CLASS_NAME = "className";

    protected static final String ATTRIBUTE_CHOICE_CLASS =
        "ChoiceValuesClassName";

    protected static final String ATTRIBUTE_CHOICE_VALUES_ELEMENT =
        "ChoiceValues";

    protected static final String ATTRIBUTE_CHOICE_VALUE_ELEMENT =
        "ChoiceValue";

    protected static final String ATTRIBUTE_COS_QUALIFIER = "cosQualifier";

    protected static final String ATTRIBUTE_BOOLEAN_VALUES_ELEMENT =
        "BooleanValues";

    protected static final String ATTRIBUTE_TRUE_BOOLEAN_ELEMENT =
        "BooleanTrueValue";

    protected static final String ATTRIBUTE_FALSE_BOOLEAN_ELEMENT =
        "BooleanFalseValue";

    protected static int counter = 0;

    static int getInstanceID() {
        return counter++;
    }

    public static String getUniqueID() {
        return String.valueOf(getInstanceID());
    }

    // Performs a deep copy of the Map
    public static Map<String, Object> copyAttributes(Map<String, Object> attributes) {
        if (attributes == null) {
            return new HashMap<String, Object>();
        }
        Map<String, Object> answer = attributes instanceof CaseInsensitiveHashMap ?
                new CaseInsensitiveHashMap(attributes.size()) : new HashMap<String, Object>(attributes.size());

        if (attributes.isEmpty()) {
            return answer;
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Set) {
                Set<String> set = (Set<String>) value;
                if (set.isEmpty()) {
                    if (set == Collections.EMPTY_SET) {
                        answer.put(attrName, Collections.EMPTY_SET);
                    } else {
                        answer.put(attrName, new HashSet<String>(0));
                    }
                } else {
                    // Copy the HashSet
                    answer.put(attrName, new HashSet(set));
                }
            } else {
                answer.put(attrName, value);
            }
        }
        return answer;
    }

    static Map<String, Set<String>> getAttrsFromEntry(SMSEntry entry) {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("SMSUtils: obtains attrs from entry: " + entry.getDN());
        }
        Map<String, Set<String>> answer = new HashMap<String, Set<String>>();
        String[] attrValues = entry.getAttributeValues(SMSEntry.ATTR_KEYVAL);
        String[] searchableAttrValues = entry.getAttributeValues(SMSEntry.ATTR_XML_KEYVAL);
        if (attrValues == null && searchableAttrValues == null) {
            return answer;
        }

        addAttributesToMap(entry, attrValues, answer);
        addAttributesToMap(entry, searchableAttrValues, answer);

        return answer;
    }

    private static void addAttributesToMap(SMSEntry entry, String[] attrs, Map<String, Set<String>> attrMap) {
        if (attrs != null) {
            for (String attribute : attrs) {
                // Get attribute name and value
                int idx = attribute.indexOf('=');
                if (idx == -1) {
                    // Error in attribute values
                    SMSEntry.debug.error("SMSUtils: Invalid attribute entry: " + attribute
                            + "\nIn SMSEntry: " + entry);
                    continue;
                }
                String attrName = attribute.substring(0, idx);
                String attrValue = null;
                if (attribute.length() > idx + 1) {
                    attrValue = attribute.substring(idx + 1);
                }
                Set<String> values = attrMap.get(attrName);
                if (values == null) {
                    values = new HashSet<String>();
                    attrMap.put(attrName, values);
                }
                if (attrValue != null && !attrValue.isEmpty()) {
                    values.add(attrValue);
                }
            }
        }
    }

    static void setAttributeValuePairs(SMSEntry e, Map<String, Object> attrs, Set<String> searchAttrNames)
            throws SMSException {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("SMSUtils: setting attrs to entry: " + e.getDN());
        }
        if (attrs != null) {
            Set<String> values = new HashSet<String>();
            Set<String> srchValues = new HashSet<String>();

            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                String attrName = entry.getKey();
                Object attrValue = entry.getValue();
                boolean isSearchable = searchAttrNames.contains(attrName.toLowerCase());

                if (attrValue == null) {
                    // do nothing
                } else if (attrValue instanceof String) {
                    if (isSearchable) {
                        srchValues.add(attrName + "=" + attrValue);
                    } else {
                        values.add(attrName + "=" + attrValue);
                    }
                } else if (attrValue instanceof Set) {
                    Set<String> set = (Set<String>) attrValue;
                    if (set.isEmpty()) {
                        // an attribute with no values
                        if (isSearchable) {
                            srchValues.add(attrName + "=");
                        } else {
                            values.add(attrName + "=");
                        }
                    } else {
                        for (String item : set) {
                            if (isSearchable) {
                                srchValues.add(attrName + "=" + item);
                            } else {
                                values.add(attrName + "=" + item);
                            }
                        }
                    }
                }
            }

            if (!values.isEmpty()) {
                e.setAttribute(SMSEntry.ATTR_KEYVAL, values.toArray(new String[values.size()]));
            }
            if (!srchValues.isEmpty()) {
                e.setAttribute(SMSEntry.ATTR_XML_KEYVAL, srchValues.toArray(new String[srchValues.size()]));
            }
        }
    }

    static void addAttribute(SMSEntry e, String attrName, Set<String> values, Set<String> searchableAttrNames)
            throws SMSException {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("SMSUtils: adding attributes to entry: " + e.getDN());
        }
        if (attrName == null || values == null) {
            return;
        }

        if (searchableAttrNames.contains(attrName.toLowerCase())) {
            for (String value : values) {
                e.addAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "=" + value);
            }
        } else {
            for (String value : values) {
                e.addAttribute(SMSEntry.ATTR_KEYVAL, attrName + "=" + value);
            }
        }
    }

    static void removeAttribute(SMSEntry e, String attrName) throws SMSException {
        String[] attrValues = e.getAttributeValues(SMSEntry.ATTR_KEYVAL);
        String[] searchableAttrValues = e.getAttributeValues(SMSEntry.ATTR_XML_KEYVAL);

        if (attrValues == null && searchableAttrValues == null) {
            return;
        }
        if (attrValues != null) {
            String matchString = attrName + "=";
            for (String attrValue : attrValues) {
                if (attrValue.startsWith(matchString)) {
                    e.removeAttribute(SMSEntry.ATTR_KEYVAL, attrValue);
                }
            }
        }
        if (searchableAttrValues != null) {
            String matchStr = attrName + "=";
            for (String searchableAttrValue : searchableAttrValues) {
                if (searchableAttrValue.startsWith(matchStr)) {
                    e.removeAttribute(SMSEntry.ATTR_XML_KEYVAL, searchableAttrValue);
                }
            }
        }
    }

    static void removeAttributeValues(SMSEntry e, String attrName, Set<String> values, Set searchableAttrNames)
            throws SMSException {
        if (attrName == null || values == null || values.isEmpty()) {
            return;
        }
        if (searchableAttrNames.contains(attrName.toLowerCase())) {
            for (String value : values) {
                e.removeAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "=" + value);
            }
        } else {
            for (String value : values) {
                e.removeAttribute(SMSEntry.ATTR_KEYVAL, attrName + "=" + value);
            }
        }
    }

    static void replaceAttributeValue(SMSEntry entry, String attrName, String oldValue, String newValue,
            Set<String> searchableAttrNames) throws SMSException {
        if (searchableAttrNames.contains(attrName.toLowerCase())) {
            entry.removeAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "=" + oldValue);
            entry.addAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "=" + newValue);
        } else {
            entry.removeAttribute(SMSEntry.ATTR_KEYVAL, attrName + "=" + oldValue);
            entry.addAttribute(SMSEntry.ATTR_KEYVAL, attrName + "=" + newValue);
        }
    }

    static void replaceAttributeValues(SMSEntry entry, String attrName, Set<String> oldValues, Set<String> newValues,
            Set<String> searchableAttrNames) throws SMSException {
        removeAttributeValues(entry, attrName, oldValues, searchableAttrNames);

        // Add other values
        if (newValues == null || newValues.isEmpty()) {
            return;
        }
        if (searchableAttrNames.contains(attrName.toLowerCase())) {
            for (String value : newValues) {
                entry.addAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "=" + value);
            }
        } else {
            for (String value : newValues) {
                entry.addAttribute(SMSEntry.ATTR_KEYVAL, attrName + "=" + value);
            }
        }
    }

    static String toAttributeValuePairXML(Map<String, Set<String>> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }

        StringBuilder buff = new StringBuilder();

        for (Map.Entry<String, Set<String>> e : map.entrySet()) {
            buff.append("<").append(SMSUtils.ATTRIBUTE_VALUE_PAIR).append(">\n");
            buff.append("    <").append(SMSUtils.ATTRIBUTE)
                    .append(" ").append(SMSUtils.NAME).append("=\"").append(e.getKey()).append("\"/>\n");
            Set<String> values = e.getValue();
            for (String value : values) {
                buff.append("    <").append(SMSUtils.ATTRIBUTE_VALUE).append(">")
                        .append(SMSSchema.escapeSpecialCharacters(value))
                        .append("</").append(SMSUtils.ATTRIBUTE_VALUE).append(">\n");
            }
            buff.append("</").append(SMSUtils.ATTRIBUTE_VALUE_PAIR).append(">\n");
        }

        return buff.toString();
    }

    /**
     * Remove the validator attributes from the given attribute defaults map.
     * @param attributeDefaults The attribute defaults.
     * @param serviceSchema The service schema in which the attributes are specified.
     * @return A copy of the attribute defaults with the validators removed.
     */
    public static Map<String, Set<String>> removeValidators(Map<String, Set<String>> attributeDefaults,
                                                            ServiceSchema serviceSchema) {

        final Map<String, Set<String>> subset = new HashMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> entry : attributeDefaults.entrySet()) {
            final String name = entry.getKey();
            if (!AttributeSchema.Type.VALIDATOR.equals(serviceSchema.getAttributeSchema(name).getType())) {
                subset.put(name, new HashSet<String>(entry.getValue()));
            }
        }
        return subset;
    }
}
