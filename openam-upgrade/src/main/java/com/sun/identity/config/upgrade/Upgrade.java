/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2016 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted 2026 3A Systems LLC.
 */
package com.sun.identity.config.upgrade;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.security.AccessController;
import org.openidentityplatform.openam.config.servlet.ConfiguratorAction;
import org.openidentityplatform.openam.config.servlet.SetupPage;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.forgerock.openam.upgrade.VersionUtils;

/**
 * OpenAM upgrade page.
 */
public class Upgrade extends SetupPage {

    private UpgradeServices upgrade;
    private SSOToken adminToken;
    private Debug debug = UpgradeUtils.debug;
    private boolean error = false;

    // No onSecurityCheck() override: the old Upgrade extended AjaxPage directly (not
    // ProtectedPage), so it stayed reachable regardless of AMSetupServlet.isConfigured() - that's
    // how the options page (increment 6) reaches it to run an upgrade on an already-configured
    // install. SetupPage's default onSecurityCheck() (always true) already matches that.

    public Upgrade() {
        try {
            debug.message("Initializing upgrade subsystem.");
            adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            upgrade = UpgradeServices.getInstance();
        } catch (Exception ue) {
            error = true;
            debug.error("An error occured, while initializing Upgrade page", ue);
        }
    }

    /**
     * Building the UI model in {@code onGet()} (called only when the request is actually going
     * to render, not on an {@code ?actionLink=} dispatch - see
     * {@code org.openidentityplatform.openam.config.servlet.ConfiguratorServlet}) makes sure
     * {@code generateShortUpgradeReport} is not redone on every {@code doUpgrade}/
     * {@code saveReport} call. This is necessary since Click always creates new instances for
     * actionLink events, and the old code relied on {@code onRender()} for the identical reason -
     * {@code onRender()} itself has no equivalent in {@code ConfiguratorServlet}'s lifecycle.
     */
    @Override
    public void onGet() {
        if (error) {
            addModel("error", true);
        } else {
            addModel("currentVersion", VersionUtils.getCurrentVersion());
            addModel("newVersion", VersionUtils.getWarFileVersion());
            addModel("changelist", upgrade.generateShortUpgradeReport(adminToken, true));
        }
    }

    @ConfiguratorAction
    public boolean doUpgrade() {
        try {
            SystemProperties.initializeProperties(Constants.SYS_PROPERTY_INSTALL_TIME, "true");
            upgrade.upgrade(adminToken, isLicenseAccepted());
            SystemProperties.initializeProperties(Constants.SYS_PROPERTY_INSTALL_TIME, "false");
            writeToResponse("true");
        } catch (UpgradeException ue) {
            writeToResponse(ue.getMessage());
            debug.error("Error occured while upgrading OpenAM", ue);
        } finally {
            UpgradeProgress.closeOutputStream();
        }
        return false;
    }

    @ConfiguratorAction
    public boolean saveReport() {
        getContext().getResponse().setContentType("application/force-download; charset=\"UTF-8\"");
        getContext().getResponse().setHeader(
                "Content-Disposition", "attachment; filename=\"upgradereport." + currentTimeMillis() + "\"");
        getContext().getResponse().setHeader("Content-Description", "File Transfer");
        writeToResponse(upgrade.generateDetailedUpgradeReport(adminToken, false));
        return false;
    }

    /**
     * Checks whether the user has accepted the terms of the license agreement.
     *
     * @return true if the license acceptance parameter is present and correct, otherwise false.
     */
    private boolean isLicenseAccepted() {
        return Boolean.parseBoolean(getContext().getRequest().getParameter(SetupConstants.ACCEPT_LICENSE_PARAM));
    }
}
