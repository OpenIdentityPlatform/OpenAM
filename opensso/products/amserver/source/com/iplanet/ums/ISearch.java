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
 * $Id: ISearch.java,v 1.6 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.ums;

/**
 * Represents interface for search methods. Using ISearch, client can get
 * children, parent for the PersistentObject. It's main purpose is to find nodes
 * matching to the search criterial. Customized search behavior is set through
 * SearchControl. Results are captured in SearchResults. Each result is a
 * PersistentObject. Filter String is defined based on LDAP search filter syntax
 * (ftp://ftp.isi.edu/in-notes/rfc2254.txt)
 * 
 * <p>
 * Examples:
 * <p>
 * To modify a user's telphone number
 * <p>
 * 
 * <pre>
 *       SearchResults searchResults = 
 *              organization.search(
 *                    &quot;(uid=smith)&quot;, { &quot;telephone&quot; }, null);
 *       if (searchResults.hasMoreElements() == true) {
 *                  PersistentObject userSmith = searchResults.next();
 *           userSmith.modify(&quot;telephone&quot;, &quot;408-888-8888&quot;);
 *           userSmith.save();
 *             }
 * </pre>
 * 
 * <p>
 * To read data using VLV
 * <p>
 * 
 * <pre>
 *       SearchControl searchControl = new SearchControl();
 * 
 *       pageSize = 20;
 *       startPos = 1;
 *       for (pageIndex = 0; pageIndex &lt; 10; pageIndex ++) {
 *           searchControl.setVLVRange(startPos,  0, pageSize);
 *           searchControl.setSortAttributeNames( { &quot;cn&quot; } );
 *               SearchResults searchResults = 
 *                 organization.search(&quot;(department=sun)&quot;,
 *                        { &quot;cn&quot;, &quot;sn&quot;, &quot;uid&quot; }, 
 *                              searchControl);
 *           while (searchResults.hasMoreElements() == true) {
 *                      PersistentObject user = searchResults.next();
 *               display(user);
 *                 }
 *           startPos = startPos + pageSize + 1;
 *       }
 * </pre>
 * 
 * @see com.iplanet.ums.SearchControl
 * 
 * @supported.all.api
 */
public interface ISearch {

    /**
     * Find all IDs for immediate children under current node based on search
     * criteria specified in filter. Search behavior is controlled by
     * searchControl.
     * 
     * @param filter
     *            search filter
     * @param searchControl
     *            search control, use default setting if searchControl == null
     * @exception InvalidSearchFilterException
     *                if invalid search filter.
     * @exception UMSException
     *                if no result matches.
     */
    public SearchResults getChildren(String filter, SearchControl searchControl)
            throws InvalidSearchFilterException, UMSException;

    /**
     * Search entries on immediate children based on criteria specified in
     * filter. Each result contains values for given attribute names. Search
     * behavior is controlled by searchControl.
     * 
     * @param filter
     *            search filter
     * @param resultAttributeNames
     *            attribute name array
     * @param searchControl
     *            search control, use default setting if searchControl == null
     * @exception InvalidSearchFilterException
     *                if invalid search filter.
     * @exception UMSException
     *                if no result matches.
     */
    public SearchResults getChildren(String filter,
            String[] resultAttributeNames, SearchControl searchControl)
            throws InvalidSearchFilterException, UMSException;

    /**
     * Find all IDs for immediate children under current node based on search
     * criteria specified in template, and return attributes specified there.
     * Search behavior is controlled by searchControl.
     * 
     * @param template
     *            search template
     * @param searchControl
     *            search control, use default setting if searchControl == null
     */
    public SearchResults getChildren(SearchTemplate template,
            SearchControl searchControl) throws UMSException;

    /**
     * Search (subtree) entry IDs under currrent node based on criteria
     * specified in search filter and searchControl
     * 
     * @param filter
     *            search filter
     * @param searchControl
     *            search control, use default setting if searchControl == null
     * @exception com.iplanet.ums.UMSException
     * @return An search result class for reading IDs.
     * @exception InvalidSearchFilterException
     *                if search filter is invalid.
     * @exception UMSException
     *                if no result matches.
     */
    public SearchResults search(String filter, SearchControl searchControl)
            throws InvalidSearchFilterException, UMSException;

    /**
     * Search (subtree) entries under current node based on criteria specified
     * in filter. Search behavior is controlled by searchControl. Each result
     * contains values for given attribute names.
     * 
     * @param filter Search filter.
     * @param resultAttributeNames Attribute name array for retrieving.
     * @param searchControl Search control, use default setting if
     *        searchControl is <code>null</code>.
     * @return An search result class for reading entries.
     * @exception InvalidSearchFilterException
     * @exception UMSException
     */
    public SearchResults search(
        String filter,
        String resultAttributeNames[],
        SearchControl searchControl
    ) throws InvalidSearchFilterException, UMSException;

    /**
     * Search (subtree) entries under current node based on criteria specified
     * in template, which also indicates which attributes to return. Search
     * behavior is controlled by searchControl.
     * 
     * @param template
     *            search template
     * @param searchControl
     *            search control, use default setting if searchControl == null
     * @exception com.iplanet.ums.UMSException
     */
    public SearchResults search(SearchTemplate template,
            SearchControl searchControl) throws UMSException;

    /**
     * Search for immediate parent ID
     * 
     * @param searchControl
     *            search control, use default setting if searchControl == null
     * @return null if current node is root public String
     *         getParentID(SearchControl searchControl);
     */

    /**
     * Search for immediate parent ID
     * 
     * @return null if current node is root
     */
    public Guid getParentGuid();
}
