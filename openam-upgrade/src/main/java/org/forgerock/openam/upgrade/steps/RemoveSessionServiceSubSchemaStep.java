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
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

import javax.inject.Inject;
import java.security.PrivilegedAction;

import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.ServiceSchema;
import com.google.common.collect.ImmutableMap;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;

/**
 * Removes the sub schema configuration from the SessionService
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public class RemoveSessionServiceSubSchemaStep extends AbstractUpgradeStep {

    private final String TO_BE_REMOVED_SUBSCHEMA_NAME = "Site";

    private final String AM_SESSION_SERVICE = "iPlanetAMSessionService";

    private boolean upgradeRequired;

    @Inject
    public RemoveSessionServiceSubSchemaStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return upgradeRequired;
    }

    @Override
    public void initialize() throws UpgradeException {
        upgradeRequired = UpgradeUtils.getServiceSchema(AM_SESSION_SERVICE, TO_BE_REMOVED_SUBSCHEMA_NAME,
                UpgradeUtils.SCHEMA_TYPE_GLOBAL, getAdminToken()) != null;
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            UpgradeProgress.reportStart("upgrade.sessionservice.subschema.removal.start");
            if (upgradeRequired) {
                ServiceSchema serviceSchema = UpgradeUtils.getServiceSchema(AM_SESSION_SERVICE, null,
                        UpgradeUtils.SCHEMA_TYPE_GLOBAL, getAdminToken());
                if (serviceSchema == null) {
                    throw new UpgradeException("Could not obtain ServiceSchema for SessionService. " +
                            "SessionService SubSchema elements cannot be removed.");
                }
                UpgradeUtils.removeSubSchema(AM_SESSION_SERVICE, TO_BE_REMOVED_SUBSCHEMA_NAME, serviceSchema);
            }
            UpgradeProgress.reportEnd("upgrade.success");
        } catch (Exception e) {
            DEBUG.error("Unexpected exception caught in RemoveSessionServiceSubSchemaStep#perform: " + e.getMessage(), e);
            UpgradeProgress.reportEnd("upgrade.failed");
            throw new UpgradeException("Upgrade of Session Service failed: " + e.getMessage(), e);
        }

    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString("upgrade.sessionservice.subschema.removal.short") + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        return tagSwapReport(ImmutableMap.of(LF, delimiter), "upgrade.sessionservice.subschema.removal.detailed");
    }
}
