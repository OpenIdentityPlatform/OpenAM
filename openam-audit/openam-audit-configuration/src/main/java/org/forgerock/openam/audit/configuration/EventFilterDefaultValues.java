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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.audit.configuration;

import com.sun.identity.sm.DefaultValues;
import org.forgerock.openam.utils.CollectionUtils;

import java.util.Set;

/**
 * Default event filter values used to filter fields and values out of audit events.
 *
 * @since 13.0.0
 */
public final class EventFilterDefaultValues extends DefaultValues {

    private static final String[] DEFAULT_VALUES = new String[] {
        "/access/http/request/cookies/%AM_COOKIE_NAME%",
        "/access/http/request/headers/accept-encoding",
        "/access/http/request/headers/accept-language",
        "/access/http/request/headers/%AM_COOKIE_NAME%",
        "/access/http/request/headers/%AM_AUTH_COOKIE_NAME%",
        "/access/http/request/headers/authorization",
        "/access/http/request/headers/cache-control",
        "/access/http/request/headers/connection",
        "/access/http/request/headers/content-length",
        "/access/http/request/headers/content-type",
        "/access/http/request/headers/proxy-authorization",
        "/access/http/request/headers/X-OpenAM-Password",
        "/access/http/request/queryParameters/access_token",
        "/access/http/request/queryParameters/id_token_hint",
        "/access/http/request/queryParameters/IDToken1",
        "/access/http/request/queryParameters/Login.Token1",
        "/access/http/request/queryParameters/redirect_uri",
        "/access/http/request/queryParameters/requester",
        "/access/http/request/queryParameters/sessionUpgradeSSOTokenId",
        "/access/http/request/queryParameters/tokenId",
        "/config/after",
        "/config/before"
    };

    @Override
    public Set getDefaultValues() {
        return CollectionUtils.asSet(DEFAULT_VALUES);
    }
}
