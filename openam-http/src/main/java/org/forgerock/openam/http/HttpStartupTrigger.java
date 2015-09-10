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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.http;

import com.sun.identity.setup.SetupListener;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.HttpApplication;

/**
 * This class makes sure that the Guice configuration for the HTTP and REST
 * services are computed during container startup rather than during the first
 * request.
 *
 * @since 13.0.0
 */
public class HttpStartupTrigger implements SetupListener {

    @Override
    public void setupComplete() {
        InjectorHolder.getInstance(HttpApplication.class);
    }
}
