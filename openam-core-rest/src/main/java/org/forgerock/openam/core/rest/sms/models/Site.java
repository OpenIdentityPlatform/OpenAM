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
import org.forgerock.api.annotations.UniqueItems;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Site bean.
 */
@Title("Site")
@Description("Site description")
public final class Site {

    @NotNull
    @Title("Name")
    @Description("Site name")
    private final String _id;

    @NotNull
    @Title("Site id")
    @Description("Site id")
    private final String id;

    @NotNull
    @Title("Primary URL")
    @Description("Site URL")
    private final String url;

    @Title("Secondary URLs")
    @Description("Secondary URLs for this site")
    @UniqueItems
    private List<String> secondaryUrls = new ArrayList<>();

    @Title("Assigned Servers")
    @Description("Servers assigned to this site")
    @UniqueItems
    private List<SiteServer> servers = new ArrayList<>();

    /**
     * Default Site Constructor
     * @param _id site name
     * @param id site id
     * @param url site URL
     */
    public Site(String _id, String id, String url) {
        this._id = _id;
        this.id = id;
        this.url = url;
    }

    public List<SiteServer> getServers() {
        return servers;
    }

    public void setServers(List<SiteServer> servers) {
        this.servers = servers;
    }

    public List<String> getSecondaryUrls() {
        return secondaryUrls;
    }

    public void setSecondaryUrls(List<String> secondaryUrls) {
        this.secondaryUrls = secondaryUrls;
    }

    public String get_id() {
        return _id;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }
}
