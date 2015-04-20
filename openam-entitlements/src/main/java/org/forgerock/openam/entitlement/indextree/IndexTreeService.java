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

import java.util.Set;

/**
 * Service manages index rules within a tree structure.
 *
 * @author apforrest
 */
public interface IndexTreeService {

    /**
     * Given a resource searches the tree for all matching index rules with the specified realm.
     *
     * @param resource
     *         The resource to be used to search the tree.
     * @param realm
     *         The realm for which the search is to take place.
     * @return A set of matched index rules.
     * @throws EntitlementException
     *         When some system error halts the search from completing.
     */
    public Set<String> searchTree(String resource, String realm) throws EntitlementException;

}
