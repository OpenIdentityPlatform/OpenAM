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
* $Id: AMAdminUtils.java,v 1.9 2009/10/19 18:17:37 asyhuang Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base.model;

import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.common.SearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.security.AccessController;
import java.util.TreeSet;
import com.sun.identity.shared.ldap.util.DN;

/* - NEED NOT LOG - */

/**
 * This provides a set helper methods to access
 * Access and Service Management SDKs.
 */
public class AMAdminUtils {
    private static Debug debug = Debug.getInstance(
        AMAdminConstants.CONSOLE_DEBUG_FILENAME);

    /**
     * Returns the Single-Sign-On Token of super administrator.
     *
     * @return Single-Sign-On Token of super administrator.
     * @throws SecurityException if security check fails.
     */
    public static final SSOToken getSuperAdminSSOToken()
        throws SecurityException
    {
        return (SSOToken)
            AccessController.doPrivileged(AdminTokenAction.getInstance());
    }

    /**
     * Returns the first value of a collection. Returns empty string if set is
     * collection or null.
     *
     * @param collection Collection to act on.
     */
    public static Object getValue(Collection collection) {
        return ((collection == null) || collection.isEmpty()) ? "" :
            collection.iterator().next();
    }

    /**
     * Returns the first value of a set. Returns -1 if set is empty or null.
     * Returns int equals of the string.
     *
     * @param set Set to act on.
     * @param defaultValue Default Value of set is empty.
     * @throws NumberFormatException if the item in the set is not an integer
     */
    public static int getIntValue(Set set, int defaultValue)
        throws NumberFormatException
    {
        int value = defaultValue;

        if ((set != null) && !set.isEmpty()) {
            String str = (String)set.iterator().next();
            str = str.trim();
            if (str.length() > 0) {
                value = Integer.parseInt(str);
            }
        }

        return value;
    }

    /**
     * Returns a hash set containing a given object.
     *
     * @param obj Object to be added in set.
     * @return a hash set containing a given object.
     */
    public static Set wrapInSet(Object obj) {
        Set set = null;
        if (obj != null){
            set = new HashSet(2);
            set.add(obj);
        } else {
            set = Collections.EMPTY_SET;
        }
        return set;
    }

    /**
     * Returns a set that contains all items in array.
     *
     * @param array Array which contains the items.
     * @return a set that contains all items in array.
     */
    public static Set toSet(Object[] array) {
        Set set = null;

        if ((array != null) && (array.length > 0)) {
            set = new HashSet(array.length *2);

            for (int i = 0; i < array.length; i++) {
                set.add((array[i].toString()).trim());
            }
        }
        
        return (set == null) ? Collections.EMPTY_SET : set;
    }

    /**
     * Returns a set that contains all items in array.
     *
     * @param array Array which contains the items.
     * @return a set that contains all items in array.
     */
    public static Set toOrderedSet(Object[] array) {
        Set set = null;

        if ((array != null) && (array.length > 0)) {
            set = new OrderedSet();

            for (int i = 0; i < array.length; i++) {
                set.add((array[i].toString()).trim());
            }
        }

        return (set == null) ? Collections.EMPTY_SET : set;
    }

    /**
     * Returns a list that contains all items in array.
     *
     * @param array Array which contains the items.
     * @return a list that contains all items in array.
     */
    public static List toList(Object[] array) {
        List list = null;

        if ((array != null) && (array.length > 0)) {
            list = new ArrayList(array.length);

            for (int i = 0; i < array.length; i++) {
                list.add((array[i].toString()).trim());
            }
        }
        
        return (list == null) ? Collections.EMPTY_LIST : list;
    }

    /**
     * Returns integer value of a item in a set.
     *
     * @param set Set that contains the item.
     * @param defaultValue default value if set is null or empty.
     * @return integer value of a item in a set.
     * @throws NumberFormatException if the item in the set in non integer.
     */
    public static int getInt(Set set, int defaultValue)
        throws NumberFormatException
    {
        int val = defaultValue;

        if ((set != null) && !set.isEmpty()) {
            String str = (String)set.iterator().next();
            val = Integer.parseInt(str);
        }

        return val;
    }

    /**
     * Removes the mapping of which keys matches with items in a given set.
     *
     * @param map Map object.
     * @param set Set of keys to be removed from <code>map</code>.
     */
    public static void removeMapEntries(Map map, Set set) {
        if ((set != null) && !set.isEmpty()) {
            for (Iterator iter = set.iterator(); iter.hasNext(); ) {
                map.remove(iter.next());
            }
        }
    }

    /**
     * Returns a map of upper cased keys.
     *
     * @param map Map to operate on.
     * @return a map of upper cased keys.
     */
    public static Map upCaseKeys(Map map) {
        Map uc = new HashMap(map.size() *2);

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            String key = ((String)entry.getKey()).toUpperCase();
            uc.put(key, entry.getValue());
        }

        return uc;
    }

    /**
     * Returns the integer value of an attribute.
     *
     * @param svcSchemaMgr service schema manager
     * @param type schema type
     * @param attribute name
     * @return string value of an attribute
     * @throws NumberFormatException if value is not numeric
     * @throws SMSException if operation fails
     */
    public static int getIntegerAttribute(
        ServiceSchemaManager svcSchemaMgr,
        SchemaType type,
        String attribute
    ) throws NumberFormatException, SMSException
    {
        int value = -1;
        Set tmp = getAttribute(svcSchemaMgr, type, attribute);

        if ((tmp != null) && !tmp.isEmpty()) {
            for (Iterator iter = tmp.iterator(); iter.hasNext(); ) {
                String s = (String) iter.next();
                if ((s != null) && (s.length() > 0)) {
                    value = Integer.parseInt(s);
                }
            }
        }

        return value;
    }

    /**
     * Returns the default value of an attribute from the service schema.
     *
     * @param svcSchemaMgr Service schema manager.
     * @param type Schema type.
     * @param attribute Attribute name.
     * @return values of service schema attribute.
     * @throws SMSException if operations fails.
     */
    public static Set getAttribute(
        ServiceSchemaManager svcSchemaMgr,
        SchemaType type,
        String attribute)
        throws SMSException
    {
        Set value = null;
        ServiceSchema schema = svcSchemaMgr.getSchema(type);

        if (schema != null) {
            AttributeSchema as = schema.getAttributeSchema(attribute);
            if (as != null) {
                value = as.getDefaultValues();
            }
        }

        return (value == null) ? Collections.EMPTY_SET : value;
    }

    public static String getStringFromInputStream(InputStream is) {
        StringBuilder buff = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line = reader.readLine();
            while (line != null) {
                buff.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            debug.error("PolicyModelImpl.getStringFromInputStream", e);
        }
        return buff.toString();
    }

    public static InputStream getInputStreamFromString(String buff) {
        return new ByteArrayInputStream(buff.getBytes());
    }

    /**
     * Removes a set of string entries in a set with case insensitivity check.
     *
     * @param master Master Set.
     * @param deletingSet Set that contains strings to be removed from master
     *        set.
     */
    public static void removeAllCaseIgnore(Set master, Set deletingSet) {
        if ((deletingSet != null) && !deletingSet.isEmpty()) {
            Set lcSet = lowerCase(deletingSet);
            for (Iterator iter = master.iterator(); iter.hasNext(); ) {
                String e = (String)iter.next();
                if (lcSet.contains(e.toLowerCase())) {
                    iter.remove();
                }
            }
        }
    }

    public static Set lowerCase(Set set) {
        Set lcSet = new HashSet(set.size() *2);
        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            String e = (String)iter.next();
            lcSet.add(e.toLowerCase());
        }
        return lcSet;
    }

    /**
     * Returns the parent DN for the given DN.
     *
     * @param dn name of the child object
     * @return parent of <code>dn</code>
     */
    public static String getParent(String dn) {
        if (dn != null) {
            return new DN(dn).getParent().toString();
        }
        return "";
    }


   /**
     * Returns the first <code>String</code> element from the given set.
     * If the set is empty, or null, an empty string will be returned.
     *
     * @param set where element resides
     * @return first String element from the set.
     */
    public static String getFirstElement(Set set) {
        return ((set != null) && !set.isEmpty())
            ? (String)set.iterator().next(): "";
    }

    /**
     * Gets search results warning message.  <code>SearchResult</code>
     * returns an error code whenever size or time limit is reached.
     * This method interprets the error code and return the appropriate 
     * warning message.  Empty string is returned if no limits are reached.
     *
     * @param results Access Management Search Result object.
     * @param model to retrieve localized string.
     * @return search results warning message.
     */
    public static String getSearchResultWarningMessage(
        SearchResults results,
        AMModel model
    ) {
        String message = null;

        if (results != null) {
            int errorCode = results.getErrorCode();

            if (errorCode == SearchResults.SIZE_LIMIT_EXCEEDED) {
                message = model.getLocalizedString(
                    "sizeLimitExceeded.message");
            } else if (errorCode == SearchResults.TIME_LIMIT_EXCEEDED) {
                message = model.getLocalizedString(
                    "timeLimitExceeded.message");
            }
        } 

        return (message != null) ? message : "";
    }

    /**
     * Returns a concatenation string of all entries in a collection.
     *
     * @param collection Collection.
     * @param delimiter Delimiter
     * @param reverse true to traversal the entries from bottom up.
     * @return a concatenation string of all entries in a collection.
     */
    public static String getString(
        Collection collection,
        String delimiter,
        boolean reverse
    ) {
        StringBuilder buff = new StringBuilder();
        if ((collection != null) && !collection.isEmpty()) {
            boolean empty = true;

            for (Iterator i = collection.iterator(); i.hasNext(); ) {
                if (!reverse) {
                    if (empty) {
                        empty = false;
                    } else {
                        buff.append(delimiter);
                    }
                    buff.append(i.next().toString());
                } else {
                    buff.insert(0, delimiter)
                        .insert(0, i.next().toString());
                }
            }
        }
        return buff.toString();
    }

    /**
     * Returns the string value of an attribute.
     *   
     * @param svcSchemaMgr service schema manager
     * @param type schema type
     * @param attribute name
     * @return string value of an attribute
     * @throws SMSException if operation fails
     */  
    public static String getStringAttribute(
        ServiceSchemaManager svcSchemaMgr,
        SchemaType type,
        String attribute)
        throws SMSException
    {
        String value = "";
        Set tmp = getAttribute(svcSchemaMgr, type, attribute);

        if ((tmp != null) && (!tmp.isEmpty())) {
            Iterator iter = tmp.iterator();

            if (iter.hasNext()) {
                value = (String) iter.next();
            }
        }

        return value;
    }

    /**
     * Replaces character/s in a string with another.
     *
     * @param string String to work on.
     * @param orig Original character/s.
     * @param substitution Substitution.
     * @return Manipulated string.
     */
    public static String replaceString(
        String string,
        String orig,
        String substitution
    ) {
        int idx = string.indexOf(orig);
        while (idx != -1) {
            string = string.substring(0, idx) + substitution +
                string.substring(idx+orig.length());
            idx = string.indexOf(orig, idx+substitution.length());
        }
        return string;
    }

    /**
     * Replaces character/s in a string with another
     *
     * @param list List of string.
     * @param orig Original character/s.
     * @param substitution Substitution.
     * @return Manipulated list.
     */
    public static List replaceString(
        List list,
        String orig,
        String substitution
    ) {
        List manipulated = new ArrayList(list.size());
        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            String str = (String)iter.next();
            manipulated.add(replaceString(str, orig, substitution));
        }
        return manipulated;
    }

    /**
     * Returns a map of String of String from a delimited string. Example
     * <code>key1=x|key2=y</code>.
     *
     * @param deString Delimited String
     * @param delimiter Delimiter.
     * @return a map of String of String from a delimited string.
     */
    public static Map getValuesFromDelimitedString(
        String deString,
        String delimiter
    ) {
        Map map = new HashMap();
        StringTokenizer st = new StringTokenizer(deString, delimiter);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int idx = token.indexOf('=');
            if (idx != -1) {
                map.put(token.substring(0, idx), token.substring(idx+1));
            }
        }
        return map;
    }

    /**
     * Returns a set of string from a delimited string. Example
     * <code>val1,val2,...,valN</code>.
     *
     * @param deString Delimited String
     * @param delimiter Delimiter.
     * @return Set of string from a delimited string.
     */
    public static Set getDelimitedValues(String deString, String delimiter) {
        Set set = new HashSet();
        if ((deString != null) && (deString.trim().length() > 0)) {
            StringTokenizer st = new StringTokenizer(deString, delimiter);
            while (st.hasMoreTokens()) {
                set.add(st.nextToken().trim());
            }
        }
        return set;
    }

    /**
     * Returns attribute schema object.
     *
     * @param serviceName Name of service.
     * @param schemaType Type of Schema.
     * @param subSchemaName Name of sub schema.
     * @param attributeName Name of attribute schema.
     * @return attribute schema object.
     */
    public static AttributeSchema getAttributeSchema(
        String serviceName,
        SchemaType schemaType,
        String subSchemaName,
        String attributeName) {
        Set attributeSchemas = getAttributeSchemas(serviceName, schemaType,
            subSchemaName);
        AttributeSchema attrSchema = null;

        for (Iterator i = attributeSchemas.iterator();
            i.hasNext() && (attrSchema == null);
        ) {
            AttributeSchema as = (AttributeSchema)i.next();
            if (as.getName().equals(attributeName)) {
                attrSchema = as;
            }
        }

        return attrSchema;
    }

    private static Set getAttributeSchemas(
        String serviceName,
        SchemaType schemaType,
        String subSchemaName
    ) {
        Set attributeSchemas = null;
        SSOToken adminSSOToken = (SSOToken)
            AccessController.doPrivileged(AdminTokenAction.getInstance());

        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, adminSSOToken);
            ServiceSchema ss = mgr.getSchema(schemaType);

            if (ss != null) {
                if (subSchemaName != null) {
                    ss = ss.getSubSchema(subSchemaName);
                }
                if (ss != null) {
                    attributeSchemas = ss.getAttributeSchemas(); 
                }
            }
        } catch (SSOException e) {
            debug.warning("AMAdminUtils.getAttributeSchemas", e);
        } catch (SMSException e) {
            debug.warning("AMAdminUtils.getAttributeSchemas", e);
        }

        return (attributeSchemas != null) ?
            attributeSchemas : Collections.EMPTY_SET;
    }

    private static Set getAttributeSchemas(
        String serviceName,
        SchemaType schemaType
    ) {
        return getAttributeSchemas(serviceName, schemaType, null);
    }

    /**
     * Returns a set of attribute schemas that can be displayed in console 
     * for a service of a given schema type.
     *
     * @param serviceName Name of Service.
     * @param schemaType Schema Type.
     * @return set of attribute schemas that can be displayed in console.
     */
    public static Set getDisplayableAttributeNames(
        String serviceName,
        SchemaType schemaType
    ) {
        Set displayable = null;
        Set attributeSchemas = getAttributeSchemas(serviceName, schemaType); 

        if ((attributeSchemas != null) && !attributeSchemas.isEmpty()) {
            displayable = new HashSet(attributeSchemas.size() *2);

            for (Iterator i = attributeSchemas.iterator(); i.hasNext();) {
                AttributeSchema as = (AttributeSchema)i.next();
                String i18nKey = as.getI18NKey();
                if ((i18nKey != null) && (i18nKey.trim().length() > 0)){
                    displayable.add(as);
                }
            }
        }

        return (displayable != null) ? displayable : Collections.EMPTY_SET;
    }

    /**
     * Returns service schema for an IdType.
     *
     * @param serviceName Name of Service.
     * @param idType IdType.
     * @return service schema for an IdType.
     */
    public static ServiceSchema getSchemaSchema(
        String serviceName,
        IdType idType
    ) {
        ServiceSchema serviceSchema = null;
         SSOToken adminSSOToken = (SSOToken)
            AccessController.doPrivileged(AdminTokenAction.getInstance());

        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, adminSSOToken);
            serviceSchema = mgr.getSchema(idType.getName());
        } catch (SSOException e) {
            debug.warning("AMAdminUtils.getAttributeSchemas", e);
        } catch (SMSException e) {
            debug.warning("AMAdminUtils.getAttributeSchemas", e);
        }

        return serviceSchema;
    }

    public static void makeMapValuesEmpty(Map map) {
        for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            entry.setValue(Collections.EMPTY_SET);
        }
    }

    public static List toList(OptionList optList) {
        List list = new ArrayList();
        if (optList != null) {
            Option[] options = optList.toOptionArray();
            if ((options != null) && (options.length > 0)) {
                for (int i = 0; i < options.length; i++) {
                    list.add(options[i].getValue());
                }
            }
        }
        return list;
    }

    /**
     * Returns a list of strings in the order of string length. Longest 
     * string is in front of the list.
     *
     * @param collection Collections of String.
     * @return a list of strings in the order of string length.
     */
    public static List orderByStringLength(Collection collection) {
        List ordered = new ArrayList(collection);
        Collections.sort(ordered, new StringLengthComparator());
        return ordered;
    }

    /**
     * Returns a clone map of String to Set.
     *
     * @param orig Map to be cloned.
     * @return cloned map.
     */
    public static Map cloneStringToSetMap(Map orig) {
        Map cloned = null;
        if (orig != null) {
            cloned = new HashMap(orig.size() *2);

            for (Iterator i = orig.keySet().iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                Set values = (Set)orig.get(key);
                Set cloneValues = null;

                if (values != null) {
                    cloneValues = new HashSet(values.size() *2);
                    for (Iterator j = values.iterator(); j.hasNext(); ) {
                        String val = (String)j.next();
                        cloneValues.add(val);
                    }
                }
                cloned.put(key, cloneValues);
            }
        }

        return cloned;
    }

    /**
     * Removes all matching entries from a set if entries exist in another set.
     * entries in the sets are LDAP entries.
     *
     * @param originalSet Original Set.
     * @param toDelete Collection of entries to be deleted.
     */
    public static void removeAllByDN(Set originalSet, Collection toDelete) {
        Set setDNs = toDNs(toDelete);

        for (Iterator iter = originalSet.iterator(); iter.hasNext(); ) {
            String strDN = (String)iter.next();
            DN dn = new DN(strDN.toLowerCase());
            if (containsDN(setDNs, dn)) {
                iter.remove();
            }
        }
    }

    private static Set toDNs(Collection set) {
        Set setDNs = new HashSet();
        for (Iterator i = set.iterator(); i.hasNext(); ) {
            String strDN = (String)i.next();
            DN dn = new DN(strDN.toLowerCase());
            setDNs.add(dn);
        }
        return setDNs;
    }

    private static boolean containsDN(Set set, DN dn) {
        boolean contain = false;
        for (Iterator i = set.iterator(); i.hasNext() && !contain; ) {
            DN d = (DN)i.next();
            contain = d.equals(dn);
        }
        return contain;
    }

    public static Set getUserAttributeNames() {
        Set userAttributeNames = new TreeSet();
        SSOToken token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                Constants.SVC_NAME_USER, token);
            ServiceSchema ss = mgr.getSchema(SchemaType.USER);
            Set attributeSchemas = ss.getAttributeSchemas();
            if ((attributeSchemas != null) && !attributeSchemas.isEmpty()) {
                for (Iterator i = attributeSchemas.iterator(); i.hasNext();) {
                    AttributeSchema as = (AttributeSchema) i.next();
                    String i18nKey = as.getI18NKey();
                    if ((i18nKey != null) && (i18nKey.length() > 0)) {
                        AttributeSchema.Type type = as.getType();

                        if (type.equals(AttributeSchema.Type.SINGLE)) {
                            AttributeSchema.UIType uiType = as.getUIType();
                            AttributeSchema.Syntax syntax = as.getSyntax();

                            if ((uiType == null) && 
                                !syntax.equals(AttributeSchema.Syntax.PASSWORD)
                            ) {
                                userAttributeNames.add(as.getName());
                            }
                        }
                    }
                }
            }
        } catch (SSOException e) {
            debug.error("AMAdminUtil.getUserAttributeNames", e);
        } catch (SMSException e) {
            debug.error("AMAdminUtil.getUserAttributeNames", e);
        }
        return userAttributeNames;
    }
}
