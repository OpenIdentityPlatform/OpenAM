/*
 * Copyright 2013-2015 ForgeRock AS.
 *
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
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;

import com.iplanet.sso.SSOToken;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.EmbeddedOpenDS;
import com.sun.identity.sm.SMSEntry;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.DirectoryContentUpgrader;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;

/**
 * This upgrade step is meant to upgrade the directory schema/content for external configuration stores. For the
 * details on the performed changes see {@link DirectoryContentUpgrader}.
 */
@UpgradeStepInfo
public class UpgradeDirectoryContentStep extends AbstractUpgradeStep {

    private static final String DIRECTORY_DATA = "%DIRECTORY_DATA%";
    private DirectoryContentUpgrader upgrader;

    @Inject
    public UpgradeDirectoryContentStep(final PrivilegedAction<SSOToken> adminTokenAction,
                                       @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return upgrader.isApplicable();
    }

    @Override
    public void initialize() throws UpgradeException {
        String baseDir = AMSetupServlet.getBaseDir();
        String baseDN = SMSEntry.getRootSuffix();
        upgrader = new DirectoryContentUpgrader(baseDir, baseDN);
    }

    @Override
    public void perform() throws UpgradeException {
        upgrader.upgrade(false);
        if (EmbeddedOpenDS.isStarted()) {
            UpgradeServices.getInstance().setRebuildIndexes(true);
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        return BUNDLE.getString("upgrade.directory") + delimiter;
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);
        StringBuilder sb = new StringBuilder();
        sb.append(BUNDLE.getString("upgrade.directory.ldif")).append(delimiter);
        for (String ldif : upgrader.getLDIFPaths()) {
            sb.append(INDENT).append(ldif).append(delimiter);
        }
        tags.put(DIRECTORY_DATA, sb.toString());
        return tagSwapReport(tags, "upgrade.directoryreport");
        
    }
}
