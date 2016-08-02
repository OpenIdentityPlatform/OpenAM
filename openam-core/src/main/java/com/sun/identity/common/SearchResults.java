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
 * $Id: SearchResults.java,v 1.2 2008/06/25 05:42:26 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.common;

import java.util.List;

/**
 * This class encapsulates search results data. 
 */
public class SearchResults<T> {

    /**
     * Successful search.
     */   
    public static final int SUCCESS = 0;

    /**
     * Search was unsuccessful because the size limit is reached.
     */   
    public static final int SIZE_LIMIT_EXCEEDED = 1;

    /**
     * Search was unsuccessful because the time limit is reached.
     */   
    public static final int TIME_LIMIT_EXCEEDED = 2;

    /**
     * Search size when search fails.
     */
    public static final int UNDEFINED_RESULT_COUNT = -1;

    /**
     * Total result size.
     */
    private final int count;

    /**
     * Set of search results.
     */
    private final List<T> searchResults;

    /**
     * Error Code.
     */
    private final int errorCode;

    /**
     * Constructs the <code>SearchResults</code> object.
     *
     * @param count Number of entries which match the search criteria.
     * @param results Set of <code> DNs </code> from the search.
     * @param errorCode Error Code.
     */
    public SearchResults(int count, List<T> results, int errorCode) {
        this.count = count;
        searchResults = results;
        this.errorCode = errorCode;
    }

    /**
     * Returns the search results. 
     *
     * @return Search results.
     */
    public List<T> getSearchResults() {
        return searchResults;
    }

    /**
     * Returns the size of all results which match the search criteria. 
     * This number may be different from the size of set of search results
     * which can be retrieved from <code>getSearchResults()</code> method.
     * They are different when the size/time limits is reached.
     *
     * @return size of results matching the search. Returns
     *         <code>UNDEFINED_RESULT_COUNT</code> if search fails.
     */
    public int getTotalResultCount() {
        return count;
    }

    /**
     * Returns the error code of search. 
     *
     * @return Error code of search.
     */
    public int getErrorCode() {
        return errorCode;
    }
}
