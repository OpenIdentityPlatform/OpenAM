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
 * $Id: FSIDPProxy.java,v 1.3 2008/06/25 05:46:54 qcheng Exp $
 *
 */


package com.sun.identity.federation.services;

import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.common.FSRedirectException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interface <code>FSIDPProxy</code> is used to find a preferred Identity
 * Authenticating provider to proxy the authentication request.
 * @supported.all.api
 * @deprecated This SPI is deprecated.
 * @see com.sun.identity.federation.services.FSRealmIDPProxy
 */ 
public interface FSIDPProxy {

    /**
     * Returns the preferred IDP.
     * @param authnRequest original authnrequest
     * @param hostProviderID ProxyIDP providerID.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @return providerID of the authenticating provider to be proxied.
     *  <code>null</code> to disable the proxying and continue for the local 
     *  authenticating provider. 
     * @exception FSRedirectException if redirect was done
     * @deprecated This method is deprecated.
     * @see com.sun.identity.federation.services.FSRealmIDPProxy
     */
    public String getPreferredIDP (
          FSAuthnRequest authnRequest, 
          String hostProviderID,
          HttpServletRequest request,
          HttpServletResponse response 
    ) throws FSRedirectException;
}
