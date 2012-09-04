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
 * $Id: AmWebPolicyResult.java,v 1.2 2008/06/25 05:51:57 qcheng Exp $
 *
 */

package com.sun.identity.agents.policy;

import java.util.Map;

import com.sun.identity.agents.util.NameValuePair;

/**
 *  The class represents a web policy evaluation result
 */
public class AmWebPolicyResult {
    
    public AmWebPolicyResult() {
        this(AmWebPolicyResultStatus.STATUS_DENY, EMPTY_NVP);
    }
    
    public AmWebPolicyResult(AmWebPolicyResultStatus status) {
        this(status, EMPTY_NVP);
    }
    
    public AmWebPolicyResult(AmWebPolicyResultStatus status, 
            NameValuePair[] nvp) 
    {
        setPolicyResultStatus(status);
        setNameValuePairs(nvp);
    }
    
    public void setResponseAttributes(Map responseAttributes) {
        _responseAttributes = responseAttributes;
    }
    
    public AmWebPolicyResultStatus getPolicyResultStatus() {
        return _resultStatus;
    }
    
    public boolean hasNameValuePairs() {
        return (getNameValuePairs() != null && getNameValuePairs().length > 0);
    }
    
    public NameValuePair[] getNameValuePairs() {
        return _nvp;
    }
    
    public Map getResponseAttributes() {
        return _responseAttributes;
    }
    
    private void setPolicyResultStatus(AmWebPolicyResultStatus resultStatus) {
        if (resultStatus != null) {
            _resultStatus = resultStatus;
        } else {
            _resultStatus = AmWebPolicyResultStatus.STATUS_DENY;
        }
    }
    
    private void setNameValuePairs(NameValuePair[] nvp) {
        if (nvp != null) {
            _nvp = nvp;
        } else {
            _nvp = EMPTY_NVP;
        }
    }

    private NameValuePair[] _nvp;
    private AmWebPolicyResultStatus   _resultStatus;
    private Map _responseAttributes;

    public static final NameValuePair[] EMPTY_NVP = new NameValuePair[] { };
    public static final AmWebPolicyResult DENY = new AmWebPolicyResult();
}
