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

package org.forgerock.openam.rest;

import org.forgerock.http.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 *
 */
public class ExceptionableAuthenticationFilter implements Filter {

    private final Filter authenticationFilter;
    private final AuthenticationModule authenticationModule;

    ExceptionableAuthenticationFilter(Filter authenticationFilter, AuthenticationModule authenticationModule) {
        this.authenticationFilter = authenticationFilter;
        this.authenticationModule = authenticationModule;
    }

    public ExceptionableAuthenticationFilter exceptCreate() {
        authenticationModule.exceptCreate();
        return this;
    }

    public ExceptionableAuthenticationFilter exceptRead() {
        authenticationModule.exceptRead();
        return this;
    }

    public ExceptionableAuthenticationFilter exceptUpdate() {
        authenticationModule.exceptUpdate();
        return this;
    }

    public ExceptionableAuthenticationFilter exceptDelete() {
        authenticationModule.exceptDelete();
        return this;
    }

    public ExceptionableAuthenticationFilter exceptPatch() {
        authenticationModule.exceptPatch();
        return this;
    }

    public ExceptionableAuthenticationFilter exceptActions(String... actions) {
        authenticationModule.exceptActions(actions);
        return this;
    }

    public ExceptionableAuthenticationFilter exceptQuery() {
        authenticationModule.exceptQuery();
        return this;
    }

    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        return authenticationFilter.filter(context, request, next);
    }
}
