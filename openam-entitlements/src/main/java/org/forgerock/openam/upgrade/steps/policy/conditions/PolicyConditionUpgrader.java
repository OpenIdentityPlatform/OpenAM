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
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.LogicalCondition;
import com.sun.identity.entitlement.LogicalSubject;
import com.sun.identity.entitlement.NoSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.opensso.PolicyCondition;
import com.sun.identity.entitlement.opensso.PolicySubject;
import java.util.HashSet;
import java.util.Set;
import org.forgerock.openam.upgrade.UpgradeException;

/**
 * Checks if a policy can be automatically upgraded and performs a dry run of the migration.
 *
 * @since 12.0.0
 */
class PolicyConditionUpgrader {

    private final PolicyConditionUpgradeMap conditionUpgradeMap;

    /**
     * Constructs a new instance of the PolicyConditionUpgrader.
     *
     * @param conditionUpgradeMap An instance of the {@code PolicyConditionUpgradeMap}.
     */
    PolicyConditionUpgrader(PolicyConditionUpgradeMap conditionUpgradeMap) {
        this.conditionUpgradeMap = conditionUpgradeMap;
    }

    /**
     * Returns {@code true} if the policy can be automatically upgraded.
     *
     * @param policy The policy to upgrade.
     * @return {@code true} if the policy can be automatically upgraded.
     */
    boolean isPolicyUpgradable(Privilege policy) {
        return isSubjectConditionUpgradable(policy.getSubject())
                && isEnvironmentConditionUpgradable(policy.getCondition());
    }

    private boolean isSubjectConditionUpgradable(EntitlementSubject subject) {

        if (subject == null) {
            return true;
        }

        if (subject instanceof NoSubject) {
            return true;
        }

        if (subject instanceof LogicalSubject) {
            LogicalSubject logicalSubject = (LogicalSubject) subject;
            boolean upgradable = true;
            for (EntitlementSubject sub : logicalSubject.getESubjects()) {
                upgradable &= isUpgradablePolicySubject(sub);
            }
            return upgradable;
        }

        return isUpgradablePolicySubject(subject);
    }

    private boolean isUpgradablePolicySubject(EntitlementSubject subject) {
        return subject instanceof PolicySubject
                && conditionUpgradeMap.containsSubjectCondition(((PolicySubject) subject).getClassName());
    }

    private boolean isEnvironmentConditionUpgradable(EntitlementCondition condition) {

        if (condition == null) {
            return true;
        }

        if (condition instanceof LogicalCondition) {
            LogicalCondition logicalCondition = (LogicalCondition) condition;
            boolean upgradable = true;
            for (EntitlementCondition c : logicalCondition.getEConditions()) {
                upgradable &= isUpgradablePolicyCondition(c);
            }
            return upgradable;
        }

        return isUpgradablePolicyCondition(condition);
    }

    private boolean isUpgradablePolicyCondition(EntitlementCondition condition) {
        return condition instanceof PolicyCondition
                && conditionUpgradeMap.containsEnvironmentCondition(((PolicyCondition) condition).getClassName());
    }

    /**
     * <p>Performs the upgrade of the specified policy without saving.</p>
     *
     * <p>The given policy will be updated with the migrated conditions.</p>
     *
     * @param policy The policy to upgrade.
     * @return A migration report detailing what migration was performed.
     * @throws EntitlementException If the policy could not be migrated.
     * @throws UpgradeException If the policy could not be migrated.
     */
    MigrationReport dryRunPolicyUpgrade(Privilege policy) throws EntitlementException, UpgradeException {

        MigrationReport migrationReport = new MigrationReport(policy.getName());

        migrateSubjectConditions(policy, migrationReport);
        migrateEnvironmentConditions(policy, migrationReport);

        return migrationReport;
    }

    private void migrateSubjectConditions(Privilege privilege, MigrationReport migrationReport) throws UpgradeException, EntitlementException {

        if (privilege.getSubject() == null) {
            return;
        }

        if (privilege.getSubject() instanceof NoSubject) {
            return;
        }

        if (privilege.getSubject() instanceof LogicalSubject) {
            LogicalSubject logicalSubject = (LogicalSubject) privilege.getSubject();
            Set<EntitlementSubject> subjects = logicalSubject.getESubjects();
            Set<EntitlementSubject> migratedSubjects = new HashSet<EntitlementSubject>();
            for (EntitlementSubject subject : subjects) {

                if (subject instanceof NoSubject) {
                    migratedSubjects.add(subject); //pass this through directly
                } else if (!(subject instanceof PolicySubject)) {
                    //This should never happen due to check in initialise
                    throw new UpgradeException("Cannot upgrade a subject condition that is not of PolicySubject type!");
                } else {
                    migratedSubjects.add(migrateSubjectCondition((PolicySubject) subject, migrationReport));
                }
            }
            logicalSubject.setESubjects(migratedSubjects);
        } else if (privilege.getSubject() instanceof PolicySubject) {
            privilege.setSubject(migrateSubjectCondition((PolicySubject) privilege.getSubject(), migrationReport));
        } else {
            //This should never happen due to check in initialise
            throw new UpgradeException("Cannot upgrade a subject condition that is not of PolicySubject type!");
        }
    }

    private EntitlementSubject migrateSubjectCondition(PolicySubject subject, MigrationReport migrationReport) {
        return conditionUpgradeMap.migrateSubjectCondition(subject.getClassName(), subject, migrationReport);
    }

    private void migrateEnvironmentConditions(Privilege privilege, MigrationReport migrationReport)
            throws UpgradeException, EntitlementException {

        if (privilege.getCondition() == null) {
            return;
        }

        if (privilege.getCondition() instanceof LogicalCondition) {
            LogicalCondition logicalCondition = (LogicalCondition) privilege.getCondition();
            Set<EntitlementCondition> conditions = logicalCondition.getEConditions();
            Set<EntitlementCondition> migratedConditions = new HashSet<EntitlementCondition>();
            for (EntitlementCondition condition : conditions) {
                if (!(condition instanceof PolicyCondition)) {
                    //This should never happen due to check in initialise
                    throw new UpgradeException("Cannot upgrade a environment condition that is not of PolicyCondition type!");
                }

                migratedConditions.add(migrateEnvironmentCondition((PolicyCondition) condition, migrationReport));
            }
            logicalCondition.setEConditions(migratedConditions);
        } else if (privilege.getCondition() instanceof PolicyCondition) {
            privilege.setCondition(migrateEnvironmentCondition((PolicyCondition) privilege.getCondition(), migrationReport));
        } else {
            //This should never happen due to check in initialise
            throw new UpgradeException("Cannot upgrade a environment condition that is not of PolicyCondition type!");
        }
    }

    private EntitlementCondition migrateEnvironmentCondition(PolicyCondition condition, MigrationReport migrationReport)
            throws EntitlementException {
        final EntitlementCondition migrated = conditionUpgradeMap.migrateEnvironmentCondition(condition.getClassName(),
                condition, migrationReport);
        migrated.validate();
        return migrated;
    }
}
