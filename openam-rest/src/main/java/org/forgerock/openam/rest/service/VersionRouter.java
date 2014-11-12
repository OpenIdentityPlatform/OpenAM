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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.AcceptAPIVersion;
import org.forgerock.json.resource.AdviceWarning;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Version;
import org.forgerock.json.resource.VersionConstants;
import org.forgerock.json.resource.VersionRoute;
import org.forgerock.json.resource.VersionSelector;
import org.forgerock.util.annotations.VisibleForTesting;
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

    private static final Debug logger = Debug.getInstance("amAuthREST");

    private static final String EQUALS = "=";
    private static final String COMMA = ",";

    // agent name for warning headers
    private final String AGENT_NAME = "OpenAM REST";

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

        AdviceWarning warningHeader = null;
        try {
            HttpServletRequest httpRequest = getHttpRequest(request);
            String versionHeader = httpRequest.getHeader(VersionConstants.ACCEPT_API_VERSION);
            final AcceptAPIVersion apiVersion = parseAcceptAPIVersion(versionHeader);

            final VersionRoute<Restlet> versionRoute = versionSelector.select(apiVersion.getResourceVersion(),
                                                                                                            routes);
            final Restlet selectedResource = versionRoute.getRequestHandler();
            final Version selectedVersion = versionRoute.getVersion();

            selectedResource.handle(request, response);
            addContentAPIVersion(response, enforcedProtocolVersion, selectedVersion);

            // If warnings are wanted, and we haven't thrown an exception by this point let's see what we can do...
            if (headerWarning) {
                if (versionHeader == null) {
                    // If no version specified at all, we can warn about that
                    warningHeader = AdviceWarning.newAdviceWarning(AGENT_NAME,
                            "No " + VersionConstants.ACCEPT_API_VERSION + " specified");

                } else if (!selectedVersion.equals(apiVersion.getResourceVersion())) {
                    // alternatively, if the user requested one version and we gave them a slightly different one
                    // then warn them about that
                    Version headerResourceVersion = apiVersion.getResourceVersion();
                    warningHeader = AdviceWarning.newAdviceWarning(AGENT_NAME,
                            VersionConstants.ACCEPT_API_VERSION
                                    + ": Requested version '%s', resolved version '%s'",
                            headerResourceVersion == null ? "null" : headerResourceVersion.toString(),
                            selectedVersion.toString());
                }
            }

        } catch (InternalServerErrorException e) {
            logger.error("Internal configuration error: no routes available", e);
            response.setStatus(new Status(e.getCode()), "Could not route request because of an internal config error");
            warningHeader = AdviceWarning.newAdviceWarning(AGENT_NAME, e.getMessage());

        } catch (BadRequestException e) {
            // The user didn't specify an accept API version and the default behaviour is set to NONE.
            String text = "No " + VersionConstants.ACCEPT_API_VERSION + " found and behaviour set to NONE";
            logger.error(text, e);
            response.setStatus(new Status(e.getCode()), text);
            warningHeader = AdviceWarning.newAdviceWarning(AGENT_NAME, e.getMessage());

        } catch (NotFoundException e) {
            // Either the version or protocol specified was not valid.  The error
            // message will tell us which.
            logger.error(VersionConstants.ACCEPT_API_VERSION + " error: " + e.getMessage(), e);
            response.setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE,
                    VersionConstants.ACCEPT_API_VERSION + ": " + e.getMessage());
            warningHeader = AdviceWarning.newAdviceWarning(AGENT_NAME, e.getMessage());

        } catch (ResourceException e) {
            // This is to keep the compiler happy.  ResourceException.getException is declared to throw
            // ResourceException, although it should only throw one of the exceptions above.
            logger.error("Internal configuration error: unexpected exception while attempting to route request", e);
            response.setStatus(Status.SERVER_ERROR_INTERNAL,
                    "Unexpected exception while attempting to route request");
            warningHeader = AdviceWarning.newAdviceWarning(AGENT_NAME, e.getMessage());

        } finally {
            if (headerWarning && warningHeader != null) {
                getHttpResponse(response).addHeader("Warning", warningHeader.toString());
            }
        }
    }

    /**
     * Parses the version header from the request and checks immediately if the protocol is valid.
     *
     * @param versionHeader the value of Accept-API-Version from the headers.
     * @return An {@link AcceptAPIVersion} object containing the requested resource version.
     * @throws {@link NotFoundException} if the specified protocol is invalid.
     */
    private AcceptAPIVersion parseAcceptAPIVersion(String versionHeader) throws ResourceException {

        AcceptAPIVersion apiVersion = AcceptAPIVersion.newBuilder(versionHeader)
                .withDefaultProtocolVersion("1.0")
                .expectsProtocolVersion()
                .build();

        Version protocolVersion = apiVersion.getProtocolVersion();

        if (protocolVersion.getMajor() != enforcedProtocolVersion.getMajor()) {
            throw new NotFoundException("Unsupported major version: " + protocolVersion);
        }

        if (protocolVersion.getMinor() > enforcedProtocolVersion.getMinor()) {
            throw new NotFoundException("Unsupported minor version: " + protocolVersion);
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
        getHttpResponse(response).addHeader(VersionConstants.CONTENT_API_VERSION, new StringBuilder()
                .append(VersionConstants.PROTOCOL)
                .append(EQUALS)
                .append(protocolVersion.toString())
                .append(COMMA)
                .append(VersionConstants.RESOURCE)
                .append(EQUALS)
                .append(resourceVersion.toString())
                .toString());
    }

    @VisibleForTesting
    HttpServletRequest getHttpRequest(Request request) {
        return ServletUtils.getRequest(request);
    }

    @VisibleForTesting
    HttpServletResponse getHttpResponse(Response response) {
        return ServletUtils.getResponse(response);
    }
}
