/**
 * Copyright 2005-2024 Qlik
 *
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: Apache 2.0 or or EPL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 *
 * You can obtain a copy of the Apache 2.0 license at
 * http://www.opensource.org/licenses/apache-2.0
 *
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0
 *
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 *
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * https://restlet.talend.com/
 *
 * Restlet is a registered trademark of QlikTech International AB.
 */

package org.forgerock.openam.rest.jakarta.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.engine.adapter.Call;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.adapter.HttpResponse;
import org.forgerock.openam.rest.jakarta.servlet.internal.ServletCall;

/**
 * Servlet related utilities.
 *
 * @author Jerome Louvel
 */
public final class ServletUtils {

    /**
     * Returns the Servlet request that was used to generate the given Restlet
     * request.
     *
     * @param request
     *            The Restlet request.
     * @return The Servlet request or null.
     */
    public static HttpServletRequest getRequest(Request request) {
        HttpServletRequest result = null;

        if (request instanceof HttpRequest) {
            Call call = ((HttpRequest) request).getHttpCall();

            if (call instanceof ServletCall) {
                result = ((ServletCall) call).getRequest();
            }
        }

        return result;
    }

    /**
     * Returns the Servlet response that was used to generate the given Restlet
     * response.
     *
     * @param response
     *            The Restlet response.
     * @return The Servlet request or null.
     */
    public static HttpServletResponse getResponse(Response response) {
        HttpServletResponse result = null;

        if (response instanceof HttpResponse) {
            Call call = ((HttpResponse) response).getHttpCall();

            if (call instanceof ServletCall) {
                result = ((ServletCall) call).getResponse();
            }
        }

        return result;
    }

}
