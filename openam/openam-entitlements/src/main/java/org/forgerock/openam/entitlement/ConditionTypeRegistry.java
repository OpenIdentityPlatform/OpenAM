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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement;

import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;

import java.util.Collection;

/**
 * Registry for subject and environment conditions.
 *
 * @since 12.0.0
 */
public interface ConditionTypeRegistry {

    /**
     * Gets all subject conditions in the registry.
     *
     * @return The subject conditions.
     */
    Collection<Class<? extends EntitlementSubject>> getSubjectConditions();

    /**
     * Gets all the environment conditions in the registry.
     *
     * @return The environment conditions.
     */
    Collection<Class<? extends EntitlementCondition>> getEnvironmentConditions();
}
