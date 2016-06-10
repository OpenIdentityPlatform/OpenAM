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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

/**
 * A factory for creating OAuth2Request instances.
 *
 * @since 12.0.0
 */
@Singleton
public class OAuth2RequestFactory {

    private static final String OAUTH2_REQ_ATTR = "OAUTH2_REQ_ATTR";
    private final JacksonRepresentationFactory jacksonRepresentationFactory;

    /**
     * Guice injection constructor.
     * @param jacksonRepresentationFactory The factory for {@code JacksonRepresentation} instances.
     */
    @Inject
    public OAuth2RequestFactory(JacksonRepresentationFactory jacksonRepresentationFactory) {
        this.jacksonRepresentationFactory = jacksonRepresentationFactory;
    }

    /**
     * Creates a new OAuth2Request for the underlying HTTP request.
     *
     * @param request The underlying request.
     * @return The OAuth2Request.
     */
    public OAuth2Request create(Request request) {
        HttpServletRequest req = ServletUtils.getRequest(request);
        OAuth2Request o2request = req == null ? null : (OAuth2Request) req.getAttribute(OAUTH2_REQ_ATTR);
        if (o2request == null) {
            o2request = new OAuth2Request(jacksonRepresentationFactory, request);
            if (req != null) {
                req.setAttribute(OAUTH2_REQ_ATTR, o2request);
            }
        }
        return o2request;
    }
}
