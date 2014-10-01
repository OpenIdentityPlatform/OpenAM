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
 */

package com.sun.identity.config.util;

import com.sun.identity.setup.AMSetupServlet;

/**
 * Any page that needs to be protected post OpenAM setup should extend this class, for example, all the Config Wizard
 * pages.
 */
public class ProtectedPage extends AjaxPage {

    @Override
    public boolean onSecurityCheck() {

        // If we have already been configured then return false to trigger the Click framework to
        // not allow access to the page.
        if (AMSetupServlet.isConfigured()) {
            setPath(null);
            return false;
        } else {
            return true;
        }
    }
}
