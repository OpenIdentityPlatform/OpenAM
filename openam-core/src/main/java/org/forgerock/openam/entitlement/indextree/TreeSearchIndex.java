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

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.guice.InjectorHolder;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation provides search index by means of an in memory rule index cache.
 * <p />
 * Expects the passed resource to have already been normalised.
 *
 * @author apforrest
 */
public class TreeSearchIndex implements ISearchIndex {

    private final IndexTreeService indexTreeService;

    /**
     * This constructor has been added as the entitlements framework expects a no args constructor due to its use of
     * reflection for object instantiation. However, as this implementation has dependencies, and to reduce coupling,
     * where possible this constructor should be avoided and an alternative should be used in favour.
     */
    public TreeSearchIndex() {
        this(InjectorHolder.getInstance(IndexTreeService.class));
    }

    @Inject
    public TreeSearchIndex(IndexTreeService indexTreeService) {
        this.indexTreeService = indexTreeService;
    }

    @Override
    public ResourceSearchIndexes getIndexes(String resource, String realm) throws EntitlementException {
        // Ignore host and parent path indexes.
        Set<String> hostIndexes = Collections.emptySet();
        Set<String> parentPathIndexes = Collections.emptySet();

        // Indexes are handled in lower case.
        resource = resource.toLowerCase();
        // Search the index tree for matching path indexes.
        Set<String> pathIndexes = indexTreeService.searchTree(resource, realm);

        return new ResourceSearchIndexes(hostIndexes, escapePathIndexes(pathIndexes), parentPathIndexes);
    }



    /**
     * Prepare path indexes for LDAP search.
     * <p/>
     * TODO: This should use Filter#escapeAssertionValue() as provided by the OpenDJ SDK once it becomes available.
     *
     * @param pathIndexes
     *         Path Indexes to be escaped.
     * @return Escaped path indexes.
     */
    protected Set<String> escapePathIndexes(Set<String> pathIndexes) {
        Set<String> escapedPathIndexes = new HashSet<String>();

        for (String pathIndex : pathIndexes) {
            pathIndex = pathIndex.replace("*", "\\2A");
            escapedPathIndexes.add(pathIndex);
        }

        return escapedPathIndexes;
    }

}
