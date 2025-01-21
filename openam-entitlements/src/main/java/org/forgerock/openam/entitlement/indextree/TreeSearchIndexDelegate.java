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
 * Copyright 2013-2014 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.entitlement.indextree;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.util.ResourceNameSplitter;
import org.forgerock.guice.core.InjectorHolder;

import jakarta.inject.Inject;
import java.util.Set;

/**
 * Implementation provides search index by means of an in memory rule index cache.
 * <p/>
 * Expects the passed resource to have already been normalised.
 */
public class TreeSearchIndexDelegate implements ISearchIndex {

    private static final ISearchIndex legacySearchIndex;

    private final IndexTreeService indexTreeService;

    static {
        // TODO: Deprecate the need to call into the earlier implementation to assist with subtree mode.
        legacySearchIndex = new ResourceNameSplitter();
    }

    /**
     * This constructor has been added as the entitlements framework expects a no args constructor due to its use of
     * reflection for object instantiation. However, as this implementation has dependencies, and to reduce coupling,
     * where possible this constructor should be avoided and an alternative should be used in favour.
     */
    public TreeSearchIndexDelegate() {
        this(InjectorHolder.getInstance(IndexTreeService.class));
    }

    @Inject
    public TreeSearchIndexDelegate(IndexTreeService indexTreeService) {
        this.indexTreeService = indexTreeService;
    }

    @Override
    public ResourceSearchIndexes getIndexes(String resource, String realm) throws EntitlementException {
        // Create legacy indexes first.
        ResourceSearchIndexes legacyIndexes = legacySearchIndex.getIndexes(resource, realm);

        // Indexes are handled in lower case.
        resource = resource.toLowerCase();
        // Search the index tree for matching path indexes.
        Set<String> pathIndexes = indexTreeService.searchTree(resource, realm);

        return new ResourceSearchIndexes(
                legacyIndexes.getHostIndexes(), pathIndexes, legacyIndexes.getParentPathIndexes());
    }
}
