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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.idrepo.ldap.psearch;

import static org.forgerock.openam.ldap.LDAPConstants.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.iplanet.services.ldap.event.LDAPv3PersistentSearch;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.idrepo.ldap.IdentityMovedOrRenamedListener;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.controls.PersistentSearchChangeType;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;

/**
 * This class will execute persistent search request against the configured datastore. When a result is received, the
 * internal caches will be notified about the changes, so the caches can be dirtied.
 *
 * @author Peter Major
 */
public class DJLDAPv3PersistentSearch extends LDAPv3PersistentSearch<IdRepoListener, Set<IdType>> {

    private static final Debug DEBUG = Debug.getInstance("PersistentSearch");
    private final SearchResultEntryHandler resultEntryHandler = new PSearchResultEntryHandler();
    private final Set<IdentityMovedOrRenamedListener> movedOrRenamedListenerSet = new HashSet<>(1);
    private final String usersSearchAttributeName;

    public DJLDAPv3PersistentSearch(Map<String, Set<String>> configMap, ConnectionFactory factory) {
        super(CollectionHelper.getIntMapAttr(configMap, LDAP_RETRY_INTERVAL, 3000, DEBUG),
                DN.valueOf(CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_BASE_DN)), LDAPUtils
                        .parseFilter(CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_FILTER),
                                Filter.objectClassPresent()), LDAPUtils
                        .getSearchScope(CollectionHelper.getMapAttr(configMap, LDAP_PERSISTENT_SEARCH_SCOPE),
                                SearchScope.WHOLE_SUBTREE), factory,
                CollectionHelper.getMapAttr(configMap, LDAP_USER_SEARCH_ATTR));
        usersSearchAttributeName = CollectionHelper.getMapAttr(configMap, LDAP_USER_SEARCH_ATTR);

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

    @Override
    protected void clearCaches() {
        for (IdRepoListener idRepoListener : getListeners().keySet()) {
            idRepoListener.allObjectsChanged();
        }
    }

    @Override
    protected SearchResultEntryHandler getSearchResultEntryHandler() {
        return resultEntryHandler;
    }

    private final class PSearchResultEntryHandler implements LDAPv3PersistentSearch.SearchResultEntryHandler {

        @Override
        public boolean handle(SearchResultEntry entry, String dn, DN previousDn, PersistentSearchChangeType type) {
            if (type != null) {
                if (previousDn != null) {
                    for (IdentityMovedOrRenamedListener listener : movedOrRenamedListenerSet) {
                        listener.identityMovedOrRenamed(previousDn);
                    }
                }

                if (PersistentSearchChangeType.DELETE.equals(type)) {
                    for (IdentityMovedOrRenamedListener listener : movedOrRenamedListenerSet) {
                        listener.identityMovedOrRenamed(entry.getName());
                    }
                }

                for (Map.Entry<IdRepoListener, Set<IdType>> listenerEntry : getListeners().entrySet()) {
                    IdRepoListener listener = listenerEntry.getKey();

                    for (IdType idType : listenerEntry.getValue()) {
                        listener.objectChanged(dn, idType, type.intValue(), listener.getConfigMap());
                        if (idType.equals(IdType.USER)) {
                            listener.objectChanged(entry.parseAttribute(usersSearchAttributeName).asString(), idType,
                                    type.intValue(), listener.getConfigMap());
                        }
                    }
                }
            }
            return true;
        }
    }
}
