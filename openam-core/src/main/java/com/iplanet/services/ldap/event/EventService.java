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
 * $Id: EventService.java,v 1.19 2009/09/28 21:47:33 ww203982 Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */

package com.iplanet.services.ldap.event;

import java.math.BigInteger;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.iplanet.am.sdk.ldap.ACIEventListener;
import com.iplanet.am.sdk.ldap.EntryEventListener;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ldap.LDAPEventManager;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.PersistentSearchChangeType;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

/**
 *
 *
 * @supported.api
 */
public class EventService {

    private static Debug logger = Debug.getInstance("amEventService");
    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);
    private static DSConfigMgr cm = null;
    private static final String EVENT_CONNECTION_RETRY_INTERVAL =
            "com.iplanet.am.event.connection.delay.between.retries";
    private static final int RETRY_INTERVAL = SystemProperties.getAsInt(EVENT_CONNECTION_RETRY_INTERVAL, 3000);
    private static final String EVENT_LISTENER_DISABLE_LIST = "com.sun.am.event.connection.disable.list";
    private static final Class<? extends IDSEventListener> ACI_EVENT_LISTENER_CLASS_NAME = ACIEventListener.class;
    private static final Class<? extends IDSEventListener> ENTRY_EVENT_LISTENER_CLASS_NAME = EntryEventListener.class;
    private static final Class<? extends IDSEventListener> LDAP_EVENT_LISTENER_CLASS_NAME = LDAPEventManager.class;

    private static volatile boolean isShutdownCalled = false;
    private static volatile boolean isRunning = false;

    private ConnectionFactory adminConnectionFactory;
    private ConnectionFactory smsConnectionFactory;
    private final Map<Class<? extends IDSEventListener>, ListenerSearch> persistentSearches = new HashMap<>();

    private static final class ListenerSearch {
        private final IDSEventListener listener;
        private final EventServicePersistentSearch search;
        private ListenerSearch(IDSEventListener listener, EventServicePersistentSearch search) {
            this.listener = listener;
            this.search = search;
        }
    }

    private enum Singleton {
        INSTANCE;

        private EventService eventService;
        private EventException eventException;

        Singleton() {
            try {
                eventService = new EventService();
                ShutdownManager shutdownMan = com.sun.identity.common.ShutdownManager.getInstance();
                shutdownMan.addShutdownListener(
                        new ShutdownListener() {
                            public void shutdown() {
                                if (eventService != null) {
                                    eventService.stopPSearches();
                                }
                            }
                        });
            } catch (EventException e) {
                eventException = e;
            }
        }

        private EventService getEventService() throws EventException {
            if (eventException != null) {
                throw eventException;
            }
            return eventService;
        }
    }

    private EventService() throws EventException {
        try {
            cm = DSConfigMgr.getDSConfigMgr();
        } catch (LDAPServiceException lse) {
            logger.error("EventService.getConfigManager() - Failed to get handle to Configuration Manager", lse);
            throw new EventException(i18n.getString(IUMSConstants.DSCFG_NOCFGMGR), lse);
        }
    }

    /**
     *
     *
     * @supported.api
     */
    public synchronized static EventService getEventService() throws EventException, LdapException {
        if (isShutdownCalled) {
            return null;
        }
        return Singleton.INSTANCE.getEventService();
    }

    //Question: is ok to not actually restart running psearches if the listener is still enabled?
    public synchronized void restartPSearches() {
        List<Class<? extends IDSEventListener>> listenersClasses = getEnabledListenersClasses();

        for (Iterator<Class<? extends IDSEventListener>> iterator = persistentSearches.keySet().iterator();
             iterator.hasNext();) {
            Class<? extends IDSEventListener> pSearchListenerClass = iterator.next();
            //remove any running psearches that do not have enabled listeners
            if (!listenersClasses.contains(pSearchListenerClass)) {
                persistentSearches.get(pSearchListenerClass).search.stopSearch();
                iterator.remove();
            } else {
                listenersClasses.remove(pSearchListenerClass);
            }
        }

        for (Iterator<Class<? extends IDSEventListener>> iterator = listenersClasses.iterator(); iterator.hasNext();) {
            Class<? extends IDSEventListener> listenerClass = iterator.next();

            try {
                IDSEventListener listener = listenerClass.newInstance();

                EventServicePersistentSearch pSearch = new EventServicePersistentSearch(RETRY_INTERVAL,
                        DN.valueOf(listener.getBase()), Filter.valueOf(listener.getFilter()),
                        SearchScope.valueOf(listener.getScope()), getConnectionFactory(listener.getClass()),
                        "objectclass");

                pSearch.addListener(listener, new BigInteger(130, new Random()).toString());

                pSearch.startSearch();
                persistentSearches.put(listenerClass, new ListenerSearch(listener, pSearch));
                logger.message("EventService.restartPSearches() - successfully initialized: {}", listenerClass);
                iterator.remove();
            } catch (Exception e) {
                logger.error("EventService.restartPSearches() Unable to start listener {}", listenerClass, e);
            }
        }

        if (!listenersClasses.isEmpty()) {
            for (Class<? extends IDSEventListener> listenerClass : listenersClasses) {
                logger.error("EventService.restartPSearches(): unable add listener: {}", listenerClass);
            }
        }
        isRunning = true;
    }

    public synchronized void stopPSearches() {
        isShutdownCalled = true;
        for (ListenerSearch pSearch : persistentSearches.values()) {
            pSearch.search.removeListener(pSearch.listener);
            pSearch.search.stopSearch();
        }
    }

    public static boolean isStarted() {
        return isRunning && !isShutdownCalled;
    }

    public IDSEventListener getListener(Class<? extends IDSEventListener> listenerClass) {
        return persistentSearches.get(listenerClass).listener;
    }

    private static List<Class<? extends IDSEventListener>> getEnabledListenersClasses() {

        Collection<String> disabledListeners = getDisabledListeners();
        boolean disableACI = disabledListeners.contains("aci");
        boolean disableUM = disabledListeners.contains("um");
        boolean disableSM = disabledListeners.contains("sm");

        if (!disableUM || !disableACI) {
            // Check if AMSDK is configured
            if (!isAMSDKConfigured()) {
                disableUM = true;
                disableACI = true;
                if (logger.messageEnabled()) {
                    logger.message("EventService.getListenerList(): AMSDK is not configured or config time. "
                            + "Disabling UM and ACI event listeners");
                }
            }
        }

        //psearch terminated if you disable the DB notifications, or add 'sm' to the list of disabled
        if (!disableSM) {
            disableSM = !Boolean.parseBoolean(SystemProperties.get(Constants.SMS_ENABLE_DB_NOTIFICATION));
        }

        if (logger.messageEnabled()) {
            logger.message("EventService.getListenerList(): SMS listener is enabled: {}", !disableSM);
        }

        List<Class<? extends IDSEventListener>> listeners = new ArrayList<>();
        // Disable the selected listeners
        if (!disableACI) {
            listeners.add(ACI_EVENT_LISTENER_CLASS_NAME);
        }
        if (!disableUM) {
            listeners.add(ENTRY_EVENT_LISTENER_CLASS_NAME);
        }
        if (!disableSM) {
            listeners.add(LDAP_EVENT_LISTENER_CLASS_NAME);
        }

        if (disableACI && disableUM && disableSM) {
            logger.message("EventService.getListenerList() - all listeners are disabled, EventService won't start");
        }

        return listeners;
    }

    private ConnectionFactory getConnectionFactory(Class<? extends IDSEventListener> listenerClass)
            throws LDAPServiceException {
        if (LDAPEventManager.class.equals(listenerClass) && cm.getServerGroup("sms") != null) {
            return getSmsConnectionFactory();
        } else {
            return getAdminConnectionFactory();
        }
    }

    private ConnectionFactory getAdminConnectionFactory() throws LDAPServiceException {
        if (adminConnectionFactory == null) {
            adminConnectionFactory = DSConfigMgr.getDSConfigMgr().getNewAdminConnectionFactory();
        }
        return adminConnectionFactory;
    }

    private ConnectionFactory getSmsConnectionFactory() throws LDAPServiceException {
        if (smsConnectionFactory == null) {
            smsConnectionFactory = DSConfigMgr.getDSConfigMgr()
                    .getNewConnectionFactory("sms", LDAPUser.Type.AUTH_ADMIN);
        }
        return smsConnectionFactory;
    }

    private void dispatchException(Exception e, String requestId, IDSEventListener listener) {
        logger.error("EventService.dispatchException() - dispatching exception to the listener: {} Listener: {}",
                requestId, listener, e);
        listener.eventError(e.toString());
    }

    private void dispatchEvent(DSEvent dirEvent, IDSEventListener listener) {
        listener.entryChanged(dirEvent);
    }

    private DSEvent createDSEvent(Entry entry, PersistentSearchChangeType changeType, String requestId,
            IDSEventListener listener) throws Exception {
        DSEvent dsEvent = new DSEvent();

        logger.message("EventService.createDSEvent() - Notifying event to: {}", listener);

        // Get the dn from the entry
        String dn = entry.getName().toString();
        dsEvent.setID(dn);

        // Get information on the type of change made
        dsEvent.setEventType(changeType.intValue());

        // Pass the search ID as the event's change info
        dsEvent.setSearchID(requestId);

        // set the object class name
        String className = entry.getAttribute("objectclass").toString();
        dsEvent.setClassName(className);

        return dsEvent;
    }

    private static Collection<String> getDisabledListeners() {
        List<String> disabledListeners = new ArrayList<>();

        String list = SystemProperties.get(EVENT_LISTENER_DISABLE_LIST, "");
        logger.message("EventService.getListenerList(): {}: {}", EVENT_LISTENER_DISABLE_LIST, list);

        for (String disabledListener : list.split(",")) {
            disabledListeners.add(disabledListener.trim());
        }

        return disabledListeners;
    }

    private static boolean isDuringConfigurationTime() {
        return Boolean.parseBoolean(SystemProperties.get(Constants.SYS_PROPERTY_INSTALL_TIME));
    }

    private static boolean isAMSDKConfigured() {
        boolean isAMSDKConfigured = false;

        boolean configTime = isDuringConfigurationTime();
        logger.message("EventService.getListenerList(): {}: {}", Constants.SYS_PROPERTY_INSTALL_TIME, configTime);
        if (!configTime) {
            try {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        AccessController.doPrivileged(AdminTokenAction.getInstance()), IdConstants.REPO_SERVICE, "1.0");
                ServiceSchema idRepoSubSchema = scm.getOrganizationSchema();
                Set idRepoPlugins = idRepoSubSchema.getSubSchemaNames();
                if (idRepoPlugins.contains("amSDK")) {
                    isAMSDKConfigured = true;
                }
            } catch (SMSException ex) {
                logger.warning("EventService.getListenerList() - Unable to obtain idrepo service", ex);
            } catch (SSOException ex) {
                // Should not happen, ignore the exception
            }
        }
        return isAMSDKConfigured;
    }

    private final class EventServicePersistentSearch extends LDAPv3PersistentSearch<IDSEventListener, String> {

        private final SearchResultEntryHandler resultEntryHandler = new PSearchResultEntryHandler();

        public EventServicePersistentSearch(int retryInterval, DN pSearchBaseDN, Filter pSearchFilter,
                SearchScope pSearchScope, ConnectionFactory factory, String... attributeNames) {
            super(retryInterval, pSearchBaseDN, pSearchFilter, pSearchScope, factory, attributeNames);
        }

        @Override
        protected void clearCaches() {
            for (IDSEventListener listener : getListeners().keySet()) {
                listener.allEntriesChanged();
            }
        }

        @Override
        protected SearchResultEntryHandler getSearchResultEntryHandler() {
            return resultEntryHandler;
        }

        private final class PSearchResultEntryHandler implements LDAPv3PersistentSearch.SearchResultEntryHandler {

            private final Exception EXCEPTION =
                    new Exception("EventService - Cannot create NamingEvent, no change control info");

            @Override
            public boolean handle(SearchResultEntry entry, String dn, DN previousDn, PersistentSearchChangeType type) {
                for (Map.Entry<IDSEventListener, String> listener : getListeners().entrySet()) {
                    if (type != null) {
                        logger.message("EventService.processSearchResultMessage() changeCtrl = {}", type.toString());
                        // Convert control into a DSEvent and dispatch to listeners
                        try {
                            DSEvent event = createDSEvent(entry, type, listener.getValue(), listener.getKey());
                            dispatchEvent(event, listener.getKey());
                        } catch (Exception ex) {
                            dispatchException(ex, listener.getValue(), listener.getKey());
                        }
                    } else {
                        dispatchException(EXCEPTION, listener.getValue(), listener.getKey());
                    }
                }
                return true;
            }
        }
    }
}
