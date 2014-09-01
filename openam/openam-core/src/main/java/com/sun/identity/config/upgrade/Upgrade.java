/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponseWrapper;
import org.apache.click.control.ActionLink;
import org.forgerock.openam.upgrade.ServiceUpgradeWrapper;
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
    private ServiceUpgradeWrapper wrapper;
    private SSOToken adminToken;
    private Debug debug = UpgradeUtils.debug;
    public ActionLink doUpgradeLink = new ActionLink("doUpgrade", this, "doUpgrade");
    public ActionLink saveReportLink = new ActionLink("saveReport", this, "saveReport");

    public Upgrade() {
        try {
            adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
            upgrade = new UpgradeServices();
            wrapper = upgrade.preUpgrade(adminToken);
            addModel("currentVersion", UpgradeUtils.getCurrentVersion());
            addModel("newVersion", UpgradeUtils.getWarFileVersion());
            addModel("changelist", upgrade.generateShortUpgradeReport(adminToken, wrapper, true));
        } catch (Exception ue) {
            addModel("error", true);
            debug.error("An error occured, while initializing Upgrade page", ue);
        }
    }

    public boolean doUpgrade() {
        HttpServletRequestWrapper request =
                new HttpServletRequestWrapper(getContext().getRequest());
        HttpServletResponseWrapper response =
                new HttpServletResponseWrapper(getContext().getResponse());

        try {
            upgrade.upgrade(adminToken, wrapper);
            writeToResponse("true");
            setPath(null);
        } catch (UpgradeException ue) {
            writeToResponse(ue.getMessage());
            setPath(null);
            debug.error("Error occured while upgrading OpenAM", ue);
        } finally {
            UpgradeProgress.closeOutputStream();
        }
        return false;
    }

    public boolean saveReport() {
        try {
            String report = upgrade.
                    generateDetailedUpgradeReport(adminToken, wrapper, false);
            writeToResponse(report);
            getContext().getResponse().setContentType("application/force-download; charset=\"UTF-8\"");
            getContext().getResponse().setHeader(
                    "Content-Disposition", "attachment; filename=\"upgradereport." + System.currentTimeMillis() + "\"");
            getContext().getResponse().setHeader("Content-Description", "File Transfer");
            
            setPath(null);
        } catch (UpgradeException ue) {
            debug.error("Error occured while generating detailed report", ue);
        }
        return false;
    }
}
