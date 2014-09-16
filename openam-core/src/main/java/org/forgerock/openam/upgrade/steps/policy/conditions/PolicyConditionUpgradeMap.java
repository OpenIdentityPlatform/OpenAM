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

import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.opensso.PolicyCondition;
import com.sun.identity.entitlement.opensso.PolicySubject;

import java.util.HashMap;
import java.util.Map;

/**
 * A map containing all the migration logic from an old policy condition to a new entitlement condition.
 *
 * @since 12.0.0
 */
class PolicyConditionUpgradeMap {

    private final Map<String, SubjectConditionMigrator> subjectConditionsUpgradeMap =
            new HashMap<String, SubjectConditionMigrator>();
    private final Map<String, EntitlementConditionMigrator> environmentConditionsUpgradeMap =
            new HashMap<String, EntitlementConditionMigrator>();

    {
        /* This is way the migration mapping declarations will go for example:

        subjectConditionsUpgradeMap.put(AuthenticatedUsers.class.getName(), new SubjectConditionMigrator() {
                     @Override
                     public EntitlementSubject migrate(PolicySubject subject, MigrationReport migrationReport) {
                         migrationReport.migratedSubjectCondition(AuthenticatedUsers.class.getName(), AuthenticatedESubject.class.getName());
                         return new AuthenticatedESubject();
                     }
                 });

        environmentConditionsUpgradeMap.put(SimpleTimeCondition.class.getName(), new EntitlementConditionMigrator() {
                     @Override
                     public EntitlementCondition migrate(PolicyCondition condition, MigrationReport migrationReport) {
                         migrationReport.migratedEnvironmentCondition(SimpleTimeCondition.class.getName(), TimeCondition.class.getName());
                         return new TimeCondition();
                     }
                 });
        */
    }

    /**
     * Returns {@code true} if there exists an entry for migrating the specified old policy subject condition class.
     *
     * @param conditionClassName The old policy subject condition class name.
     * @return {@code true} if there exists an entry for migrating the specified old policy subject condition class.
     */
    boolean containsSubjectCondition(String conditionClassName) {
        return subjectConditionsUpgradeMap.containsKey(conditionClassName);
    }

    /**
     * Returns {@code true} if there exists an entry for migrating the specified old policy environment condition class.
     *
     * @param conditionClassName The old policy environment condition class name.
     * @return {@code true} if there exists an entry for migrating the specified old policy environment condition class.
     */
    boolean containsEnvironmentCondition(String conditionClassName) {
        return environmentConditionsUpgradeMap.containsKey(conditionClassName);
    }

    /**
     * Migrates the specified subject from the old policy subject condition class to the corresponding new entitlement
     * subject condition.
     *
     * @param conditionClassName The old policy subject condition class name.
     * @param subject The subject condition to migrate
     * @param migrationReport The migration report to update.
     * @return A new {@code EntitlementSubject} of the migrated old policy subject condition
     */
    EntitlementSubject migrateSubjectCondition(String conditionClassName, PolicySubject subject,
            MigrationReport migrationReport) {
        return subjectConditionsUpgradeMap.get(conditionClassName).migrate(subject, migrationReport);
    }

    /**
     * Migrates the specified subject from the old policy environment condition class to the corresponding new entitlement
     * environment condition.
     *
     * @param conditionClassName The old policy environment condition class name.
     * @param condition The environment condition to migrate
     * @param migrationReport The migration report to update.
     * @return A new {@code EntitlementSubject} of the migrated old policy environment condition
     */
    EntitlementCondition migrateEnvironmentCondition(String conditionClassName,
            PolicyCondition condition, MigrationReport migrationReport) {
        return environmentConditionsUpgradeMap.get(conditionClassName).migrate(condition, migrationReport);
    }
}
