/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Options.java,v 1.7 2009/01/05 23:17:09 veiming Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems LLC.
 */

package com.sun.identity.config;

import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.EmbeddedOpenDS;
import org.forgerock.openam.upgrade.VersionUtils;
import org.openidentityplatform.openam.config.servlet.SetupPage;

public class Options extends SetupPage {

    // Unlike the wizard/summary pages, this has no onSecurityCheck() override: the old Options
    // extended TemplatedPage (not ProtectedPage), so it stayed reachable even once OpenAM is
    // configured - that's how a completed install reaches the upgrade-options branch below.
    // SetupPage's default onSecurityCheck() (always true) already matches that.

    @Override
    public void onInit() {
        super.onInit();

        boolean upgrade = !isNewInstall();
        addModel("upgrade", Boolean.valueOf(upgrade));

        boolean upgradeCompleted = AMSetupServlet.isUpgradeCompleted();
        addModel("upgradeCompleted", Boolean.valueOf(upgradeCompleted));

        if (upgrade) {
            addModel("currentVersion", VersionUtils.getCurrentVersion());
        }

        boolean isOpenDS1x = EmbeddedOpenDS.isOpenDSVer1Installed();
        addModel("isOpenDS1x", Boolean.valueOf(isOpenDS1x));

        if (isOpenDS1x) {
            addModel("odsdir", AMSetupServlet.getBaseDir());
        }

        if (getContext().getRequest().getParameter("debug") != null) {
            AMSetupServlet.enableDebug();
        }
    }

    /**
     * @return boolean If <code>true</code>, the options.htm page will be customized for a new installation,
     * otherwise it will be customized for an upgrade.
     *
     * Automatic upgrades from 9.5 onwards are supported.
     */
    public boolean isNewInstall() {
        return !(AMSetupServlet.isConfigured() && VersionUtils.isVersionNewer());
    }

}
