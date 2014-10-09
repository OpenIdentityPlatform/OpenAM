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
package org.forgerock.openam.upgrade.steps;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.ServicesDefaultValues;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.opendj.ldap.ConnectionFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

/**
 * This upgrade step check whether the new privilege for agents already exists and if it doesn't creates it. This
 * privilege will give policy agents default access to evaluate policies against the new policy resource endpoint.
 *
 * @since 12.0.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class AllowEvaluateForAgentsUpgradeStep extends AbstractUpgradeStep {

    private static final String DELEGATION_POLICY_FILE = "/WEB-INF/template/sms/policyServiceDelegationPolicies.xml";
    private static final String HIDDEN_REALM = "/sunamhiddenrealmdelegationservicepermissions";
    private static final String EVALUATE_POLICY = "AgentAccessToEvaluatePolicies";

    private static final String AUDIT_NEW_POLICY_START = "upgrade.privileges.new.pa.evaluation.start";
    private static final String AUDIT_NEW_POLICY = "upgrade.privileges.new.pa.evaluation";
    private static final String AUDIT_UPGRADE_SUCCESS = "upgrade.success";
    private static final String AUDIT_UPGRADE_FAIL = "upgrade.failed";
    private static final String DATA_PLACEHOLDER = "%DATA_PLACEHOLDER%";
    private static final String AUDIT_REPORT = "upgrade.privileges";

    private PolicyManager manager;
    private boolean applicable;

    @Inject
    public AllowEvaluateForAgentsUpgradeStep(final PrivilegedAction<SSOToken> adminTokenAction,
                                             @Named(DataLayerConstants.DATA_LAYER_BINDING)
                                             final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        try {
            // Does the policy already exist...
            manager = new PolicyManager(getAdminToken(), HIDDEN_REALM);
            applicable = manager.getPolicyNames(EVALUATE_POLICY).isEmpty();
        } catch (SSOException ssoE) {
            throw new UpgradeException("Failed to identify existing privileges", ssoE);
        } catch (PolicyException pE) {
            throw new UpgradeException("Failed to identify existing privileges", pE);
        }
    }

    @Override
    public boolean isApplicable() {
        return applicable;
    }

    @Override
    public void perform() throws UpgradeException {

        try {
            UpgradeProgress.reportStart(AUDIT_NEW_POLICY_START);

            // Creates a new policy entry to represent the new agent privilege.
            DEBUG.message("Creating new default privilege for agents called " + EVALUATE_POLICY);
            String policy = AMSetupServlet.readFile(DELEGATION_POLICY_FILE).toString();
            policy = ServicesDefaultValues.tagSwap(policy, true);
            PolicyUtils.createPolicies(manager, new ByteArrayInputStream(policy.getBytes()));

            UpgradeProgress.reportEnd(AUDIT_UPGRADE_SUCCESS);

        } catch (IOException ioE) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed during the creation of a new privilege for agents", ioE);
        } catch (SSOException ssoE) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed during the creation of a new privilege for agents", ssoE);
        } catch (PolicyException pE) {
            UpgradeProgress.reportEnd(AUDIT_UPGRADE_FAIL);
            throw new UpgradeException("Failed during the creation of a new privilege for agents", pE);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        final StringBuilder builder = new StringBuilder();
        builder.append(BUNDLE.getString(AUDIT_NEW_POLICY))
                .append(delimiter);
        return builder.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        final StringBuilder builder = new StringBuilder();
        builder.append(BUNDLE.getString(AUDIT_NEW_POLICY))
                .append(':')
                .append(delimiter)
                .append(EVALUATE_POLICY);

        final Map<String, String> reportContents = new HashMap<String, String>();
        reportContents.put(DATA_PLACEHOLDER, builder.toString());
        reportContents.put(UpgradeServices.LF, delimiter);

        return UpgradeServices.tagSwapReport(reportContents, AUDIT_REPORT);
    }

}
