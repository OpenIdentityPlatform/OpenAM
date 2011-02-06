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
 * $Id: SearchModel.java,v 1.2 2008/06/25 05:42:57 qcheng Exp $
 *
 */

package com.sun.identity.console.dm.model;

import java.util.Set;
import java.util.Map;
import com.sun.identity.console.base.model.AMConsoleException;

/* - NEED NOT LOG - */

/**
 * <code>SearchModel</code> defines a set of methods required by role
 * navigation view bean.
 */
public interface SearchModel
    extends DMModel
{
    public static final String SEARCH_TYPE = "SearchType";
    public static final String BACK_VB_NAME = "BackButtonViewBean";
    public static final String GROUP_SEARCH = "Group";
    public static final String SEARCH_SCOPE = "searchScope";
    public static final String GROUP_NAME = "groupName";

    public static final int SEARCH = 0;
    public static final int MEMBERSHIP = 1;

    public Map getDataMap();
    public String getSearchXML();

    public Map getGroupDataMap();
    public String getGroupSearchXML();

    public void addMembers(String entry, Set groups, String parentType)
         throws AMConsoleException;

    /**
     * Searches for users
     *
     * @param logicalOp logical operator
     * @param scope scope level
     * @param avPairs attribute value pairs
     * @return a set of users which match the search criterias
     */
    public Set searchUsers(String logicalOp, String scope, Map avPairs);

    /**
     * Searches for users with a default search scope of subtree
     *
     * @param logicalOp logical operator
     * @param avPairs attribute value pairs
     * @param location where to search for users
     * @return a set of users which match the search criterias
     */
    public Set searchUsers(String logicalOp, Map avPairs, String location);
    
    public Set searchGroups(String location, Map avPairs);

    /**
     * Gets the search results map. Key is the user DN, value is the user return
     * attribute.
     *
     * @return Map of search results
     */    
    public Map getResultsMap();

    public String getError();

    /**
     * Gets search type
     *
     * @return search type
     */
    public int getSearchType();

    /**
     * Set search type
     *
     * @param type of search
     */
    public void setSearchType(int type);

    /**
     * Determines whether the User service is denied to the user
     * accessing the console.
     *
     * @return true if the User service is denied.
     */
    public boolean isUserServiceDenied();

    /**
     * Gets the localized message of the service denied dialog.
     *
     * @return the message
     */
    public String getNoServiceAccessMessage();

    /**
     * Gets the localized message for no search attributes.
     *
     * @return the message
     */
    public String getNoAttributeAccessMessage();

    /**
     * Gets no search result message
     *
     * @return no search result message
     */
    public String getNoMatchMsg();
   
    /**
     * Gets warning title
     *
     * @return warning title
     */
    public String getWarningTitle();

    /**
     * Checks if time or size limit error occurred.
     *
     * @return true if time or size limit reach
     */
    public boolean isTimeSizeLimit();

    /**
     * Returns the value for the specified attribute.
     *
     * @param key attribute name.
     * @return values for the attribute.
     */
    public Set getAttrValue(String key);

    /**
     * Store the map of attribute name and values in the model.
     *
     * @param valueMap of attribute name to values.
     */
    public void setAttrValues(Map valueMap);
}

