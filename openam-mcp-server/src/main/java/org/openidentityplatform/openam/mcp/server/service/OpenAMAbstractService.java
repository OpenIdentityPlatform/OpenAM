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
 * Copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.mcp.server.service;

import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public abstract class OpenAMAbstractService {

    protected final RestClient openAMRestClient;

    protected final OpenAMConfig openAMConfig;

    protected final static String DEFAULT_REALM = "root";

    public OpenAMAbstractService(RestClient openAMRestClient, OpenAMConfig openAMConfig) {
        this.openAMRestClient = openAMRestClient;
        this.openAMConfig = openAMConfig;
    }

    protected String getTokenId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return (String) attrs.getAttribute("tokenId", RequestAttributes.SCOPE_REQUEST);
    }

    protected String getRealmOrDefault(String realm) {
        return realm != null ? realm : DEFAULT_REALM;
    }

}
