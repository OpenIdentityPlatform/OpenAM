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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.VersionUtils.isCurrentVersionLessThan;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationManagerWrapper;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.steps.policy.AbstractEntitlementUpgradeStep;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * If upgrading from any version prior to 13.0.0, this upgrade step will attempt to remove
 * a known list of redundant default applications, given they no longer have a purpose.
 *
 * @since 13.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public final class RemoveRedundantDefaultApplication extends AbstractEntitlementUpgradeStep {

    private final static String AUDIT_REDUNDANT_APPLICATIONS = "upgrade.policy.applications.redundant";
    private final static String AUDIT_REMOVING_APPLICATION = "upgrade.policy.applications.removal";
    private final static String AUDIT_REMOVAL_SUCCESS = "upgrade.policy.applications.removal.success";
    private final static String AUDIT_REMOVAL_FAILURE = "upgrade.policy.applications.removal.failure";
    private final static String AUDIT_APPLICATION_REMOVAL_REPORT = "upgrade.policy.applications.removal.report";

    private final static int AM_13 = 1300;
    private final static String DEFAULT_REALM = "/";

    private final Set<String> removedDefaultApplications;
    private final ApplicationManagerWrapper applicationService;

    private final Set<String> defaultApplicationsToBeRemoved;
    private final Set<String> successfulApplicationRemovals;
    private final Set<String> failedApplicationRemovals;

    @Inject
    public RemoveRedundantDefaultApplication(@Named("removedDefaultApplications") Set<String> removedDefaultApplications,
            ApplicationManagerWrapper applicationService, PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
        this.removedDefaultApplications = removedDefaultApplications;
        this.applicationService = applicationService;
        defaultApplicationsToBeRemoved = new HashSet<>();
        successfulApplicationRemovals = new HashSet<>();
        failedApplicationRemovals = new HashSet<>();
    }

    @Override
    public void initialize() throws UpgradeException {
        if (isCurrentVersionLessThan(AM_13, true)) {
            try {
                identifyApplicationsToBeRemoved();
            } catch (EntitlementException eE) {
                throw new UpgradeException("Failed to identify applications to be removed", eE);
            }
        }
    }

    private void identifyApplicationsToBeRemoved() throws EntitlementException {
        // Default applications are only in the root realm.
        defaultApplicationsToBeRemoved.addAll(applicationService.getApplicationNames(getAdminSubject(), DEFAULT_REALM));
        defaultApplicationsToBeRemoved.retainAll(removedDefaultApplications);
    }

    @Override
    public boolean isApplicable() {
        return isNotEmpty(defaultApplicationsToBeRemoved);
    }

    @Override
    public void perform() throws UpgradeException {
        for (String applicationName : defaultApplicationsToBeRemoved) {
            try {
                UpgradeProgress.reportStart(AUDIT_REMOVING_APPLICATION, applicationName);
                applicationService.deleteApplication(getAdminSubject(), DEFAULT_REALM, applicationName);
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);
                successfulApplicationRemovals.add(applicationName);

            } catch (EntitlementException eE) {
                UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
                failedApplicationRemovals.add(applicationName);
                DEBUG.warning("Failed to remove default application " + applicationName, eE);
            }
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString(AUDIT_REDUNDANT_APPLICATIONS) + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        StringBuilder builder = new StringBuilder();

        if (isNotEmpty(successfulApplicationRemovals)) {
            builder.append(BUNDLE.getString(AUDIT_REMOVAL_SUCCESS))
                    .append(':')
                    .append(delimiter);

            for (String applicationName : successfulApplicationRemovals) {
                builder.append(applicationName)
                        .append(delimiter);
            }
        }

        if (isNotEmpty(failedApplicationRemovals)) {
            builder.append(BUNDLE.getString(AUDIT_REMOVAL_FAILURE))
                    .append(':')
                    .append(delimiter);

            for (String applicationName : failedApplicationRemovals) {
                builder.append(applicationName)
                        .append(delimiter);
            }
        }

        Map<String, String> reportEntries = new HashMap<>();
        reportEntries.put("%REPORT_TEXT%", builder.toString());
        reportEntries.put(LF, delimiter);

        return UpgradeServices.tagSwapReport(reportEntries, AUDIT_APPLICATION_REMOVAL_REPORT);
    }

}
