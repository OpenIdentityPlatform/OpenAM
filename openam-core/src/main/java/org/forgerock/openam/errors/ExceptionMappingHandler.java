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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.errors;

import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idsvcs.IdServicesException;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.Request;

/**
 * Translates errors from underlying processes into appropriate errors for the given task.
 *
 * @since 12.0.0
 */
public interface ExceptionMappingHandler<E extends Exception, R extends Exception> {

    /**
     * Converts an exception into an appropriate resource exception, with debug logging.
     *
     * @param context the server context from which to read the preferred language.
     * @param debug a debug message to store.
     * @param request the request that failed with an error.
     * @param error the error that occurred.
     * @return an appropriate exception.
     */
    R handleError(Context context, String debug, Request request, E error);

    /**
     * Converts an exception into an appropriate resource exception, with debug logging.
     *
     * @param debug a debug message to store.
     * @param request the request that failed with an error.
     * @param error the error that occurred.
     * @return an appropriate exception.
     */
    R handleError(String debug, Request request, E error);

    /**
     * Converts an exception into an appropriate resource exception.
     *
     * @param context the server context from which to read the preferred language.
     * @param request the request that failed with an error.
     * @param error the error that occurred.
     * @return an appropriate exception.
     */
    R handleError(Context context, Request request, E error);

    /**
     * Converts an exception into an appropriate resource exception.
     *
     * @param request the request that failed with an error.
     * @param error the error that occurred.
     * @return an appropriate exception.
     */
    R handleError(Request request, E error);

    /**
     * Converts an exception into an appropriate resource exception.
     *
     * @param error the error that occurred.
     * @return an appropriate exception.
     */
    R handleError(E error);

}
