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
 * Copyright 2014 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

import com.sun.identity.setup.AMSetupServlet;

/**
 * A {@link SetupPage} subclass that blocks access once OpenAM has already been configured.
 * This restores the single source of truth from the old Click-era {@code ProtectedPage} base
 * class, so that every setup/wizard page automatically guards against re-entry without
 * needing to duplicate the override.
 *
 * <p>Pages that must remain reachable on configured installs (e.g. {@code Options},
 * {@code Upgrade}) should extend {@link SetupPage} directly.
 */
public abstract class ProtectedSetupPage extends SetupPage {

    @Override
    public boolean onSecurityCheck() {
        // Ported from the old com.sun.identity.config.util.ProtectedPage: block re-entry once
        // OpenAM has already been configured.
        if (AMSetupServlet.isConfigured()) {
            skipRender();
            return false;
        }
        return true;
    }
}
