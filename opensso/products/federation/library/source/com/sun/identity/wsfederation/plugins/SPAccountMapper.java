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
 * $Id: SPAccountMapper.java,v 1.5 2008/08/19 19:11:16 veiming Exp $
 *
 */


package com.sun.identity.wsfederation.plugins;

import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.profile.RequestSecurityTokenResponse;

/**
 * The class <code>PartnerAccountMapper</code> is an interface
 * that is implemented to map partner account to user account
 * in OpenSSO.  
 * <p>
 * Different partner would need to have a different implementation
 * of the interface. The mappings between the partner source ID and 
 * the implementation class are configured at the <code>Partner URLs</code> 
 * field in SAML service.
 *
 * @supported.all.api
 */

public interface SPAccountMapper {
    /**
     * Returns user's distinguished name or the universal ID for the
     * RSTR. This method will be invoked by the WS-Federation framework 
     * while processing the <code>RequestSecurityTokenResponse</code> and 
     * retrieves the identity information. 
     *
     * @param rstr the incoming <code>RequestSecurityTokenResponse</code>
     * @param hostEntityID <code>EntityID</code> of the hosted provider.
     * @param targetURL final target URL.
     * @return user's disntinguished name or the universal ID.
     * @exception WSFederationException if any failure.
     */
    public String getIdentity(RequestSecurityTokenResponse rstr,
        String hostEntityID, String targetURL) throws WSFederationException;
}
