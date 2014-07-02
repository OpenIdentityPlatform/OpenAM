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
 * Copyright 2013-2014 ForgeRock AS
 */
package org.forgerock.openam.idrepo.ldap.psearch;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static org.forgerock.openam.idrepo.ldap.LDAPConstants.*;
import org.forgerock.openam.idrepo.ldap.IdentityMovedOrRenamedListener;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.FutureResult;
import org.forgerock.opendj.ldap.RootDSE;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.PersistentSearchChangeType;
import org.forgerock.opendj.ldap.controls.PersistentSearchRequestControl;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.SearchResultHandler;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.EntryChangeNotificationResponseControl;
import org.forgerock.opendj.ldap.controls.GenericControl;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldap.responses.SearchResultReference;

/**
 * This class will execute persistent search request against the configured datastore. When a result is received, the
 * internal caches will be notified about the changes, so the caches can be dirtied.
 *
 * @author Peter Major
 */
public class DJLDAPv3PersistentSearch {

    private static final Debug DEBUG = Debug.getInstance("PersistentSearch");
    private final ConnectionFactory factory;
    private final Map<IdRepoListener, Set<IdType>> listenerMap = new ConcurrentHashMap<IdRepoListener, Set<IdType>>(1);
    private final Set<IdentityMovedOrRenamedListener> movedOrRenamedListenerSet =
            new HashSet<IdentityMovedOrRenamedListener>(1);
    private final int retryInterval;
    private final DN pSearchBaseDN;
    private final Filter pSearchFilter;
    private final SearchScope pSearchScope;
    private volatile boolean shutdown = false;
    private volatile Connection conn;
    private FutureResult<Result> futureResult;
    private PersistentSearchMode mode;
    private RetryTask retryTask;

    private enum PersistentSearchMode {

        STANDARD, AD, NONE
    }

    public DJLDAPv3PersistentSearch(Map<String, Set<String>> configMap, ConnectionFactory factory) {
        retryInterval = CollectionHelper.getIntMapAttr(configMap, LDAP_RETRY_INTERVAL, 3000, DEBUG);
        pSearchBaseDN = DN.valueOf(CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_BASE_DN));
        pSearchFilter = LDAPUtils.parseFilter(CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_FILTER),
                Filter.objectClassPresent());
        pSearchScope = LDAPUtils.getSearchScope(
                CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_SCOPE), SearchScope.WHOLE_SUBTREE);
        this.factory = factory;
    }

    private void detectPersistentSearchMode(Connection conn) throws ErrorResultException {
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
    public void addListener(IdRepoListener idRepoListener, Set<IdType> supportedTypes) {
        listenerMap.put(idRepoListener, supportedTypes);
    }

    /**
     * Removes an {@link IdRepoListener} if it was registered to persistent search notifications.
     * The caller must ensure that calls to addListener/removeListener/hasListeners invocations are synchronized
     * correctly.
     *
     * @param idRepoListener The {@link IdRepoListener} instance that needs to be notified about changes.
     */
    public void removeListener(IdRepoListener idRepoListener) {
        listenerMap.remove(idRepoListener);
    }

    /**
     * Checks if there are any registered listeners for this persistent search connection.
     * The caller must ensure that calls to addListener/removeListener/hasListeners invocations are synchronized
     * correctly.
     */
    public boolean hasListeners() {
        return !listenerMap.isEmpty();
    }

    /**
     * Adds an {@link IdentityMovedOrRenamedListener} object, which needs to be notified about persistent search results
     * where the identity has been renamed or moved.
     * The caller must ensure that calls to addMovedOrRenamedListener/removeMovedOrRenamedListener invocations are
     * synchronized correctly.
     *
     * @param movedOrRenamedListener The {@link IdentityMovedOrRenamedListener} instance that needs to be notified about
     *                               changes.
     */
    public void addMovedOrRenamedListener(IdentityMovedOrRenamedListener movedOrRenamedListener) {
        movedOrRenamedListenerSet.add(movedOrRenamedListener);
    }

    /**
     * Removes an {@link IdentityMovedOrRenamedListener} if it was registered to get persistent search notifications.
     * The caller must ensure that calls to addMovedOrRenamedListener/removeMovedOrRenamedListener invocations are
     * synchronized correctly.
     *
     * @param movedOrRenamedListener The {@link IdentityMovedOrRenamedListener} instance to remove from the listeners
     */
    public void removeMovedOrRenamedListener(IdentityMovedOrRenamedListener movedOrRenamedListener) {
        movedOrRenamedListenerSet.remove(movedOrRenamedListener);
    }

    /**
     * Starts the persistent search connection against the directory. The caller must ensure that calls made to
     * startPSearch and stopPsearch are properly synchronized.
     */
    public void startPSearch() {
        try {
            conn = factory.getConnection();
            startPSearch(conn);
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to initiate persistent search connection", ere);
            restartPSearch();
        }
    }

    private void startPSearch(Connection conn) throws ErrorResultException {
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
                control = PersistentSearchRequestControl.newControl(true, true, true,
                        EnumSet.allOf(PersistentSearchChangeType.class));
                attrs = new String[]{DN_ATTR};
            }
            break;
            case AD: {
                control = GenericControl.newControl(AD_NOTIFICATION_OID, true);
                attrs = new String[]{DN_ATTR, AD_IS_DELETED_ATTR, AD_WHEN_CHANGED_ATTR, AD_WHEN_CREATED_ATTR};
            }
        }
        SearchRequest searchRequest = Requests.newSearchRequest(pSearchBaseDN, pSearchScope, pSearchFilter, attrs);
        searchRequest.addControl(control);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Starting persistent search against baseDN: " + pSearchBaseDN
                    + ", scope: " + pSearchScope.toString() + ", filter: " + pSearchFilter
                    + ", attrs: " + Arrays.toString(attrs) + " against " + factory.toString());
        }
        //since psearch wasn't running until now, let's clear the caches to make sure that if something got into the
        //cache, while PS was stopped, those gets cleared out and we start with a clean cache.
        clearCaches();
        futureResult = conn.searchAsync(searchRequest, null, new PSearchResultHandler());
    }

    /**
     * Stops the persistent search request, and terminates the LDAP connection. The caller must ensure that calls made
     * to startPSearch and stopPsearch are properly synchronized.
     */
    public void stopPSearch() {
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

    private void restartPSearch() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Restarting persistent search connection against: " + factory.toString());
        }
        //just to be really sure
        IOUtils.closeIfNotNull(conn);
        if (!shutdown) {
            //we shouldn't try to restart psearch if we are in shutdown mode.
            retryTask = new RetryTask();
            SystemTimerPool.getTimerPool().schedule(retryTask,
                    new Date(System.currentTimeMillis() + retryInterval / 1000 * 1000));
        }
    }

    private void clearCaches() {
        for (IdRepoListener idRepoListener : listenerMap.keySet()) {
            idRepoListener.allObjectsChanged();
        }
    }

    private class PSearchResultHandler implements SearchResultHandler {

        public boolean handleEntry(SearchResultEntry entry) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Processing persistent search response: " + entry.toString());
            }
            String dn = entry.getName().toString();
            DN previousDn = null;
            int type = -1;
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
                            type = changeType.intValue();
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
                        type = PersistentSearchChangeType.DELETE.intValue();
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
                            type = PersistentSearchChangeType.ADD.intValue();
                        } else {
                            type = PersistentSearchChangeType.MODIFY.intValue();
                        }
                    }
                }
                break;
                default:
                    throw new IllegalStateException("Persistent search mode has invalid value: " + mode);
            }

            if (type != -1) {
                if (previousDn != null) {
                    for (IdentityMovedOrRenamedListener listener : movedOrRenamedListenerSet) {
                            listener.identityMovedOrRenamed(previousDn);
                    }
                }

                for (Map.Entry<IdRepoListener, Set<IdType>> listenerEntry : listenerMap.entrySet()) {
                    IdRepoListener listener = listenerEntry.getKey();

                    for (IdType idType : listenerEntry.getValue()) {
                        listener.objectChanged(dn, idType, type, listener.getConfigMap());
                    }
                }
            }
            return true;
        }

        public boolean handleReference(SearchResultReference reference) {
            //ignoring references
            return true;
        }

        public void handleErrorResult(ErrorResultException error) {
            if (!shutdown) {
            	DEBUG.error("An error occurred while executing persistent search", error);
            	clearCaches();
                restartPSearch();
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
                startPSearch(conn);
                //everything seems to work, let's disable retryTask and reset the debug limit
                runPeriod = -1;
                lastLogged = 0;
            } catch (Exception ex) {
                long now = System.currentTimeMillis();
                if (now - lastLogged > 60000) {
                    DEBUG.error("Unable to start persistent search: " + ex.getMessage());
                    lastLogged = now;
                }
                IOUtils.closeIfNotNull(conn);
            }
        }
    }
}
