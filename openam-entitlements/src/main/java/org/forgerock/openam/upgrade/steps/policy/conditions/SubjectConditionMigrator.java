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

package org.forgerock.openam.upgrade.steps.policy.conditions;

import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.opensso.PolicySubject;

/**
 * Migrator for migrating old policy subject conditions to new entitlement subject conditions.
 *
 * @since 12.0.0
 */
interface SubjectConditionMigrator {

    /**
     * Migrates the specified old policy subject condition to a new entitlement subject condition.
     *
     * @param subject The subject condition to migrate
     * @param migrationReport The migration report to update.
     * @return A new {@code EntitlementSubject} of the migrated old policy subject condition
     */
    EntitlementSubject migrate(PolicySubject subject, MigrationReport migrationReport);
}

