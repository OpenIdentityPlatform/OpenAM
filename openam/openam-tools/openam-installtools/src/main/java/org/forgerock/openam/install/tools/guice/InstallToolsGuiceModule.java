/*
 * Copyright 2014 ForgeRock, AS.
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
 */

package org.forgerock.openam.install.tools.guice;

import com.google.inject.AbstractModule;
import com.sun.identity.install.tools.util.ConfigUtil;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.install.tools.logs.DebugFileLog;
import org.forgerock.openam.install.tools.logs.DebugLog;
import org.forgerock.openam.license.LicenseLog;
import org.forgerock.openam.license.PropertiesFileLicenseLog;

import java.io.File;

/**
 * Guice configuration for the install tools. Work in progress.
 *
 * @since 12.0.0
 */
@GuiceModule
public class InstallToolsGuiceModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(LicenseLog.class).toInstance(new PropertiesFileLicenseLog(new File(ConfigUtil.getDataDirPath())));
        bind(DebugLog.class).to(DebugFileLog.class);

    }
}
