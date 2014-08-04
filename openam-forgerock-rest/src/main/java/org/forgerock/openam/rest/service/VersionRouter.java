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
import org.forgerock.json.resource.VersionSelector;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A router for Restlet service endpoints which routes based on the requested version of a service.
 *
 * @since 12.0.0
 */
public class VersionRouter {

    static final String HEADER_X_VERSION_API = "Accept-API-Version";

    private final VersionSelector versionSelector;
    private final Map<Version, Restlet> routes = new ConcurrentHashMap<Version, Restlet>();

    /**
     * Constructs a new VersionRouter instance.
     *
     * @param versionSelector An instance of the VersionSelector.
     */
    VersionRouter(VersionSelector versionSelector) {
        this.versionSelector = versionSelector;
    }

    /**
     * Adds a new route to a service endpoint with a specific version.
     *
     * @param version The version of the endpoint.
     * @param resource The endpoint.
     * @return This router.
     */
    public VersionRouter addVersion(String version, Restlet resource) {
        routes.put(Version.valueOf(version), resource);
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
        versionSelector.defaultToOldest();
    }

    /**
     * Handles the selection of the service endpoint based on the requested version of the endpoint.
     *
     * @param request The Restlet request.
     * @param response The Restlet response.
     */
    void handle(Request request, Response response) {

        try {
            AcceptAPIVersion apiVersion = parseAcceptAPIVersion(request);
            versionSelector.select(apiVersion.getResourceVersion(), routes).handle(request, response);
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
        String versionHeader = httpRequest.getHeader(HEADER_X_VERSION_API);
        AcceptAPIVersion apiVersion = AcceptAPIVersion.newBuilder(versionHeader)
                .withDefaultProtocolVersion("1.0")
                .expectsProtocolVersion()
                .build();

        Version protocolVersion = apiVersion.getProtocolVersion();
        Version enforcedProtocolVersion = Version.valueOf("1.0");

        if (protocolVersion.getMajor() != enforcedProtocolVersion.getMajor()) {
            throw new BadRequestException("Unsupported major version: " + protocolVersion);
        }

        if (protocolVersion.getMinor() > enforcedProtocolVersion.getMinor()) {
            throw new BadRequestException("Unsupported minor version: " + protocolVersion);
        }

        return apiVersion;
    }

    HttpServletRequest getHttpRequest(Request request) {
        return ServletUtils.getRequest(request);
    }
}
