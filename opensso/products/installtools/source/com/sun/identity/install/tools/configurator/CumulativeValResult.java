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
 * $Id: CumulativeValResult.java,v 1.3 2008/06/25 05:51:18 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.HashMap;

import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * @author krishc
 *
 * Class to store the cumulative result from validators.
 * 
 */
public class CumulativeValResult {

    public CumulativeValResult() {
        setCumValResult(false);
        setCalcKeyValPairs(new HashMap());
    }

    public CumulativeValResult(boolean res, HashMap nameValpairs) {
        setCumValResult(res);
        setCalcKeyValPairs(nameValpairs);
    }

    void setCalcKeyValPairs(HashMap map) {
        calculatedKeys = map;
    }

    HashMap getCalcKeyValPairs() {
        return calculatedKeys;
    }

    void setCumValResult(boolean result) {
        cumValResStat = result;
    }

    boolean getCumValResult() {
        return cumValResStat;
    }

    void setErrorMessage(LocalizedMessage errorMsg) {
        errorMessage = errorMsg;
    }
    
    void setWarningMessage(LocalizedMessage warningMsg) {
        warningMessage = warningMsg;
    }

    LocalizedMessage getErrorMessage() {
        return errorMessage;
    }

    LocalizedMessage getWarningMessage() {
        return warningMessage;
    }
    
    private boolean cumValResStat;

    private HashMap calculatedKeys;

    private LocalizedMessage errorMessage;
    
    private LocalizedMessage warningMessage;

}
