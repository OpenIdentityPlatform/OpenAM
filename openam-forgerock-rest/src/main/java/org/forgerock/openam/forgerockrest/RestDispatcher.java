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
 * Copyright 2012 ForgeRock Inc.
 */
package org.forgerock.openam.forgerockrest;

import com.sun.identity.shared.debug.Debug;

import java.lang.String;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.lang.reflect.Method;

import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServiceUnavailableException;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class RestDispatcher {

    public static Debug debug = Debug.getInstance("frRest");

    private static RestDispatcher instance = null;
    private RequestHandler handler = null;
    private ConnectionFactory factory = null;

    private RestDispatcher() {

    }

    public final static RestDispatcher getInstance() {
        if (instance == null) instance = new RestDispatcher();
        return instance;
    }

    private void callConfigClass(String className, Router router) {
        // Check for configured connection factory class first.
        if (className != null) {
            final ClassLoader cl = this.getClass().getClassLoader();
            try {
                final Class<?> cls = Class.forName(className, true, cl);
                // Try method which accepts ServletConfig.
                final Method factoryMethod = cls.getMethod("initDispatcher", Router.class);

                factoryMethod.invoke(null, router);
                return;
            } catch (final Exception e) {
            }
        }

    }

    /**
     * Build the initial dispatcher.
     * This is a separate method so that we can modify the dispatching
     * dynamically
     */

    public ConnectionFactory buildConnectionFactory(ServletConfig config) throws ResourceException {
        final Router router = new Router();
        String roots = config.getInitParameter("rootContexts");

        if(roots == null){
            throw new ServiceUnavailableException();
        }
        if (roots != null) {
            String[] initClasses = config.getInitParameter("rootContexts").split(","); // not really much to do

            for (String ctx : initClasses) {
                callConfigClass(ctx.trim(), router);
            }
        }
        factory = Resources.newInternalConnectionFactory(router);
        return factory;
    }

    public static ConnectionFactory getConnectionFactory(ServletConfig config) throws ServletException {
        try {
            return getInstance().buildConnectionFactory(config);
        } catch (final Exception e) {
            throw new ServletException(e);
        }
    }
}