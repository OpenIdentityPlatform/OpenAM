/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ValidValues.java,v 1.2 2008/06/25 05:43:45 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import java.util.*;

/**
 * This class <code>ValidValues</code> provides search results and a error 
 * code indicating if the search was successfully or time limit exceeded or
 * search limit exceeded.
 *
 * @supported.all.api
 */
public class ValidValues {
    /**
     * Code used to indicate a successful search
     */   
    public static final int SUCCESS = 0;

    /**
     * Code used to indicate that the search was unsuccessful 
     *  as the size limit exceeded during the search process.
     */   
    public static final int SIZE_LIMIT_EXCEEDED = 1;

    /**
     * Code used to indicate that the search was unsuccessful 
     *  as the time limit exceeded during the search process.
     */   
    public static final int TIME_LIMIT_EXCEEDED = 2;

    Set searchResults = null; // set contains the result entries 
    int errorCode;

    /**
     * Constructs a <code>ValidValues</code> given <code>errorCode</code>
     * and a set of values 
     *
     * @param errorCode error code
     * @param results set of values to be included 
     *
     * @see #SUCCESS
     * @see #SIZE_LIMIT_EXCEEDED
     * @see #TIME_LIMIT_EXCEEDED
     */
    public ValidValues(int errorCode, Set results) {
        searchResults = results;
        this.errorCode = errorCode;
    }

    /**
     * Returns the search results as a set. 
     * @return set of entries matching the search criteria. Each element in the
     * Set is a String.
     */
    public Set getSearchResults() {
        return searchResults;
    }

    /**
     * Returns the error code of search. 
     * @return Error code of search. The possible values are 
     * <code>SUCCESS</code>, <code>SIZE_LIMIT_EXCEEDED</code>
     * and <code>TIME_LIMIT_EXCEEDED</code>
     * @see #SUCCESS
     * @see #SIZE_LIMIT_EXCEEDED
     * @see #TIME_LIMIT_EXCEEDED
     */
    public int getErrorCode() {
        return errorCode;
    }
}
