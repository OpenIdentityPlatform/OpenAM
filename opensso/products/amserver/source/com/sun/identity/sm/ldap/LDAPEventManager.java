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
 * $Id: LDAPEventManager.java,v 1.8 2009/01/28 05:35:04 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.sm.ldap;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.ldap.event.EventException;
import java.util.Map;

import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.event.DSEvent;
import com.iplanet.services.ldap.event.EventService;
import com.iplanet.services.ldap.event.IDSEventListener;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSObjectListener;

/**
 * This class registers itself as a listener to <class>
 * com.iplanet.services.ldap.event.EventService</class> which sets up
 * persistant search connections with the event service for any
 * changes to SMS object classes
 */
public class LDAPEventManager implements IDSEventListener {
    
    // String base DN
    private static String baseDN;

    // Notification list
    protected static SMSObjectListener changeListener;

    protected static Debug debug = Debug.getInstance("amSMSEvent");

    // Used by EventService via IDSEventListener
    private Map listeners;

    static void addObjectChangeListener(SMSObjectListener cl) {
        if (cl == null) {
            debug.error("LDAPEventManager.addObjectChangeListener " +
                "NULL object for change listener");
        }
        changeListener = cl;
        if (debug.messageEnabled()) {
            debug.message("LDAPEventManager.addObjectChangeListener " +
                "Adding listener: " + cl.getClass().getName());
        }
        try {
            // Verify is sms is not part of the disabled list
            String disabledList = SystemProperties.get(
                Constants.EVENT_LISTENER_DISABLE_LIST, "");
            if (disabledList.indexOf("sm") != -1) {
                String installTime = SystemProperties.get(
                    Constants.SYS_PROPERTY_INSTALL_TIME, "false");
                if (installTime.equals("false")) {
                    debug.error("LDAPEventManager.addObjectChangeListener " +
                        "Persistent search for SMS has been disabled by the " +
                        "property: " + Constants.EVENT_LISTENER_DISABLE_LIST);
                }
            }
            // Initialize Event Service for persistent search
            EventService.getEventService().resetAllSearches(false);
        } catch (EventException ex) {
            debug.error("LDAPEventManager.addObjectChangeListener " +
                "Unable to set persistent search", ex);
        } catch (LDAPException ex) {
            debug.error("LDAPEventManager.addObjectChangeListener " +
                "Unable to set persistent search", ex);
        }
    }
    
    static void removeObjectChangeListener() {
        changeListener = null;
        // Need to call EventService to disable SMS notifications
        try {
            EventService.getEventService().resetAllSearches(false);
        } catch (EventException ex) {
            debug.error("LDAPEventManager.removeObjectChangeListener " +
                "Unable to remove persistent search", ex);
        } catch (LDAPException ex) {
            debug.error("LDAPEventManager.removeObjectChangeListener " +
                "Unable to remove persistent search", ex);
        }
    }

    public synchronized void entryChanged(DSEvent dsEvent) {
        if (changeListener == null) {
            if (debug.warningEnabled()) {
                debug.warning("LDAPEventManager.entryChanged " +
                    "No listner objects registred");
            }
            return;
        }
        // Process entry changed events
        int event = dsEvent.getEventType();
        String dn = dsEvent.getID();
        switch (event) {
        case DSEvent.OBJECT_ADDED:
            event = SMSObjectListener.ADD;
            break;
        case DSEvent.OBJECT_REMOVED:
        case DSEvent.OBJECT_RENAMED:
            event = SMSObjectListener.DELETE;
            break;
        case DSEvent.OBJECT_CHANGED:
            event = SMSObjectListener.MODIFY;
            break;
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPEventManager:entry changed " + "for: "
                    + dn + " sending object changed notifications");
        }
        changeListener.objectChanged(dn, event);
    }

    public void eventError(String errorStr) {
        debug.error("LDAPEventManager.eventError(): " + errorStr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#allEntriesChanged()
     */
    public void allEntriesChanged() {
        if (changeListener == null) {
            if (debug.warningEnabled()) {
                debug.warning("LDAPEventManager.entryChanged " +
                    "No listner objects registred");
            }
            return;
        }
        if (debug.warningEnabled()) {
            debug.warning("LDAPEventManager: received all entries "
                + "changed event from EventService");
        }
        changeListener.allObjectsChanged();
    }

    /**
     * Returns the base DN for the persistent searches. Since this function
     * can be called asynchronously by the EventService, should not have
     * dependency on any classes in SMS package.
     * @see com.iplanet.services.ldap.event.IDSEventListener#getBase()
     */
    public String getBase() {
        if (baseDN != null) {
            return (baseDN);
        }
        try {
            // Obtain server instance for SMS, group=sms get baseDN
            // else use the default group
            ServerInstance serverInstance = null;
            DSConfigMgr mgr = DSConfigMgr.getDSConfigMgr();
            if (mgr != null) {
                // Try SMS first
                serverInstance = mgr.getServerInstance(
                    "sms", LDAPUser.Type.AUTH_PROXY);
                if (serverInstance == null) {
                    serverInstance = mgr.getServerInstance(
                        LDAPUser.Type.AUTH_PROXY);
                    if (debug.messageEnabled()) {
                        debug.message("LDAPEventManager: SMS servergroup " +
                            "not available. Using default AMSDK DN");
                    }
                }
                if (serverInstance != null) {
                    baseDN = serverInstance.getBaseDN();
                } else {
                    if (debug.warningEnabled()) {
                        debug.warning("LDAPEventManager: SMS & AMSDK " +
                            "servergroup not available. Using hardcoded value");
                    }
                }
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("LDAPEventManager: DSConfigMgr is NULL " +
                        "Using hardcoded value");
                }
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("LDAPEventManager: Exception obtaing baseDN " +
                    "from DSConfigMgr and ServerInstances", e);
            }
        }
        if (baseDN == null) {
            debug.error("LDAPEventManager.getBase(): Unable to get base DN " +
                " from serverconfig.xml");
        }
        return ((baseDN == null) ? "o=isp" : baseDN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#getFilter()
     */
    public String getFilter() {
        return SEARCH_FILTER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#getOperations()
     */
    public int getOperations() {
        return (LDAPPersistSearchControl.ADD
            | LDAPPersistSearchControl.MODIFY | LDAPPersistSearchControl.DELETE
            | LDAPPersistSearchControl.MODDN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#getScope()
     */
    public int getScope() {
        return LDAPConnection.SCOPE_SUB;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#setListeners()
     */
    public void setListeners(Map listener) {
        this.listeners = listener;
    }
    
    // ********** Work Around for Dead lock issue while starting EventService
    // Avoid initializing SMSEntry.
    protected static final String OC_SERVICE = "sunService";

    protected static final String OC_SERVICE_COMP = "sunServiceComponent";

    protected static final String OC_SERVICE_REALM = "sunRealmService";

    protected static final String SEARCH_FILTER = "(|(objectclass="
            + OC_SERVICE + ")(objectclass=" + OC_SERVICE_COMP
            + ")(objectclass=" + OC_SERVICE_REALM + "))";
}
