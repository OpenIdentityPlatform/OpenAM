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
 * $Id: AgentConfigNotificationHandler.java,v 1.3 2008/08/04 20:03:33 huacui Exp $
 *
 */

package com.sun.identity.agents.arch;

import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.share.Notification;

import java.util.Vector;

/**
 * <code>AgentConfigNotificationHandler</code> implements
 * <code>com.iplanet.services.comm.client.NotificationHandler</code> and
 * processes the notifications for agent property configurations.
 * The result is that that the agent hot swap properties will
 * take on the new values at centralized configuration OpenSSO server.
 */
public class AgentConfigNotificationHandler implements NotificationHandler {
    
    //later move this constant to part of client SDK
    public static final String AGENT_CONFIG_SERVICE = "agentconfig"; 
    
    /**
     * Constructs <code>AgentConfigNotificationHandler</code>
     */
    public AgentConfigNotificationHandler() {
    }

   /**
    * Process the notification.
    * This notification is actually just a ping from the OpenSSO server to let the
    * agent know that the agent's configuration has been updated on OpenSSO server.
    * There is no message content that is used. Upon receiving the notification
    * it will make a call to OpenSSO server to get the latest set of agent
    * configuration properties and subsequently update the agent configuration
    * to reflect the new property values.
    * No debug or logging is done in this class. If logging was to be done, it 
    * would be logged in the same logging global agentcore service as the 
    * AgentConfiguration.java
    *
    * @param notifications array of notifications to be processed.
    */
    public void process(Vector notifications) {
        if (notifications != null) {
            //Notifications are just empty pings so do not need only need
            //to process first one, can ignore others if more notifications.
            //Also, we do not use the content in notification.getContent()
            if (notifications.size() > 0) {
                Notification notification = 
                        (Notification) notifications.elementAt(0);               
                AgentConfiguration.updatePropertiesUponNotification();              
            }
        }  
    }
}
