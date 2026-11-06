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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems LLC.
 */
package com.sun.identity.config.upgrade;

import org.openidentityplatform.openam.config.servlet.ConfiguratorPageProvider;
import org.openidentityplatform.openam.config.servlet.SetupPage;

/**
 * Registers {@link Upgrade} with {@code ConfiguratorServlet} via {@link java.util.ServiceLoader}
 * (see {@code META-INF/services/org.openidentityplatform.openam.config.servlet.ConfiguratorPageProvider}
 * in this module's resources). {@code openam-upgrade} depends on {@code openam-core}, not the
 * other way round, so this self-registration is what lets {@code Upgrade} join the migrated-page
 * registry without {@code openam-core} needing a compile-time reference to it.
 */
public class UpgradePageProvider implements ConfiguratorPageProvider {

    @Override
    public String getPath() {
        return "/config/upgrade/upgrade.htm";
    }

    @Override
    public Class<? extends SetupPage> getPageClass() {
        return Upgrade.class;
    }
}
