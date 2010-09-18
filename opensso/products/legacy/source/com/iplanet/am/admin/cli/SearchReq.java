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
 * $Id: SearchReq.java,v 1.2 2008/06/25 05:52:34 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;

abstract class SearchReq
    extends AdminReq
{
    protected int errorCode = AMSearchResults.SUCCESS;
    protected String filter = "*";
    protected int timeLimit = 0;
    protected int sizeLimit = 0;

    /**
     * Constructs a new <code>SearchReq</code>.
     *
     * @param targetDN container distinguished name.
     */
    SearchReq(String targetDN) {
        super(targetDN);
    }

    /**
     * Returns error code.
     *
     * @return error code.
     */
    int getErrorCode() {
        return errorCode;
    }

    /**
     * Set size limit for the search.
     *
     * @param limit Size limit for the search.
     * @throws AdminException if <code>limit</code> if not
     *         an integer.
     */
    void setSizeLimit(String limit)
        throws AdminException
    {
        if ((limit != null) && (limit.trim().length() > 0)) {
            try {
                sizeLimit = Integer.parseInt(limit.trim());
            } catch (NumberFormatException nfe) {
                throw new AdminException(nfe);
            }
        }
    }

    /**
     * Set time limit for the search.
     *
     * @param limit Time limit for the search.
     * @throws AdminException if <code>limit</code> if not
     *         an integer.
     */
    void setTimeLimit(String limit)
        throws AdminException
    {
        if ((limit != null) && (limit.trim().length() > 0)) {
            try {
                timeLimit = Integer.parseInt(limit.trim());
            } catch (NumberFormatException nfe) {
                throw new AdminException(nfe);
            }
        }
    }

    /**
     * Returns size limit for the search.
     *
     * @return size limit for the search.
     */
    int getSizeLimit() {
        return sizeLimit;
    }

    /**
     * Returns time limit for the search.
     *
     * @return time limit for the search.
     */
    int getTimeLimit() {
        return timeLimit;
    }

    /**
     * Set filter for the search.
     *
     * @param filter Filter for the search.
     */
    void setFilter(String filter) {
        if ((filter != null) && (filter.trim().length() > 0)) {
            this.filter = filter;
        }
    }

    /**
     * Returns filter for the search.
     *
     * @return Filter for the search.
     */
    String getFilter() {
        return filter;
    }

    AMSearchControl createSearchControl(int scope) {
        AMSearchControl searchControl = new AMSearchControl();
        searchControl.setSearchScope(scope);
        searchControl.setMaxResults(sizeLimit);
        searchControl.setTimeOut(timeLimit);
        return searchControl;
    }

    /**
     * Prints search limit error message if size or time limits
     * is reached.
     */
    void printSearchLimitError() {
        String msg = null;

        switch(errorCode) {
        case AMSearchResults.TIME_LIMIT_EXCEEDED:
            msg = bundle.getString("searchTimeLimitExceeded");
            break;
        case AMSearchResults.SIZE_LIMIT_EXCEEDED:
            msg = bundle.getString("searchSizeLimitExceeded");
            break;
        }

        if (msg != null) {
            System.out.println();
            System.out.println(msg);
            System.out.println();
        }
    }
}
