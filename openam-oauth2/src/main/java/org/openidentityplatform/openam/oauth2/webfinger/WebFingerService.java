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

package org.openidentityplatform.openam.oauth2.webfinger;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.openam.http.annotations.Contextual;
import org.forgerock.openam.http.annotations.Get;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openidconnect.OpenIDConnectProviderDiscovery;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;

/**
 * OpenID Connect issuer discovery over WebFinger (RFC 7033) in the realm of the request.
 */
public class WebFingerService {

    /** The discovery errors carry no charset, as the answers of the former Restlet resource did. */
    static final String ERROR_CONTENT_TYPE = "application/json";

    private final OpenIDConnectProviderDiscovery providerDiscovery;
    private final BaseURLProviderFactory baseURLProviderFactory;

    /**
     * Creates the endpoint.
     *
     * @param providerDiscovery Builds the JRD and validates the parameters.
     * @param baseURLProviderFactory Gives the deployment URL of a realm.
     */
    @Inject
    public WebFingerService(OpenIDConnectProviderDiscovery providerDiscovery,
            BaseURLProviderFactory baseURLProviderFactory) {
        this.providerDiscovery = providerDiscovery;
        this.baseURLProviderFactory = baseURLProviderFactory;
    }

    /**
     * Answers the JRD that names the issuer for the {@code resource} and {@code rel} query parameters. A repeated
     * parameter counts with its first value.
     *
     * @param context The request context, holding the realm and the servlet request.
     * @param request The request.
     * @return 200 with the JRD, or the OAuth2 error of the discovery.
     */
    @Get
    public Response discover(@Contextual Context context, @Contextual Request request) {
        Form query = new Form().fromRequestQuery(request);
        String realm = context.asContext(RealmContext.class).getRealm().asPath();
        HttpServletRequest servletRequest = (HttpServletRequest) context.asContext(AttributesContext.class)
                .getAttributes().get(HttpServletRequest.class.getName());
        try {
            String deploymentUrl = baseURLProviderFactory.get(realm).getRootURL(servletRequest);
            return new Response(Status.OK).setEntity(providerDiscovery.discover(
                    query.getFirst("resource"), query.getFirst("rel"), deploymentUrl, null));
        } catch (OAuth2Exception e) {
            return error(e);
        }
    }

    private static Response error(OAuth2Exception e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getError());
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            body.put("error_description", e.getMessage());
        }
        Response response = new Response(Status.valueOf(e.getStatusCode())).setEntity(body);
        // After setEntity, which forces "application/json; charset=UTF-8".
        response.getHeaders().put(ContentTypeHeader.NAME, ERROR_CONTENT_TYPE);
        return response;
    }
}
