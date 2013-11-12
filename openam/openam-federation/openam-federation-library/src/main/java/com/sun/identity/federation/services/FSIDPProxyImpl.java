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
 * $Id: FSIDPProxyImpl.java,v 1.3 2008/06/25 05:46:54 qcheng Exp $
 *
 */


package com.sun.identity.federation.services;

import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSRedirectException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * This class <code>FSIDPProxyImpl</code> is used to find a preferred Identity
 * Authenticating provider to proxy the authentication request.
 * @deprecated
 * @see com.sun.identity.federation.services.FSRealmIDPProxyImpl
 */ 
public class FSIDPProxyImpl implements FSIDPProxy {


    /**
     * Default Constructor.
     */
    public FSIDPProxyImpl(){}

    /**
     * Returns the preferred IDP.
     * @param authnRequest original authnrequest
     * @param hostEntityID ProxyIDP entity ID.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @return providerID of the authenticating provider to be proxied.
     * @exception FSRedirectException if redirect was done
     */
    public String getPreferredIDP(
        FSAuthnRequest authnRequest, 
        String hostEntityID,
        HttpServletRequest request,
        HttpServletResponse response)
        throws FSRedirectException 
    {
        
        FSUtils.debug.message("FSIDPProxyImpl.getPreferredIDP:Init");
        try {
            Map attributes = IDFFMetaUtils.getAttributes(
                FSUtils.getIDFFMetaManager().getSPDescriptorConfig(
                    "/", authnRequest.getProviderId()));
            String useIntroductionForProxying = 
                IDFFMetaUtils.getFirstAttributeValue(
                    attributes, IFSConstants.USE_INTRODUCTION_FOR_IDP_PROXY);
            if (useIntroductionForProxying == null ||
                !useIntroductionForProxying.equals("true")) 
            {
                List proxyIDPs = (List) attributes.get(
                    IFSConstants.IDP_PROXY_LIST);
                if (proxyIDPs == null || proxyIDPs.isEmpty()) {
                    FSUtils.debug.error("FSIDPProxyImpl.getPrefferedIDP:" +
                        "Preferred IDPs are null.");
                    return null;
                }
                return (String)proxyIDPs.iterator().next();
            } else {
                StringBuffer redirectURL = new StringBuffer(100);
                String baseURL = FSServiceUtils.getBaseURL(request);
                redirectURL.append(baseURL).append(IFSConstants.IDP_FINDER_URL)
                        .append("?").append("RequestID=")
                        .append(authnRequest.getRequestID())
                        .append("&").append("ProviderID=")
                        .append(hostEntityID);
                FSUtils.forwardRequest(
                    request, response, redirectURL.toString());
                throw new FSRedirectException(FSUtils.bundle.getString(
                    "Redirection_Happened"));
            }
        } catch (IDFFMetaException ex) {
            FSUtils.debug.error("FSIDPProxyImpl.getPreferredIDP: " +
                "meta Exception in retrieving the preferred IDP", ex);
            return null;
        } catch (Exception e) {
            FSUtils.debug.error("FSIDPProxyImpl.getPreferredIDP: " +
                "Exception in retrieving the preferred IDP", e);
            return null;
        }
    }
}
