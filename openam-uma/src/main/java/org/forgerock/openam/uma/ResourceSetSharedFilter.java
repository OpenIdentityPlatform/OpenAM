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

package org.forgerock.openam.uma;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.oauth2.core.ResourceSetFilter;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.uma.rest.ResourceSetService;

import com.google.inject.Inject;

/**
 * Filters the ResourceSets based on whether they are shared with the specified user.
 *
 * @since 13.0.0
 */
public class ResourceSetSharedFilter implements ResourceSetFilter {

    private final String userId;
    private final ResourceSetService resourceSetService;
    private final String realm;

    /**
     * Constructor for this filter.
     * @param resourceSetService The service used to check entitlement.
     * @param userId The id of the user to check.
     * @param realm The realm to check in.
     */
    @Inject
    public ResourceSetSharedFilter(ResourceSetService resourceSetService, String userId, String realm) {
        this.userId = userId;
        this.resourceSetService = resourceSetService;
        this.realm = realm;
    }

    @Override
    public Set<ResourceSetDescription> filter(Set<ResourceSetDescription> values) throws ServerException {
        Set<ResourceSetDescription> results = new HashSet<>();
        try {
            for (ResourceSetDescription resourceSetDescription : values) {
                if (userId.equals(resourceSetDescription.getResourceOwnerId())
                        || resourceSetService.isSharedWith(resourceSetDescription, userId, realm)) {
                    results.add(resourceSetDescription);
                }
            }
        } catch (InternalServerErrorException isee) {
            throw new ServerException(isee);
        }
        return results;
    }
}
