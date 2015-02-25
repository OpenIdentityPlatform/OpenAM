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

import java.util.HashMap;
import java.util.Map;

/**
 * Report that holds all information about what old policy condition was migrated to what new entitlement condition.
 *
 * @since 12.0.0
 */
class MigrationReport {

    private final String policyName;
    private final Map<String, String> subjectConditionMigration = new HashMap<String, String>();
    private final Map<String, String> environmentConditionMigration = new HashMap<String, String>();

    /**
     * Constructs a new instance of a MigrationReport.
     *
     * @param policyName The policy name.
     */
    MigrationReport(String policyName) {
        this.policyName = policyName;
    }

    /**
     * Adds a migration from an old policy subject condition to a new entitlement subject condition.
     *
     * @param from The old policy subject condition class name.
     * @param to The new entitlement subject condition class name.
     */
    void migratedSubjectCondition(String from, String to) {
        subjectConditionMigration.put(from, to);
    }

    /**
     * Adds a migration from an old policy environment condition to a new entitlement environment condition.
     *
     * @param from The old policy environment condition class name.
     * @param to The new entitlement environment condition class name.
     */
    void migratedEnvironmentCondition(String from, String to) {
        environmentConditionMigration.put(from, to);
    }

    String getPolicyName() {
        return policyName;
    }

    Map<String, String> getSubjectConditionMigration() {
        return subjectConditionMigration;
    }

    Map<String, String> getEnvironmentConditionMigration() {
        return environmentConditionMigration;
    }
}
