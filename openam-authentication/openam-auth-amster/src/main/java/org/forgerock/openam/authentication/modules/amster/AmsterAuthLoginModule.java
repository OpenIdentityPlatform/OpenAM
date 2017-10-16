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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.amster;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import com.google.common.annotations.VisibleForTesting;
import org.forgerock.openam.authentication.modules.common.AuthLoginModule;
import org.forgerock.openam.core.CoreWrapper;

/**
 * The {@link AuthLoginModule} for the {@link Amster} login module.
 */
class AmsterAuthLoginModule extends AuthLoginModule {

    private final CoreWrapper coreWrapper;

    /** Constructs an instance. Used by the {@link Amster} in a server deployment environment. */
    public AmsterAuthLoginModule() {
        this(new CoreWrapper());
    }

    /**
     * Constructs an instance. Used in a unit test environment.
     *
     * @param coreWrapper An instance of the CoreWrapper.
     */
    @VisibleForTesting
    AmsterAuthLoginModule(CoreWrapper coreWrapper) {
        this.coreWrapper = coreWrapper;
    }

    @Override
    public void init(Subject subject, Map sharedState, Map options) {

    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        return 0;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }
}
