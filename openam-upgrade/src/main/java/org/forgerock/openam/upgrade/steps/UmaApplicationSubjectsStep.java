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

import static org.forgerock.openam.entitlement.EntitlementRegistry.getSubjectTypeName;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.uma.UmaConstants;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeStepInfo;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.JwtClaimSubject;

/**
 * An upgrade step to make sure that UMA automatically-created policy {@link com.sun.identity.entitlement.Application}s
 * have the subject types they use declared.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeEntitlementsStep")
public class UmaApplicationSubjectsStep extends AbstractUpgradeStep {

    private static final Set<String> SUBJECT_TYPES = asSet(getSubjectTypeName(JwtClaimSubject.class));
    private final Map<String, Set<Application>> needUpgrade = new HashMap<>();
    private final ApplicationServiceFactory applicationServiceFactory;
    private final ApplicationTypeManagerWrapper applicationTypeManagerWrapper;
    private int applicationCount;

    @Inject
    public UmaApplicationSubjectsStep(ApplicationServiceFactory applicationServiceFactory,
            PrivilegedAction<SSOToken> adminTokenAction, ApplicationTypeManagerWrapper applicationTypeManagerWrapper,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
        this.applicationServiceFactory = applicationServiceFactory;
        this.applicationTypeManagerWrapper = applicationTypeManagerWrapper;
    }

    @Override
    public void initialize() throws UpgradeException {
        ApplicationType type = applicationTypeManagerWrapper.getApplicationType(getAdminSubject(),
                UmaConstants.UMA_POLICY_APPLICATION_TYPE);
        try {
            for (String realm : getRealmNames()) {
                ApplicationService appService = applicationServiceFactory.create(getAdminSubject(), realm);
                Set<Application> affected = new HashSet<>();
                for (Application application : appService.getApplications()) {
                    if (application.getApplicationType().equals(type)
                            && !application.getSubjects().containsAll(SUBJECT_TYPES)) {
                        affected.add(application);
                        applicationCount++;
                    }
                }
                if (!affected.isEmpty()) {
                    needUpgrade.put(realm, affected);
                }
            }
        } catch (Exception ex) {
            DEBUG.error("An error occurred while trying to look for upgradable UMA policy applications", ex);
            throw new UpgradeException("Unable to retrieve UMA policy applications", ex);
        }
    }

    @Override
    public boolean isApplicable() {
        return !needUpgrade.isEmpty();
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            for (Map.Entry<String, Set<Application>> realmApplications : needUpgrade.entrySet()) {
                ApplicationService appService = applicationServiceFactory.create(getAdminSubject(),
                        realmApplications.getKey());
                for (Application application : realmApplications.getValue()) {
                    application.getSubjects().addAll(SUBJECT_TYPES);
                    appService.saveApplication(application);
                }
            }
        } catch (Exception ex) {
            DEBUG.error("An error occurred while trying to upgrade UMA policy applications", ex);
            throw new UpgradeException("Unable to upgrade UMA policy applications", ex);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder sb = new StringBuilder();
        if (applicationCount != 0) {
            sb.append(BUNDLE.getString("upgrade.uma.applications")).append(" (").append(applicationCount).append(')')
                    .append(delimiter);
        }
        return sb.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        if (needUpgrade.isEmpty()) {
            return "";
        }
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<Application>> entry : needUpgrade.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (Application application : entry.getValue()) {
                sb.append(INDENT).append(application.getName()).append(delimiter);
            }
        }
        tags.put("%REPORT_DATA%", sb.toString());
        return tagSwapReport(tags, "upgrade.uma.applicationsreport");
    }
}
