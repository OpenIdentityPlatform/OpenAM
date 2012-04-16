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
 * $Id: AmTrustAssociationInterceptor.java,v 1.2 2008/11/21 22:21:45 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 * This is customized IBM TAI class for Websphere agent.
 */

public class AmTrustAssociationInterceptor implements
		TrustAssociationInterceptor {

	/**
	 * This method will return true if this TAI should handle this
	 * request; false tells WebSphere Application Server to ignore 
	 * this TAI.
	 * 
	 * @param request the incoming HTTP servlet request
	 * @return true if the request must be handled by this interceptor,
	 * 			false otherwise.
	 * @throws WebTrustAssociationException if a processing error occurs
	 */			
	public boolean isTargetInterceptor(HttpServletRequest request)
			throws WebTrustAssociationException {
		IAmIdentityAsserter identityAsserter = 
			AmWebsphereManager.getAmIdentityAsserterInstance();
		return identityAsserter.needToProcessRequest(request);
	}

	/**
	 * This method returns a TAIResult object which indicates the 
	 * status of the request that is being processed. The HTTP response
	 * object can be modified if needed.
	 * 
	 * @param request the incoming HTTP servlet request
	 * @param response the corresponding HTTP servlet response
	 * @return the <code>TAIResult</code> instance indicating the followup
	 *         action to be taken by WAS security system.
	 */
	public TAIResult negotiateValidateandEstablishTrust(
                    HttpServletRequest request, HttpServletResponse response)
                    throws WebTrustAssociationFailedException {
		IAmIdentityAsserter identityAsserter = 
			AmWebsphereManager.getAmIdentityAsserterInstance();
		return identityAsserter.processRequest(request, response);
	}

	public int initialize(Properties arg0)
			throws WebTrustAssociationFailedException {
		// FIXME
		return 0;
	}

	public String getVersion() {
		// FIXME - see if the agent version can be passed back
		return "3.0";
	}

	public String getType() {
		return this.getClass().getName();
	}

	public void cleanup() {
		// No cleanup required
	}
}
