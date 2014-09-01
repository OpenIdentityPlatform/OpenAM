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
 * $Id: IDPSSOUtil.java,v 1.3 2009/10/28 23:58:59 exu Exp $
 *
 */


package com.sun.identity.wsfederation.profile;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerEndpointElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * The utility class is used by the identity provider to process 
 * the authentication request from a service provider and send back
 * a proper response.
 * The identity provider can also send unsolicited response to a service
 * provider to do single sign on and/or federation.
 */
public class IDPSSOUtil {
    private static Debug debug = WSFederationUtils.debug;

    /**
     * Returns the authentication service <code>URL</code> of the 
     * identity provider
     *
     * @param realm the realm name of the identity provider
     * @param hostEntityId the entity id of the identity provider 
     * @param request the <code>HttpServletRequest</code> object 
     *
     * @return the authentication service <code>URL</code> of the 
     * identity provider
     */
    public static String getAuthenticationServiceURL(
                                          String realm,
                                          String hostEntityId,
                                          HttpServletRequest request) {
        String classMethod = "IDPSSOUtil.getAuthenticationServiceURL: ";

        // Try to get authUrl from system configuration
        String authUrl = SystemConfigurationUtil.getProperty(
             IFSConstants.IDP_LOGIN_URL);

        if ((authUrl == null) || (authUrl.trim().length() == 0)) {
            // It's not in the system config,
            // try to get it from IDP config
            try {
                IDPSSOConfigElement config = 
                    WSFederationUtils.getMetaManager().getIDPSSOConfig(
                        realm, hostEntityId);
                authUrl = WSFederationMetaUtils.getAttribute(config, 
                    SAML2Constants.AUTH_URL);
            } catch (WSFederationMetaException sme) {
                if (debug.messageEnabled()) {
                    debug.message(classMethod +  "get IDPSSOConfig failed:", sme);
                }
            }

            if (authUrl == null) {
                // It's not in IDP config
                // need to get it from the request
                String uri = request.getRequestURI();
                String deploymentURI = uri;
                int firstSlashIndex = uri.indexOf("/");
                int secondSlashIndex = uri.indexOf("/", firstSlashIndex+1);
                if (secondSlashIndex != -1) {
                    deploymentURI = uri.substring(0, secondSlashIndex);
                } 
                StringBuffer sb = new StringBuffer(100);
                sb.append(request.getScheme()).append("://")
                  .append(request.getServerName()).append(":")
                  .append(request.getServerPort())
                  .append(deploymentURI) 
                  .append("/UI/Login?realm=").append(realm);
                authUrl = sb.toString();
            }
        }
        
        if (debug.messageEnabled()) {
            debug.message(classMethod + "auth url=:" + authUrl);
        }
        return authUrl;
    }

    /**
     * Returns the assertion consumer service (ACS) URL for the entity.
     * @param entityId entity ID of provider
     * @param realm realm of the provider
     * @param wreply the ACSURL supplied by the requestor. If supplied, this is 
     * checked against the URLs registered for the provider.
     * @return assertion consumer service (ACS) URL for the entity.
     */
    public static String getACSurl(String entityId, String realm, 
        String wreply) throws WSFederationMetaException
    {
        WSFederationMetaManager metaManager = 
            WSFederationUtils.getMetaManager();
        FederationElement sp = 
            metaManager.getEntityDescriptor(realm, entityId);
        if ( wreply == null )
        {
            // Get first ACS URL for this SP
            return metaManager.getTokenIssuerEndpoint(sp);
        }
        else
        {
            // Check that wreply is registered on this SP
            // Just return first TokenIssuerEndpoint in the Federation
            for ( Object o: sp.getAny() )
            {
                if ( o instanceof TokenIssuerEndpointElement )
                {
                    try {
                        URL replyUrl = new URL(wreply);
                        URL thisUrl = new URL(
                          ((TokenIssuerEndpointElement)o).getAddress().
                          getValue());
                        if ( replyUrl.equals(thisUrl))
                            return wreply;
                    } catch (MalformedURLException mue) {
                        return null;
                    }
                }
            }
        }
        return null;
    }    
}
