/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMSNotificationManager.java,v 1.14 2009/11/10 21:49:44 hengming Exp $
 *
 */
package com.sun.identity.sm;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.sm.jaxrpc.SMSJAXRPCObject;
import com.sun.identity.sm.jaxrpc.SMSJAXRPCObjectImpl;
import java.net.URL;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Handles all the notification events for SMS.
 * 
 * Classes that will register for notifications are:
 * 1) SMSEventListnerManager -- to send notifications to internal objects
 *      Framework classes i.e., Cached* & *Impls that maintain read only caches
 * 2) SMSJAXRPCObjectImpl -- to send notifications to remote clients
 * 3) SMSLdapObjectImpl -- to clear its internal cache
 * Of the above (2) and (3) would happen only on the Server only.
 * 
 * This class will resgister itself with either one of them:
 * On the Server, if "enableDataStoreNotification" is true it would
 * register with SMSObject.registerCallbackHandler(..) via SMSEntry,
 * else will send notifications by itself.
 * On the Client, it will register with SMSJAXRPCObject, again via
 * registerCallbackHandler(..)
 */
public class SMSNotificationManager implements SMSObjectListener {
    
    private static SMSNotificationManager instance;
    private static Map changeListeners = Collections.synchronizedMap(
        new HashMap());
    // SMS objects listener should be called first
    private static SMSEventListenerManager internalEventListener;
    private static Debug debug = Debug.getInstance("amSMSEvent");
    static final String AGENTGROUP_RDN = "ou=agentgroup,ou=Instances";
    
    // Notification variabled
    static boolean enableDataStoreNotification;
    static boolean cachedEnabled;
    static boolean isClient;
    
    private SMSNotificationManager() {
        initializeProperties();
    }
    
    protected void initializeProperties() {
        // Check if notifications are obtained from the datastore
        boolean previousDataStoreNotification = enableDataStoreNotification;
        enableDataStoreNotification = Boolean.valueOf(
            SystemProperties.get(Constants.SMS_ENABLE_DB_NOTIFICATION)).
                booleanValue();
        boolean configTime = Boolean.valueOf(SystemProperties.get(
            Constants.SYS_PROPERTY_INSTALL_TIME)).booleanValue();
        SMSObject object = SMSEntry.getSMSObject();
        isClient = object instanceof SMSJAXRPCObject;
        // In the case of CLI, disable datastore notification since it
        // adds additional psearch connections to the directory
        if (!isClient && !SystemProperties.isServerMode()) {
            enableDataStoreNotification = false;
        }
        // If not config time and in legacy mode, enable dataStoreNotification
        // ServiceManager will be called only if not config time
        // Also disable the check for clients
        if (!isClient && !enableDataStoreNotification && !configTime &&
            !ServiceManager.isRealmEnabled()) {
            enableDataStoreNotification = true;
        }
        cachedEnabled = Boolean.valueOf(SystemProperties.get(
            Constants.SDK_GLOBAL_CACHE_PROPERTY, "true")).booleanValue();
            
        if (!cachedEnabled) {
            // Check SMS cache property
            cachedEnabled = Boolean.valueOf(SystemProperties.get(
                Constants.SMS_CACHE_PROPERTY, "false")).booleanValue();
                
        }
        if (debug.messageEnabled()) {
            debug.message("SMSNotificationManager.init " +
                "DataStore Notification: " + enableDataStoreNotification +
                " CacheEnabled: " + cachedEnabled);
        }
        // Register for callbacks if in client or if server should be
        // based on enableDataStoreNotification. In the case of legacy mode
        // registration for notification is handled by AMSDK code in DataLayer
        // Since configuration change observer is registered, check if status
        // of datastore notification has changed
        if (cachedEnabled && (enableDataStoreNotification || isClient) &&
            (previousDataStoreNotification != enableDataStoreNotification)) {
            try {
                object.registerCallbackHandler(this);
                if (debug.messageEnabled()) {
                    debug.message("SMSNotificationManager.init " +
                        "Registered for notification with: " +
                        object.getClass().getName());
                }
            } catch (SMSException ex) {
                debug.error("SMSNotificationManager.init Error during " +
                    "notification registration", ex);
            }
        } else if (previousDataStoreNotification !=
            enableDataStoreNotification) {
            // Deregister the callback handler from SMSObject
            object.deregisterCallbackHandler(null);
            if (debug.messageEnabled()) {
                debug.message("SMSNotificationManager.init " +
                    "deregistering for notification with: " +
                    object.getClass().getName());
            }
        }
    }
    
    protected synchronized void deregisterListener(SMSObject object) {
        if (enableDataStoreNotification) {
            object.deregisterCallbackHandler(null);
            enableDataStoreNotification = false;
        }
    }
    
    public static synchronized SMSNotificationManager getInstance() {
        if (instance == null) {
            instance = new SMSNotificationManager();
        }
        return (instance);
    }
    
    public static boolean isCacheEnabled() {
        getInstance();
        return (cachedEnabled);
    }
    
    public static boolean isDataStoreNotificationEnabled() {
        getInstance();
        return (enableDataStoreNotification);
    }
    
    public String registerCallbackHandler(
        SMSObjectListener listener) {
        String id = SMSUtils.getUniqueID();
        if (listener instanceof SMSEventListenerManager) {
            internalEventListener = (SMSEventListenerManager) listener;
        } else {
            changeListeners.put(id, listener);
        }
        return (id);
    }
    
    public void removeCallbackHandler(String id) {
        changeListeners.remove(id);
    }

    // Methods called by SMSEntry for local notification
    void localObjectChanged(String name, int type) {
        // If SMSEventListererManager is provided send notifications first.
        // In general notifications are delayed by the schedule causing
        // issues with write-through cache.
        if (internalEventListener != null) {
            internalEventListener.objectChanged(name, type);
        }
        
        // If cache disabled or client or dataStoreNotification not enabled,
        // send change notifications. If cache is disable, it is assumed that
        // there will be no notifications from datastores and hence components
        // that have cached SMS objects must be notified of the changes
        // In the case of client, internal notifications must be sent, since
        // the notifications from the server is not guaranteed 
        // Also send notifications during install time
        String configTime = SystemProperties.get(
            Constants.SYS_PROPERTY_INSTALL_TIME, "false");
        boolean bConfigTime = configTime.equalsIgnoreCase("true");
        if (!cachedEnabled || isClient || !enableDataStoreNotification ||
            bConfigTime) {
            // Since directly called by SMSEntry, this should be
            // executed within a TimerTask
            LocalChangeNotifcationTask changes = new
                LocalChangeNotifcationTask(name, type, false);
            SMSThreadPool.scheduleTask(changes);
        }
    }
    
    // Method called by SMSObject implementations
    public void objectChanged(String name, int type) {
        // If SMSEventListererManager is provided send notifications first.
        // Other notifications rely on this cache being upto date, hence
        // this must be processed first. If isLocal == true, this would have
        // already been processed
        if (internalEventListener != null) {
            internalEventListener.objectChanged(name, type);
        }
        
        // Execute within a TimerTask, since the duration of external
        // calls cannot be predicted
        LocalChangeNotifcationTask changes = new
            LocalChangeNotifcationTask(name, type, true);
        SMSThreadPool.scheduleTask(changes);
    }
    
    // Method Executed asynchronously by the ThreadPool and
    // called directly by SMSEventListenerManager to send sub-tree
    // delete notifications when datastore notification is not enabled
    void sendNotifications(String name, int type, boolean localOnly) {
        // Since agentgroup placeholder node is added after install time,
        // fix it here to avoid sending notifications.
        if ((type == SMSObjectListener.ADD) &&
            (name.indexOf(AGENTGROUP_RDN) >= 0)) {
            return;
        }
        
        // If running as Server and changes are local and DB notification is
        // disabled, schedule sending notifications to other servers.
        // Also schedule this task only if there are more than 1
        // server instances and while running as server
        String installTime = SystemProperties.get(
            Constants.SYS_PROPERTY_INSTALL_TIME, "false");
        if (!localOnly && !enableDataStoreNotification && !isClient &&
            !installTime.equals("true") && (!SystemProperties.isServerMode() ||
            ServiceManager.getAMServerInstances().size() > 1)) {
            // To be executed by a new TimerTask, since it could be a while
            ServerNotificationTask nt = new ServerNotificationTask(name, type);
            SMSThreadPool.scheduleTask(nt);
        }
        
        // Called by one of the following
        // SMSLdapObject, SMSJAXRPCObjectImpl or LocalChangeNotifications
        // which are all called by individual threads.
        // Iterate over changeListeners and send notifications
        Set listenerIds = (Set) changeListeners.keySet();
        Set nlists = new HashSet();
        synchronized (changeListeners) {
            // Make a local copy of the change listeners
            for (Iterator items = listenerIds.iterator(); items.hasNext();) {
                nlists.add(changeListeners.get(items.next()));
            }
        }
        // Change listeners could be one of the following
        // OrganizationConfigManager, ServiceSchemaManager
        // ServiceConfigManager
        // SMSLdapObject (s) -- will be exectuted by this thread 
        // SMSJAXRPCObjectImpl (s)-- should create a new task/thread
        // SMSJAXRPCObject (c) -- will be executed by this thread
        SMSObjectListener jaxrpclistener = null;
        for (Iterator items = nlists.iterator(); items.hasNext();) {
            SMSObjectListener listener = (SMSObjectListener) items.next();
            // Listeners might through exceptions, use try-catch
            try {
                if ((!isClient) && (listener instanceof SMSJAXRPCObjectImpl)) {
                    // Process this at the end
                    jaxrpclistener = listener;
                } else {
                    listener.objectChanged(name, type);
                }
            } catch (Throwable t) {
                debug.error("SMSNotificationManager.objectChanged " +
                    "Exception for class: " +
                    listener.getClass().getName(), t);
            }
        }
        if (jaxrpclistener != null) {
            try {
                jaxrpclistener.objectChanged(name, type);
            } catch (Throwable t) {
                debug.error("SMSNotificationManager.objectChanged " +
                    "Exception for JAXRPC class: " +
                    jaxrpclistener.getClass().getName(), t);
            }
        }
    }

    // Method called by SMSObject implementations
    public void allObjectsChanged() {
        // Called by one of the following
        // SMSLdapObject, SMSJAXRPCObjectImpl or LocalChangeNotifications
        // which are all called by individual threads
        try {
            // Clear all caches first
            (new ServiceManager((SSOToken) AccessController.
            doPrivileged(AdminTokenAction.getInstance()))).clearCache();
        } catch (SSOException ex) {
            debug.error("SMSNotificationManager.allObjectsChanged " +
                "Invalid AdminSSOToken: ", ex);
        } catch (SMSException ex) {
            debug.error("SMSNotificationManager.allObjectsChanged " +
                "SMSException in clearing cache: ", ex);
        }
        
        // Iterate over changeListeners and send notifications
        Set listenerIds = (Set) changeListeners.keySet();
        Set nlists = new HashSet();
        synchronized (changeListeners) {
            // Make a local copy of the change listeners
            for (Iterator items = listenerIds.iterator(); items.hasNext();) {
                nlists.add(changeListeners.get(items.next()));
            }
        }
        // Change listeners could be one of the following
        // SMSEventListnerManager c,s -- will be executed by this thread
        // SMSLdapObject (s) -- will be exectuted by this thread 
        // SMSJAXRPCObjectImpl (s)-- should create a new task/thread
        // SMSJAXRPCObject (c) -- will be executed by this thread
        for (Iterator items = nlists.iterator(); items.hasNext();) {
            SMSObjectListener listener = (SMSObjectListener) items.next();
            // Listeners might through exceptions, use try-catch
            try {
                listener.allObjectsChanged();
            } catch (Throwable t) {
                debug.error("SMSNotificationManager.allObjectsChanged " +
                    "Exception for class: " +
                    listener.getClass().getName(), t);
            }
        }
    }
    
    private class LocalChangeNotifcationTask implements Runnable {
        String name;
        int type;
        boolean localOnly;
        
        private LocalChangeNotifcationTask(String name, int type,
            boolean localOnly) {
            this.name = name;
            this.type = type;
            this.localOnly = localOnly;
        }

        public void run() {
            instance.sendNotifications(name, type, localOnly);
        }
    }
    
    /**
     * Server URL to determine if notifications must be sent
     */
    private static String serverURL;
    
    /**
     * To send no Servers from a Server or from CLI that uses the server impls.
     * Used only of datatore notifications is disabled
     */ 
    private class ServerNotificationTask implements Runnable {
        SSOToken adminSSOToken;
        String name;
        int type;

        ServerNotificationTask(String name, int type) {
            this.name = name;
            this.type = type;
            
            // Construct server URL
            if (serverURL == null) {
                if (SystemProperties.isServerMode()) {
                    String namingURL = SystemProperties.get(
                        Constants.AM_NAMING_URL);
                    if (namingURL != null) {
                        int index = namingURL.toLowerCase().indexOf(
                            "/namingservice");
                        if (index != -1) {
                            serverURL = namingURL.substring(0, index)
                                .toLowerCase();
                        } else {
                            serverURL = "";
                        }
                    } else {
                        serverURL = SystemProperties.getServerInstanceName();
                        if (serverURL == null) {
                            serverURL = "";
                        } else {
                            serverURL = serverURL.toLowerCase();
                        }
                    }
                } else {
                    serverURL = "";
                }
            }
        }
        
        public void run() {
            // At install time and creation of placeholder nodes
            // should not send notifications
            // This would be determined if type == ADD and the DNs are
            // 1) ou=<serviceName>,<rootSuffix>
            // 2) ou=globalconfig
            // 3) ou=organizationconfig
            // 4) ou=instances
            // 5) ou=services
            // 6) ...
            
            // Since agentgroup placeholder node is added after install time,
            // fix it here to avoid sending notifications.
            if (type == SMSObjectListener.ADD
                && ((new StringTokenizer(name, ",")).countTokens() 
                    <= (SMSEntry.baseDNCount + 1) || 
                        (name.indexOf(AGENTGROUP_RDN) >= 0))) {
                return;
            }
            if (debug.messageEnabled()) {
                debug.message("ServerNotificationTask.run " +
                    "Sending notifications to servers. DN: " + name +
                    " ChangeType: " + type);
            }

            // Get servers from ServiceManager and send notifications
            // and ignore the local server URL
            try {
                Iterator sl = ServiceManager.getAMServerInstances().iterator();
                while (sl != null && sl.hasNext()) {
                    URL url = new URL((String) sl.next());
                    URL weburl = WebtopNaming.getServiceURL("jaxrpc",
                        url, false);
                    String surl = weburl.toString();
                    // Check if local server
                    if ((serverURL.length() > 0) &&
                        (surl.toLowerCase().startsWith(serverURL))) {
                        if (debug.messageEnabled()) {
                            debug.message("ServerNotificationTask.run NOT " +
                                "sending notification to local server:" + 
                                    surl);
                        }
                        continue;
                    }
                    if (!surl.endsWith("/")) {
                        surl += "/SMSObjectIF";
                    } else {
                        surl += "SMSObjectIF";
                    }
                    try {
                        // Send notification
                        SOAPClient client = new SOAPClient();
                        client.setURL(surl);
                        Object[] params = new Object[2];
                        params[0] = name;
                        params[1] = new Integer(type);
                        if (debug.messageEnabled()) {
                            debug.message("ServerNotificationTask.run " +
                                "Sending to URL: " + surl);
                        }

                        // Should not set LB cookie, since it is a 
                        // server-server communication 
                        client.send("notifyObjectChanged", params, null, null);

                    } catch (Throwable t) {
                        if (debug.errorEnabled()) {
                            debug.error("ServerNotificationTask.run " +
                                "Unable to send notification to: " + surl, t);
                        }
                    }
                }
            } catch (Throwable t) {
                if (debug.warningEnabled()) {
                    debug.warning("ServerNotificationTask.run " +
                        "Unable to send notifications", t);
                }
            }
        }
    }
}
