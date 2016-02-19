/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 */

package com.iplanet.services.ldap.event;

import static org.forgerock.openam.ldap.LDAPConstants.*;
import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.LdapPromise;
import org.forgerock.opendj.ldap.RootDSE;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.EntryChangeNotificationResponseControl;
import org.forgerock.opendj.ldap.controls.GenericControl;
import org.forgerock.opendj.ldap.controls.PersistentSearchChangeType;
import org.forgerock.opendj.ldap.controls.PersistentSearchRequestControl;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.responses.SearchResultReference;

public abstract class LDAPv3PersistentSearch<T, H> {

    private static final Debug DEBUG = Debug.getInstance("PersistentSearch");
    private static final boolean CHANGES_ONLY = true;
    private static final boolean RETURN_CONTROLS = true;
    private static final boolean IS_CRITICAL = true;
    private static final List<String> AD_DEFAULT_ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(
            AD_IS_DELETED_ATTR, AD_WHEN_CHANGED_ATTR, AD_WHEN_CREATED_ATTR));

    private final ConnectionFactory factory;
    private final Map<T, H> listeners = new ConcurrentHashMap<>(1);
    private final int retryInterval;
    private final DN searchBaseDN;
    private final Filter searchFilter;
    private final SearchScope searchScope;
    private final List<String> attributeNames;
    private volatile boolean shutdown = false;
    private volatile Connection conn;
    private LdapPromise<Result> futureResult;
    private PersistentSearchMode mode;
    private RetryTask retryTask;

    private enum PersistentSearchMode {
        STANDARD, AD, NONE
    }

    public LDAPv3PersistentSearch(int retryInterval, DN searchBaseDN, Filter searchFilter,
            SearchScope searchScope, ConnectionFactory factory, String... attributeNames) {
        this.retryInterval = retryInterval;
        this.searchBaseDN = searchBaseDN;
        this.searchFilter = searchFilter;
        this.searchScope = searchScope;
        this.factory = factory;
        this.attributeNames = Arrays.asList(attributeNames);
    }

    private void detectPersistentSearchMode(Connection conn) throws LdapException {
        RootDSE dse = RootDSE.readRootDSE(conn);
        Collection<String> supportedControls = dse.getSupportedControls();
        if (supportedControls.contains(PersistentSearchRequestControl.OID)) {
            mode = PersistentSearchMode.STANDARD;
        } else if (supportedControls.contains(AD_NOTIFICATION_OID)) {
            mode = PersistentSearchMode.AD;
        } else {
            mode = PersistentSearchMode.NONE;
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Persistent search mode detected: " + mode.name());
        }
    }

    /**
     * Adds an {@link IdRepoListener} object, which needs to be notified about persistent search results.
     * The caller must ensure that calls to addListener/removeListener/hasListeners invocations are synchronized
     * correctly.
     *
     * @param idRepoListener The {@link IdRepoListener} instance that needs to be notified about changes.
     * @param supportedTypes The supported {@link IdType}s for which events needs to be generated.
     */
    public void addListener(T idRepoListener, H supportedTypes) {
        listeners.put(idRepoListener, supportedTypes);
    }

    /**
     * Removes an {@link IdRepoListener} if it was registered to persistent search notifications.
     * The caller must ensure that calls to addListener/removeListener/hasListeners invocations are synchronized
     * correctly.
     *
     * @param idRepoListener The {@link IdRepoListener} instance that needs to be notified about changes.
     */
    public void removeListener(T idRepoListener) {
        listeners.remove(idRepoListener);
    }

    /**
     * Checks if there are any registered listeners for this persistent search connection.
     * The caller must ensure that calls to addListener/removeListener/hasListeners invocations are synchronized
     * correctly.
     */
    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    /**
     * Starts the persistent search connection against the directory. The caller must ensure that calls made to
     * startPSearch and stopPsearch are properly synchronized.
     */
    public void startSearch() {
        try {
            conn = factory.getConnection();
            startSearch(conn);
        } catch (LdapException ere) {
            DEBUG.error("An error occurred while trying to initiate persistent search connection", ere);
            DEBUG.message("Restarting persistent search");
            restartSearch();
        }
    }

    private void startSearch(Connection conn) throws LdapException {
        if (mode == null) {
            detectPersistentSearchMode(conn);
        }
        Control control = null;
        String[] attrs = null;
        //mode shouldn't be null here, if something failed during the detection it should've resulted in an
        //exception already.
        switch (mode) {
            case NONE: {
                DEBUG.error("Persistent search is not supported by the directory, persistent search will be disabled");
                return;
            }
            case STANDARD: {
                control = PersistentSearchRequestControl.newControl(IS_CRITICAL, CHANGES_ONLY, RETURN_CONTROLS,
                        EnumSet.allOf(PersistentSearchChangeType.class));
                List<String> attributes = new ArrayList<>(attributeNames);
                attributes.add(DN_ATTR);
                attrs = attributes.toArray(new String[0]);
            }
            break;
            case AD: {
                control = GenericControl.newControl(AD_NOTIFICATION_OID, true);
                List<String> attributes = new ArrayList<>(attributeNames);
                attributes.addAll(AD_DEFAULT_ATTRIBUTES);
                attributes.add(DN_ATTR);
                attrs = attributes.toArray(new String[0]);
            }
        }
        SearchRequest searchRequest = LDAPRequests.newSearchRequest(searchBaseDN, searchScope, searchFilter, attrs);
        searchRequest.addControl(control);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Starting persistent search against baseDN: " + searchBaseDN
                    + ", scope: " + searchScope.toString() + ", filter: " + searchFilter
                    + ", attrs: " + Arrays.toString(attrs) + " against " + factory.toString());
        }
        //since psearch wasn't running until now, let's clear the caches to make sure that if something got into the
        //cache, while PS was stopped, those gets cleared out and we start with a clean cache.
        clearCaches();
        futureResult = conn.searchAsync(searchRequest, null, new PersistentSearchResultHandler());
    }

    /**
     * Stops the persistent search request, and terminates the LDAP connection. The caller must ensure that calls made
     * to startPSearch and stopPsearch are properly synchronized.
     */
    public void stopSearch() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Stopping persistent search against: " + factory.toString());
        }
        if (hasListeners()) {
            throw new IllegalStateException("Persistent search has assigned listeners, unable to stop.");
        }
        shutdown = true;
        if (futureResult != null) {
            futureResult.cancel(true);
        }
        if (retryTask != null) {
            retryTask.cancel();
        }
        IOUtils.closeIfNotNull(conn);
        IOUtils.closeIfNotNull(factory);
    }

    private void restartSearch() {
        DEBUG.message("Restarting persistent search connection against: {}", factory.toString());
        //just to be really sure
        IOUtils.closeIfNotNull(conn);
        if (!shutdown) {
            //we shouldn't try to restart psearch if we are in shutdown mode.
            retryTask = new RetryTask();
            try {
               // Schedules the task for the exact second without any non-zero milliseconds
               SystemTimerPool.getTimerPool().schedule(retryTask,
                    new Date(currentTimeMillis() + retryInterval / 1000 * 1000));
            } catch (IllegalMonitorStateException e) {
                DEBUG.warning("PSearch was not restarted, application may be shutting down:", e);
            }
        }
    }

    protected abstract void clearCaches();

    protected Map<T, H> getListeners() {
        return Collections.unmodifiableMap(listeners);
    }

    protected interface SearchResultEntryHandler {
        boolean handle(SearchResultEntry entry, String dn, DN previousDn, PersistentSearchChangeType type);
    }

    protected abstract SearchResultEntryHandler getSearchResultEntryHandler();

    private class PersistentSearchResultHandler implements SearchResultHandler {

        public boolean handleEntry(SearchResultEntry entry) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Processing persistent search response: " + entry.toString());
            }
            String dn = entry.getName().toString();
            DN previousDn = null;
            PersistentSearchChangeType type = null;
            switch (mode) {
                case STANDARD: {
                    try {
                        EntryChangeNotificationResponseControl control = entry.getControl(
                                EntryChangeNotificationResponseControl.DECODER, new DecodeOptions());
                        if (control != null) {
                            PersistentSearchChangeType changeType = control.getChangeType();
                            if (changeType.equals(PersistentSearchChangeType.MODIFY_DN)) {
                                previousDn = control.getPreviousName();
                            }
                            type = changeType;
                        }
                    } catch (DecodeException de) {
                        DEBUG.warning("Unable to decode EntryChangeNotificationResponseControl", de);
                    }
                }
                break;
                case AD: {
                    boolean isDeleted = false;
                    Attribute attr = entry.getAttribute(AD_IS_DELETED_ATTR);
                    if (attr != null && attr.size() == 1) {
                        isDeleted = entry.parseAttribute(AD_IS_DELETED_ATTR).asBoolean(false);
                    }
                    if (isDeleted) {
                        type = PersistentSearchChangeType.DELETE;
                    } else {
                        String whenCreated = entry.parseAttribute(AD_WHEN_CREATED_ATTR).asString();
                        if (whenCreated == null) {
                            if (DEBUG.warningEnabled()) {
                                DEBUG.warning("Missing attribute " + AD_WHEN_CREATED_ATTR + " in persistent search response");
                            }
                            //advance to the next entry and ignore this one
                            return true;
                        }
                        String whenChanged = entry.parseAttribute(AD_WHEN_CHANGED_ATTR).asString();
                        if (whenChanged == null) {
                            if (DEBUG.warningEnabled()) {
                                DEBUG.warning("Missing attribute " + AD_WHEN_CHANGED_ATTR + " in persistent search response");
                            }
                            //advance to the next entry and ignore this one
                            return true;
                        }
                        if (whenCreated.equals(whenChanged)) {
                            type = PersistentSearchChangeType.ADD;
                        } else {
                            type = PersistentSearchChangeType.MODIFY;
                        }
                    }
                }
                break;
                default:
                    throw new IllegalStateException("Persistent search mode has invalid value: " + mode);
            }

            return getSearchResultEntryHandler().handle(entry, dn, previousDn, type);
        }

        public boolean handleReference(SearchResultReference reference) {
            //ignoring references
            return true;
        }

        public void handleErrorResult(LdapException error) {
            if (!shutdown) {
                DEBUG.error("An error occurred while executing persistent search", error);
                DEBUG.message("Restarting persistent search. Some changes may have been missed in the interim.");
                clearCaches();
                restartSearch();
            } else {
                DEBUG.message("Persistence search has been cancelled",error);
            }
        }

        public void handleResult(Result result) {
        }
    }

    private class RetryTask extends GeneralTaskRunnable {

        private long runPeriod;
        private long lastLogged = 0;

        public RetryTask() {
            runPeriod = retryInterval;
        }

        public boolean addElement(Object key) {
            return false;
        }

        public boolean removeElement(Object key) {
            return false;
        }

        public boolean isEmpty() {
            return true;
        }

        public long getRunPeriod() {
            return runPeriod;
        }

        public void run() {
            try {
                conn = factory.getConnection();
                startSearch(conn);
                //everything seems to work, let's disable retryTask and reset the debug limit
                runPeriod = -1;
                lastLogged = 0;
            } catch (Exception ex) {
                long now = currentTimeMillis();
                if (now - lastLogged > 60000) {
                    DEBUG.error("Unable to start persistent search: " + ex.getMessage());
                    lastLogged = now;
                }
                IOUtils.closeIfNotNull(conn);
            }
        }
    }
}
