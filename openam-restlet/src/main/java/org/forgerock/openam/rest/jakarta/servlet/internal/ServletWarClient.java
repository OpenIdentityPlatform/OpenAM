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

import java.util.List;

import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.engine.connector.ClientHelper;

/**
 * Connector acting as a WAR client for a Servlet Application. It internally
 * uses one of the available connectors registered with the current Restlet
 * implementation.<br>
 * <br>
 * Here is an example of WAR URI that can be resolved by this client:
 * "war:///WEB-INF/web.xml"
 *
 * Concurrency note: instances of this class or its subclasses can be invoked by
 * several threads at the same time and therefore must be thread-safe. You
 * should be especially careful when storing state in member variables.
 *
 * @author Jerome Louvel
 */
public class ServletWarClient extends Client {
    /** The helper provided by the implementation. */
    private volatile ClientHelper helper;

    /**
     * Constructor.
     *
     * @param parentContext
     *            The parent context.
     */
    public ServletWarClient(Context parentContext,
                            jakarta.servlet.ServletContext servletContext) {
        super(parentContext.createChildContext(), (List<Protocol>) null);
        getProtocols().add(Protocol.WAR);
        this.helper = new ServletWarClientHelper(this, servletContext);
    }

    /**
     * Returns the helper provided by the implementation.
     *
     * @return The helper provided by the implementation.
     */
    private ClientHelper getHelper() {
        return this.helper;
    }

    /**
     * Handles a call.
     *
     * @param request
     *            The request to handle.
     * @param response
     *            The response to update.
     */
    @Override
    public void handle(Request request, Response response) {
        super.handle(request, response);
        getHelper().handle(request, response);
    }

    /** Start callback. */
    @Override
    public void start() throws Exception {
        super.start();
        getHelper().start();
    }

    /** Stop callback. */
    @Override
    public void stop() throws Exception {
        getHelper().stop();
        super.stop();
    }

}
