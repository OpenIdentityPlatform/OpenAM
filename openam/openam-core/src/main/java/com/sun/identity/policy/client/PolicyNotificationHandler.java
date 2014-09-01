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
 * $Id: PolicyNotificationHandler.java,v 1.3 2008/06/25 05:43:46 qcheng Exp $
 *
 */



package com.sun.identity.policy.client;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.share.Notification;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.remote.PolicyNotification;
import com.sun.identity.policy.remote.PolicyService;
import java.util.Vector;

/** 
 *  Class that handles policy notifications from 
 *  <code>PLLNotificationServlet</code>. An instance of this class 
 *  is registered as <code>NotificationHandler</code> for policy 
 *  service with <code>PLLClient</code>, by <code>ResourceResultCache</code>.  
 *  This handler delegates the handling of notifications to 
 *  <code>ResourceResultCache</code> which removes the 
 *  affected cache entries
 */ 
public class PolicyNotificationHandler implements NotificationHandler {

    static Debug debug =  PolicyEvaluator.debug;
    private ResourceResultCache resourceResultCache;

    /**
     * Initializes <code>PolicyNotificationHandler</code> with a 
     * <code>ResourceResultCache</code>. 
     * @param resourceResultCache cache that would be updated based on
     * notification
     */
    public PolicyNotificationHandler(ResourceResultCache resourceResultCache) {
        this.resourceResultCache = resourceResultCache;
    }

    /**
     * Processes PLL notifications
     * @param notifications PLL notification to be processed
     */
    public void process(Vector notifications) {
        processPLLNotifications(notifications);
    }

    /**
     * Processes PLL notifications
     * @param notifications PLL notification to be processed
     */
    void processPLLNotifications(Vector notifications) {
	for (int i = 0; i < notifications.size(); i++) {
	    Notification  notification 
                    = (Notification) notifications.elementAt(i);
            if (debug.messageEnabled()) {
                debug.message("PolicyNotificationHandler."
                        + "processPLLNotifications():"
                        + "got notification: " 
                        + notification.getContent());
            }
            
            try {
	        PolicyService ps 
                        = PolicyService.parseXML(notification.getContent());
                PolicyNotification pn = ps.getPolicyNotification();
            
	        if (pn != null) {
		    processPolicyNotification(pn);
 	        }
            } catch (PolicyException pe) {
                debug.error("PolicyNotificationHandler."
                        + "processPLLNotifications():"
                        + "invalid notifcation format",
                        pe);
            }
         }
    }

    /**
     * Processes a policy notification. Delegates to 
     * <code>RessourceResultCache</code>. 
     * <code>ResourceResultCache</code> removes the affected cache entries.
     *
     * @param pn policy notification to process 
     */
    void processPolicyNotification(PolicyNotification pn) {
        try {
            resourceResultCache.processPolicyNotification(pn);
        } catch (Exception e) {
            debug.error("PolicyNotificationHandler.processPolicyNotification():"
                    + "Error while handling policy notification",
                    e);
        }
    }
}


