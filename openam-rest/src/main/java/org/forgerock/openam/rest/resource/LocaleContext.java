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

import java.util.Locale;

import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.services.context.AbstractContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;

import com.sun.identity.common.ISLocaleContext;

/**
 * Determines the locale corresponding to the incoming CREST request by utilizing {@link ISLocaleContext}, and makes
 * the locale available as a {@link Context}.
 */
public class LocaleContext extends AbstractContext {

    private Locale locale = null;

    public LocaleContext(Context parent) {
        super(parent, "locale");
        Reject.ifFalse(parent.containsContext(HttpContext.class), "Parent context must contain an HttpContext");
    }

    /**
     * The {@link Locale} corresponding to the incoming request.
     * @return The client's preferred locale.
     */
    public Locale getLocale() {
        if (locale == null) {
            final HttpContext httpContext = asContext(HttpContext.class);
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(httpContext);
            locale = localeContext.getLocale();
        }
        return locale;
    }
}
