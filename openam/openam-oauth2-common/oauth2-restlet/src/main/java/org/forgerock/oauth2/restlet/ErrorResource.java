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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import java.util.Map;

/**
 * Restlet resource for an OAuth2 error.
 *
 * @since 11.0.0
 */
public class ErrorResource extends ServerResource {

    private final ExceptionHandler exceptionHandler;
    private final OAuth2Exception e;

    /**
     * Constructs a new ErrorResource.
     *
     * @param exceptionHandler An instance of the ExceptionHandler.
     * @param e The exception to display.
     */
    public ErrorResource(ExceptionHandler exceptionHandler, OAuth2Exception e) {
        this.exceptionHandler = exceptionHandler;
        this.e = e;
    }

    /**
     * Calls {@link #doCatch(Throwable)} if the exception is not {@code null}. Otherwise sets the status of the
     * response to a internal server error.
     *
     * @return {@inheritDoc}
     * @throws ResourceException {@inheritDoc}
     */
    protected Representation doHandle() {
        Representation result = null;
        if (e == null) {
            getResponse().setStatus(new Status(Status.SERVER_ERROR_INTERNAL, "Unknown exception"));
        } else {
            doCatch(new OAuth2RestletException(e.getStatusCode(), e.getError(), e.getMessage(), null));
        }
        return result;
    }

    /**
     * Calls {@link #doHandle()}.
     *
     * @return {@inheritDoc}
     * @throws ResourceException {@inheritDoc}
     */
    protected Representation doConditionalHandle() {
        return doHandle();
    }

    /**
     * Converts the throwable into a Json representation and set it as the body of the response.
     *
     * @param throwable {@inheritDoc}
     */
    @Override
    protected void doCatch(Throwable throwable) {
        exceptionHandler.handle(throwable, getResponse());
    }
}
