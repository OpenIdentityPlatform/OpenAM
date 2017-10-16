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

import static com.sun.identity.common.configuration.ServerConfiguration.*;
import static com.google.common.collect.Maps.fromProperties;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.VersionUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.sm.SMSException;

/**
 * When no keystore type is defined, the crypto APIs default to JKS. This step explicitly states
 * this default, to avoid this property being set to the wrong type by newer versions of AM (such as JCEKS).
 *
 * @since 13.5.0
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeServiceSchemaStep")
public final class MakeExplicitDefaultKeyStoreTypeStep extends AbstractUpgradeStep {

    private final static String AUDIT_REPORT = "upgrade.assign.default.keystore.type.report";
    private final static String AUDIT_SIMPLE = "upgrade.assign.default.keystore.type.simple";
    private final static String AUDIT_START = "upgrade.assign.default.keystore.type.start";
    private final static String AUDIT_SUCCESS = "upgrade.assign.default.keystore.type.success";
    private final static String AUDIT_FAILURE = "upgrade.assign.default.keystore.type.failure";

    private static final String KEYSTORE_TYPE = "com.sun.identity.saml.xmlsig.storetype";
    private static final int AM_13_5 = 1350;

    private boolean applicability;

    /**
     * Step constructor that passes through expected parameters.
     *
     * @param adminTokenAction
     *         admin token action
     * @param connectionFactory
     *         connection factory
     */
    @Inject
    public MakeExplicitDefaultKeyStoreTypeStep(PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory) {
        super(adminTokenAction, connectionFactory);
    }

    @Override
    public void initialize() throws UpgradeException {
        if (!VersionUtils.isCurrentVersionLessThan(AM_13_5, true)) {
            // Prior to 13.5.0 the default keystore type was not defined
            return;
        }

        try {
            Properties existingDefaults = getServerInstance(getAdminToken(), ServerConfiguration.DEFAULT_SERVER_CONFIG);
            applicability = isEmpty(existingDefaults.getProperty(KEYSTORE_TYPE));
        } catch (SSOException | IOException | SMSException e) {
            throw new UpgradeException("Failed to read server defaults", e);
        }
    }

    @Override
    public boolean isApplicable() {
        return applicability;
    }

    @Override
    public void perform() throws UpgradeException {
        try {
            UpgradeProgress.reportStart(AUDIT_START);
            Map<String, String> serverDefaults = new HashMap<>(fromProperties(
                    getServerInstance(getAdminToken(), ServerConfiguration.DEFAULT_SERVER_CONFIG)));
            serverDefaults.put(KEYSTORE_TYPE, "JKS");

            ServerConfiguration.upgradeServerInstance(getAdminToken(),
                    DEFAULT_SERVER_CONFIG, DEFAULT_SERVER_ID, serverDefaults);
            UpgradeProgress.reportEnd(AUDIT_SUCCESS);
        } catch (SSOException | IOException | ConfigurationException | SMSException e) {
            UpgradeProgress.reportEnd(AUDIT_FAILURE);
            throw new UpgradeException("Failed to update server defaults", e);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString(AUDIT_SIMPLE) + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        String report = getShortReport(delimiter);
        Map<String, String> reportEntries = new HashMap<>();
        reportEntries.put("%REPORT_TEXT%", report);
        reportEntries.put(LF, delimiter);
        return UpgradeServices.tagSwapReport(reportEntries, AUDIT_REPORT);
    }

}
