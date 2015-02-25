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

import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyManager;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.forgerock.opendj.ldap.ConnectionFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

/**
 * This upgrade step has been implemented to explicitly handle the case when OpenAM is upgraded from 11.0.0. This is
 * necessary as due to OPENAM-3509 in 11.0.0 the entitlement indexes has been resaved without maintaining the trailing
 * slash ('/') characters. Because of this behavior the originally introduced UpgradeEntitlementsStep is not able to
 * regenerate the entitlement indexes (the entitlement index entries do not contain the original resourceName with the
 * trailing slash), this upgrade step has been introduced to manually resave the policies.
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class ResavePoliciesStep extends AbstractUpgradeStep {

    private static final String POLICY_DATA = "%POLICY_DATA%";
    private final Map<String, Set<String>> policyMap = new HashMap<String, Set<String>>();

    @Inject
    public ResavePoliciesStep(final PrivilegedAction<SSOToken> adminTokenAction,
                              @Named(DataLayerConstants.DATA_LAYER_BINDING) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    public boolean isApplicable() {
        return !policyMap.isEmpty();
    }

    public void initialize() throws UpgradeException {
        DEBUG.message("Initializing ResavePoliciesStep");
        if (UpgradeUtils.isCurrentVersionEqualTo(UpgradeUtils.ELEVEN_VERSION_NUMBER)) {
            try {
                for (String realm : getRealmNames()) {
                    PolicyManager pm = new PolicyManager(getAdminToken(), realm);
                    Set<String> policyNames = pm.getPolicyNames();
                    if (policyNames != null && !policyNames.isEmpty()) {
                        policyMap.put(realm, new HashSet<String>(policyNames));
                    }
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Discovered following policies:\n" + policyMap);
                }
            } catch (Exception ex) {
                DEBUG.error("Error while trying to retrieve policy names", ex);
                throw new UpgradeException(ex);
            }
        }
    }

    public void perform() throws UpgradeException {
        try {
            for (Map.Entry<String, Set<String>> entry : policyMap.entrySet()) {
                String realm = entry.getKey();
                Set<String> policyNames = entry.getValue();
                PolicyManager pm = new PolicyManager(getAdminToken(), realm);
                for (String policyName : policyNames) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Resaving the following policy: " + policyName);
                    }
                    UpgradeProgress.reportStart("upgrade.policy.start", policyName);
                    Policy policy = pm.getPolicy(policyName);
                    pm.replacePolicy(policy);
                    UpgradeProgress.reportEnd("upgrade.success");
                }
            }
        } catch (Exception ex) {
            UpgradeProgress.reportEnd("upgrade.failed");
            DEBUG.error("An error occurred while trying to resave policies", ex);
            throw new UpgradeException(ex);
        }
    }

    public String getShortReport(String delimiter) {
        int policyCount = 0;
        for (Set<String> policyNames : policyMap.values()) {
            policyCount += policyNames.size();
        }
        StringBuilder sb = new StringBuilder();
        if (policyCount != 0) {
            sb.append(BUNDLE.getString("upgrade.policy.short")).append(" (").append(policyCount).append(')')
                    .append(delimiter);
        }
        return sb.toString();
    }

    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : policyMap.entrySet()) {
            sb.append(BUNDLE.getString("upgrade.realm")).append(": ").append(entry.getKey()).append(delimiter);
            for (String policyNames : entry.getValue()) {
                sb.append(INDENT).append(policyNames).append(delimiter);
            }
        }
        tags.put(POLICY_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.policyreport");
    }
}
