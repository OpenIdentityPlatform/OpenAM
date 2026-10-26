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

package org.forgerock.openam.rest.jakarta.servlet.internal;

import java.util.Enumeration;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.adapter.ServerAdapter;
import org.restlet.engine.adapter.ServerCall;

/**
 * Default server adapter from Servlet calls to Restlet calls. This class is
 * used by the {@code ServerServlet} to ensure that Servlet specific concepts
 * are properly transfered to Restlet.<br>
 * Especially, it makes sure that the "jsessionid" matrix parameter is removed
 * from the resource's reference, as this may interfer with the routing process,
 * and because this parameter is useless for Restlet-based applications.<br>
 * <br>
 * it also copies the Servlet's request attributes into the Restlet's request
 * attributes map.
 *
 * @author Jeremy Gustie
 */
public class ServletServerAdapter extends ServerAdapter {

    /**
     * Constructor.
     *
     * @param context
     *            The context to use.
     */
    public ServletServerAdapter(Context context) {
        super(context);
    }

    @Override
    public HttpRequest toRequest(ServerCall httpCall) {
        final HttpRequest result = super.toRequest(httpCall);

        // Remove the Servlet API "jsessionid" matrix parameter
        Form matrixForm = result.getResourceRef().getMatrixAsForm();
        if (matrixForm.removeAll("jsessionid", true)) {
            String lastSegment = result.getResourceRef().getLastSegment();
            final int matrixIndex = lastSegment.indexOf(';');
            if (matrixForm.isEmpty()) {
                // No matrix left
                lastSegment = lastSegment.substring(0, matrixIndex);
            } else {
                // Add the remaining matrix parameters back in
                lastSegment = lastSegment.substring(0, matrixIndex + 1)
                        + matrixForm.getMatrixString();
            }
            result.getResourceRef().setLastSegment(lastSegment);
        }

        if (httpCall instanceof ServletCall) {
            ServletCall servletCall = (ServletCall) httpCall;

            // Copy all Servlet's request attributes
            String attributeName;
            for (final Enumeration<String> namesEnum = servletCall.getRequest()
                    .getAttributeNames(); namesEnum.hasMoreElements();) {
                attributeName = namesEnum.nextElement();
                Object attribute = servletCall.getRequest().getAttribute(attributeName);
                if (attribute != null) {
                    result.getAttributes().put(attributeName, attribute);
                }
            }
        }

        return result;
    }

}
