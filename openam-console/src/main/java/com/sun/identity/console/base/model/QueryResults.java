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
 * $Id: QueryResults.java,v 1.2 2008/06/25 05:42:50 qcheng Exp $
 *
 */

package com.sun.identity.console.base.model;

/**
 * This contains a search results, plus a error string. Typical use of this
 * class is when a method needs to return a result plus an exception message
 *
 * @author    vs125812
 * July 26, 2005
 */
public class QueryResults {
    private Object results;
    private String strError;

    /**
     * Creates a <code>QueryResults</code> object
     *
     * @param results   Description of the Parameter
     * @param strError  Description of the Parameter
     */
    public QueryResults(Object results, String strError) {
        this.results = results;
        this.strError = strError;
    }

    /**
     * Returns the value of error string.
     *
     * @return   value of error string.
     */
    public String getStrError() {
        return strError;
    }

    /**
     * Returns the value of results.
     *
     * @return   The results value
     */
    public Object getResults() {
        return results;
    }
}

