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
 * $Id: RepoSearchResults.java,v 1.3 2008/06/25 05:43:29 qcheng Exp $
 *
 */

package com.sun.identity.idm;

import java.util.Map;
import java.util.Set;

/**
 * This class <code>RepoSearchResults</code> provides to obtain the search
 * results.
 */
public class RepoSearchResults {

    /**
     * Code used to indicate a successful search
     */
    public static final int SUCCESS = 0;

    /**
     * Code used to indicate that the search was unsuccessful as the size limit
     * exceeded during the search process.
     */
    public static final int SIZE_LIMIT_EXCEEDED = 1;

    /**
     * Code used to indicate that the search was unsuccessful as the time limit
     * exceeded during the search process.
     */
    public static final int TIME_LIMIT_EXCEEDED = 2;

    /**
     * Value returned when the total number of search results could not be
     * obtained.
     */
    public static final int UNDEFINED_RESULT_COUNT = -1;

    protected Set searchResults = null; // Ordered set contain the

    // result identities
    protected Map resultsMap = null; // Contains result of identities and

    // the attributes requested as part
    // of the search
    protected int errorCode;

    private IdType searchType;

    /**
     * Constructs the <code>RepoSearchResults</code> object.
     * 
     * @param type 
     *            search for <code> IdType </code>
     * @param results
     *            Set of <code> names </code> from the search
     * @param errorCode
     *            Error Code
     * @param map
     *            Map of attributes and values if requested for
     */
    public RepoSearchResults(Set results, int errorCode, Map map, IdType type) {
        searchResults = results;
        this.errorCode = errorCode;
        resultsMap = map;
        searchType = type;
    }

    /**
     * Method which returns the search results as a map containing AMIdentity
     * objects as key and the attribute value String. The attribute value is a
     * Set.
     * 
     * @return Map containing DN's as the key and Maps of attribute-values of
     *         the attributes specified as part of the search. The Maps contains
     *         attribute names as keys and Set containing values of those
     *         attributes. Returns an empty Map if no attributes were specified
     *         as part of search request.
     */
    public Map getResultAttributes() {
        return resultsMap;
    }

    /**
     * Method which returns the search results as an ordered set.
     * 
     * @return Set of DNs of matching the search criteria
     */
    public Set getSearchResults() {
        // return convertToIdentityObjects();
        return searchResults;
    }

    /**
     * Method which returns the error code of search.
     * 
     * @return Error code of search. The possible values are
     *         <code>SUCCESS</code>, <code>SIZE_LIMIT_EXCEEDED</code> and
     *         <code>TIME_LIMIT_EXCEEDED</code>
     * @see #SUCCESS
     * @see #SIZE_LIMIT_EXCEEDED
     * @see #TIME_LIMIT_EXCEEDED
     */
    public int getErrorCode() {
        return errorCode;
    }

    public IdType getType() {
        return searchType;
    }

}
