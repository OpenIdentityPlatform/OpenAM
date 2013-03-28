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

import com.sun.identity.entitlement.ResourceSaveIndexes;
import com.sun.identity.entitlement.interfaces.ISaveIndex;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TreeSaveIndex implements ISaveIndex {

    @Override
    public ResourceSaveIndexes getIndexes(String resource) {
        // Ignore host and parent path indexes.
        Set<String> hostIndexes = Collections.emptySet();
        Set<String> parentPathIndexes = Collections.emptySet();

        // Capture the full resource path as the path index.
        Set<String> pathIndexes = new HashSet<String>();
        pathIndexes.add(normaliseResource(resource));

        return new ResourceSaveIndexes(hostIndexes, pathIndexes, parentPathIndexes);
    }

    /**
     * Normalises the resource string to ensure consistency around whitespace, case and special characters
     *
     * @param resource
     *         The resource.
     * @return The normalised resource.
     */
    protected String normaliseResource(String resource) {
        resource = resource.trim();
        resource = resource.toLowerCase();
        resource = resource.replace("-*-", "^");
        return resource;
    }

}
