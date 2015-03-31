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
package org.forgerock.openam.entitlement.constraints;

import java.util.Set;

/**
 * Current validates resources and actions against the constraints within a resource type.
 * <p/>
 * This implementation is currently very specific but will become more general purpose, not only concerning itself
 * with resource constraints but also subject and environment constraints.
 *
 * @since 13.0.0
 */
public interface ConstraintValidator {

    /**
     * Verifies the passed resources against passed resource type.
     *
     * @param resources
     *         the resources to be verified
     *
     * @return the resource handler definition instance
     */
    ResourceMatchUsing verifyResources(Set<String> resources);

    /**
     * Verifies the passed actions against passed resource type.
     *
     * @param actions
     *         the actions to be verified
     *
     * @return the resource type definition instance
     */
    AgainstResourceType verifyActions(Set<String> actions);

}
