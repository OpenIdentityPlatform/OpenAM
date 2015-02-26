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
 * $Id: IPRPSignoutRequest.java,v 1.7 2009/10/28 23:59:00 exu Exp $
 *
 */

package com.sun.identity.wsfederation.servlet;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.logging.LogUtil;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class implements the sign-out request for both identity provider and
 * service provider.
 */
public class IPRPSignoutRequest extends WSFederationAction {
    private static Debug debug = WSFederationUtils.debug;

    private String wreply;

    /**
     * Creates a new instance of <code>RPSigninRequest</code>.
     *
     * @param request HTTPServletRequest for this interaction
     * @param response HTTPServletResponse for this interaction
     * @param wreply Reply URL, to which control is to be transferred
     * on successful completion of sign-out. May be null, in which case, control
     * stays with the provider.
     */
    public IPRPSignoutRequest(HttpServletRequest request,
        HttpServletResponse response, String wreply) {
        super(request,response);
        this.wreply = wreply;
    }

    /**
     * Processes the sign-out request, returning a response via the 
     * HttpServletResponse passed to the constructor.
     */
    public void process() throws IOException, WSFederationException
    {
        String classMethod = "IPRPSignoutRequest.process: ";

        String metaAlias = WSFederationMetaUtils.getMetaAliasByUri(
                                            request.getRequestURI());
        if ((metaAlias == null) || (metaAlias.trim().length() == 0)) {
            debug.error(classMethod + "Unable to get meta alias from request");
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("MetaAliasNotFound"));
        }

        String realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
        if ((realm == null) || (realm.trim().length() == 0)) {
            debug.error(classMethod + "Unable to get realm from request");
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("nullRealm"));
        }
        
        WSFederationMetaManager metaManager = 
            WSFederationUtils.getMetaManager();
        // retrieve entity id from meta alias            
        String entityId = metaManager.getEntityByMetaAlias(metaAlias);
        if ((entityId == null) || (entityId.trim().length() == 0)) {
            debug.error(classMethod + "Unable to get Entity ID from metaAlias" + 
                metaAlias);
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("nullEntityID"));
        }
        
        Object session = null;
            
        try {            
            session = WSFederationUtils.sessionProvider.getSession(request);
        } catch (SessionException se) {
            if ( debug.messageEnabled() ) {
                debug.message(classMethod +
                    "Session exception" + se.getLocalizedMessage());
            }
            // Don't care too much about session exceptions here - usual cause
            // is trying to log out after the session has expired
        }
        
        try {
            // Strategy here is to do logouts in parallel via iframes, provide a
            // link to wreply, if any
            BaseConfigType config = 
                metaManager.getBaseConfig(realm,entityId);
            String displayName = 
                WSFederationMetaUtils.getAttribute(config,
                WSFederationConstants.DISPLAY_NAME);
            if ( displayName == null || displayName.length() == 0 )
            {
                displayName = entityId;
            }
            request.setAttribute(WSFederationConstants.LOGOUT_DISPLAY_NAME, 
                displayName);
            request.setAttribute(WSFederationConstants.LOGOUT_WREPLY, 
                wreply);
            request.setAttribute(WSFederationConstants.REALM_PARAM, realm);
            request.setAttribute(WSFederationConstants.ENTITYID_PARAM, 
                entityId);

            LinkedHashMap<String, String> providerList = 
                new LinkedHashMap<String, String>();

            if ( session != null )
            {
                String[] idpList = WSFederationUtils.sessionProvider.
                    getProperty(session, WSFederationConstants.SESSION_IDP);

                if ( idpList != null && idpList.length > 0 
                    && idpList[0] != null && idpList[0].length()>0 )
                {
                    FederationElement fed = 
                        metaManager.getEntityDescriptor(realm, 
                        idpList[0]);
                    String endpoint = 
                        metaManager.getTokenIssuerEndpoint(fed);
                    String url = endpoint + "?wa=" + 
                        WSFederationConstants.WSIGNOUT10;

                    config = 
                        metaManager.getBaseConfig(realm,idpList[0]);
                    displayName = 
                        WSFederationMetaUtils.getAttribute(config,
                        WSFederationConstants.DISPLAY_NAME);
                    if ( displayName == null )
                    {
                        displayName = idpList[0];
                    }
                    if (debug.messageEnabled()) {
                        debug.message(classMethod + "sending signout to " + 
                            url);
                    }
                    providerList.put(url, displayName);
                }

                String[] spList = WSFederationUtils.sessionProvider.
                    getProperty(session, WSFederationConstants.SESSION_SP_LIST);

                if ( spList != null && spList.length > 0 
                    && spList[0] != null && spList[0].length()>0 )
                {
                    for ( int i = 0; i < spList.length; i++ )
                    {
                        config = 
                            metaManager.
                            getBaseConfig(realm,spList[i]);
                        displayName = WSFederationMetaUtils.getAttribute(config,
                            WSFederationConstants.DISPLAY_NAME);
                        if ( displayName == null )
                        {
                            displayName = spList[i];
                        }
                        FederationElement fed = 
                            metaManager.getEntityDescriptor(realm, 
                            spList[i]);
                        String endpoint = 
                            metaManager.getTokenIssuerEndpoint(fed);
                        String url = 
                            endpoint + "?wa=" + 
                            WSFederationConstants.WSIGNOUT10;
                        if (debug.messageEnabled()) {
                            debug.message(classMethod + "sending signout to " + 
                                url);
                        }
                        providerList.put(url, displayName);
                    }

                    // Can't remove a session property, so just set it to 
                    // an empty string
                    String[] empty = {""};
                    WSFederationUtils.sessionProvider.setProperty(session, 
                        WSFederationConstants.SESSION_SP_LIST, empty);
                }
                
                if (debug.messageEnabled()) {
                    debug.message(classMethod
                        + "destroying session " + session);
                }

                MultiProtocolUtils.invalidateSession(session, 
                    request, response, SingleLogoutManager.WS_FED);
            }

            request.setAttribute(WSFederationConstants.LOGOUT_PROVIDER_LIST, 
                providerList);

            request.getRequestDispatcher("/wsfederation/jsp/logout.jsp").
                forward(request, response);
        } catch (ServletException se) {
            if ( debug.messageEnabled() ) {
                debug.message(classMethod +
                    "Servlet exception" + se.getLocalizedMessage());
            }
            throw new WSFederationException(se);
        } catch (SessionException se) {
            if ( debug.messageEnabled() ) {
                debug.message(classMethod +
                    "Session exception" + se.getLocalizedMessage());
            }
            throw new WSFederationException(se);
        }
        
        // Can't pass session, since we just invalidated it!
        String[] data = {wreply};
        LogUtil.access(Level.INFO,
                LogUtil.SLO_SUCCESSFUL,
                data,
                null);
    }
}
