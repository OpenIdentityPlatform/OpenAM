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
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */

package com.sun.identity.sm;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
        return (counter++);
    }

    public static String getUniqueID() {
        return String.valueOf(getInstanceID());
    }

    // Performs a deep copy of the Map
    public static Map copyAttributes(Map attributes) {
        if (attributes == null) {
            return new HashMap();
        }
        HashMap answer = attributes instanceof CaseInsensitiveHashMap ? 
                new CaseInsensitiveHashMap()
                : new HashMap();

        if (attributes.isEmpty()) {
            return (answer);
        }

        Iterator items = attributes.keySet().iterator();
        while (items.hasNext()) {
            String attrName = (String) items.next();
            Object o = attributes.get(attrName);
            if (o != null && o instanceof Set) {
                Set set = (Set) o;
                if (set.isEmpty()) {
                    if (set == Collections.EMPTY_SET) {
                        answer.put(attrName, Collections.EMPTY_SET);
                    } else {
                        answer.put(attrName, new HashSet());
                    }
                } else {
                    // Copy the HashSet
                    HashSet newSet = new HashSet();
                    for (Iterator nitems = set.iterator(); nitems.hasNext();) {
                        newSet.add(nitems.next());
                    }
                    answer.put(attrName, new HashSet(newSet));
                }
            } else {
                answer.put(attrName, o);
            }
        }
        return (answer);
    }

    static Map getAttrsFromEntry(SMSEntry entry) {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("SMSUtils: obtains attrs from entry: "
                    + entry.getDN());
        }
        Map answer = new HashMap();
        String[] attrValues = entry.getAttributeValues(SMSEntry.ATTR_KEYVAL);
        String[] searchableAttrValues = entry
                .getAttributeValues(SMSEntry.ATTR_XML_KEYVAL);
        if ((attrValues == null) && (searchableAttrValues == null)) {
            return (answer);
        }

        // Parse the attribute values
        for (int i = 0; attrValues != null && i < attrValues.length; i++) {
            String sattrvalue = attrValues[i];
            // Get attribute name and value
            int index = sattrvalue.indexOf('=');
            if (index == -1) {
                // Error in attribute values
                SMSEntry.debug.error("SMSUtils: Invalid attribute entry: "
                        + sattrvalue + "\nIn SMSEntry: " + entry);
                continue;
            }
            String attrName = sattrvalue.substring(0, index);
            String attrValue = null;
            if (sattrvalue.length() > (index + 1)) {
                attrValue = sattrvalue.substring(index + 1);
            }
            Set values = (Set) answer.get(attrName);
            if (values == null) {
                values = new HashSet();
                answer.put(attrName, values);
            }
            if ((attrValue != null) && attrValue.length() != 0) {
                values.add(attrValue);
            }
        }

        for (int j = 0; searchableAttrValues != null
                && j < searchableAttrValues.length; j++) {
            String searchAttrvalue = searchableAttrValues[j];
            // Get searchable attribute name and value
            int indx = searchAttrvalue.indexOf('=');
            if (indx == -1) {
                // Error in searchable attribute values
                SMSEntry.debug.error("SMSUtils: Invalid searchable attribute "
                        + "entry: " + searchAttrvalue + "\nIn SMSEntry: "
                        + entry);
                continue;
            }
            String srchAttrName = searchAttrvalue.substring(0, indx);
            String srchAttrValue = null;
            if (searchAttrvalue.length() > (indx + 1)) {
                srchAttrValue = searchAttrvalue.substring(indx + 1);
            }
            Set srchValues = (Set) answer.get(srchAttrName);
            if (srchValues == null) {
                srchValues = new HashSet();
                answer.put(srchAttrName, srchValues);
            }
            if ((srchAttrValue != null) && srchAttrValue.length() != 0) {
                srchValues.add(srchAttrValue);
            }
        }
        return (answer);
    }

    static void setAttributeValuePairs(SMSEntry e, Map attrs,
            Set searchAttrNames) throws SMSException {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("SMSUtils: setting attrs to entry: "
                    + e.getDN());
        }
        if (attrs != null) {
            Set values = new HashSet();
            Set srchValues = new HashSet();

            for (Iterator attrNames = attrs.keySet().iterator();
                attrNames.hasNext();
            ) {
                String attrName = (String) attrNames.next();
                Object o = attrs.get(attrName);
                boolean bSearch = !searchAttrNames.isEmpty() &&
                    searchAttrNames.contains(attrName.toLowerCase());

                if (o == null) {
                    // do nothing
                } else if (o instanceof String) {
                    if (bSearch) {
                        srchValues.add(attrName + "=" + (String) o);
                    } else {
                        values.add(attrName + "=" + (String) o);
                    }
                } else if ((o instanceof Set)) {
                    Set set = (Set)o;
                    if (set.isEmpty()) {
                        // an attribute with no values
                        if (bSearch) {
                            srchValues.add(attrName + "=");
                        } else {
                            values.add(attrName + "=");
                        }
                    } else {
                        for (Iterator i = set.iterator(); i.hasNext(); ) {
                            String item = (String)i.next();
                            if (bSearch) {
                                srchValues.add(attrName + "=" + item);
                            } else {
                                values.add(attrName + "=" + item);
                            }
                        }
                    }
                }
            }

            if (!values.isEmpty()) {
                e.setAttribute(SMSEntry.ATTR_KEYVAL, (String[]) values
                        .toArray(new String[values.size()]));
            }
            if (!srchValues.isEmpty()) {
                e.setAttribute(SMSEntry.ATTR_XML_KEYVAL, (String[]) srchValues
                        .toArray(new String[srchValues.size()]));
            }
        }
    }

    static void addAttribute(SMSEntry e, String attrName, Set values,
            Set searchableAttrNames) throws SMSException {
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("SMSUtils: adding attributes to entry: "
                    + e.getDN());
        }
        if ((attrName == null) || (values == null)) {
            return;
        }

        if ((!searchableAttrNames.isEmpty())
                && (searchableAttrNames.contains(attrName.toLowerCase()))) {
            for (Iterator vals = values.iterator(); vals.hasNext();) {
                e.addAttribute(SMSEntry.ATTR_XML_KEYVAL,
                        (attrName + "=" + (String) vals.next()));
            }
        } else {
            for (Iterator vals = values.iterator(); vals.hasNext();) {
                e.addAttribute(SMSEntry.ATTR_KEYVAL,
                        (attrName + "=" + (String) vals.next()));
            }

        }
    }

    static void removeAttribute(SMSEntry e, String attrName)
            throws SMSException {

        String[] attrValues = e.getAttributeValues(SMSEntry.ATTR_KEYVAL);
        String[] searchableAttrValues = e
                .getAttributeValues(SMSEntry.ATTR_XML_KEYVAL);

        if ((attrValues == null) && (searchableAttrValues == null)) {
            return;
        }
        if (attrValues != null) {
            String matchString = attrName + "=";
            for (int i = 0; i < attrValues.length; i++) {
                if (attrValues[i].startsWith(matchString)) {
                    e.removeAttribute(SMSEntry.ATTR_KEYVAL, attrValues[i]);
                }
            }
        }
        if (searchableAttrValues != null) {
            String matchStr = attrName + "=";
            for (int j = 0; j < searchableAttrValues.length; j++) {
                if (searchableAttrValues[j].startsWith(matchStr)) {
                    e.removeAttribute(SMSEntry.ATTR_XML_KEYVAL,
                            searchableAttrValues[j]);
                }
            }
        }
    }

    static void removeAttributeValues(SMSEntry e, String attrName, Set values,
            Set searchableAttrNames) throws SMSException {

        if ((attrName == null) || (values == null) || values.isEmpty()) {
            return;
        }
        if ((!searchableAttrNames.isEmpty())
                && (searchableAttrNames.contains(attrName.toLowerCase()))) {
            for (Iterator items = values.iterator(); items.hasNext();) {
                String value = (String) items.next();
                e.removeAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "="
                        + value);
            }
        } else {
            for (Iterator items = values.iterator(); items.hasNext();) {
                String value = (String) items.next();
                e.removeAttribute(SMSEntry.ATTR_KEYVAL, attrName + "=" + value);
            }
        }
    }

    static void replaceAttributeValue(SMSEntry entry, String attrName,
            String oldValue, String newValue, Set searchableAttrNames)
            throws SMSException {
        if ((!searchableAttrNames.isEmpty())
                && (searchableAttrNames.contains(attrName.toLowerCase()))) {
            entry.removeAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "="
                    + oldValue);
            entry.addAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "="
                    + newValue);
        } else {
            entry.removeAttribute(SMSEntry.ATTR_KEYVAL, attrName + "="
                    + oldValue);
            entry.addAttribute(SMSEntry.ATTR_KEYVAL, attrName + "=" + newValue);
        }
    }

    static void replaceAttributeValues(SMSEntry entry, String attrName,
            Set oldValues, Set newValues, Set searchableAttrNames)
            throws SMSException {
        removeAttributeValues(entry, attrName, oldValues, searchableAttrNames);

        // Add other values
        if ((newValues == null) || newValues.isEmpty()) {
            return;
        }
        if ((!searchableAttrNames.isEmpty())
                && (searchableAttrNames.contains(attrName.toLowerCase()))) {
            for (Iterator items = newValues.iterator(); items.hasNext();) {
                String value = (String) items.next();
                entry.addAttribute(SMSEntry.ATTR_XML_KEYVAL, attrName + "="
                        + value);
            }
        } else {
            for (Iterator items = newValues.iterator(); items.hasNext();) {
                String value = (String) items.next();
                entry
                        .addAttribute(SMSEntry.ATTR_KEYVAL, attrName + "="
                                + value);
            }
        }
    }
    
    static String toAttributeValuePairXML(Map map) {
        if ((map == null) || map.isEmpty()) {
            return "";
        }
        
        StringBuilder buff = new StringBuilder();
        
        for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry)i.next();
            buff.append("<").append(SMSUtils.ATTRIBUTE_VALUE_PAIR)
                .append(">\n");
            buff.append("    <").append(SMSUtils.ATTRIBUTE)
                .append(" ").append(SMSUtils.NAME).append("=\"")
                .append(e.getKey()).append("\"/>\n");
            Set values = (Set)e.getValue();
            for (Iterator j = values.iterator(); j.hasNext(); ) {
                buff.append("    <").append(SMSUtils.ATTRIBUTE_VALUE)
                    .append(">")
                    .append(SMSSchema.escapeSpecialCharacters((String)j.next()))
                    .append("</").append(SMSUtils.ATTRIBUTE_VALUE)
                    .append(">\n");
            }
            buff.append("</").append(SMSUtils.ATTRIBUTE_VALUE_PAIR)
                .append(">\n");
        }
        
        return buff.toString();
    }
}
