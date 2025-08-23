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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.upgrade.steps.policy.policysets;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.steps.AbstractUpgradeStep;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;

/**
 * Policy Sets created by OpenAM 13.0.0 for OAuth2 were missing the list of allowed/accepted subjects and conditions.
 * This upgrade step will enable all currently supported subjects and conditions on any existing policy set called
 * OAuth2 that has not been configured with supported subjects or conditions.
 * By upgrading the Policy Sets we ensure that the XUI policy editor is able to display policies utilizing previously
 * corrupted policies.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class UpdateOAuth2PolicySetsStep extends AbstractUpgradeStep {

    private static final String OAUTH2_POLICY_SET_NAME = "OAuth2";
    private static final String REPORT_TEXT = "%REPORT_TEXT%";
    private final ApplicationServiceFactory applicationServiceFactory;
    private final Set<String> affectedRealms = new HashSet<>();

    @Inject
    public UpdateOAuth2PolicySetsStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory,
            final ApplicationServiceFactory applicationServiceFactory) {
        super(adminTokenAction, connectionFactory);
        this.applicationServiceFactory = applicationServiceFactory;
    }

    @Override
    public boolean isApplicable() {
        return isNotEmpty(affectedRealms);
    }

    @Override
    public void initialize() throws UpgradeException {
        for (String realm : getRealmNames()) {
            try {
                DEBUG.message("Looking for OAuth2 policy sets under realm {}", realm);
                ApplicationService applicationService = applicationServiceFactory.create(getAdminSubject(), realm);
                final Application application = applicationService.getApplication(OAUTH2_POLICY_SET_NAME);
                if (application != null
                        && (isEmpty(application.getSubjects()) || isEmpty(application.getConditions()))) {
                    affectedRealms.add(realm);
                }
            } catch (EntitlementException ee) {
                DEBUG.error("An error occurred while initializing OAuth2 policy set upgrade step", ee);
                throw new UpgradeException(ee);
            }
        }
        DEBUG.message("Realms found with OAuth2 policy sets having incorrect subjects/conditions configuration: {}",
                affectedRealms);
    }

    @Override
    public void perform() throws UpgradeException {
        for (String realm : affectedRealms) {
            try {
                ApplicationService applicationService = applicationServiceFactory.create(getAdminSubject(), realm);
                final Application application = applicationService.getApplication(OAUTH2_POLICY_SET_NAME);
                UpgradeProgress.reportStart("upgrade.policy.oauth2.policyset.progress", realm);
                if (isEmpty(application.getSubjects())) {
                    DEBUG.message("Updating list of allowed subjects for OAuth2 under realm: {}", realm);
                    application.setSubjects(EntitlementUtils.getSubjectsShortNames());
                }
                if (isEmpty(application.getConditions())) {
                    DEBUG.message("Updating list of allowed conditions for OAuth2 under realm: {}", realm);
                    application.setConditions(EntitlementUtils.getConditionsShortNames());
                }

                applicationService.saveApplication(application);
                UpgradeProgress.reportEnd("upgrade.success");
                DEBUG.message("OAuth2 Policy Set successfully updated in realm: {}", realm);
                applicationService.clearCache();
            } catch (EntitlementException ee) {
                DEBUG.error("An error occurred while upgrading the OAuth2 Policy Set in realm: {}", realm, ee);
                throw new UpgradeException(ee);
            }
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return MessageFormat.format(BUNDLE.getString("upgrade.policy.oauth2.policyset.short"), affectedRealms.size())
                + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        sb.append(BUNDLE.getString("upgrade.policy.oauth2.policyset.detailed")).append(delimiter);
        for (String realm : affectedRealms) {
            sb.append(INDENT).append(realm).append(delimiter);
        }
        sb.append(delimiter);
        tags.put(REPORT_TEXT, sb.toString());
        return tagSwapReport(tags, "upgrade.policy.oauth2.policyset.report");
    }
}
