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

package org.forgerock.openam.services.baseurl;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.utils.OpenAMSettings;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A {@link BaseURLProvider} implementation that uses the scheme, serverName and serverPort properties of
 * the {@link javax.servlet.http.HttpServletRequest}.
 */
public class RequestValuesBaseURLProvider extends BaseURLProvider {

    private Debug debug = Debug.getInstance("amBaseURL");

    @Override
    protected String getBaseURL(HttpServletRequest request) {
        return getBaseUrl(request.getScheme(), request.getServerName(), request.getServerPort());
    }

    @Override
    protected String getBaseURL(HttpContext context) {
        // get URI
        String path = context.getPath();
        try {
            URI uri = new URI(path);
            return getBaseUrl(uri.getScheme(), uri.getHost(), uri.getPort());
        } catch (URISyntaxException e) {
            debug.error("URL '" + path + "' can't be parsed", e);
            throw new IllegalArgumentException("URL '" + path + "' can't be parsed", e);
        }
    }

    private String getBaseUrl(String scheme, String host, int port) {
        return scheme + "://" + host + ":" + port;
    }

    @Override
    void init(OpenAMSettings settings, String realm) {
        // no-op
    }
}
