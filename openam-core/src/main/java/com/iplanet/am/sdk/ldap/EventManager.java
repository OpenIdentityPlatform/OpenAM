/*
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
 * $Id: EventManager.java,v 1.7 2009/01/28 05:34:48 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */

package com.iplanet.am.sdk.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.iplanet.services.ldap.event.EventService;
import com.iplanet.services.ldap.event.IDSEventListener;

import com.iplanet.am.sdk.AMEventManagerException;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.AMStoreConnection;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.opendj.ldap.SearchScope;

/**
 * This <code>AMEventManager</code> adds Listeners to the EventService and
 * defines all the constants and other parameters needed to add the listeners.
 * Once initialized, it adds two separate listeners which listen to
 * notifications on ACI changes and entry modification/renaming/deletion events.
 * 
 * <p>
 * <b>NOTE:</b> This class is not a singleton class. So it should not be
 * instantiated directly. It is only instantiated when an instance of
 * AMCacheManager is created and since AMCacheManager class is a singleton it is
 * guaranted to have only one instance of this class.
 * <p>
 */
class EventManager {
    protected static final int EVENT_SCOPE = SearchScope.WHOLE_SUBTREE.intValue();

    protected static String EVENT_BASE_NODE = 
        AMStoreConnection.getAMSdkBaseDN();

    protected static final Class<? extends IDSEventListener> ACI_EVENT_LISTENER_CLASS = ACIEventListener.class;

    protected static final Class<? extends IDSEventListener> ENTRY_EVENT_LISTENER_CLASS = EntryEventListener.class;

    protected static final List<Class<? extends IDSEventListener>> PSEARCH_LISTENERS = new ArrayList<>();

    static {
        PSEARCH_LISTENERS.add(ACI_EVENT_LISTENER_CLASS);
        PSEARCH_LISTENERS.add(ENTRY_EVENT_LISTENER_CLASS);
    }

    private static Debug debug = Debug.getInstance("amEventService");

    /**
     * Constructor initializes the underlying UMS EventService and adds the
     * listeners
     */
    protected EventManager() throws AMEventManagerException {
    }

    protected static Debug getDebug() {
        return debug;
    }

    /**
     * This method starts the EventService of the UMS and registers two
     * listeners EntryEventListener and ACIEventListener to the EventService
     * inorder to receive notifications. Both the above listeners implement the
     * <code>com.iplanet.services.ldap.event.
     * IDSListener</code> interface.
     * <p>
     * 
     * NOTE: This method should be invoked only once.
     * 
     * @throws AMEventManagerException
     *             when encounters errors in starting the underlying
     *             EventService.
     */
    protected void addListeners(Map listeners) throws AMEventManagerException {
        // Get a handle to the (singleton object) Event Manager
        EventService eventService = null;
        try {
            if (debug.messageEnabled()) {
                debug.message("EventManager.start() - Getting EventService"
                        + " instance");
            }
            eventService = EventService.getEventService();
            synchronized (eventService) {
                if (!EventService.isStarted()) {
                    eventService.restartPSearches();
                }
            }
        } catch (Exception e) {
            debug.error("EventManager.start() Unable to get EventService ", e);
            throw new AMEventManagerException(AMSDKBundle.getString("501"),
                    "501");
        }

        // Initialize the listeners
        if (eventService != null) { // If "null" then disabled!!
            for (Class<? extends IDSEventListener> listenerClass : PSEARCH_LISTENERS) {
                IDSEventListener pSearchListener = eventService.getListener(listenerClass);
                if (pSearchListener != null) {
                    pSearchListener.setListeners(listeners);
                    debug.message("EventManager.start() - Added listeners to "
                            + "pSearch Listener: " + listenerClass.getSimpleName());
                }
            }
        }
    }
}
