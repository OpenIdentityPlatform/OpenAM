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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.entitlement.indextree;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManagementDAO;
import org.forgerock.openam.core.guice.CoreGuiceModule.DNWrapper;
import org.forgerock.openam.core.guice.CoreGuiceModule.ShutdownManagerWrapper;
import org.forgerock.openam.entitlement.indextree.events.ModificationEvent;
import org.forgerock.openam.entitlement.indextree.events.ModificationEventType;
import org.forgerock.openam.entitlement.indextree.events.ErrorEventType;
import org.forgerock.openam.entitlement.indextree.events.EventType;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeEvent;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeObserver;
import org.forgerock.openam.entitlement.utils.indextree.IndexRuleTree;
import org.forgerock.openam.entitlement.utils.indextree.SimpleReferenceTree;

import javax.inject.Inject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides a search implementation that takes on a lazy approach to policy rule retrieval. Policy rules for a given
 * realm are only loaded into a index rule tree instance as search requests are made against that realm. This avoids
 * there being a potentially large memory consumption earlier on and instead builds up the data as it is required.
 *
 * @author apforrest
 */
public class IndexTreeServiceImpl implements IndexTreeService, IndexChangeObserver, ShutdownListener {

    private static final Debug DEBUG = Debug.getInstance("amEntitlements");

    private static final String INDEX_PATH_ATT = "pathindex";
    private static final String SEARCH_FILTER = "(sunserviceID=indexes)";
    private static final String REALM_DN_TEMPLATE =
            "ou=default,ou=OrganizationConfig,ou=1.0,ou=sunEntitlementIndexes,ou=services,%s";

    private final ConcurrentMap<String, IndexRuleTree> indexTreeCache;

    private final IndexChangeManager manager;
    private final PrivilegedAction<SSOToken> adminAction;
    private final ServiceManagementDAO smDAO;
    private final DNWrapper dnMapper;

    @Inject
    public IndexTreeServiceImpl(IndexChangeManager manager, PrivilegedAction<SSOToken> adminTokenAction,
                                ServiceManagementDAO smDAO, DNWrapper dnMapper,
                                ShutdownManagerWrapper shutdownManager) {

        this.manager = manager;
        this.adminAction = adminTokenAction;
        this.smDAO = smDAO;
        this.dnMapper = dnMapper;

        indexTreeCache = new ConcurrentHashMap<String, IndexRuleTree>();

        // Register to the shutdown to clean up appropriate resources.
        shutdownManager.addShutdownListener(this);

        // Initiate the index change manager so that index changes are propagated to the
        // cached index trees, ensuring the integrity and consistency of the data models.
        manager.registerObserver(this);
        manager.init();
    }

    @Override
    public Set<String> searchTree(String resource, String realm) throws EntitlementException {
        IndexRuleTree indexRuleTree = getIndexTree(realm);

        if (indexRuleTree == null) {
            return Collections.emptySet();
        }

        Set<String> results = indexRuleTree.searchTree(resource);

        if (DEBUG.messageEnabled()) {
            DEBUG.message(String.format("Matched index rules (resource:%s, realm:%s): %s", resource, realm, results));
        }

        return results;
    }

    /**
     * Retrieves the index rule tree for the given realm.
     *
     * @param realm
     *         The realm.
     * @return An index rule tree.
     * @throws EntitlementException
     *         When an error occurs reading policy data..
     */
    private IndexRuleTree getIndexTree(String realm) throws EntitlementException {
        IndexRuleTree indexTree = null;

        // It is important to note here that get() is used on the cache as opposed to contains() followed by a get().
        // This is done to make the retrieval of the tree atomic, whereas contains() follow by get() is not atomic
        // and therefore the result of contains() instantly becomes unreliable when get() is reached.
        indexTree = indexTreeCache.get(realm);

        if (indexTree == null) {

            synchronized (indexTreeCache) {
                // Double checking mechanism used here to help performance within a synchronised block.
                indexTree = indexTreeCache.get(realm);

                if (indexTree == null) {
                    // Create a new tree instance for the realm.
                    indexTree = createAndPopulateTree(realm);

                    if (indexTree != null) {
                        // Valid tree entry create, add to the cache.
                        indexTreeCache.put(realm, indexTree);
                    }
                }
            }
        }

        return indexTree;
    }

    /**
     * Populates a new instance of a index rule tree with policy path indexes retrieved from the associated realm.
     *
     * @param realm
     *         The realm for which policy path indexes are to be read from.
     * @return A newly created tree populated with rules configured against the realm.
     * @throws EntitlementException
     *         When an error occurs reading policy data.
     */
    private IndexRuleTree createAndPopulateTree(String realm) throws EntitlementException {
        IndexRuleTree indexTree = null;

        String baseDN = String.format(REALM_DN_TEMPLATE, dnMapper.orgNameToDN(realm));
        SSOToken token = AccessController.doPrivileged(adminAction);

        if (smDAO.checkIfEntryExists(baseDN, token)) {
            indexTree = new SimpleReferenceTree();

            try {
                Set<String> excludes = Collections.emptySet();
                // Carry out search.
                Iterator<SMSDataEntry> i = smDAO.search(token, baseDN, SEARCH_FILTER, 0, 0, false, false, excludes);

                while (i.hasNext()) {
                    SMSDataEntry e = i.next();

                    // Suppressed warning as unchecked assignment is valid.
                    @SuppressWarnings("unchecked")
                    Set<String> policyPathIndexes = e.getAttributeValues(INDEX_PATH_ATT);

                    indexTree.addIndexRules(policyPathIndexes);
                }

            } catch (SMSException smsE) {
                throw new EntitlementException(52, new Object[] {baseDN}, smsE);
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message(String.format("Index rule tree created for '%s'.", realm));
            }
        }

        return indexTree;
    }

    @Override
    public void update(IndexChangeEvent event) {
        EventType type = event.getType();

        if (ModificationEventType.contains(type)) {
            // Modification event received, update the appropriate cached tree.
            ModificationEventType modificationType = (ModificationEventType)type;
            ModificationEvent modification = (ModificationEvent)event;

            String realm = modification.getRealm();
            IndexRuleTree tree = indexTreeCache.get(realm);

            if (tree != null) {
                String pathIndex = modification.getPathIndex();

                switch (modificationType) {
                    case ADD:
                        tree.addIndexRule(pathIndex);
                        break;
                    case DELETE:
                        tree.removeIndexRule(pathIndex);
                        break;

                }

                if (DEBUG.messageEnabled()) {
                    DEBUG.message(String.format("Policy path index '%s' updated for realm '%s'.", pathIndex, realm));
                }
            }
        } else if (type == ErrorEventType.DATA_LOSS) {
            // Error event received, destroy the cache as policy updates may well have been lost, resulting in cached
            // trees becoming inconsistent. This will force all trees to be reloaded with clean data.
            indexTreeCache.clear();

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Potential policy path index loss, cached index trees cleared.");
            }
        }
    }


    @Override
    public void shutdown() {
        manager.removeObserver(this);
        manager.shutdown();
    }

}