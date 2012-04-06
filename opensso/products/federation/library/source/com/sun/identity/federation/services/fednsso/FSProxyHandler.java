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
 * $Id: FSProxyHandler.java,v 1.3 2008/06/25 05:46:58 qcheng Exp $
 *
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.saml.assertion.NameIdentifier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class <code>FSProxyHandler</code> handles the single sign-on requests
 * by a proxy identity provider. This class will be invoked by an identity
 * provider that is also acting as a proxy and needs to handle
 * browser artifact and post profiles.
 */
public class FSProxyHandler extends FSSSOAndFedHandler {

    /**
     * Constructor.
     * This constructor is primarily used by the proxying identity provider
     * which acts as a service provider for handling single sign-on requests. 
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     * @param authnRequest original authentication request that is issued
     *        by the service provider.
     * @param spDescriptor requesting service provider descriptor.
     * @param spConfig requesting service provider's extended meta Config
     * @param spEntityId requesting service provider's entity id
     * @param relayState targetURL to be redirected.
     * @param ssoToken credentials of a user at a proxy identity provider.
     */
    public FSProxyHandler(
             HttpServletRequest request,
             HttpServletResponse response, 
             FSAuthnRequest authnRequest,
             SPDescriptorType spDescriptor,
             BaseConfigType spConfig,
             String spEntityId,
             String relayState,
             Object ssoToken
    ) 
    {
       super(request, response, authnRequest, spDescriptor, 
             spConfig, spEntityId, relayState, ssoToken);
    }

    /**
     * Constructor.
     * This constructor is primarily used by the identity provider to 
     * send a proxy authentication request.
     * @param request <code>HttpServletRequest</code> object
     * @param response <code>HttpServletResponse</code> object
     */
    public FSProxyHandler(
        HttpServletRequest request,
        HttpServletResponse response) 
    {
        this.request = request;
        this.response = response;
    }
    
    /**
     * Does the single sign-on in a proxy IDP with the
     * requesting service provider.
     * @param ssoToken credentials of the user
     * @param inResponseTo <code>InResponseTo</code> attribute of the request.
     * @param spNameIdentifier <code>SP</code> Provided NameIdentifier.
     * @param idpNameIdentifier <code>IDP</code> Provided NameIdentifier.
     * @return boolean <code>true</code> if successful.
     */
    public boolean doSingleSignOn(
        Object ssoToken,
        String inResponseTo,
        NameIdentifier spNameIdentifier,
        NameIdentifier idpNameIdentifier)
    {
        FSUtils.debug.message("FSProxyHandler.doSingleSignOn:Init");
        String protocolProfile = authnRequest.getProtocolProfile();
        if (protocolProfile == null ||
            protocolProfile.equals(IFSConstants.SSO_PROF_BROWSER_ART)) 
        {

            FSSSOBrowserArtifactProfileHandler handler =
                new FSSSOBrowserArtifactProfileHandler(
                    request, response, authnRequest, spDescriptor, 
                    spConfig, spEntityId, relayState);
            handler.setHostedEntityId(hostedEntityId);
            handler.setHostedDescriptor(hostedDesc);
            handler.setHostedDescriptorConfig(hostedConfig);
            handler.setMetaAlias(metaAlias);
            handler.setRealm(realm);

            return handler.doSingleSignOn(
                ssoToken, inResponseTo, spNameIdentifier, idpNameIdentifier);

        } else if (protocolProfile.equals(IFSConstants.SSO_PROF_BROWSER_POST)) {
            FSSSOBrowserPostProfileHandler handler =
                new FSSSOBrowserPostProfileHandler(
                    request, response, authnRequest, spDescriptor,
                    spConfig, spEntityId, relayState);
            handler.setHostedEntityId(hostedEntityId);
            handler.setHostedDescriptor(hostedDesc);
            handler.setHostedDescriptorConfig(hostedConfig);
            handler.setMetaAlias(metaAlias);
            handler.setRealm(realm);

            return handler.doSingleSignOn(
                ssoToken, inResponseTo, spNameIdentifier, idpNameIdentifier);

        } else {
            FSUtils.debug.error("FSProxyHandler.doProxySingleSignOn:" +
                "Unsupported protocol profile.");
            return false;
        }
    }
    
}
