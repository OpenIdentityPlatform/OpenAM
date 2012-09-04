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
 *
 */

package com.sun.identity.agents.common;

import java.util.List;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.federation.common.IFSConstants;

/**
 * @author 
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface ILibertyAuthnResponseHelper {
    public abstract void initialize(int skewFactor);

    /**
     * Gets the encrypted SSOToken string.
     * 
     * <p> This method first validates the AuthnResponse to verify its 
     * authenticity. The verification process includes validating the requestID,
     * the response status, issuer's authenticity and the assertion conditions.      
     *
     * @param requestID the Liberty AuthnRequest's requestID
     * @param trustedIDProvider the identity provider URL (Identity Server Login
     * URL which when configured for CDSSO will be the CDC Servlet URL)
     * @param serviceProvider the service provider URL which is the URL of the
     * agent protected application.
     * 
     * @throws AgentException if the validation fails.   
     */
    public abstract String getSSOTokenString(String encodedAuthnResponse,
            String requestID, List trustedIDProviders, String serviceProvider)
            throws AgentException;
    
    public static final String AUTHN_PARAM_NAME = 
        IFSConstants.POST_AUTHN_RESPONSE_PARAM;
}
