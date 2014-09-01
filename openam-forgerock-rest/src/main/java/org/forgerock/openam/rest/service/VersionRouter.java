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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.service;

import org.forgerock.json.resource.AcceptAPIVersion;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Version;
import org.forgerock.json.resource.VersionRoute;
import org.forgerock.json.resource.VersionSelector;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.forgerock.json.resource.VersionConstants.*;

/**
 * A router for Restlet service endpoints which routes based on the requested version of a service.
 *
 * @since 12.0.0
 */
public class VersionRouter {

    private static final String EQUALS = "=";
    private static final String COMMA = ",";

    private final VersionSelector versionSelector;
    private final Map<Version, VersionRoute> routes = new ConcurrentHashMap<Version, VersionRoute>();
    private final Version enforcedProtocolVersion = Version.valueOf("1.0");
    private boolean headerWarning = true; //will be used to determine whether to include the warning

    /**
     * Constructs a new VersionRouter instance.
     *
     * @param versionSelector An instance of the VersionSelector.
     */
    VersionRouter(VersionSelector versionSelector) {
        this.versionSelector = versionSelector;
    }

    public void setHeaderWarning(boolean headerWarning) {
        this.headerWarning = headerWarning;
    }

    /**
     * Adds a new route to a service endpoint with a specific version.
     *
     * @param version The version of the endpoint.
     * @param resource The endpoint.
     * @return This router.
     */
    public VersionRouter addVersion(String version, Restlet resource) {
        final Version value = Version.valueOf(version);
        routes.put(value, new VersionRoute(value, resource));
        return this;
    }

    /**
     * Sets the behaviour of the selection process to always use the latest resource version when the requested version
     * is {@code null}.
     *
     * @see VersionSelector#defaultToLatest()
     */
    public void defaultToLatest() {
        versionSelector.defaultToLatest();
    }

    /**
     * Sets the behaviour of the selection process to always use the oldest resource version when the requested version
     * is {@code null}.
     *
     * @see VersionSelector#defaultToOldest()
     */
    public void defaultToOldest() {
        versionSelector.defaultToOldest();
    }

    /**
     * Removes the default behaviour of the selection process which will result in {@code NotFoundException}s when
     * the requested version is {@code null}.
     *
     * @see VersionSelector#noDefault()
     */
    public void noDefault() {
        versionSelector.noDefault();
    }

    /**
     * Handles the selection of the service endpoint based on the requested version of the endpoint.
     *
     * @param request The Restlet request.
     * @param response The Restlet response.
     */
    void handle(Request request, Response response) {

        try {
            final AcceptAPIVersion apiVersion = parseAcceptAPIVersion(request);
            final VersionRoute<Restlet> selectedRoute = versionSelector.select(apiVersion.getResourceVersion(), routes);
            selectedRoute.getRequestHandler().handle(request, response);
            addContentAPIVersion(response, enforcedProtocolVersion, selectedRoute.getVersion());
        } catch (NotFoundException e) {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, String.format("Version of resource '%s' not found",
                    request.getResourceRef().getLastSegment()));
        } catch (BadRequestException e) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Parses the version header from the request.
     *
     * @param request The Restlet request.
     * @return An {@link AcceptAPIVersion} object containing the requested resource version.
     */
    private AcceptAPIVersion parseAcceptAPIVersion(Request request) throws BadRequestException {
        HttpServletRequest httpRequest = getHttpRequest(request);
        String versionHeader = httpRequest.getHeader(ACCEPT_API_VERSION);
        AcceptAPIVersion apiVersion = AcceptAPIVersion.newBuilder(versionHeader)
                .withDefaultProtocolVersion("1.0")
                .expectsProtocolVersion()
                .build();

        Version protocolVersion = apiVersion.getProtocolVersion();

        if (protocolVersion.getMajor() != enforcedProtocolVersion.getMajor()) {
            throw new BadRequestException("Unsupported major version: " + protocolVersion);
        }

        if (protocolVersion.getMinor() > enforcedProtocolVersion.getMinor()) {
            throw new BadRequestException("Unsupported minor version: " + protocolVersion);
        }

        return apiVersion;
    }

    /**
     * Add the Content API Version header to the given Response.
     * @param response The response to add the header to.
     * @param protocolVersion The selected protocol version.
     * @param resourceVersion The selected resource version.
     */
    private void addContentAPIVersion(Response response, Version protocolVersion, Version resourceVersion) {
        getHttpResponse(response).addHeader(CONTENT_API_VERSION, new StringBuilder()
                .append(PROTOCOL)
                .append(EQUALS)
                .append(protocolVersion.toString())
                .append(COMMA)
                .append(RESOURCE)
                .append(EQUALS)
                .append(resourceVersion.toString())
                .toString());
    }

    HttpServletRequest getHttpRequest(Request request) {
        return ServletUtils.getRequest(request);
    }

    HttpServletResponse getHttpResponse(Response response) {
        return ServletUtils.getResponse(response);
    }
}
