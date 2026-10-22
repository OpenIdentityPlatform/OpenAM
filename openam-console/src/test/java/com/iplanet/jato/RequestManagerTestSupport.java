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
 * Copyright 2026 3A Systems, LLC.
 */
package com.iplanet.jato;

/**
 * Test-only access to the JATO per-thread request context. {@link RequestManager#setRequestContext} is
 * {@code protected static} and normally called by the JATO servlet for each request; living in the same
 * package lets a test install the context that page-session code reads through
 * {@link RequestManager#getRequestContext()}, without a production seam.
 */
public final class RequestManagerTestSupport {

    private RequestManagerTestSupport() {
    }

    /**
     * Installs {@code context} as the current thread's JATO request context; {@code null} clears it.
     *
     * @param context the request context, or {@code null}.
     */
    public static void install(RequestContext context) {
        RequestManager.setRequestContext(context);
    }
}
