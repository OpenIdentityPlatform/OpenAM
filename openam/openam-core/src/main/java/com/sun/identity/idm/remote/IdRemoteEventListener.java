/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
* $Id: IdRemoteEventListener.java,v 1.7 2009/01/28 05:35:00 ww203982 Exp $
*/

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.idm.remote;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.sm.CreateServiceConfig;
import com.sun.identity.sm.SMSSchema;

/**
 * The <code>IdRemoteEventListener</code> handles the events generated from the
 * server. If notification URL is provided, the class registers the URL with the
 * Server and waits for notifications. Else it polls the server at configurable
 * time periods to get updates from the server.
 */
public class IdRemoteEventListener {

    private static Debug debug;
    
    private static SOAPClient client;
    
    private static IdRemoteEventListener instance;

    private static String remoteId;
    
    private static final String NOTIFICATION_PROPERTY = 
        "com.sun.identity.idm.remote.notification.enabled";
    
    private static final String CACHE_POLLING_TIME_PROPERTY = 
        "com.iplanet.am.sdk.remote.pollingTime";   
    
    private static final int DEFAULT_CACHE_POLLING_TIME = 1;
    
    private static final String IDREPO_SERVICE = "IdRepoServiceIF";
    
    public synchronized static IdRemoteEventListener getInstance() {
        if (instance == null) {
            instance = new IdRemoteEventListener();
        }
        return instance;
    }

    /**
     * Constructor for <class>EventListener</class>. Should be instantiated
     * once by <code>RemoteServicesImpl</code>
     *
     */
    private IdRemoteEventListener() {
        
        // Set debug
        if (debug == null) {
            debug = IdRemoteServicesImpl.getDebug();
        }

        // Construct the SOAP Client
        if (client == null) {
            client = new SOAPClient("DirectoryManagerIF");
        }
                     
        // Check if notification is enabled 
        // Default the notification enabled to true if the property
        // is not found for backward compatibility.
        String notificationFlag = SystemProperties.get(
                NOTIFICATION_PROPERTY, "true");

        if (notificationFlag.equalsIgnoreCase("true")) {
            // Run in notifications mode
            // Check if notification URL is provided
            URL url = null;
            try {
                // Throws exception if NotificationURL is null
                url = WebtopNaming.getNotificationURL();
                
                // Register for IdRepo Service
                Object result = client.send("registerNotificationURL_idrepo", url.toString(), null, null);
                if (result != null) {
                    remoteId = result.toString();
                }
                if (remoteId != null) {
                    if (debug.messageEnabled()) {
                        debug.message("IdRemoteEventListener: registerNotificationURL_idrepo returned ID " + remoteId);
                    }
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("IdRemoteEventListener: registerNotificationURL_idrepo returned null ID");
                    }
                }
                ShutdownManager shutdownMan = ShutdownManager.getInstance();
                if (shutdownMan.acquireValidLock()) {
                    try {
                        shutdownMan.addShutdownListener(new ShutdownListener() {
                            @Override
                            public void shutdown() {
                                try {
                                    if (remoteId != null) {
                                        client.send("deRegisterNotificationURL_idrepo", remoteId, null, null);
                                        if (debug.messageEnabled()) {
                                            debug.message("IdRemoteEventListener: deRegisterNotificationURL_idrepo for "
                                                    + remoteId);
                                        }
                                    } else {
                                        if (debug.messageEnabled()) {
                                            debug.message("IdRemoteEventListener: Could not " +
                                                    "deRegisterNotificationURL_idrepo due to null ID");
                                        }
                                    }
                                } catch (Exception e) {
                                    debug.error("IdRemoteEventListener: There was a problem calling " +
                                            "deRegisterNotificationURL_idrepo with ID " +  remoteId, e);
                                }
                            }
                        });
                    } finally {
                        shutdownMan.releaseLockAndNotify();
                    }
                }
                // Register with PLLClient for notification
                PLLClient.addNotificationHandler(IDREPO_SERVICE,
                        new IdRepoEventNotificationHandler());

                if (debug.messageEnabled()) {
                    debug.message("IdRemoteEventListener: Using notification "
                            + "mechanism for cache updates: "
                            + url.toString());
                }
            } catch (Exception e) {
                // Use polling mechanism to update caches
                if (debug.warningEnabled()) {
                    debug.warning("IdRemoteEventListener: Registering for "
                            + "notification via URL failed for " + url 
                            + " Exception: " + e.getMessage()
                            + "\nUsing polling mechanism for updates");
                }
                // Start Polling thread only if enabled.
                startPollingThreadIfEnabled(getCachePollingInterval());
            }            
        } else {
            // Start Polling thread only if enabled.
            startPollingThreadIfEnabled(getCachePollingInterval());
        }
        
    }
    
    private int getCachePollingInterval() {
        // If the property is not configured, default it to 1 minute. 
        String cachePollingTimeStr = SystemProperties.get(
                CACHE_POLLING_TIME_PROPERTY);
        int cachePollingInterval = DEFAULT_CACHE_POLLING_TIME;
        if (cachePollingTimeStr != null) { 
            try {
                cachePollingInterval = Integer.parseInt(cachePollingTimeStr);            
            } catch (NumberFormatException nfe) {
                debug.error("IdRemoteEventListener::NotificationRunnable:: "
                        + "Invalid Polling Time: " + cachePollingTimeStr + 
                        " Defaulting to " + DEFAULT_CACHE_POLLING_TIME  + 
                        " minute");
            }
        }        
        return cachePollingInterval;
    }    
    
    private static void startPollingThreadIfEnabled(int cachePollingInterval) {
        if (cachePollingInterval > 0) {
            if (debug.messageEnabled()) {
                debug.message("IdRemoteEventListener: Polling mode enabled. " +
                        "Starting the polling thread..");
            }
            // Run in polling mode
            NotificationRunnable nt = new NotificationRunnable(
                    cachePollingInterval);
            SystemTimer.getTimer().schedule(nt, new Date((
                System.currentTimeMillis() / 1000) * 1000));
        } else {
            if (debug.warningEnabled()) {
                debug.warning("IdRemoteEventListener: Polling mode DISABLED. " +
                         CACHE_POLLING_TIME_PROPERTY + "=" + 
                         cachePollingInterval);
            }
        }
    }

    /**
     * Sends notifications to listeners added via <code>addListener</code>.
     * The parameter <code>nItem</code> is an XML document having a single
     * notification event, using the following DTD.
     * <p>
     * 
     * <pre>
     *       &lt;!-- EventNotification element specifes the change notification
     *       which contains AttributeValuePairs. The attributes defined
     *       are &quot;method&quot;, &quot;entityName&quot;, &quot;
     *       eventType&quot; and &quot;attrNames&quot;. --&gt;
     *       &lt;!ELEMENT EventNotification ( AttributeValuePairs )* &gt;
     *  
     *       &lt;!-- AttributeValuePair element contains attribute name and 
     *       values --&gt;
     *       &lt;!ELEMENT AttributeValuPair ( Attribute, Value*) &gt;
     *  
     *       &lt;!-- Attribute contains the attribute names, and the allowed 
     *       names are &quot;method&quot;, &quot;entityName&quot;, 
     *       &quot;eventType&quot; and &quot;attrNames&quot; --&gt;
     *       &lt;!ELEMENT Attribute EMPTY&gt;
     *       &lt;!ATTRLIST Attribute
     *       name ( method | entityName | eventType | attrNames ) 
     *       &quot;method&quot;
     *       &gt;
     *  
     *       &lt;!-- Value element specifies the values for the attributes 
     *       --&gt; &lt;!ELEMENT Value (#PCDATA) &gt;
     * </pre>
     * 
     * @param nItem
     *            notification event as a xml document
     * 
     */
    static void sendIdRepoNotification(String nItem) {
        if (debug.messageEnabled()) {
            debug.message("IdRemoteEventListener::sendIdRepoNotification: "
                    + "Received notification.");
        }

        // Construct the XML document
        StringBuilder sb = new StringBuilder(nItem.length() + 50);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(nItem);
        try {
            // The second argument is set to false so that the xml escaped 
            // chars done by server code/IdRepoJAXRPCObjectImpl will not be 
            // unescaped.
            Map attrs = CreateServiceConfig.getAttributeValuePairs(SMSSchema
                .getXMLDocument(sb.toString(), false).getDocumentElement(), 
                    false);
            if (attrs == null || attrs.isEmpty()) {
                if (debug.warningEnabled()) {
                    debug.warning("IdRemoteEventListener::sendIdRepoNotification: "
                            + "Invalid event: " + attrs);
                }
                return;
            } else if (debug.messageEnabled()) {
                debug.message("IdRemoteEventListener::sendIdRepoNotification "
                        + "Decoded Event: " + attrs);
            }

            // Parse to get the entity name and the method
            String entityName = getAttributeValue(attrs, ENTITY_NAME);
            String method = getAttributeValue(attrs, METHOD);
            if (entityName == null || entityName.length() == 0
                    || method == null || method.length() == 0) {
                if (debug.warningEnabled()) {
                    debug.warning("IdRemoteEventListener::sendIdRepoNotification: "
                            + "Invalid universalID or method: " + entityName
                            + " method");
                }
                return;
            }

            // Construct IdRepoListener and set the realm
            IdRepoListener repoListener = new IdRepoListener();
            String realm = null;
            if (entityName.toLowerCase().indexOf(",amsdkdn=") != -1) {
                AMIdentity id = new AMIdentity(null, entityName);
                realm = id.getRealm();
            } else {
                DN entityDN = new DN(entityName);
                realm = entityDN.getParent().getParent().toRFCString();
            }
            if (debug.messageEnabled()) {
                debug.message("IdRemoteEventListener::sendIdRepoNotification: " +
                    "modified UUID: " + entityName + " realm: " + realm);
            }
            Map configMap = new HashMap();
            configMap.put("realm", realm);
            repoListener.setConfigMap(configMap);

            // Send the notification change
            if (method.equalsIgnoreCase(OBJECT_CHANGED)) {
                int eventType = getEventType((Set) attrs.get(EVENT_TYPE));
                repoListener.objectChanged(entityName, null, eventType, null);
            } else if (method.equalsIgnoreCase(ALL_OBJECTS_CHANGED)) {
                repoListener.allObjectsChanged();
            } else {
                // Invalid method name
                handleError("invalid method name: " + method);
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("IdRemoteEventListener::sendIdRepoNotification: "
                        + "Unable to send notification: " + nItem, e);
            }
        }
    }

    static void handleError(String msg) throws Exception {
        // Debug the message and throw an exception
        debug.error("EventListener::sendNotification: " + msg);
        throw (new Exception(msg));
    }

    static String getAttributeValue(Map attrs, String attrName) {
        String answer = null;
        Set set = (Set) attrs.get(attrName);
        if (set != null && set.size() == 1) {
            answer = (String) set.iterator().next();
        }
        return (answer);
    }

    static int getEventType(Set eventSet) throws Exception {
        if (eventSet == null || eventSet.size() != 1) {
            // throw an exception
            throw (new Exception("IdRemoteEventListener::sendNotification: "
                    + "invalid event type: " + eventSet));
        }
        String eventString = (String) eventSet.iterator().next();
        return (Integer.parseInt(eventString));
    }

    static class NotificationRunnable extends GeneralTaskRunnable {

        private int pollingTime;        
        private long runPeriod;

        NotificationRunnable(int interval) {
            pollingTime = interval;
            runPeriod = pollingTime * 1000 * 60;            
        }

        public boolean addElement(Object obj) {
            return false;
        }
        
        public boolean removeElement(Object obj) {
            return false;
        }
        
        public boolean isEmpty() {
            return true;
        }
        
        public long getRunPeriod() {
            return runPeriod;
        }
        
        public void run() {
            String installTime = SystemProperties.get(
                Constants.SYS_PROPERTY_INSTALL_TIME, "false");
            if (installTime.equalsIgnoreCase("true")) {
                return;
            }

            try {
                Object obj[] = { new Integer(pollingTime) };
                Set mods = (Set) client.send("objectsChanged_idrepo", obj,
                    null, null);
                if (debug.messageEnabled()) {
                    debug.message("IdRemoteEventListener:NotificationRunnable "
                        + "retrieved idrepo changes: " + mods);
                }
                Iterator items = mods.iterator();
                while (items.hasNext()) {
                    sendIdRepoNotification((String) items.next());
                }
            } catch (Exception ex) {
                if (debug.warningEnabled()) {
                    debug.warning("IdRemoteEventListener::" +
                        "NotificationRunnable:run Exception", ex);
                }
            }
        }
    }

    static class IdRepoEventNotificationHandler implements NotificationHandler {

        IdRepoEventNotificationHandler() {
            // Empty constructor
        }

        public void process(Vector notifications) {
            for (int i = 0; i < notifications.size(); i++) {
                Notification notification = (Notification) notifications
                        .elementAt(i);
                String content = notification.getContent();
                if (debug.messageEnabled()) {
                    debug.message("IdRemoteEventListener:" 
                            + "IdRepoEventNotificationHandler: "
                            + " received notification: " + content);
                }
                // Send notification
                sendIdRepoNotification(content);
            }
        }
    }

    // Static varaibles
    public static final String METHOD = "method";

    public static final String ENTITY_NAME = "entityName";

    public static final String EVENT_TYPE = "eventType";

    public static final String ATTR_NAMES = "attrNames";

    public static final String OBJECT_CHANGED = "objectChanged";

    public static final String ALL_OBJECTS_CHANGED = "allObjectsChanged";
}
