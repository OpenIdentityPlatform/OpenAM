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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import java.util.Set;

import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;

/**
 * A filter that can be applied to ResourceSets.
 *
 * @since 13.0.0
 */
public interface ResourceSetFilter {
    /**
     * Filters the Set provided.
     * @param values The set to filter.
     * @return The set with the filter applied.
     */
    Set<ResourceSetDescription> filter(Set<ResourceSetDescription> values) throws ServerException;
}
