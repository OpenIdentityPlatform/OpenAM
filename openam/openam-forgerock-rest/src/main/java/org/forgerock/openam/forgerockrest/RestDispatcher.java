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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.openam.forgerockrest;

import static org.forgerock.json.resource.Context.newRootContext;

import java.lang.String;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import java.lang.reflect.Method;


import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Connections;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.exception.ResourceException;
import org.forgerock.json.resource.provider.RequestHandler;
import org.forgerock.json.resource.provider.Router;
import org.forgerock.json.resource.provider.UriTemplateRoutingStrategy;
import org.forgerock.json.resource.provider.SingletonResourceProvider;



/**
 * A simple {@code Map} based collection resource provider.
 */
public final class RestDispatcher  {

    private static RestDispatcher instance = null;
    private RequestHandler handler = null;
    private ConnectionFactory factory = null;
    private RestDispatcher() {

    }

    public final static RestDispatcher  getInstance() {
        if (instance == null)  instance = new  RestDispatcher();
        return instance;
    }

    private void callConfigClass(String className,UriTemplateRoutingStrategy routes) {
            // Check for configured connection factory class first.
        if (className != null) {
            final ClassLoader cl = this.getClass().getClassLoader();
            try {
                final Class<?> cls = Class.forName(className, true, cl);
                // Try method which accepts ServletConfig.
                final Method factoryMethod = cls.getMethod("initDispatcher", UriTemplateRoutingStrategy.class);

                factoryMethod.invoke(null, routes);
                return ;
            } catch (final Exception e) {
            }
        }

    };
    /**
     * Build the initial dispatcher.
     * This is a separate method so that we can modify the dispatching
     * dynamically
     * */

    public ConnectionFactory buildConnectionFactory(ServletConfig config) throws ResourceException {
        final UriTemplateRoutingStrategy routes = new UriTemplateRoutingStrategy();

        String roots = config.getInitParameter("rootContexts");
        routes.register("/test",new TestResource());                // Just a simply READ to make sure dispatching works
        if (roots != null)  {
            String[] initClasses = config.getInitParameter("rootContexts").split(","); // not really much to do

            for (String ctx : initClasses)  {
                callConfigClass(ctx.trim(),routes);
            }
        }
        handler = new Router(routes);
        factory = Connections.newInternalConnectionFactory(handler);
        return factory;
    }

    public static ConnectionFactory getConnectionFactory(ServletConfig config) throws ServletException {
        try {
            return getInstance().buildConnectionFactory(config);
        } catch (final Exception e) {
            throw new ServletException(e);
        }
    }

    private static void initSampleResources(ConnectionFactory factory) throws ResourceException {

// Populate with some test users and groups.
        final Connection connection = factory.getConnection();

        final JsonValue user1 = new JsonValue(new LinkedHashMap<String, Object>());
        user1.add("userName", "alice");
        user1.add("employeeNumber", 1234);
        user1.add("email", "alice@example.com");

        final JsonValue user2 = new JsonValue(new LinkedHashMap<String, Object>());
        user2.add("userName", "bob");
        user2.add("employeeNumber", 2468);
        user2.add("email", "bob@example.com");

        for (final JsonValue user : Arrays.asList(user1, user2)) {
            final CreateRequest request = Requests.newCreateRequest("/users", user);
            connection.create(newRootContext(), request);
        }

        final JsonValue group1 = new JsonValue(new LinkedHashMap<String, Object>());
        group1.add("groupName", "users");
        group1.add("members", Arrays.asList("alice", "bob"));

        final JsonValue group2 = new JsonValue(new LinkedHashMap<String, Object>());
        group2.add("groupName", "administrators");
        group2.add("members", Arrays.asList("alice"));

        for (final JsonValue user : Arrays.asList(group1, group2)) {
            final CreateRequest request = Requests.newCreateRequest("/groups", user);
            connection.create(newRootContext(), request);
        }
        connection.close();
    }
}