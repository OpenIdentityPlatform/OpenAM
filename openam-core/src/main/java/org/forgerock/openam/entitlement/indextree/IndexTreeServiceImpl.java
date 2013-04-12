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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.entitlement.utils.indextree.IndexRuleTree;
import org.forgerock.openam.entitlement.utils.indextree.SimpleReferenceTree;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManagementDAO;

/**
 * Provides a search implementation that takes on a lazy approach to policy rule retrieval. Policy rules for a given
 * realm are only loaded into a index rule tree instance as search requests are made against that realm. This avoids
 * there being a potentially large memory consumption earlier on and instead builds up the data as it is required.
 * 
 * @author apforrest
 */
public class IndexTreeServiceImpl implements IndexTreeService {

    private static final Debug LOGGER = Debug.getInstance("amEntitlements");

    private static final String REALM_DN_TEMPLATE =
            "ou=default,ou=OrganizationConfig,ou=1.0,ou=sunEntitlementIndexes,ou=services,%s";
    private static final String FILTER = "(sunserviceID=indexes)";
    private static final String ATTRIBUTE_KEY = "pathindex";

    private final Map<String, IndexRuleTree> indexTreeCache;
    private final PrivilegedAction<SSOToken> adminAction;
    private final ServiceManagementDAO smDAO;
    private final DNWrapper dnMapper;

    @Inject
    public IndexTreeServiceImpl(PrivilegedAction<SSOToken> adminTokenAction,
                                ServiceManagementDAO smDAO, DNWrapper dnMapper) {
        indexTreeCache = new HashMap<String, IndexRuleTree>();
        this.adminAction = adminTokenAction;
        this.smDAO = smDAO;
        this.dnMapper = dnMapper;
    }

    @Override
    public Set<String> searchTree(String resource, String realm) throws EntitlementException {
        IndexRuleTree indexRuleTree = getIndexTree(realm);

        if (indexRuleTree == null) {
            return Collections.emptySet();
        }

        Set<String> results = indexRuleTree.searchTree(resource);
        
        if (LOGGER.messageEnabled()) {
            LOGGER.message(String.format("Matched index rules (resource:%s, realm:%s): %s", resource, realm, results));
        }

        return results;
    }

    /**
     * Retrieves the index rule tree for the given realm.
     * 
     * @param realm
     *            The realm.
     * @return An index rule tree.
     * @throws EntitlementException
     *             When an error occurs reading policy data..
     */
    private IndexRuleTree getIndexTree(String realm) throws EntitlementException {
        // Double check surrounding the synchronised block for better performance
        if (!indexTreeCache.containsKey(realm)) {
            synchronized (indexTreeCache) {
                if (!indexTreeCache.containsKey(realm)) {
                    loadTreeIntoCache(realm);
                }
            }
        }

        return indexTreeCache.get(realm);
    }

    /**
     * Populates a new instance of a index rule tree with policy rules retrieved from the associated realm.
     * 
     * @param realm
     *            The realm for which policy rules are to be read from.
     * @throws EntitlementException
     *             When an error occurs reading policy data.
     */
    private void loadTreeIntoCache(String realm) throws EntitlementException {
        String baseDN = String.format(REALM_DN_TEMPLATE, dnMapper.orgNameToDN(realm));
        SSOToken token = AccessController.doPrivileged(adminAction);

        if (smDAO.checkIfEntryExists(baseDN, token)) {
            IndexRuleTree indexTree = new SimpleReferenceTree();

            try {
                Set<String> excludes = Collections.emptySet();
                // Carry out search.
                Iterator<SMSDataEntry> i = smDAO.search(token, baseDN, FILTER, 0, 0, false, false, excludes);

                while (i.hasNext()) {
                    SMSDataEntry e = i.next();

                    // Suppressed warning as unchecked assignment is valid.
                    @SuppressWarnings("unchecked")
                    Set<String> attributeValues = e.getAttributeValues(ATTRIBUTE_KEY);

                    indexTree.addIndexRules(attributeValues);
                }

            } catch (SMSException smsE) {
                throw new EntitlementException(52, new Object[] { baseDN }, smsE);
            }

            // Cache the tree against the realm.
            indexTreeCache.put(realm, indexTree);

            if (LOGGER.messageEnabled()) {
                LOGGER.message(String.format("Index rule tree created for '%s'.", realm));
            }
        }
    }

    /**
     * Wrapper class to remove coupling to DNMapper static methods.
     *
     * Until DNMapper is refactored, this class can be used to assist with DI.
     */
    public static class DNWrapper {

        /**
         * @see DNMapper#orgNameToDN(String)
         */
        public String orgNameToDN(String orgName) {
            return DNMapper.orgNameToDN(orgName);
        }

    }

}