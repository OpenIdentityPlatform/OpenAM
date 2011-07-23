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
 * $Id: AMSearchControl.java,v 1.4 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Set;

import com.iplanet.ums.SearchControl;

/**
 * This class provides a way to customize Search behaviors. Common behaviors are
 * time limit, result limit and Virtual list view. In future, we will provide
 * ways for client to define different hierarchical tree through
 * <code>AMSearchControl</code>.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public class AMSearchControl {

    private SearchControl searchControl;

    private Set returnAttributes = null;

    // Disabled by default
    private static boolean getAllAttributesEnabled = false;

    /**
     * Creates the <code>AMSearchControl</code> object
     */
    public AMSearchControl() {
        searchControl = new SearchControl();
    }

    protected SearchControl getSearchControl() {
        return searchControl;
    }

    /**
     * Set the return attribute names, if attributes of the entries need to be
     * obtained as part of the search.
     * 
     * NOTE: If the return attribute values are specified as part of
     * <code>AMSearchControl</code>, there could be a significant performance
     * overhead compared to when none are specified. When the return attributes
     * are set, the return attributes can be obtained as a map with DN as
     * map-key and set of attribute values as map-value from
     * <code>AMSearchResults</code> object.
     * 
     * @param attributeNames
     *            Set of attribute names whose values need to be obtained as
     *            part of the search.
     * 
     */
    public void setReturnAttributes(Set attributeNames) {
        returnAttributes = attributeNames;
    }

    /**
     * Returns the list of attributes requested to be read when the search is
     * performed.
     * 
     * @return list of attributes requested to be read.
     */
    public String[] getReturnAttributes() {
        String returnAttrs[] = null;
        if (returnAttributes != null && !returnAttributes.isEmpty()) {
            returnAttrs = (String[]) returnAttributes
                    .toArray(new String[returnAttributes.size()]);

        }
        return returnAttrs;
    }

    /**
     * Sets the specified boolean value to the variable. Boolean value is set to
     * true, if all attributes of the entries need to be obtained as part of the
     * search.
     * 
     * NOTE: If this <code>getAllReturnAttributes</code> boolean is set to
     * true as part of <code>AMSearchControl</code>, it overrides any other
     * <code>setReturnAttributes</code> set as part of the
     * <code>AMSearchControl</code>. This is similar to using a wildcard '*'
     * in search.
     * 
     * When the option for getting all attributes is set to true, the search
     * results will return a Map, where the Key is the DN of the search results,
     * and value is another Map of attribute names for keys and Sets for values
     * of those attributes.
     * 
     * @param getAllAttributes
     *            Boolean value set to true as part of the
     *            <code>AMSearchControl</code> to obtain all attributes as
     *            part of the search.
     * 
     * 
     */
    public void setAllReturnAttributes(boolean getAllAttributes) {
        SearchControl sc = getSearchControl();
        sc.setAllReturnAttributes(getAllAttributes);
        getAllAttributesEnabled = getAllAttributes;
    }

    /**
     * Returns true if the option for getting all attributes has been enabled.
     * 
     * @return true if the option for getting all attributes has been enabled.
     */
    public boolean isGetAllReturnAttributesEnabled() {
        return getAllAttributesEnabled;
    }

    /**
     * Set sort order based on attribute names.
     * 
     * @param attributeNames
     *            array of attribute names to sort on
     * 
     */
    public void setSortKeys(String[] attributeNames) {
        searchControl.setSortKeys(attributeNames);
    }

    /**
     * Set range for retrieving VLV data.
     * 
     * @param startIndex
     *            starting position.
     * @param beforeCount
     *            Number of entries before the <code>startIndex</code>.
     * @param afterCount
     *            Number of entries after the <code>startIndex</code>.
     */
    protected void setVLVRange(int startIndex, int beforeCount, int afterCount) 
    {
        searchControl.setVLVRange(startIndex, beforeCount, afterCount);
    }

    /**
     * Set range for retrieving VLV data.
     * 
     * @param jumpTo
     *            Search expression defining the result set return.
     * @param beforeCount
     *            Number of entries before the <code>startIndex</code>.
     * @param afterCount
     *            Number of entries after the <code>startIndex</code>.
     */
    protected void setVLVRange(String jumpTo, int beforeCount, int afterCount) 
    {
        searchControl.setVLVRange(jumpTo, beforeCount, afterCount);
    }

    /**
     * Get range for current VLV setting.
     * 
     * @return array of integers which contain <code>startIndex</code>,
     *         <code>beforeCount</code> and <code>afterCount</code>.
     */
    protected int[] getVLVRange() {
        return searchControl.getVLVRange();
    }

    /**
     * Returns <code>jumpTo</code> value for VLV range.
     * 
     * @return <code>jumpTo</code> value.
     * 
     */
    protected String getVLVJumpTo() {
        return searchControl.getVLVJumpTo();
    }

    /**
     * Sets the maximum number of milliseconds to wait for any operation for the
     * search.
     * 
     * @param timeOut
     *            Max number of milliseconds
     * 
     */
    public void setTimeOut(int timeOut) {
        searchControl.setTimeOut(timeOut);
    }

    /**
     * Returns current time out setting.
     * 
     * @return current time out setting.
     */
    public int getTimeOut() {
        return searchControl.getTimeOut();
    }

    /**
     * Sets the maximum number of search results to return; 0 means there is no
     * limit.
     * 
     * @param maxNumber
     *            Max number of results
     */
    public void setMaxResults(int maxNumber) {
        searchControl.setMaxResults(maxNumber);
    }

    /**
     * Returns the maximum number of search results. return 0 means there is no
     * limit.
     * 
     * @return the maximum number of search results.
     */
    public int getMaxResults() {
        return searchControl.getMaxResults();
    }

    /**
     * Sets the search scope in <code>AMSearchControl</code>.
     * 
     * @param scope
     *            Search scope defined in the <code>AMSearchControl</code> to
     *            be used with the search API
     */
    public void setSearchScope(int scope) {
        searchControl.setSearchScope(scope);
    }

    /**
     * Gets the search scope defined in the <code>AMSearchControl</code>.
     * 
     * @return search scope defined in the <code>AMSearchControl</code>. If
     *         search scope is never defined in the
     *         <code>AMSearchControl</code> <code>SCOPE_SUB</code> for
     *         subtree type of search is assumed.
     */
    public int getSearchScope() {
        return searchControl.getSearchScope();
    }

    /**
     * Gets the search scope defined in the <code>AMSearchControl</code>.
     * Allows a user to specify default search scope if nothing has been defined
     * in the <code>AMSearchControl</code> yet.
     * 
     * @param defaultScope
     *            Scope value to be used in case the
     *            <code>AMSearchControl</code> is not set up with a search
     *            scope
     * 
     * @return Search scope defined in the <code>AMSearchControl</code>.
     *         Return <code>defaultScope</code> if scope is not defined in the
     *         control.
     */
    public int getSearchScope(int defaultScope) {
        return searchControl.getSearchScope(defaultScope);
    }
}
