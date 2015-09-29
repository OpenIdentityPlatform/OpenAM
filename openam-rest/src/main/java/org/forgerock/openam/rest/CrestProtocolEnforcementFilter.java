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

package org.forgerock.openam.rest;

import static org.forgerock.http.routing.Version.version;
import static org.forgerock.util.promise.Promises.newResultPromise;

import org.forgerock.services.context.Context;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.Version;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

/**
 * Enforces that the {@literal Accept-API-Version} header protocol version is
 * set to {@literal 1.0}. Meaning that if it is not set it will be defaulted to
 * {@literal 1.0} and if it is set to any other value a JSON Bad Request will
 * be returned to the client.
 *
 * @since 13.0.0
 */
public class CrestProtocolEnforcementFilter implements Filter {

    private static final Version ENFORCE_PROTOCOL_VERSION = version(1);

    /**
     * If the request does not contain a protocol version in the
     * {@literal Accept-API-Version} header it will be defaulted to
     * {@literal 1.0}, otherwise if the protocol version is not {@literal 1.0}
     * a JSON Bad Request will be returned to the client.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param next {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
        try {
            enforceProtocolVersion(defaultProtocolVersion(request));
            return next.handle(context, request);
        } catch (BadRequestException e) {
            Response response = new Response()
                    .setStatus(Status.valueOf(e.getCode()))
                    .setEntity(e.toJsonValue().getObject());
            return newResultPromise(response);
        }
    }

    private Version defaultProtocolVersion(Request request) throws BadRequestException {
        AcceptApiVersionHeader apiVersionHeader;
        try {
            apiVersionHeader = AcceptApiVersionHeader.valueOf(request);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
        apiVersionHeader.withDefaultProtocolVersion(ENFORCE_PROTOCOL_VERSION);
        request.getHeaders().put(apiVersionHeader);
        return apiVersionHeader.getProtocolVersion();
    }

    private void enforceProtocolVersion(Version protocolVersion) throws BadRequestException {
        if (protocolVersion != null && protocolVersion.getMajor() != ENFORCE_PROTOCOL_VERSION.getMajor()) {
            throw new BadRequestException("Unsupported major version: " + protocolVersion);
        } else if (protocolVersion != null && protocolVersion.getMinor() > ENFORCE_PROTOCOL_VERSION.getMinor()) {
            throw new BadRequestException("Unsupported minor version: " + protocolVersion);
        }
    }
}
