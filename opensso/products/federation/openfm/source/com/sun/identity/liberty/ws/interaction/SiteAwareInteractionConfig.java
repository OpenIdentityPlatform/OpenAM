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
 * $Id: SiteAwareInteractionConfig.java,v 1.6 2008/08/06 17:29:25 exu Exp $
 *
 */

package com.sun.identity.liberty.ws.interaction;

import com.sun.identity.common.SystemConfigurationUtil;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class SiteAwareInteractionConfig extends InteractionConfig {

    private static Debug debug = Debug.getInstance("libIDWSF");

    public SiteAwareInteractionConfig() {
        if (debug.messageEnabled()) {
            debug.message("SiteAwareInteractionConfig: constructor called");
        }
        initialize();
    }

    public void initialize() {
        debug.message("SiteAwareInteractionConfig.initialise(): called");
        super.initialize();

        String protocol = SystemConfigurationUtil.getProperty(
            Constants.AM_SERVER_PROTOCOL);
        String host = SystemConfigurationUtil.getProperty(
            Constants.AM_SERVER_HOST);
        String port = SystemConfigurationUtil.getProperty(
            Constants.AM_SERVER_PORT);
        String contextPath = SystemConfigurationUtil.getProperty(
                Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

        if (debug.messageEnabled()) {
            debug.message("SiteAwareInteractionConfig.initialise():"
                    + "server protocol: " + protocol
                    + ", server host:" + host
                    + ", server port:" + port
                    + ", contextPath:" + contextPath);
        }

        try {
            String serverId = WebtopNaming.getServerID(protocol, host, port,
                contextPath);
            String server = WebtopNaming.getServerFromID(serverId);
            wspRedirectHandler = server + "/"  
                    + WSP_REDIRECT_HANDLER_SERVLET;


            boolean siteEnabled = WebtopNaming.isSiteEnabled(
                protocol, host, port, contextPath);

            if (debug.messageEnabled()) {
                debug.message("SiteAwareInteractionConfig.initialise():"
                        + "server id:" + serverId
                        + ", server:" + server
                        + ",siteEnabled:" + siteEnabled);
            }

            if (siteEnabled) {
                String siteId = WebtopNaming.getSiteID(serverId);
                lbWspRedirectHandler = WebtopNaming.getServerFromID(siteId)
                        + "/" + WSP_REDIRECT_HANDLER_SERVLET;
                Set siteNodes = WebtopNaming.getSiteNodes(siteId); 
                trustedWspRedirectHandlers.clear(); 
                if ((siteNodes !=null) && !siteNodes.isEmpty()) {
                    for (Iterator iter = siteNodes.iterator(); 
                            iter.hasNext();) {
                        serverId = (String)iter.next();
                        trustedWspRedirectHandlers.put(serverId,
                                WebtopNaming.getServerFromID(serverId)
                                + "/" + WSP_REDIRECT_HANDLER_SERVLET);
                    }
                }
            }
        } catch (Exception e) { 
            debug.error("SiteAwareInteractionConfig.initialise():"
                + "error while initializing", e);
        }

        if (debug.messageEnabled()) {
            debug.message("SiteAwareInteractionConfig.initialise():"
                    + "wspRedirectHandler:" + wspRedirectHandler
                    + "lbWspRedirectHandler:" + lbWspRedirectHandler
                    + "trustedWspRedirectHandlers:" 
                    + trustedWspRedirectHandlers);
        }

    }

}
