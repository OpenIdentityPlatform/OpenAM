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
 * $Id: SMSEventListenerManager.java,v 1.12 2009/01/28 05:35:03 ww203982 Exp $
 *
 * Portions Copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.sm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.SearchScope;

import com.sun.identity.shared.debug.Debug;

/**
 * Receives notifications for all SMS object changes from
 * <class>SMSNotificationManager</class> and dispatches the same to <class>
 * CachedSMSEntry</class> and <class>CachedSubEntries</class>. This class
 * also handles the case of sending deleted event for recursive deletes.
 */
class SMSEventListenerManager implements SMSObjectListener {

    // CachedSMSEntry objects
    private static final SetMultimap<DN, Subscription> nodeChanges = newSubscriberMultimap();
    
    // CachedSubEntries objects
    private static final SetMultimap<DN, Subscription> subNodeChanges = newSubscriberMultimap();

    // Static Initialization variables
    private static final Debug debug = SMSEntry.eventDebug;
    protected static boolean initialized;

    static void initialize() {
        if (!initialized) {
            try {
                if (SMSNotificationManager.isCacheEnabled()) {
                    SMSNotificationManager.getInstance().registerCallbackHandler(new SMSEventListenerManager());
                }
                debug.message("Initialized SMS Event listener");
                initialized = true;
            } catch (Exception e) {
                debug.error("SMSEventListenerManager::initialize Unable to initialize SMS listener: {}", e);
            }
        }
    }

    private SMSEventListenerManager() {
        // do nothing
    }

    // Processes object changed notifications
    @Override
    public void objectChanged(String odn, int event) {
        objectChanged(DN.valueOf(odn), event, false);
    }

    // Processes object changed notifications. The flag isLocal is used
    // distingush the self generated DELETE notifications for recursive
    // deletes, especially when data store notifications are disabled.
    // In which case, delete notifications will never be generated.
    private void objectChanged(DN dn, int event, boolean isLocal) {
        debug.message("SMSEventListener::entry changed for: {} type: {}", dn, event);

        // Normalize the DN

        // If event is delete, need to send notifications for sub-entries
        // Even if backend datastore notification is enabled, they woould
        // arrive much later causing write-through cache issues.
        if (!isLocal && (event == SMSObjectListener.DELETE)) {
            // Collect the immediate children of the current sdn
            // from nodeChanges. All "subNodeChanges" entries would
            // have an entry in "nodeChanges", hence do not have to
            // iterate throught it
            Set<DN> childDNs = new HashSet<>();
            synchronized (nodeChanges) {
                childDNs.addAll(nodeChanges.keySet());
            }
            for (Iterator<DN> it = childDNs.iterator(); it.hasNext();) {
                if (!it.next().isInScopeOf(dn, SearchScope.SUBORDINATES)) {
                    it.remove();
                }
            }
            // Send the notifications
            debug.message("SMSEventListener::objectChanged: Sending delete event of: {} to child nodes: {}", dn,
                    childDNs);

            for (DN item : childDNs) {
                objectChanged(item, event, true);
                // Send notifications to external listeners also if
                // data store notification is not enabled
                if (!SMSNotificationManager.isDataStoreNotificationEnabled()) {
                    SMSNotificationManager.getInstance().sendNotifications(item.toString().toLowerCase(), event, true);
                }
            }
        }

        // Send notifications to CachedSMSEntries
        sendNotifications(nodeChanges.get(dn), dn, event);
        
        // Process sub-entry changed events, not interested in attribute mods
        if (event == SMSObjectListener.ADD || event == SMSObjectListener.DELETE) {
            // Send notifications to CachedSubEntries
            debug.message("SMSEventListener::entry changed for: {} sending notifications to its parents", dn);

            sendNotifications(subNodeChanges.get(dn.parent()), dn, event);
        }
    }

    @Override
    public void allObjectsChanged() {
        if (debug.messageEnabled()) {
            debug.message("SMSEventListenerManager::allObjectsChanged called");
        }
        // Collect all the DNs from "nodeChanges" and send notifications
        // Send MODIFY notifications
        for (DN item : nodeChanges.keySet()) {
            objectChanged(item, SMSObjectListener.MODIFY, false);
        }
    }

    /**
     * Registers notification for changes to nodes
     */
    static Subscription registerForNotifyChangesToNode(String dn, SMSEventListener eventListener) {
        initialize();
        return addNotificationObject(nodeChanges, DN.valueOf(dn), eventListener);
    }

    /**
     * Registers notification for changes to its sub-nodes
     */
    static Subscription registerForNotifyChangesToSubNodes(String dn, SMSEventListener eventListener) {
        initialize();
        return addNotificationObject(subNodeChanges, DN.valueOf(dn), eventListener);
    }

    /**
     * Adds notification method to the map
     */
    private static Subscription addNotificationObject(SetMultimap<DN, Subscription> nodeChangeSubscribers, DN dn,
            SMSEventListener eventListener) {

        final Subscription subscription = new Subscription(eventListener, dn, nodeChangeSubscribers);
        nodeChangeSubscribers.put(dn, subscription);

        return subscription;
    }
    
    /**
     * Sends notification to methods and objects within the set
     */
    private static void sendNotifications(Set<Subscription> subscribers, DN dn, int event) {
        if (CollectionUtils.isEmpty(subscribers)) {
            return;
        }

        for (Subscription no : subscribers) {
            try {
                no.notifyEvent(dn, event);
            } catch (Exception e) {
                debug.error("SMSEvent notification: Unable to send notification: ", e);
            }
        }
    }

    static class Subscription {

        private final SMSEventListener eventListener;
        private final DN dn;
        private final SetMultimap<DN, Subscription> map;

        Subscription(SMSEventListener eventListener, DN dn,
                SetMultimap<DN, Subscription> map) {
            this.eventListener = eventListener;
            this.dn = dn;
            this.map = map;
        }

        public void cancel() {
            map.remove(dn, this);
        }

        private void notifyEvent(DN dn, int event) {
            eventListener.notifySMSEvent(dn, event);
        }
    }

    private static SetMultimap<DN, Subscription> newSubscriberMultimap() {
        return Multimaps.synchronizedSetMultimap(HashMultimap.<DN, Subscription>create());
    }
}
