/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmSubjectAsserter.java,v 1.3 2009/01/15 22:33:42 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import java.util.Hashtable;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.SSOValidationResult;

/**
 * Asserter class with subject for Websphere application server.
 */
public class AmSubjectAsserter extends AmIdentityAsserterBase {
    
    public AmSubjectAsserter(Manager manager) {
        super(manager);
    }
    
    protected TAIResult getAuthenticatedResult(
            HttpServletRequest request, HttpServletResponse response,
            SSOValidationResult ssoValidationResult)
            throws Exception {
        String userName = ssoValidationResult.getUserId();
        
        return TAIResult.create(HttpServletResponse.SC_OK, userName);
    }
}
