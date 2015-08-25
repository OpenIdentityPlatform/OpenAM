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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.utils.ForwardedHeader;
import org.forgerock.openam.utils.OpenAMSettings;

/**
 * A {@link BaseURLProvider} that uses the Forwarded headers to deduce the base URL.
 * @see <a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>
 */
public class ForwardedHeaderBaseURLProvider extends BaseURLProvider {

    @Override
    protected String getBaseURL(HttpServletRequest request) {
        return getBaseURL(ForwardedHeader.parse(request));
    }

    @Override
    protected String getBaseURL(HttpContext context) {
        return getBaseURL(ForwardedHeader.parse(context));
    }

    /**
     * Get Base URL from headers
     * @param header
     * @return base URL
     */
    private String getBaseURL(ForwardedHeader header) {
        List<String> host = header.getHostValues();
        if (host.size() != 1) {
            throw new IllegalArgumentException("Cannot deduce host value from headers: " + host);
        }
        List<String> proto = header.getProtoValues();
        if (proto.size() != 1) {
            throw new IllegalArgumentException("Cannot deduce proto value from headers: " + proto);
        }
        return proto.get(0) + "://" + host.get(0);
    }

    @Override
    void init(OpenAMSettings settings, String realm) {
        // no-op
    }

}
