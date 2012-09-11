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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import javax.servlet.ServletException;


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



/**
 * A simple {@code Map} based collection resource provider.
 */
public final class IdentityDispatcher  {

    private static IdentityDispatcher instance = null;
    private RequestHandler handler = null;
    private ConnectionFactory factory = null;
    private IdentityDispatcher() {

    }

    public final static IdentityDispatcher  getInstance() {
        if (instance == null)  instance = new  IdentityDispatcher();
        return instance;
    }

    /**
     * Build the initial dispatcher.
     * This is a separate method so that we can modify the dispatching
     * dynamically
     * */

    public ConnectionFactory buildConnectionFactory() throws ResourceException {
        final UriTemplateRoutingStrategy routes = new UriTemplateRoutingStrategy();
        routes.register("/users", new IdentityResource());
        routes.register("/groups", new IdentityResource());
        handler = new Router(routes);
        factory = Connections.newInternalConnectionFactory(handler);
        initSampleResources(factory);
        return factory;
    }

    public static ConnectionFactory getConnectionFactory() throws ServletException {
        try {
            return getInstance().buildConnectionFactory();
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