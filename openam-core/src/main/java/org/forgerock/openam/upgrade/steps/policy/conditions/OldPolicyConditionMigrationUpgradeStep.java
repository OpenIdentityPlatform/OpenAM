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

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.upgrade.UpgradeException;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.steps.AbstractUpgradeStep;
import org.forgerock.opendj.ldap.ConnectionFactory;

/**
 * <p>Will attempt to migrate old policy conditions to new entitlement conditions.</p>
 *
 * <p>Will only migrate conditions that have a defined migration path from old to new. Any policies which can't be
 * upgrade will be listed in the long report and will have to be manually upgraded at a later date.</p>
 *
 * @since 12.0.0
 */
@UpgradeStepInfo(dependsOn = {"org.forgerock.openam.upgrade.steps.ResavePoliciesStep",
        "org.forgerock.openam.upgrade.steps.UpgradeEntitlementsStep"})
public class OldPolicyConditionMigrationUpgradeStep extends AbstractUpgradeStep {

    private static final String ENTITLEMENT_DATA = "%ENTITLEMENT_DATA%";


    private final Map<String, Set<Privilege>> privilegesToUpgrade = new HashMap<String, Set<Privilege>>();
    private final Map<String, Set<String>> unUpgradablePolicies = new HashMap<String, Set<String>>();
    private final Map<String, Set<MigrationReport>> migrationReports = new HashMap<String, Set<MigrationReport>>();
    private final PolicyConditionUpgrader conditionUpgrader;

    /**
     * Constructs a new OldPolicyConditionMigrationUpgradeStep instance.
     *
     * @param adminTokenAction An instance of the admin action.
     * @param connectionFactory An instance of a {@code ConnectionFactory}.
     */
    @Inject
    public OldPolicyConditionMigrationUpgradeStep(PrivilegedAction<SSOToken> adminTokenAction,
            @Named(DataLayerConstants.DATA_LAYER_BINDING) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
        this.conditionUpgrader = new PolicyConditionUpgrader(new PolicyConditionUpgradeMap());
    }

    private PrivilegeManager getPrivilegeManager(String realm) {
        Subject adminSubject = SubjectUtils.createSubject(getAdminToken());
        return PrivilegeManager.getInstance(realm, adminSubject);
    }

    /**
     * Checks what policies could be automatically upgraded and performs the upgrade without saving so that the
     * migrated policy can be validated to ensure the upgrade went well.
     *
     * @throws UpgradeException If a problem occurred checking the policies.
     */
    @Override
    public void initialize() throws UpgradeException {

        try {
            DEBUG.message("Initializing OldPolicyConditionMigrationStep");

            for (String realm : getRealmNames()) {

                if (!realm.startsWith("/")) {
                    realm = "/" + realm;
                }

                PrivilegeManager privilegeManager = getPrivilegeManager(realm);
                List<Privilege> privileges;
                try {
                    privileges = privilegeManager.search(null);
                } catch (EntitlementException e) {
                    continue;
                }
                for (Privilege privilege : privileges) {

                    if (conditionUpgrader.isPolicyUpgradable(privilege)) {
                        try {
                            MigrationReport report = conditionUpgrader.dryRunPolicyUpgrade(privilege);
                            addReport(realm, report);

                            addUpgradablePolicy(realm, privilege);
                        } catch (Exception e) {
                            addUnupgradablePolicy(realm, privilege);
                        }
                    }
                }
            }

        } catch (UpgradeException e) {
            DEBUG.error("Error while trying to detect changes in entitlements", e);
            throw e;
        } catch (Exception ex) {
            DEBUG.error("Error while trying to detect changes in entitlements", ex);
            throw new UpgradeException(ex);
        }
    }

    private void addReport(String realm, MigrationReport report) {
        Set<MigrationReport> realmReports = migrationReports.get(realm);
        if (realmReports == null) {
            realmReports = new HashSet<MigrationReport>();
            migrationReports.put(realm, realmReports);
        }
        realmReports.add(report);
    }

    private void addUpgradablePolicy(String realm, Privilege policy) {

        Set<Privilege> realmPolicies = privilegesToUpgrade.get(realm);
        if (realmPolicies == null) {
            realmPolicies = new HashSet<Privilege>();
            privilegesToUpgrade.put(realm, realmPolicies);
        }
        realmPolicies.add(policy);
    }

    private void addUnupgradablePolicy(String realm, Privilege policy) {

        Set<String> realmReports = unUpgradablePolicies.get(realm);
        if (realmReports == null) {
            realmReports = new HashSet<String>();
            unUpgradablePolicies.put(realm, realmReports);
        }
        realmReports.add(policy.getName());
        if (DEBUG.warningEnabled()) {
            DEBUG.warning("Cannot upgrade policy, " + policy.getName() + " dues to one or more "
                    + "subject and/or environment conditions not being able to be migrated to new "
                    + "Entitlement conditions. This policy will have to be manually migrated to use "
                    + "the new environment conditions. See documentation for more details.");
        }
    }

    /**
     * Returns {@code true} if there are any policies that can be upgraded.
     *
     * @return {@code true} if there are any policies that can be upgraded.
     */
    @Override
    public boolean isApplicable() {
        return !privilegesToUpgrade.isEmpty();
    }

    /**
     * Does the persisting of the upgraded policies.
     *
     * @throws UpgradeException If there is a problem saving the policies.
     */
    @Override
    public void perform() throws UpgradeException {

        for (Map.Entry<String, Set<Privilege>> entry : privilegesToUpgrade.entrySet()) {
            String realm = entry.getKey();
            ApplicationManager.clearCache(realm); //ensure reading apps cleanly

            PrivilegeManager privilegeManager = getPrivilegeManager(realm);

            for (Privilege privilege : entry.getValue()) {

                privilege.getEntitlement().clearCache();

                try {
                    privilegeManager.modify(privilege.getName(), privilege);
                } catch (EntitlementException e) {
                    DEBUG.error("Failed to modify privilege!", e);
                    throw new UpgradeException("Failed to modify privilege!", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (privilegesToUpgrade.size() != 0) {
            sb.append(BUNDLE.getString("upgrade.entitlement.migrated")).append(" (").append(privilegesToUpgrade.size())
                    .append(')').append(delimiter);
        }
        if (unUpgradablePolicies.size() != 0) {
            sb.append(BUNDLE.getString("upgrade.entitlement.notmigrated")).append(" (")
                    .append(unUpgradablePolicies.size()).append(')').append(delimiter);
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();

        writeUnupgradablePoliciesReport(sb, delimiter);
        writeUpgradedPoliciesReport(sb, delimiter);

        tags.put(ENTITLEMENT_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.entitlementmigrationreport");
    }

    private void writeUnupgradablePoliciesReport(StringBuilder sb, String delimiter) {

        sb.append(BUNDLE.getString("unupgradable.policies.heading")).append(delimiter);
        for (Map.Entry<String, Set<String>> entry : unUpgradablePolicies.entrySet()) {
            sb.append(INDENT).append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey())
                    .append(delimiter);
            for (String policyName : entry.getValue()) {
                sb.append(INDENT).append(INDENT).append(policyName).append(": ")
                        .append(BUNDLE.getString("upgrade.entitlement.migration.failed"));
            }
        }
    }

    private void writeUpgradedPoliciesReport(StringBuilder sb, String delimiter) {
        sb.append(BUNDLE.getString("upgraded.policies.heading")).append(delimiter);
        for (Map.Entry<String, Set<MigrationReport>> reports : migrationReports.entrySet()) {
            sb.append(INDENT).append(BUNDLE.getString("upgrade.realm")).append(": ").append(reports.getKey())
                    .append(delimiter);
            for (MigrationReport report : reports.getValue()) {
                writeUpgradedPolicySubjectConditionsReport(sb, report);
                writeUpgradedPolicyEnvironmentConditionsReport(sb, report);
            }
        }
    }

    private void writeUpgradedPolicySubjectConditionsReport(StringBuilder sb, MigrationReport report) {
        sb.append(INDENT).append(INDENT).append(report.getPolicyName()).append(": ")
                .append("migrated subject conditions");
        for (Map.Entry<String, String> subjectMigration : report.getSubjectConditionMigration().entrySet()) {
            sb.append(INDENT).append(INDENT).append(INDENT).append(subjectMigration.getKey()).append(" ")
                    .append(BUNDLE.getString("upgrade.entitlement.to")).append(" ")
                    .append(subjectMigration.getValue());
        }
    }

    private void writeUpgradedPolicyEnvironmentConditionsReport(StringBuilder sb, MigrationReport report) {
        sb.append(INDENT).append(INDENT).append(report.getPolicyName()).append(": ")
                .append("migrated environment conditions");
        for (Map.Entry<String, String> environmentMigration : report.getEnvironmentConditionMigration().entrySet()) {
            sb.append(INDENT).append(INDENT).append(INDENT).append(environmentMigration.getKey()).append(" ")
                    .append(BUNDLE.getString("upgrade.entitlement.to")).append(" ")
                    .append(environmentMigration.getValue());
        }
    }
}
