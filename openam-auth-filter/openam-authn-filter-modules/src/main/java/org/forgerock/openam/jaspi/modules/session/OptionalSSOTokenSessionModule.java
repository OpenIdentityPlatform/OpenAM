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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.modules.session;

import javax.security.auth.message.AuthStatus;

/**
 * An AuthModule that will validate a SSOToken if it's present, else will allow the request through anyway.
 *
 * @since 12.0.0
 */
public class OptionalSSOTokenSessionModule extends LocalSSOTokenSessionModule {

    @Override
    protected AuthStatus getDefaultAuthStatus() {
        return AuthStatus.SUCCESS;
    }
}
