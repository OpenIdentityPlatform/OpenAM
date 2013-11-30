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
 * $Id: EventService.java,v 1.19 2009/09/28 21:47:33 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.iplanet.services.ldap.event;

import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPControl;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPInterruptedException;
import com.sun.identity.shared.ldap.LDAPMessage;
import com.sun.identity.shared.ldap.LDAPResponse;
import com.sun.identity.shared.ldap.LDAPSearchConstraints;
import com.sun.identity.shared.ldap.LDAPSearchListener;
import com.sun.identity.shared.ldap.LDAPSearchResult;
import com.sun.identity.shared.ldap.LDAPSearchResultReference;
import com.sun.identity.shared.ldap.controls.LDAPEntryChangeControl;
import com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;

/**
 * Event Service monitors changes on the server. Implemented with the persistant
 * search control. Uses ldapjdk asynchronous interfaces so that multiple search
 * requests can be processed by a single thread
 * 
 * The Type of changes that can be monitored are: - 
 * LDAPPersistSearchControl.ADD -
 * LDAPPersistSearchControl.DELETE - LDAPPersistSearchControl.MODIFY -
 * LDAPPersistSearchControl.MODDN
 * 
 * A single connection is established initially and reused to service all
 * notification requests.
 * @supported.api
 */
public class EventService implements Runnable {

    protected static DSConfigMgr cm = null;

    // list that holds notification requests
    protected Map _requestList = null;

    // Thread that listens to DS notifications
    static Thread _monitorThread = null;

    // search listener for asynch ldap searches
    static LDAPSearchListener _msgQueue;

    // A singelton patern
    protected static EventService _instance = null;

    // Don't want the server to return all the
    // entries. return only the changes.
    private static final boolean CHANGES_ONLY = true;

    // Want the server to return the entry
    // change control in the search result
    private static final boolean RETURN_CONTROLS = true;

    // Don't perform search if Persistent
    // Search control is not supported.
    private static final boolean IS_CRITICAL = true;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    protected static Debug debugger = Debug.getInstance("amEventService");

    // Parameters in AMConfig, that provide values for connection retries
    protected static final String EVENT_CONNECTION_NUM_RETRIES = 
        "com.iplanet.am.event.connection.num.retries";

    protected static final String EVENT_CONNECTION_RETRY_INTERVAL = 
        "com.iplanet.am.event.connection.delay.between.retries";

    protected static final String EVENT_CONNECTION_ERROR_CODES = 
        "com.iplanet.am.event.connection.ldap.error.codes.retries";

    // Idle timeout in minutes
    protected static final String EVENT_IDLE_TIMEOUT_INTERVAL = 
        "com.sun.am.event.connection.idle.timeout";
    
    protected static final String EVENT_LISTENER_DISABLE_LIST =
        "com.sun.am.event.connection.disable.list";
          
    private static boolean _allDisabled = false;    

    private static int _numRetries = 3;

    private static int _retryInterval = 3000;

    private static int _retryMaxInterval = 720000; // 12 minutes

    private static int _retryCount = 1;

    private static long _lastResetTime = 0;

    protected static HashSet _retryErrorCodes;

    // Connection Time Out parameters
    protected static int _idleTimeOut = 0; // Idle timeout in minutes.

    protected static long _idleTimeOutMills;
    
    // List of know listeners. The order of the listeners is important
    // since it is used to enable & disable the listeners
    private static final String[] ALL_LISTENERS = {
        "com.iplanet.am.sdk.ldap.ACIEventListener",
        "com.iplanet.am.sdk.ldap.EntryEventListener",
        "com.sun.identity.sm.ldap.LDAPEventManager"
    };

    protected static String[] listeners;

    protected static Hashtable _ideListenersMap = new Hashtable();   
    
    protected static volatile boolean _isThreadStarted = false;
    
    protected static volatile boolean _shutdownCalled = false;

    private static HashSet getPropertyRetryErrorCodes(String key) {
        HashSet codes = new HashSet();
        String retryErrorStr = SystemProperties.get(key);
        if (retryErrorStr != null && retryErrorStr.trim().length() > 0) {
            StringTokenizer stz = new StringTokenizer(retryErrorStr, ",");
            while (stz.hasMoreTokens()) {
                codes.add(stz.nextToken().trim());
            }
        }
        return codes;
    }

    private static int getPropertyIntValue(String key, int defaultValue) {
        int value = defaultValue;
        String valueStr = SystemProperties.get(key);
        if (valueStr != null && valueStr.trim().length() > 0) {
            try {
                value = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                value = defaultValue;
                if (debugger.warningEnabled()) {
                    debugger.warning("EventService.getPropertyIntValue(): "
                            + "Invalid value for property: "
                            + EVENT_CONNECTION_NUM_RETRIES
                            + " Defaulting to value: " + defaultValue);
                }
            }
        }

        if (debugger.messageEnabled()) {
            debugger.message("EventService.getPropertyIntValue(): " + key
                    + " = " + value);
        }
        return value;
    }
    
    /**
     * Determine the listener list based on the diable list property
     * and SMS DataStore notification property in Realm mode
     */
    private static void getListenerList() {
        String list = SystemProperties.get(EVENT_LISTENER_DISABLE_LIST, "");
        if (debugger.messageEnabled()) {
            debugger.message("EventService.getListenerList(): " +
                    EVENT_LISTENER_DISABLE_LIST + ": " + list);
        }
        
        boolean enableDataStoreNotification = Boolean.parseBoolean(
            SystemProperties.get(Constants.SMS_ENABLE_DB_NOTIFICATION));
        if (debugger.messageEnabled()) {
            debugger.message("EventService.getListenerList(): " +
                "com.sun.identity.sm.enableDataStoreNotification: " +
                enableDataStoreNotification);
        }
        
        boolean configTime = Boolean.parseBoolean(SystemProperties.get(
            Constants.SYS_PROPERTY_INSTALL_TIME));
        if (debugger.messageEnabled()) {
            debugger.message("EventService.getListenerList(): " +
                Constants.SYS_PROPERTY_INSTALL_TIME + ": " + configTime);
        }
        
        // Copy the default listeners
        String[] tmpListeners = new String[ALL_LISTENERS.length];
        System.arraycopy(ALL_LISTENERS, 0, tmpListeners, 0, ALL_LISTENERS.length);
        
        // Process the configured disabled list first
        boolean disableACI = false, disableUM = false, disableSM = false;
        if (list.length() != 0) {
            StringTokenizer st = new StringTokenizer(list, ",");
            String listener = "";
            while (st.hasMoreTokens()) {
                listener = st.nextToken().trim();
                if (listener.equalsIgnoreCase("aci")) {
                    disableACI = true;
                } else if (listener.equalsIgnoreCase("um")) {
                    disableUM = true;
                } else if (listener.equalsIgnoreCase("sm")) {
                    disableSM = true;
                } else {
                    debugger.error("EventService.getListenerList() - " +
                        "Invalid listener name: " + listener);
                }
            }
        }
        
        if (!disableUM || !disableACI) {
            // Check if AMSDK is configured
            boolean disableAMSDK = true;
            if (!configTime) {
                try {
                    ServiceSchemaManager scm = new ServiceSchemaManager(
                        getSSOToken(), IdConstants.REPO_SERVICE, "1.0");
                    ServiceSchema idRepoSubSchema = scm.getOrganizationSchema();
                    Set idRepoPlugins = idRepoSubSchema.getSubSchemaNames();
                    if (idRepoPlugins.contains("amSDK")) {
                        disableAMSDK = false;
                    }
                } catch (SMSException ex) {
                    if (debugger.warningEnabled()) {
                        debugger.warning("EventService.getListenerList() - " +
                            "Unable to obtain idrepo service", ex);
                    }
                } catch (SSOException ex) {
                    // Should not happen, ignore the exception
                }
            }
            if (disableAMSDK) {
                disableUM = true;
                disableACI = true;
                if (debugger.messageEnabled()) {
                    debugger.message("EventService.getListener" +
                        "List(): AMSDK is not configured or config time. " +
                        "Disabling UM and ACI event listeners");
                }
            }
        }
        
        // Verify if SMSnotification should be enabled
        if (configTime || ServiceManager.isRealmEnabled()) {
            disableSM = !enableDataStoreNotification;
            if (debugger.messageEnabled()) {
                debugger.message("EventService.getListenerList(): In realm " +
                    "mode or config time, SMS listener is set to datastore " +
                    "notification flag: " + enableDataStoreNotification);
            }
        }
        
        // Disable the selected listeners
        if (disableACI) {
            tmpListeners[0] = null;
        }
        if (disableUM) {
            tmpListeners[1] = null;
        }
        if (disableSM) {
            tmpListeners[2] = null;
        }
        listeners = tmpListeners;

        // if all disabled, signal to not start the thread
        if (disableACI && disableUM && disableSM) {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.getListenerList() - " +
                        "all listeners are disabled, EventService won't start");
                }
            _allDisabled = true;
        } else {
            _allDisabled = false;
        }
    }

    /**
     * Private Constructor
     */
    protected EventService() throws EventException {
        getConfigManager();
        _requestList = Collections.synchronizedMap(new HashMap());
    }

    /**
     * create the singelton EventService object if it doesn't exist already.
     * Check if directory server supports the Persistent Search Control and the
     * Proxy Auth Control
     * @supported.api
     */
    public synchronized static EventService getEventService()
            throws EventException, LDAPException {
        
        if (_shutdownCalled) {
            return null;
        }
        
        // Make sure only one instance of this class is created.
        if (_instance == null) {
            // Determine the Idle time out value for Event Service (LB/Firewall)
            // scenarios. Value == 0 imples no idle timeout.
            _idleTimeOut = getPropertyIntValue(EVENT_IDLE_TIMEOUT_INTERVAL,
                _idleTimeOut);
            _idleTimeOutMills = _idleTimeOut * 60000;
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    if (_idleTimeOut == 0) {
                        _instance = new EventService();
                    } else {
                        _instance = new EventServicePolling();
                    }
                    shutdownMan.addShutdownListener(new
                        ShutdownListener() {
                            public void shutdown() {
                                if (_instance != null) {
                                    _instance.finalize();
                                }
                            }
                        });
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
        }
        return _instance;
    }
    
    protected static String getName() {
        return "EventService";
    }

    /**
     * At the end, close THE Event Manager's connections Abandon all previous
     * persistent search requests
     * @supported.api
     */
    public void finalize() {
        synchronized (this) {
            _shutdownCalled = true;
            if ((_monitorThread != null) && (_monitorThread.isAlive())) {
                _monitorThread.interrupt();
                _isThreadStarted = false;
            }
        }
        synchronized (_requestList) {
            Collection requestObjs = _requestList.values();
            Iterator iter = requestObjs.iterator();
            while (iter.hasNext()) {
                Request request = (Request) iter.next();
                removeListener(request);
            }
            _requestList.clear();
        }
    }

    /**
     * Adds a listener to the directory.
     * @supported.api
     */
    protected synchronized String addListener(SSOToken token,
            IDSEventListener listener, String base, int scope, String filter,
            int operations) throws LDAPException, EventException {

        if (_shutdownCalled) {
            throw new EventException(i18n
                    .getString(IUMSConstants.DSCFG_CONNECTFAIL));
        }
        
        LDAPConnection lc = null;
        try {
            // Check for SMS listener and use "sms" group if present
            if ((listener.getClass().getName().equals(
                "com.sun.identity.sm.ldap.LDAPEventManager")) &&
                (cm.getServerGroup("sms") != null)) {
                lc = cm.getNewConnection("sms", LDAPUser.Type.AUTH_ADMIN);

            } else {
                lc = cm.getNewAdminConnection();
            }
        } catch (LDAPServiceException le) {
            throw new EventException(i18n
                    .getString(IUMSConstants.DSCFG_CONNECTFAIL), le);
        }

        LDAPSearchConstraints cons = lc.getSearchConstraints();

        // Create Persistent Search Control object
        LDAPPersistSearchControl psearchCtrl = new LDAPPersistSearchControl(
                operations, CHANGES_ONLY, RETURN_CONTROLS, IS_CRITICAL);

        // Add LDAPControl array to the search constraint object
        cons.setServerControls(psearchCtrl);
        cons.setBatchSize(1);

        // Listeners can not read attributes from the event.
        // Request only javaClassName to be able to determine object type
        String[] attrs = new String[] { "objectclass" };
        LDAPSearchListener searchListener = null;
        // Set (asynchronous) persistent search request in the DS
        try {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.addListener() - Submiting "
                        + "Persistent Search on: " + base + " for listener: "
                        + listener);
            }
            searchListener = lc.search(base, scope, filter, attrs, false,
                    null, cons);
        } catch (LDAPException le) {
            if ((lc != null) && lc.isConnected()) {
                try {
                    lc.disconnect();
                } catch (Exception ex) {
                    //ignored
                }
            }
            debugger.error("EventService.addListener() - Failed to set "
                    + "Persistent Search" + le.getMessage());
            throw le;
        }

        int[] outstandingRequests = searchListener.getMessageIDs();
        int id = outstandingRequests[outstandingRequests.length - 1];

        String reqID = Integer.toString(id);
        long startTime = System.currentTimeMillis();
        Request request = new Request(id, reqID, token, base, scope, filter,
                attrs, operations, listener, lc, startTime);
        _requestList.put(reqID, request);

        // Add this search request to the m_msgQueue so it can be
        // processed by the monitor thread
        if (_msgQueue == null) {
            _msgQueue = searchListener;
        } else {
            _msgQueue.merge(searchListener);
        }

        if (!_isThreadStarted) {
            startMonitorThread();
        } else {
            if (_requestList.size() == 1) {
                notify();
            }
        }
        
        if (debugger.messageEnabled()) {
            outstandingRequests = _msgQueue.getMessageIDs();
            debugger.message("EventService.addListener(): merged Listener: "
                    + " requestID: " + reqID + " & Request: " + request
                    + " on to message Queue. No. of current outstanding "
                    + "requests = " + outstandingRequests.length);
        }

        // Create new (EventService) Thread, if one doesn't exist.
        return reqID;
    }

    public IDSEventListener getIDSListeners(String className) {
        return (IDSEventListener) _ideListenersMap.get(className);
    }
    
    public static boolean isThreadStarted() {
        return _isThreadStarted;
    }
      
    /**
     * Main monitor thread loop. Wait for persistent search change notifications
     *
     * @supported.api
     */    
    public void run() {
        try {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.run(): Event Thread is running! "
                        + "No Idle timeout Set: " + _idleTimeOut + " minutes.");
            }
            
            boolean successState = true;
            LDAPMessage message = null;
            while (successState) {
                try {
                    if (debugger.messageEnabled()) {
                        debugger.message("EventService.run(): Waiting for "
                                + "response");
                    }
                    synchronized (this) {
                        if (_requestList.isEmpty()) {
                            wait();
                        }
                    }
                    message = _msgQueue.getResponse();
                    successState = processResponse(message);
                } catch (LDAPInterruptedException ex) {
                    if (_shutdownCalled) {
                        break;
                    } else {
                        if (debugger.warningEnabled()) {
                            debugger.warning("EventService.run() " +
                                "LDAPInterruptedException received:", ex);
                        }
                    }
                } catch (LDAPException ex) {
                    if (_shutdownCalled) {                        
                        break;
                    } else {
                        int resultCode = ex.getLDAPResultCode();
                        if (debugger.warningEnabled()) {
                            debugger.warning("EventService.run() LDAPException "
                                + "received:", ex);
                        }
                        _retryErrorCodes = getPropertyRetryErrorCodes(
                            EVENT_CONNECTION_ERROR_CODES);

                        // Catch special error codition in
                        // LDAPSearchListener.getResponse
                        String msg = ex.getMessage();
                        if ((resultCode == LDAPException.OTHER) &&
                            (msg != null) && msg.equals("Invalid response")) {
                            // We should not try to resetError and retry
                            processNetworkError(ex);
                        } else {
                            if (_retryErrorCodes.contains("" + resultCode)) {
                                resetErrorSearches(true);
                            } else { // Some other network error
                                processNetworkError(ex);
                            }
                        }
                    }
                }
            } // end of while loop
        } catch (InterruptedException ex) {
            if (!_shutdownCalled) {
                if (debugger.warningEnabled()) {
                    debugger.warning("EventService.run(): Interrupted exception"
                        + " caught.", ex);
                }
            }
        } catch (RuntimeException ex) {
            if (debugger.warningEnabled()) {
                debugger.warning("EventService.run(): Runtime exception "
                    + "caught.", ex);
            }
            // rethrow the Runtime exception to let the container handle the
            // exception.
            throw ex;
        } catch (Exception ex) {
            if (debugger.warningEnabled()) {
                debugger.warning("EventService.run(): Unknown exception "
                    + "caught.", ex);
            }
            // no need to rethrow.
        } catch (Throwable t) {
            // Catching Throwable to prevent the thread from exiting.
            if (debugger.warningEnabled()) {
                debugger.warning("EventService.run(): Unknown exception "
                    + "caught. Sleeping for a while.. ", t);
            }
            // rethrow the Error to let the container handle the error.
            throw new Error(t);
        } finally {
            synchronized (this) {
                if (!_shutdownCalled) {
                    // try to restart the monitor thread.
                    _monitorThread = null;
                    startMonitorThread();
                }
            }
        }
    } // end of thread
    
    private static synchronized void startMonitorThread() {
        if (((_monitorThread == null) || !_monitorThread.isAlive()) &&
            !_shutdownCalled) {
            // Even if the monitor thread is not alive, we should use the
            // same instance of Event Service object (as it maintains all
            // the listener information)
            _monitorThread = new Thread(_instance, getName());
            _monitorThread.setDaemon(true);
            _monitorThread.start();
            
            // Since this is a singleton class once a getEventService() 
            // is invoked the thread will be started and the variable 
            // will be set to true. This will help other components 
            // to avoid starting it once again if the thread has 
            // started.
            _isThreadStarted = true;            
        }
    }

    protected boolean retryManager(boolean clearCaches) {
        long now = System.currentTimeMillis();
        // reset _retryCount to 1 after 12 hours
        if ((now - _lastResetTime) > 43200000) {
            _retryCount = 1;
            _lastResetTime = now;
        }

        int i = _retryCount * _retryInterval;
        if (i > _retryMaxInterval) {
            i = _retryMaxInterval;
        } else {
            _retryCount *= 2;
        }

        if (debugger.messageEnabled()) {
            debugger.message("EventService.retryManager() - wait " +
                    (i / 1000) +" seconds before calling resetAllSearches");
        }
        sleepRetryInterval(i);
        return resetAllSearches(clearCaches);
    }

    /**
     * Method which process the Response received from the DS.
     * 
     * @param message -
     *            the LDAPMessage received as response
     * @return true if the reset was successful. False Otherwise.
     */        
    protected boolean processResponse(LDAPMessage message) {
        if ((message == null) && (!_requestList.isEmpty())) {
            // Some problem with the message queue. We should
            // try to reset it.
            debugger.error("EventService.processResponse() - Received a NULL Response, call retryManager");
            return retryManager(false);
        }
        
        if (debugger.messageEnabled()) {
            debugger.message("EventService.processResponse() - received "
                    + "DS message  => " + message.toString());
        }

        // To determine if the monitor thread needs to be stopped.
        boolean successState = true;

        Request request = getRequestEntry(message.getMessageID());

        // If no listeners, abandon this message id
        if (request == null) {
            // We do not have anything stored about this message id.
            // So, just log a message and do nothing.
            if (debugger.messageEnabled()) {
                debugger.message("EventService.processResponse() - Received "
                        + "ldap message with unknown id = "
                        + message.getMessageID());
            }
        } else if (message.getMessageType() ==
            LDAPMessage.LDAP_SEARCH_RESULT_MESSAGE) {
            // then must be a LDAPSearchResult carrying change control
            processSearchResultMessage((LDAPSearchResult) message, request);
            request.setLastUpdatedTime(System.currentTimeMillis());
        } else if (message.getMessageType() ==
            LDAPMessage.LDAP_RESPONSE_MESSAGE) {
            // Check for error message ...
            LDAPResponse rsp = (LDAPResponse) message;
            successState = processResponseMessage(rsp, request);
        } else if (message.getMessageType() ==
            LDAPMessage.LDAP_SEARCH_RESULT_REFERENCE_MESSAGE) { // Referral
            processSearchResultRef(
                    (LDAPSearchResultReference) message, request);
        }
        return successState;
    }

    /**
     * removes the listener from the list of Persistent Search listeners of the
     * asynchronous seach for the given search ID.
     * 
     * @param request
     *            The request returned by the addListener
     * @supported.api
     */   
    protected void removeListener(Request request) {
        LDAPConnection connection = request.getLDAPConnection();
        if (connection != null) {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.removeListener(): Removing "
                        + "listener requestID: " + request.getRequestID()
                        + " Listener: " + request.getListener());
            }
            try {
                if ((connection != null) && (connection.isConnected())) {
                    connection.abandon(request.getId());
                    connection.disconnect();
                }
            } catch (LDAPException le) {
                // Might have to check the reset codes and try to reset
                if (debugger.warningEnabled()) {
                    debugger.warning("EventService.removeListener(): "
                            + "LDAPException, when trying to remove listener",
                            le);
                }
            }
        }
    }

    
    /**
     * Reset error searches. Clear cache only if true is passed to argument
     * 
     * @param clearCaches
     */    
    protected void resetErrorSearches(boolean clearCaches) {
        
        Hashtable tmpReqList = new Hashtable();
        tmpReqList.putAll(_requestList);
       
        int[] ids = _msgQueue.getMessageIDs();
        if (ids != null) {
            for (int i = 0; i < ids.length; i++) {
                String reqID = Integer.toString(ids[i]);
                tmpReqList.remove(reqID);
            }
        }
        Collection reqList = tmpReqList.values();
        for (Iterator iter = reqList.iterator(); iter.hasNext();) {
            Request req = (Request) iter.next();
            _requestList.remove(req.getRequestID());
        }
        _retryInterval = getPropertyIntValue(EVENT_CONNECTION_RETRY_INTERVAL,
            _retryInterval);
        RetryTask task = new RetryTask(tmpReqList);
        task.clearCache(clearCaches);
        SystemTimer.getTimer().schedule(task, new Date(((
            System.currentTimeMillis() + _retryInterval) / 1000) * 1000));
    }
    
    /**
     * Reset all searches. Clear cache only if true is passed to argument
     * 
     * @param clearCaches
     * @return <code>true</code> if the reset was successful, otherwise <code>false</code>
     */    
    public synchronized boolean resetAllSearches(boolean clearCaches) {
        if (_shutdownCalled) {
            return false;
        }
        
        // Make a copy of the existing psearches
        Hashtable tmpReqList = new Hashtable();
        tmpReqList.putAll(_requestList);
        _requestList.clear(); // Will be updated in addListener method
        Collection reqList = tmpReqList.values();
        
        // Clear the cache, if parameter is set
        if (clearCaches && !reqList.isEmpty()) {
            for (Iterator iter = reqList.iterator(); iter.hasNext();) {
                Request req = (Request) iter.next();
                IDSEventListener el = req.getListener();
                el.allEntriesChanged();
            }
        }
        
        // Get the list of psearches to be enabled
        getListenerList();
        if (_allDisabled) {
            // All psearches are disabled, remove listeners if any and return
            if (debugger.messageEnabled()) {
                debugger.message("EventService.resetAllSearches(): " +
                    "All psearches have been disabled");
            }
            if (!reqList.isEmpty()) {
                for (Iterator iter = reqList.iterator(); iter.hasNext();) {
                    Request req = (Request) iter.next();
                    removeListener(req);
                    if (debugger.messageEnabled()) {
                        debugger.message("EventService.resetAllSearches(): " +
                            "Psearch disabled: " +
                            req.getListener().getClass().getName());
                    }
                }
            }
            return true;
        }
        
        // Psearches are enabled, verify and reinitilize
        // Maintain the listeners to reinitialized in tmpListenerList
        Set tmpListenerList = new HashSet();
        Set newListenerList = new HashSet();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] != null) {
                // Check if the listener is present in reqList
                boolean present = false;
                for (Iterator iter = reqList.iterator(); iter.hasNext();) {
                    Request req = (Request) iter.next();
                    IDSEventListener el = req.getListener();
                    String listenerClass = el.getClass().getName();
                    if (listenerClass.equals(listeners[i])) {
                        present = true;
                        iter.remove();
                        tmpListenerList.add(req);
                    }
                }
                if (!present) {
                    // Add the listner object
                    if (debugger.messageEnabled()) {
                        debugger.message("EventService.resetAllSearches(): " +
                            "Psearch being added: " + listeners[i]);
                    }
                    newListenerList.add(listeners[i]);
                }
            }
        }
        // Remove the listeners not configured
        if (!reqList.isEmpty()) {
            for (Iterator iter = reqList.iterator(); iter.hasNext();) {
                Request req = (Request) iter.next();
                removeListener(req);
                if (debugger.messageEnabled()) {
                    debugger.message("EventService.resetAllSearches(): " +
                        "Psearch disabled due to configuration changes: " +
                        req.getListener().getClass().getName());
                }
            }
        }
        // Reset the requested list
        reqList = tmpListenerList;
        
        // Determine the number of retry attempts in case of failure
        // If retry property is set to -1, retries will be done infinitely
        _numRetries = getPropertyIntValue(EVENT_CONNECTION_NUM_RETRIES,
            _numRetries);
        int retry = 1;
        boolean doItAgain = ((_numRetries == -1) || ((_numRetries != 0) &&
            (retry <= _numRetries))) ? true : false;
        while (doItAgain) {
            if (debugger.messageEnabled()) {
                String str = (_numRetries == -1) ? "indefinitely" : Integer
                    .toString(retry);
                debugger.message("EventService.resetAllSearches(): "
                    + "retrying = " + str);
            }

            // Note: Avoid setting the messageQueue to null and just
            // try to disconnect the connections. That way we can be sure
            // that we have not lost any responses.
            for (Iterator iter = reqList.iterator(); iter.hasNext();) {
                try {
                    Request request = (Request) iter.next();

                    // First add a new listener and then remove the old one
                    // that we do don't loose any responses to the message
                    // Queue.
                    addListener(request.getRequester(), request.getListener(),
                        request.getBaseDn(), request.getScope(),
                        request.getFilter(), request.getOperations());
                    removeListener(request);
                    iter.remove();
                } catch (LDAPServiceException e) {
                    // Ignore exception and retry as we are in the process of
                    // re-establishing the searches. Notify Listeners after the
                    // attempt
                    if (retry == _numRetries) {
                        processNetworkError(e);
                    }
                } catch (LDAPException le) {
                    // Ignore exception and retry as we are in the process of
                    // re-establishing the searches. Notify Listeners after the
                    // attempt
                    if (retry == _numRetries) {
                        processNetworkError(le);
                    }
                }       
            }
            
            // Check if new listeners need to be added
            for (Iterator iter = newListenerList.iterator(); iter.hasNext();) {
                String listnerClass = (String) iter.next();
                try {
                    Class thisClass = Class.forName(listnerClass);
                    IDSEventListener listener = (IDSEventListener)
                        thisClass.newInstance();
                    _ideListenersMap.put(listnerClass, listener);
                    _instance.addListener(getSSOToken(), listener,
                        listener.getBase(), listener.getScope(),
                        listener.getFilter(), listener.getOperations());
                    if (debugger.messageEnabled()) {
                        debugger.message("EventService.resetAllSearches() - " +
                            "successfully initialized: " + listnerClass);
                    }
                    iter.remove();
                } catch (Exception e) {
                    debugger.error("EventService.resetAllSearches() " +
                        "Unable to start listener " + listnerClass, e);
                }
            }
            
            if (reqList.isEmpty() && newListenerList.isEmpty()) {
                return true;
            } else {
                if (_numRetries != -1) {
                   doItAgain = (++retry <= _numRetries) ? true : false;
                   if (!doItAgain) {
                       // remove the requests fail to be resetted
                       // would try to reinitialized the next time
                       for (Iterator iter = reqList.iterator();
                           iter.hasNext();) {
                           Request req = (Request) iter.next();
                           removeListener(req);
                           debugger.error("EventService.resetAll" +
                               "Searches(): unable to restart: " +
                               req.getListener().getClass().getName());
                       }
                       for (Iterator iter = newListenerList.iterator();
                           iter.hasNext();) {
                           String req = (String) iter.next();
                           debugger.error("EventService.resetAll" +
                               "Searches(): unable add listener: " + req);
                       }
                   }
                }
            }
            if (doItAgain) {
                // Sleep before retry
                sleepRetryInterval();
            }
        } // end while loop
        return false;
    }
       
    protected void sleepRetryInterval() {
        _retryInterval = getPropertyIntValue(EVENT_CONNECTION_RETRY_INTERVAL,
            _retryInterval);
        try {
            Thread.sleep(_retryInterval);
        } catch (InterruptedException ie) {
            // ignore
        }
    }

    protected void sleepRetryInterval(int interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException ie) { // ignore
        }
    }

    /**
     * get a handle to the Directory Server Configuration Manager sets the value
     */    
    protected static void getConfigManager() throws EventException {
        try {
            cm = DSConfigMgr.getDSConfigMgr();
        } catch (LDAPServiceException lse) {
            debugger.error("EventService.getConfigManager() - Failed to get "
                    + "handle to Configuration Manager", lse);
            throw new EventException(i18n
                    .getString(IUMSConstants.DSCFG_NOCFGMGR), lse);
        }
    }
    
    private void dispatchException(Exception e, Request request) {
        IDSEventListener el = request.getListener();
        debugger.error("EventService.dispatchException() - dispatching "
                + "exception to the listener: " + request.getRequestID()
                + " Listener: " + request.getListener(), e);
        el.eventError(e.toString());
    }

    /**
     * Dispatch naming event to all listeners
     */    
    private void dispatchEvent(DSEvent dirEvent, Request request) {
        IDSEventListener el = request.getListener();
        el.entryChanged(dirEvent);
    }

    /**
     * On network error, create ExceptionEvent and delever it to all listeners
     * on all events.
     */    
    protected void processNetworkError(Exception ex) {
        Hashtable tmpRequestList = new Hashtable();
        tmpRequestList.putAll(_requestList);
        int[] ids = _msgQueue.getMessageIDs();
        if (ids != null) {
            for (int i = 0; i < ids.length; i++) {
                tmpRequestList.remove(Integer.toString(ids[i]));
            }
        }
        Collection reqList = tmpRequestList.values();
        for (Iterator iter = reqList.iterator(); iter.hasNext();) {
            Request request = (Request) iter.next();
            dispatchException(ex, request);
        }
    }

    /**
     * Response message carries a LDAP error. Response with the code 0
     * (SUCCESS), should never be received as persistent search never completes,
     * it has to be abandon. Referral messages are ignored
     */    
    protected boolean processResponseMessage(LDAPResponse rsp,
        Request request) {
        _retryErrorCodes = getPropertyRetryErrorCodes(
            EVENT_CONNECTION_ERROR_CODES);
        int resultCode = rsp.getResultCode();
        if (_retryErrorCodes.contains("" + resultCode)) {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.processResponseMessage() - "
                        + "received LDAP Response for requestID: "
                        + request.getRequestID() + " Listener: "
                        + request.getListener() + "Need restarting");
            }
            resetErrorSearches(false);
        } else if (resultCode != 0
                || resultCode != LDAPException.REFERRAL) { 
            // If not neither of the cases then
            if (resultCode == LDAPException.BUSY) {
                debugger.error("EventService.processResponseMessage() - received error BUSY, call retryManager");
                return retryManager(false);
            }
            LDAPException ex = new LDAPException("Error result", rsp
                    .getResultCode(), rsp.getErrorMessage(), 
                    rsp.getMatchedDN());
            dispatchException(ex, request);
        }
        return true;
    }

    /**
     * Process change notification attached as the change control to the message
     */    
    protected void processSearchResultMessage(LDAPSearchResult res,
            Request req) {
        LDAPEntry modEntry = res.getEntry();

        if (debugger.messageEnabled()) {
            debugger.message("EventService.processSearchResultMessage() - "
                    + "Changed " + modEntry.getDN());
        }

        /* Get any entry change controls. */
        LDAPControl[] ctrls = res.getControls();

        // Can not create event without change control
        if (ctrls == null) {
            Exception ex = new Exception("EventService - Cannot create "
                    + "NamingEvent, no change control info");
            dispatchException(ex, req);
        } else {
            // Multiple controls might be in the message
            for (int i = 0; i < ctrls.length; i++) {
                LDAPEntryChangeControl changeCtrl = null;

                if (ctrls[i].getType() ==
                    LDAPControl.LDAP_ENTRY_CHANGE_CONTROL) {
                    changeCtrl = (LDAPEntryChangeControl) ctrls[i];
                    if (debugger.messageEnabled()) {
                        debugger.message("EventService."
                                + "processSearchResultMessage() changeCtrl = "
                                + changeCtrl.toString());
                    }

                    // Can not create event without change control
                    if (changeCtrl.getChangeType() == -1) {
                        Exception ex = new Exception("EventService - Cannot "
                                + "create NamingEvent, no change control info");
                        dispatchException(ex, req);
                    }

                    // Convert control into a DSEvent and dispatch to listeners
                    try {
                        DSEvent event = createDSEvent(
                                            modEntry, changeCtrl, req);
                        dispatchEvent(event, req);
                    } catch (Exception ex) {
                        dispatchException(ex, req);
                    }
                }
            }
        }
    }

    /**
     * Search continuation messages are ignored.
     */    
    protected void processSearchResultRef(LDAPSearchResultReference ref,
            Request req) {
        // Do nothing, message ignored, do not dispatch ExceptionEvent
        if (debugger.messageEnabled()) {
            debugger.message("EventService.processSearchResultRef() - "
                    + "Ignoring..");
        }
    }
    
    protected static SSOToken getSSOToken() throws SSOException {
        return ((SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance()));
    }

    /**
     * Find event entry by message ID
     */    
    protected Request getRequestEntry(int id) {
        return (Request) _requestList.get(Integer.toString(id));
    }

    /**
     * Create naming event from a change control
     */    
    private DSEvent createDSEvent(LDAPEntry entry,
            LDAPEntryChangeControl changeCtrl, Request req) throws Exception {
        DSEvent dsEvent = new DSEvent();

        if (debugger.messageEnabled()) {
            debugger.message("EventService.createDSEvent() - Notifying event "
                    + "to: " + req.getListener());
        }

        // Get the dn from the entry
        String dn = entry.getDN();
        dsEvent.setID(dn);

        // Get information on the type of change made
        int changeType = changeCtrl.getChangeType();
        dsEvent.setEventType(changeType);

        // Pass the search ID as the event's change info
        dsEvent.setSearchID(req.getRequestID());

        // set the object class name
        String className = entry.getAttribute("objectclass").toString();
        dsEvent.setClassName(className);

        return dsEvent;
    }
    
    class RetryTask extends GeneralTaskRunnable {
        
        private long runPeriod;
        private Map requests;
        private boolean clearCaches;
        private int numRetries;
        
        public RetryTask(Map requests) {
            
            this.runPeriod = getPropertyIntValue(
                EVENT_CONNECTION_RETRY_INTERVAL, EventService._retryInterval);
            this.requests = requests;
            this.numRetries = _numRetries;
        }
        
        public void clearCache(boolean cc) {
            clearCaches = cc;
        }
        
        public void run() {
            for (Iterator iter = requests.values().iterator();
                iter.hasNext();) {
                Request req = (Request) iter.next();
                try {
                    // First add a new listener and then remove the old one
                    // that we do don't loose any responses to the message
                    // Queue. However before adding check if request list
                    // already has this listener initialized
                    if (!_requestList.containsValue(req)) {
                        addListener(req.getRequester(), req.getListener(),
                            req.getBaseDn(), req.getScope(),
                            req.getFilter(), req.getOperations());
                    }
                    removeListener(req);
                    if (clearCaches) {
                        // Send all entries changed notifications
                        // only after successful establishment of psearch
                        req.getListener().allEntriesChanged();
                    }
                    iter.remove();
                } catch (Exception e) {
                    debugger.error("RetryTask", e);
                    // Ignore exception and retry as we are in the process of
                    // re-establishing the searches. Notify Listeners after the
                    // attempt
                }
            }
            if (--numRetries == 0) {
                debugger.error("NumRetries " + numRetries);
                runPeriod = -1;
            }
        }
        
        public long getRunPeriod() {
            return runPeriod;
        }
        
        public boolean isEmpty() {
            return true;
        }
        
        public boolean addElement(Object obj) {
            return false;
        }
        
        public boolean removeElement(Object obj) {
            return false;
        }
    }
}
