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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import static org.forgerock.openam.upgrade.VersionUtils.isCurrentVersionLessThan;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.entitlement.util.SearchFilter;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.util.Reject;

import jakarta.inject.Inject;
import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This upgrade step simply re-saves all policies. The primary purpose of this is to capture any changes between versions
 * that may have influenced such data as the subject indexes.
 * <p>
 * More specifically a couple use cases are:
 * <ul>
 * <li>
 * In 11.0.0 the entitlement indexes has been resaved without maintaining the trailing slash ('/') characters. Because
 * of this behavior the originally introduced UpgradeEntitlementsStep is not able to regenerate the entitlement indexes
 * (the entitlement index entries do not contain the original resourceName with the trailing slash); see OPENAM-3509.
 * </li>
 * <li>
 * In 13.5.0 subject indexing has been enabled and therefore re-saving the policies ensures these indexes are in place.
 * </li>
 * </ul>
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.RemoveReferralsStep")
public class ResavePoliciesStep extends AbstractUpgradeStep {

    private static final int AM_14 = 1400;
    private static final String POLICY_DATA = "%POLICY_DATA%";

    private final Map<String, Set<String>> policyMap = new HashMap<>();

    @Inject
    public ResavePoliciesStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    public boolean isApplicable() {
        return !policyMap.isEmpty();
    }

    public void initialize() throws UpgradeException {
        DEBUG.message("Initializing ResavePoliciesStep");
        if (isCurrentVersionLessThan(AM_14, true)) {
            Subject adminSubject = getAdminSubject();

            try {
                for (String realm : getRealmNames()) {
                    PrivilegeIndexStore indexStore = getPolicyIndexStore(adminSubject, realm);
                    Set<String> policyNames = indexStore
                            .searchPrivilegeNames(Collections.<SearchFilter>emptySet(), true, 0, false, false);

                    if (isNotEmpty(policyNames)) {
                        policyMap.put(realm, new HashSet<>(policyNames));
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
        Subject adminSubject = getAdminSubject();

        try {
            for (Map.Entry<String, Set<String>> entry : policyMap.entrySet()) {
                String realm = entry.getKey();
                Set<String> policyNames = entry.getValue();

                PrivilegeIndexStore indexStore = getPolicyIndexStore(adminSubject, realm);

                for (String policyName : policyNames) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Resaving the following policy: " + policyName);
                    }
                    UpgradeProgress.reportStart("upgrade.policy.start", policyName);
                    IPrivilege policy = indexStore.getPrivilege(policyName);
                    indexStore.delete(policyName);
                    indexStore.add(Collections.singleton(policy));
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


    private PrivilegeIndexStore getPolicyIndexStore(Subject adminSubject, String realm) {
        PrivilegeIndexStore indexStore = PrivilegeIndexStore.getInstance(adminSubject, realm);
        Reject.ifNull(indexStore);
        return indexStore;
    }

}
