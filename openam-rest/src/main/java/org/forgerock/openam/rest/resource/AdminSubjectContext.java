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

package org.forgerock.openam.rest.resource;

import java.security.AccessController;

import org.forgerock.guava.common.base.Supplier;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.services.context.Context;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;

/**
 * SubjectContext implementation which contains an admin token.
 */
public class AdminSubjectContext extends SSOTokenContext {

    public AdminSubjectContext(final Debug debug, final SessionCache sessionCache, Context parent) {
        super(debug, sessionCache, parent, new Supplier<SSOToken>() {
            @Override
            public SSOToken get() {
                return AccessController.doPrivileged(AdminTokenAction.getInstance());
            }
        });
    }
}
