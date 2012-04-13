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
 * $Id: IdSearchResults.java,v 1.4 2008/06/25 05:43:29 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.idm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class <code>IdSearchResults</code> provides to obtain the search
 * results.
 *
 * @supported.all.api
 */
public class IdSearchResults {

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

    // Ordered set contain the result identities
    protected Set searchResults = new HashSet(); 
    
    // Contains result of identities and the attributes requested as part
    // of the search
    protected Map resultsMap = new HashMap(); 
                                                
    protected int errorCode = SUCCESS;

    private IdType searchType;

    private String org;

    public IdSearchResults(IdType type, String orgName) {
        searchType = type;
        org = orgName;
    }

    /**
     * Method which returns the search results as a map containing AMIdentity
     * objects as key and the attribute value String. The attribute value is a
     * Set.
     * 
     * @return Map containing AMIdentity objects as the key and Maps of
     *         attribute-valuesof the attributes specified as part of the
     *         search. The Maps contains attribute names as keys and Set
     *         containing values of those attributes. Returns an empty Map if no
     *         attributes were specified as part of search request.
     */
    public Map getResultAttributes() {
        return resultsMap;
    }

    /**
     * Method which returns the search results as an ordered set.
     * 
     * @return Set of AMIdentity objects matching the search criteria
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

    /**
     * Adds an AMIdentity object to this search result.
     * 
     * @param id
     *            AMIdentity representing the entity.
     * @param attrs
     *            Map of attrbibutes obtained while performing the search
     */
    public void addResult(AMIdentity id, Map attrs) {
        searchResults.add(id);
        resultsMap.put(id, attrs);
    }

    /**
     * Set the error code for this Search Result
     * 
     * @param error
     *            Error code of Search Result.
     * @see #SUCCESS
     * @see #SIZE_LIMIT_EXCEEDED
     * @see #TIME_LIMIT_EXCEEDED
     * 
     */
    public void setErrorCode(int error) {
        errorCode = error;
    }

    protected IdType getType() {
        return searchType;
    }

    protected String getOrgName() {
        return org;
    }
    
    /**
     * Returns String representation of the <code>IdSearchResults</code> object.
     * It returns identity names and attributes
     *
     * @return String representation of the <code>ServiceConfig</code> object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("IdSearchResults:");
        sb.append("\n\tIdentities: ").append(searchResults);
        sb.append("\n\tAttributes: ").append(resultsMap);
        return (sb.toString());
    }
}
