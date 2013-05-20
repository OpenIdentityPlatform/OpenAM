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

/**
 * Implementation provides a simple save index, which is the passed policy rule as a path index.
 * <p />
 * Expects the passed resource to have already been normalised.
 *
 * @author apforrest
 */
public class TreeSaveIndex implements ISaveIndex {

    private static final String FULL_SINGLE_LEVEL_WILDCARD = "-*-";
    private static final String ABBREVIATED_SINGLE_LEVEL_WILDCARD = "^";

    @Override
    public ResourceSaveIndexes getIndexes(String policyRule) {
        // Ignore host and parent path indexes.
        Set<String> hostIndexes = Collections.emptySet();
        Set<String> parentPathIndexes = Collections.emptySet();

        // Indexes are handled in lower case.
        policyRule = policyRule.toLowerCase();
        // Capture the full resource path as the path index.
        Set<String> pathIndexes = new HashSet<String>();
        pathIndexes.add(parsePolicyRule(policyRule));

        return new ResourceSaveIndexes(hostIndexes, pathIndexes, parentPathIndexes);
    }

    /**
     * Parse the policy rule of special wildcards into a simple form.
     *
     * @param policyRule
     *         The policy rule.
     * @return The parsed policy rule.
     */
    protected String parsePolicyRule(String policyRule) {
        return policyRule.replace(FULL_SINGLE_LEVEL_WILDCARD, ABBREVIATED_SINGLE_LEVEL_WILDCARD);
    }

}
