/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package com.sun.identity.common;

public class ServerOrSiteEntry {

    private String id;
    private String url;
    private String originalUrl;

    public ServerOrSiteEntry(String serverEntry) {
        int index = serverEntry.indexOf("|");
        if (index != -1) {
            // Keep a copy of the original URL to avoid cases where the OpenAM context is using mixed-case,
            // for example /OpenAM rather than say /openam
            originalUrl = serverEntry.substring(0, index);
            url = originalUrl.toLowerCase();
            id = serverEntry.substring(index + 1, serverEntry.length());

            index = id.indexOf("|");
            if (index != -1) {
                id = id.substring(0, 2);
            }
        } else {
            throw new IllegalArgumentException("Invalid server entry: " + serverEntry);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
