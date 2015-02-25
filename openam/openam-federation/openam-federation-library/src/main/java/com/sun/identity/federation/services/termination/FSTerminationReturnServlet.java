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
 * $Id: FSTerminationReturnServlet.java,v 1.4 2008/12/19 06:50:48 exu Exp $
 *
 */


package com.sun.identity.federation.services.termination;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;

/**
 * Handles termination return message.
 */
public class FSTerminationReturnServlet extends HttpServlet {
    ServletConfig config = null;
    IDFFMetaManager metaManager = null;
    
    /**
     * Initializes the servlet.
     * @param config the <code>ServletConfig</code> object that contains 
     *      configutation information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *               the servlet's normal operation.
     */
    public void init(ServletConfig config)
        throws ServletException 
    {
        super.init(config);
        FSUtils.debug.message("FSTerminationReturnServlet Initializing...");
        this.config = config;
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Handles the HTTP GET request.
     *
     * @param request <code>HttpServletRequest</code> object that contains the
     *      request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains 
     *      the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doGet(
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     *
     * @param request <code>HttpServletRequest</code> object that contains the
     *      request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains 
     *      the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the POST request
     * @exception IOException if the request for the POST could not be handled
     */
    public void doPost(
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        doGetPost(request, response);
    }
    
    /**
     * Handles termination return message.
     * @param request <code>HttpServletRequest</code> object that contains the
     *      request the client has made of the servlet.
     * @param response <code>HttpServletResponse</code> object that contains 
     *      the response the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the request
     * @exception IOException if the request could not be handled
     */
    private void doGetPost(
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        FSUtils.debug.message("FSTerminationReturnServlet doGetPost...");
        String providerAlias = FSServiceUtils.getMetaAlias(request);
        if (providerAlias == null || providerAlias.length() < 1) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Unable to retrieve alias, Hosted" +
                    " Provider. Cannot process request");
            }
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString("aliasNotFound"));
            return;
        }
        StringBuffer terminationDone = new StringBuffer();
        BaseConfigType hostedConfig = null;
        try {
            String hostedRole = metaManager.getProviderRoleByMetaAlias(
                providerAlias);
            String hostedEntityId = metaManager.getEntityIDByMetaAlias(
                providerAlias);
            String realm = IDFFMetaUtils.getRealmByMetaAlias(providerAlias);
            if (hostedRole != null && 
                hostedRole.equalsIgnoreCase(IFSConstants.IDP)) {
                hostedConfig = metaManager.getIDPDescriptorConfig(
                    realm, hostedEntityId);
            } else if (hostedRole != null &&
                hostedRole.equalsIgnoreCase(IFSConstants.SP)) {
                hostedConfig = metaManager.getSPDescriptorConfig(
                    realm, hostedEntityId);
            }
            if (hostedRole == null || hostedConfig == null) {
                throw new IDFFMetaException((String) null);
            }
        } catch (IDFFMetaException e){
            FSUtils.debug.error("Failed to get Hosted Provider");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                FSUtils.bundle.getString(
                    IFSConstants.FAILED_HOSTED_DESCRIPTOR));
            return;
        }
        terminationDone.append(FSServiceUtils.getTerminationDonePageURL(
            request, hostedConfig, providerAlias));

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("Final Done page URL at local end: " + 
                        terminationDone.toString());
        }
        response.sendRedirect(terminationDone.toString());
        return;
    }   
}   // FSTerminationReturnServlet
