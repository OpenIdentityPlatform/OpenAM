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
 * $Id: AMSearchResults.java,v 1.4 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

/**
 * This class <code>AMSearchResults</code> provides to obtain the search
 * results.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public class AMSearchResults {

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

    protected int vlvResponseCount; // Holds the total result count

    protected Set searchResults = null; // Ordered set contain the

    // result DN's
    protected Map resultsMap = null; // Contains result of DN's and

    // the attributes requested as part
    // of the search
    protected int errorCode;

    /**
     * Constructs the <code>AMSearchResults</code> object.
     * 
     * @param count
     *            Number of entries
     * @param results
     *            Set of <code> DNs </code> from the search
     * @param errorCode
     *            Error Code
     * @param map
     *            Map of attributes and values if requested for
     */
    public AMSearchResults(int count, Set results, int errorCode, Map map) {
        vlvResponseCount = count;
        searchResults = results;
        this.errorCode = errorCode;
        resultsMap = map;
    }

    /**
     * Method which returns the search results as a map containing DN's as key
     * and the attribute value String. The attribute value
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
        return searchResults;
    }

    /**
     * Method which returns the count of the all the results which match the
     * search criteria.
     * 
     * @return total count of results matching the VLV search. Other wise
     *         returns UNDEFINED_RESULT_COUNT
     * @see #UNDEFINED_RESULT_COUNT
     */
    public int getTotalResultCount() {
        return vlvResponseCount;
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
}
