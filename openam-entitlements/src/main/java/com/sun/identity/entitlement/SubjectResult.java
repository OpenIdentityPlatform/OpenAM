/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SubjectResult.java,v 1.1 2009/08/19 05:40:34 veiming Exp $
 */

package com.sun.identity.entitlement;

/**
 * Class to represent the result of <code>ESubject</code> evaluation
 */
public class SubjectResult {

    private boolean booleanResult;
    private String reason;

    /**
     * Constructor.
     * 
     * @param booleanResult boolean result value of the SubjectResult
     * @param reason reason for the result
     */
    public SubjectResult(boolean booleanResult, String reason) {
        this.booleanResult = booleanResult;
        this.reason = reason;
    }

    /**
     * Returns boolean result of  the SubjectResult value
     * @return boolean result of  the SubjectResult value
     */
    public boolean getBooleanResult() {
        return booleanResult;
    }

    /**
     * Returns reason for the SubjectResult value
     * @return reason for the SubjectResult value
     */
    public String getReason() {
        return reason;
    }

}
