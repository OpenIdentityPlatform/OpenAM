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
 * $Id: NotificationTaskHandler.java,v 1.8 2008/06/25 05:51:48 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.share.NotificationSet;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * notifications as received by the <code>AmFilter</code>.
 * </p>
 */
public class NotificationTaskHandler extends AmFilterTaskHandler
        implements INotificationTaskHandler {
    /**
     * The constructor that takes a <code>Manager</code> instance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     */
    public NotificationTaskHandler(Manager manager) {
        super(manager);
    }
    
    /**
     * Sets the flags for enabling session, policy and agent configuration
     * notifications. Also sets the notification url, and if notification url
     * can not be set properly then sets all flags to off so all notifications 
     * would be disabled.
     *
     */
    public void initialize(ISSOContext context, AmFilterMode mode)
    throws AgentException {
        super.initialize(context, mode);
        initSessionNotificationEnabledFlag();
        initPolicyNotificationEnabledFlag();
        initNotificationURI();
        if (isLogMessageEnabled()) {
            String message = 
                   "NotificationTaskHandler.initialize: Session Notifications";
            message += (isSessionNotificationEnabled()) ? " are enabled" : 
                                                          " are not enabled";

            message += " and Policy Notifications";
            message += (isPolicyNotificationEnabled()) ? " are enabled" : 
                                                         " are not enabled";
            logMessage(message);
        }
    }
    
    /**
     * Checks to see if the incoming request is that of a notification and
     * suggests any action needed to handle such notifications appropriately.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries 
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle notifications.
     * @throws AgentException in case the handling of this request results in
     * an unexpected error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
    throws AgentException {
        AmFilterResult result = null;
        String requestURI = ctx.getHttpServletRequest().getRequestURI();    
        if (requestURI.equals(getNotificationURI())) {
            result = handleNotification(ctx);
        }   
        return result;
    }
    
    /**
     * Returns a boolean value indicating if this task handler is enabled 
     * or not.
     * @return true if Notifications are enabled, false otherwise
     */
    public boolean isActive() {
        return true;
    }
    
    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_NOTIFICATION_TASK_HANDLER_NAME;
    }
    
    protected AmFilterResult handleNotification(AmFilterRequestContext ctx) {
        AmFilterResult result = null;
        String notificationData = getNotificationDataString(
                ctx.getHttpServletRequest());
        if (isLogMessageEnabled()) {
            logMessage("NotificationTaskHandler.handleNotification:"
                    + " notification Data: " + NEW_LINE + notificationData);
        }
        NotificationSet notificationSet =
                NotificationSet.parseXML(notificationData);
        Vector notifications = notificationSet.getNotifications();
        if (notifications != null && notifications.size() > 0) {
            String serviceID = notificationSet.getServiceID();
            if (isLogMessageEnabled()) {
                logMessage("NotificationTaskHandler.handleNotification:"
                        + " received " + serviceID + " notification");
            }
            if (serviceID != null) {
                String response = STR_NOTIFICATION_PROCESSING_FAILED;
                if (isServiceNotificationEnabled(serviceID)) {
                    NotificationHandler handler =
                            PLLClient.getNotificationHandler(serviceID);
                    if (handler == null) {
                        logError("NotificationTaskHandler.handleNotification:"
                                + " NotificationHandler for "
                                + serviceID + " not found");
                    } else {
                        handler.process(notifications);
                        response = STR_NOTIFICATION_PROCESSING_SUCCESS;
                    }
                }
                result = ctx.getServeDataResult(response);
            }
        }
        
        return result;
    }
    
    /**
     * Checks if the policy,session, or agent configuration notifications 
     * are enabled when receiving one of those notification types.
     *
     * @param serviceID is the service ID attrbute contained inside a
     *        notification xml message and identifies the type of notification
     */               
    private boolean isServiceNotificationEnabled(String serviceID) {
        boolean enabled = false;
        if (serviceID != null) {
            if(serviceID.equals(SERVICE_ID_POLICY) 
                                         && isPolicyNotificationEnabled()) {
                enabled = true;
            } else if(serviceID.equals(SERVICE_ID_SESSION) 
                                         && isSessionNotificationEnabled()) {
                enabled = true;
            } else if(serviceID.equals(SERVICE_ID_AGENT_CONFIGURATION)) {
                enabled = true;
            } else if (serviceID.equals(SERVICE_ID_SMSOBJECT)) {
                enabled = true;
            } else if (serviceID.equals(SERVICE_ID_IDREPOSERVICE)) {
                enabled = true;
            } 
        }
        return enabled;
    }
    
    private String getNotificationDataString(HttpServletRequest request) {
        String result = null;
        try {
            BufferedReader reader = request.getReader();
            StringBuffer notificationBuffer = new StringBuffer();
            String nextLine = null;
            while ( (nextLine = reader.readLine()) != null) {
                notificationBuffer.append(nextLine);
                notificationBuffer.append(NEW_LINE);
            }
            result = notificationBuffer.toString();
        } catch (UnsupportedEncodingException uex) {
            logError("NotificationTaskHandler.getNotificationDataString:"
                    + " Failed to read notification", uex);
        } catch (IllegalStateException iex) {
            logError("NotificationTaskHandler.getNotificationDataString:"
                    + "Failed to read notification", iex);
        } catch (IOException ioex) {
            logError("NotificationTaskHandler.getNotificationDataString:"
                    + "Failed to read notification", ioex);
        }
        return result;
    }
    
    private void initSessionNotificationEnabledFlag() {
        boolean flag = AgentConfiguration.isSessionNotificationEnabled();
        setSessionNotificationEnabledFlag(flag);
    }    
    
    private void initPolicyNotificationEnabledFlag() {
        boolean flag = AgentConfiguration.isPolicyNotificationEnabled();
        setPolicyNotificationEnabledFlag(flag);
    }
    
    //URI used for session, policy, and agent configuration notices
    private void initNotificationURI() {     
        if (isActive()) {
            String url = AgentConfiguration.getClientNotificationURL();
            String notificationURI = null;
            if (( url == null) || (url.trim().length() == 0)) {
                url = null;
            }
            if (url != null) {
                try {
                    URL notificationURL = new URL(url);
                    notificationURI = notificationURL.getPath();
                } catch (MalformedURLException ex) {
                    logError("NotificationTaskHandler.initNotificationURI:"
                            + " Invalid Agent Centralized Configuration" +
                             " Notification URL specified: " + url, ex);
                }
            }
            if (notificationURI != null) {
                setNotificationURI(notificationURI);
                if (isLogMessageEnabled()) {
                    logMessage("NotificationTaskHandler.initNotificationURI:"
                     + "Notifications URI => "
                     +  _notificationURI);
                }
            } else {
                logError("NotificationTaskHandler.initNotificationURI:"
                    + "Notification URI not set, so disabled due to errors"); 
                 //since no valid url, ensure flags set to false
                 setSessionNotificationEnabledFlag(false);
                 setPolicyNotificationEnabledFlag(false);
            }
        } else {
            if (isLogMessageEnabled()) {
                logMessage(
                    "NotificationTaskHandler.initNotificationURI:" +
                    " Notifications are not enabled");
            }
        }
    }
        
    private void setSessionNotificationEnabledFlag(boolean flag) {
        _sessionNotificationEnabled = flag;
    }
    
    private boolean isSessionNotificationEnabled() {
        return _sessionNotificationEnabled;
    }
    
    private void setPolicyNotificationEnabledFlag(boolean flag) {
        _policyNotificationEnabled = flag;
    }
    
    private boolean isPolicyNotificationEnabled() {
        return _policyNotificationEnabled;
    }
    
    //URI used for session, policy, and agent configuration notices
    private void setNotificationURI(String uri) {
         _notificationURI = uri;
    }
    
    //URI used for session, policy, and agent configuration notices
    private String getNotificationURI() {
        return _notificationURI;
    }
    
    private boolean                         _sessionNotificationEnabled = false;
    private boolean                         _policyNotificationEnabled  = false;
    private String                          _notificationURI;
    
    //serviceID attribute values inside notification xml messages
    private static final String SERVICE_ID_AGENT_CONFIGURATION =  "agentconfig";
    private static final String SERVICE_ID_POLICY              =  "policy";
    private static final String SERVICE_ID_SESSION             =  "session";
    private static final String SERVICE_ID_SMSOBJECT           =  "SMSObjectIF";
    private static final String SERVICE_ID_IDREPOSERVICE       =  "IdRepoServiceIF";
}
