/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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
 */
package com.sun.identity.config.upgrade;

import com.iplanet.sso.SSOToken;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import java.security.AccessController;
import org.apache.click.control.ActionLink;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeServices;
import org.forgerock.openam.upgrade.UpgradeUtils;

/**
 *
 * @author Peter Major
 */
public class Upgrade extends AjaxPage {

    private UpgradeServices upgrade;
    private SSOToken adminToken;
    private Debug debug = UpgradeUtils.debug;
    public ActionLink doUpgradeLink = new ActionLink("doUpgrade", this, "doUpgrade");
    public ActionLink saveReportLink = new ActionLink("saveReport", this, "saveReport");
    private boolean error = false;

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
     * Creating the UI models in the #onRender method makes sure that these fields are not always reinitialized when an
     * actionLink is clicked. This is necessary since Click always creates new instances for actionLink events.
     */
    @Override
    public void onRender() {
        if (error) {
            addModel("error", true);
        } else {
            addModel("currentVersion", UpgradeUtils.getCurrentVersion());
            addModel("newVersion", UpgradeUtils.getWarFileVersion());
            addModel("changelist", upgrade.generateShortUpgradeReport(adminToken, true));
        }
    }

    public boolean doUpgrade() {
        try {
            upgrade.upgrade(adminToken);
            writeToResponse("true");
        } catch (UpgradeException ue) {
            writeToResponse(ue.getMessage());
            debug.error("Error occured while upgrading OpenAM", ue);
        } finally {
            setPath(null);
            UpgradeProgress.closeOutputStream();
        }
        return false;
    }

    public boolean saveReport() {
        getContext().getResponse().setContentType("application/force-download; charset=\"UTF-8\"");
        getContext().getResponse().setHeader(
                "Content-Disposition", "attachment; filename=\"upgradereport." + System.currentTimeMillis() + "\"");
        getContext().getResponse().setHeader("Content-Description", "File Transfer");
        writeToResponse(upgrade.generateDetailedUpgradeReport(adminToken, false));
        setPath(null);
        return false;
    }
}
