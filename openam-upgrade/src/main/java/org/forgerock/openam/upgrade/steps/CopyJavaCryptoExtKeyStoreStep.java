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

import static org.forgerock.openam.upgrade.UpgradeServices.LF;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;

import org.forgerock.openam.core.guice.ServletContextCache;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.VersionUtils;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.shared.Constants;

/**
 * Copies across the JCEKS keystore added in 13.5.0 to the config directory.
 *
 * @since 13.5.0
 */
@UpgradeStepInfo
public final class CopyJavaCryptoExtKeyStoreStep extends AbstractUpgradeStep {

    private final static String AUDIT_REPORT = "upgrade.copy.keystore.to.configuration.report";
    private final static String AUDIT_SIMPLE = "upgrade.copy.keystore.to.configuration.simple";
    private final static String AUDIT_START = "upgrade.copy.keystore.to.configuration.start";
    private final static String AUDIT_SUCCESS = "upgrade.copy.keystore.to.configuration.success";
    private final static String AUDIT_FAILURE = "upgrade.copy.keystore.to.configuration.failure";

    private static final int AM_13_5 = 1350;

    private static final String KEYSTORE_NAME = "keystore.jceks";
    private static final String KEYSTORE_PATH = "/WEB-INF/template/keystore/" + KEYSTORE_NAME;

    private final ServletContext context;

    private Path keystorePath;
    private boolean applicability;

    /**
     * Step constructor that passes through expected parameters.
     *
     * @param adminTokenAction
     *         admin token action
     * @param connectionFactory
     *         connection factory
     * @param context
     *         servlet context
     */
    @Inject
    public CopyJavaCryptoExtKeyStoreStep(PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) ConnectionFactory connectionFactory,
            @Named(ServletContextCache.CONTEXT_REFERENCE) ServletContext context) {
        super(adminTokenAction, connectionFactory);
        this.context = context;
    }

    @Override
    public void initialize() throws UpgradeException {
        if (!VersionUtils.isCurrentVersionLessThan(AM_13_5, true)) {
            // JCEKS keystore introduced from 13.5.0 onwards.
            return;
        }

        String basedir = AMSetupServlet.getBaseDir();
        String uri = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

        keystorePath = Paths.get(basedir, uri, KEYSTORE_NAME);
        applicability = Files.notExists(keystorePath);
    }

    @Override
    public boolean isApplicable() {
        return applicability;
    }

    @Override
    public void perform() throws UpgradeException {
        UpgradeProgress.reportStart(AUDIT_START);

        try (InputStream inputStream = context.getResourceAsStream(KEYSTORE_PATH)) {
            Files.copy(inputStream, keystorePath);
            UpgradeProgress.reportEnd(AUDIT_SUCCESS);
        } catch (IOException ioE) {
            UpgradeProgress.reportEnd(AUDIT_FAILURE);
            throw new UpgradeException("Failed to copy JCEKS keystore across", ioE);
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
