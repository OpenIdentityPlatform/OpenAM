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

package org.forgerock.openam.core.rest.sms.models;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

import javax.validation.constraints.NotNull;

/**
 * SiteServer bean.
 */
@Title("SiteServer")
@Description("Site's model of a Server")
public class SiteServer {

    @NotNull
    @Title("Server ID")
    @Description("Unique server identifier")
    private final String id;

    @NotNull
    @Title("Server URL")
    @Description("Unique server URL")
    private final String url;

    public SiteServer(String id, String url) {
        this.id = id;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }
}
