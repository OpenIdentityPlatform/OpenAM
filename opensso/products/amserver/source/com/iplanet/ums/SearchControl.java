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
 * $Id: SearchControl.java,v 1.5 2009/01/28 05:34:51 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import java.util.Hashtable;

import com.sun.identity.shared.ldap.LDAPv2;

/**
 * This class provides a way to customize Search behaviors. Common behaviors are
 * time limit, result limit and Virtual list view. In future, we will provide
 * ways for client to define different hierarchical tree through SearchControl.
 *
 * @supported.api
 */
public class SearchControl implements java.io.Serializable {

    private static final long serialVersionUID = -8755868973524858945L;

    static final String KeyVlvRange = "vlvRange";

    static final String KeyVlvJumpTo = "vlvJumpTo";

    static final String KeyTimeOut = "timeOut";

    static final String KeySortKeys = "sortKeys";

    static final String KeyMaxResults = "maxResults";

    static final String KeySearchScope = "searchScope";

    static final int DEFAULT_MAX_RESULTS = 0;

    static final int DEFAULT_TIMEOUT = 0;

    // Disabled by default
    private static boolean getAllAttributesEnabled = false;

    /**
     * Search scope for one level. You use this search scope in getting
     * immediate children of a container. This is the default search scope in
     * getChildren method in search API. One can use
     * SearchControl.setSearchScope to override the default search scope in
     * getChildren.
     * 
     * @supported.api
     */
    public static final int SCOPE_ONE = LDAPv2.SCOPE_ONE;

    /**
     * Search scope for subtree level. This scope is used as the default search
     * scope in the search API. One can use SearchControl.setSearchScope to
     * override the default search scope in search methods.
     * 
     * @supported.api
     */
    public static final int SCOPE_SUB = LDAPv2.SCOPE_SUB;

    /**
     * Search scope for just this object.
     * 
     * @supported.api
     */
    public static final int SCOPE_BASE = LDAPv2.SCOPE_BASE;

    /**
     * Set sort order based on attribute names.
     * 
     * @param attributeNames
     *            array of attribute names to sort on
     * @supported.api
     */
    public void setSortKeys(String[] attributeNames) {
        SortKey[] sortKeys;
        if (attributeNames == null)
            return;

        sortKeys = new SortKey[attributeNames.length];
        for (int i = 0; i < sortKeys.length; i++) {
            sortKeys[i] = new SortKey();
            sortKeys[i].attributeName = attributeNames[i];
            sortKeys[i].reverse = false;
        }

        set(KeySortKeys, sortKeys);
    }

    /**
     * Set sort order based on SortKey
     * 
     * @param sortKeys
     *            array of SortKey.
     * @supported.api
     */
    public void setSortKeys(SortKey[] sortKeys) {
        set(KeySortKeys, sortKeys);
    }

    /**
     * Get existing attribute names for sorting.
     * @supported.api
     */
    public SortKey[] getSortKeys() {
        return (SortKey[]) get(KeySortKeys);
    }

    /**
     * Set range for retrieving VLV data.
     * 
     * @param startIndex
     *            starting position
     * @param beforeCount
     *            Number of entries before the startIndex
     * @param afterCount
     *            Number of entries after the startIndex.
     * @supported.api
     */
    public void setVLVRange(int startIndex, int beforeCount, int afterCount) {
        int[] range = new int[3];
        range[0] = startIndex;
        range[1] = beforeCount;
        range[2] = afterCount;

        set(KeyVlvRange, range);
    }

    /**
     * Set range for retrieving VLV data.
     * 
     * @param jumpTo
     *            Search expression defining the result set return
     * @param beforeCount
     *            Number of entries before the startIndex
     * @param afterCount
     *            Number of entries after the startIndex.
     * @supported.api
     */
    public void setVLVRange(String jumpTo, int beforeCount, int afterCount) {
        int[] range = new int[3];

        range[0] = 0;
        range[1] = beforeCount;
        range[2] = afterCount;

        set(KeyVlvJumpTo, jumpTo);
        set(KeyVlvRange, range);
    }

    /**
     * Get range for current VLV setting.
     * 
     * @return array of int which contain startIndex, beforeCount and
     *         afterCount.
     * @supported.api
     */
    public int[] getVLVRange() {
        return (int[]) get(KeyVlvRange);
    }

    /**
     * Get jumpTo value for VLV range.
     * 
     * @return jumpTo value.
     * @supported.api
     */
    public String getVLVJumpTo() {
        return (String) get(KeyVlvJumpTo);
    }

    /**
     * Sets the maximum number of milliseconds to wait for any operation for the
     * search.
     * 
     * @param timeOut
     *            Max number of milliseconds.
     * @supported.api
     */
    public void setTimeOut(int timeOut) {
        set(KeyTimeOut, new Integer(timeOut));
    }

    /**
     * Get current time out setting.
     * @supported.api
     */
    public int getTimeOut() {
        Integer i = (Integer) get(KeyTimeOut);
        if (i == null) {
            return DEFAULT_TIMEOUT;
        } else {
            return i.intValue();
        }
    }

    /**
     * Sets the maximum number of search results to return; 0 means there is no
     * limit.
     * @supported.api
     */
    public void setMaxResults(int maxNumber) {
        set(KeyMaxResults, new Integer(maxNumber));
    }

    /**
     * Gets the maximum number of search results to return. return 0 means there
     * is no limit.
     * @supported.api
     */
    public int getMaxResults() {
        Integer i = (Integer) get(KeyMaxResults);
        if (i == null) {
            return DEFAULT_MAX_RESULTS;
        } else {
            return i.intValue();
        }
    }

    /**
     * Sets the search scope in SearchControl.
     * 
     * @param scope
     *            Search scope defined in the SearchControl to be used with the
     *            search API.
     * @supported.api
     */
    public void setSearchScope(int scope) {
        set(KeySearchScope, new Integer(scope));
    }

    /**
     * Gets the search scope defined in the SearchControl.
     * 
     * @return search scope defined in the SearchControl. If search scope is
     *         never defined in the SearchControl SCOPE_SUB for subtree type of
     *         search is assumed.
     * @supported.api
     */
    public int getSearchScope() {
        Integer scope = (Integer) get(KeySearchScope);
        if (scope != null) {
            return scope.intValue();
        } else {
            return SearchControl.SCOPE_SUB;
        }
    }

    /**
     * Gets the search scope defined in the SearchControl. Allow user to specify
     * default search scope if nothing has been defined in the SearchControl
     * yet.
     * 
     * @param defaultScope
     *            Scope value to be used in case the SearchControl is not set up
     *            with a search scope
     * 
     * @return Search scope defined in the SearchControl. Return defaultScope if
     *         scope is not defined in the control.
     * @supported.api
     */
    public int getSearchScope(int defaultScope) {
        Integer scope = (Integer) get(KeySearchScope);
        if (scope != null) {
            return scope.intValue();
        } else {
            return defaultScope;
        }
    }

    /**
     * Sets internal attribute value in SearchControl
     */
    protected void set(String name, Object o) {
        m_hashTable.put(name, o);
    }

    /**
     * Gets internal attribute defined in SearchControl
     * 
     * @param name
     *            Name of attribute to get
     * @return Object representing the value of the attribute. Return null
     *         object if the given attribute name is not found
     */
    protected Object get(String name) {
        return m_hashTable.get(name);
    }

    /**
     * Checks if an internal attribute is defined for the control
     * 
     * @param name
     *            Name of internal attribute to check against
     * @return <code>true</code> if internal attribute is defined in the
     *         control and <code>false</code> otherwise
     */
    protected boolean contains(String name) {
        return m_hashTable.containsKey(name);
    }

    /**
     * Sets the specified boolean value to the variable. Boolean value is set to
     * true, if all attributes of the entries need to be obtained as part of the
     * search.
     * 
     * NOTE: If this getAllReturnAttributes boolean is set to true as part of
     * AMSearchControl, it overrides any other setReturnAttributes set as part
     * of the AMSearchControl. This is similar to using a wildcard '*' in
     * search.
     * 
     * When all the return attributes are set, the return attributes can be
     * obtained as a map with DN as map-key and set of attribute values as
     * map-value from AMSearchResults object.
     * 
     * @param getAllAttributes
     *            Boolean value set to true as part of the AMSearchControl to
     *            obtain all attributes as part of the search.
     * 
     * 
     */
    public void setAllReturnAttributes(boolean getAllAttributes) {
        getAllAttributesEnabled = getAllAttributes;
    }

    /**
     * Method to check if the boolean getAllAttributesEnabled is enabled or
     * disabled.
     * 
     * @return Returns the value of the boolean getAllAttributesEnabled. Returns
     *         true or false.
     */
    public boolean isGetAllReturnAttributesEnabled() {
        return getAllAttributesEnabled;
    }

    private Hashtable m_hashTable = new Hashtable();
}
