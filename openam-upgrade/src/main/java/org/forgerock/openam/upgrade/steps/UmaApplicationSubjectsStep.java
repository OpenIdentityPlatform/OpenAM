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

package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.entitlement.EntitlementRegistry.getSubjectTypeName;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.upgrade.VersionUtils.isCurrentVersionLessThan;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.uma.UmaConstants;
import org.forgerock.openam.uma.UmaUtils;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeStepInfo;

import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.JwtClaimSubject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;

/**
 * An upgrade step to make sure that UMA automatically-created policy {@link com.sun.identity.entitlement.Application}s
 * have the subject types they use declared.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeEntitlementsStep")
public class UmaApplicationSubjectsStep extends AbstractUpgradeStep {

    private final static int AM_13 = 1300;
    private static final Set<String> SUBJECT_TYPES = asSet(getSubjectTypeName(JwtClaimSubject.class));
    private final Map<String, Set<Application>> needUpgrade = new HashMap<>();
    private final AMIdentityRepositoryFactory idRepoFactory;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final ApplicationTypeManagerWrapper applicationTypeManagerWrapper;
    private int applicationCount;

    @Inject
    public UmaApplicationSubjectsStep(ApplicationServiceFactory applicationServiceFactory,
            PrivilegedAction<SSOToken> adminTokenAction, ApplicationTypeManagerWrapper applicationTypeManagerWrapper,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory,
            AMIdentityRepositoryFactory idRepoFactory) {
        super(adminTokenAction, connectionFactory);
        this.applicationServiceFactory = applicationServiceFactory;
        this.applicationTypeManagerWrapper = applicationTypeManagerWrapper;
        this.idRepoFactory = idRepoFactory;
    }

    @Override
    public void initialize() throws UpgradeException {
        if (!isCurrentVersionLessThan(AM_13, true)) {

            ApplicationType type = applicationTypeManagerWrapper
                    .getApplicationType(getAdminSubject(), UmaConstants.UMA_POLICY_APPLICATION_TYPE);
            try {
                for (String realm : getRealmNames()) {

                    //Get the list of OAuth2 agents
                    Set<String> agentsName = new HashSet<>();
                    SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
                    IdSearchResults searchResults = idRepoFactory.create(realm, adminToken)
                            .searchIdentities(IdType.AGENT, "*", new IdSearchControl());

                    Set<AMIdentity> results = searchResults.getSearchResults();

                    if ((results != null) && !results.isEmpty()) {
                        // Select the OAuth2 agents which are also UMA agents
                        for (AMIdentity amid : results) {
                            Map<String, Set<String>> attrValues = amid.getAttributes();
                            String agentType = CollectionHelper.getMapAttr(attrValues, IdConstants.AGENT_TYPE,
                                    "NO_TYPE");

                            if (AgentConfiguration.AGENT_TYPE_OAUTH2.equalsIgnoreCase(agentType)
                                    && UmaUtils.isUmaResourceServerAgent(attrValues)) {
                                agentsName.add(amid.getName());
                            }
                        }
                    }

                    //Now, upgrade applications which have an equivalent UMA agent
                    ApplicationService appService = applicationServiceFactory.create(getAdminSubject(), realm);
                    Set<Application> affected = new HashSet<>();
                    for (Application application : appService.getApplications()) {
                        if (application.getApplicationType().equals(type)
                                && agentsName.contains(application.getName())
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
