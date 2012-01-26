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
 * $Id: IAmIdentityAsserter.java,v 1.2 2008/11/21 22:21:46 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.security.tai.TAIResult;
import com.sun.identity.agents.arch.AgentException;

/**
 * Asserter interface.
 */
public interface IAmIdentityAsserter {
    
    public void initialize() throws AgentException;
    
    public boolean needToProcessRequest(HttpServletRequest request)
    throws WebTrustAssociationException;
    
    public TAIResult processRequest(HttpServletRequest request,
            HttpServletResponse response)
            throws WebTrustAssociationFailedException ;
}
