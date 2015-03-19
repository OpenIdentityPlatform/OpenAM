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

package org.forgerock.openam.rest.sms;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

public class SmsRequestHandler implements RequestHandler {

    private static final List<String> EXCLUDED_SERVICES = Arrays.asList();
    private final SmsCollectionProviderFactory collectionProviderFactory;
    private final SchemaType schemaType;
    private final Debug debug;

    private Router router = new Router();

    @Inject
    public SmsRequestHandler(@Assisted SchemaType type, SmsCollectionProviderFactory collectionProviderFactory,
            @Named("frRest") Debug debug)
            throws SMSException, SSOException {
        this.schemaType = type;
        this.collectionProviderFactory = collectionProviderFactory;
        this.debug = debug;

        addServices(type);
    }

    private void addServices(SchemaType type) throws SSOException, SMSException {
        SSOToken adminSSOToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        ServiceManager sm = new ServiceManager(adminSSOToken);
        Set<String> serviceNames = new HashSet<String>(Arrays.asList("iPlanetAMPlatformService")); // sm.getServiceNames();
        for (String serviceName : serviceNames) {
            if (EXCLUDED_SERVICES.contains(serviceName)) {
                debug.message("Excluding service from REST SMS: {}", serviceName);
                continue;
            }
            ServiceSchemaManager ssm = sm.getSchemaManager(serviceName, "1.0");
            if (type == SchemaType.GLOBAL) {
                debug.message("Adding global schema REST SMS endpoints for service: {}", serviceName);
                addPaths(serviceName, new ArrayList<ServiceSchema>(), ssm.getGlobalSchema());
            }
        }
    }

    private void addPaths(String parentPath, List<ServiceSchema> schemaPath, ServiceSchema schema) throws SMSException {
        String schemaName = schema.getResourceName() == null ? schema.getName() : schema.getResourceName();
        // Top-level schemas don't have a name and we don't want them in our schema path
        if (schemaName != null) {
            schemaPath.add(schema);
        }
        if (!schema.getAttributeSchemas().isEmpty()) {
            if (schema.supportsMultipleConfigurations()) {
                RequestHandler collectionHandler = Resources.newCollection(collectionProviderFactory.create(
                        new SmsJsonConverter(schema), schema, schemaType, new ArrayList<ServiceSchema>(schemaPath),
                        parentPath, true));
                String path = parentPath + "/" + schemaName;
                debug.message("Adding path {}", path);
                router.addRoute(RoutingMode.EQUALS, path, collectionHandler);
                parentPath = path + "/{" + schemaName + "}";
            } else {
                // TODO: Singleton
            }
        }
        for (String subSchema : (Set<String>) schema.getSubSchemaNames()) {
            addPaths(parentPath, new ArrayList<ServiceSchema>(schemaPath), schema.getSubSchema(subSchema));
        }
    }

    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        router.handleAction(context, request, handler);
    }

    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        router.handleCreate(context, request, handler);
    }

    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        router.handleDelete(context, request, handler);
    }

    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        router.handlePatch(context, request, handler);
    }

    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        router.handleQuery(context, request, handler);
    }

    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        router.handleRead(context, request, handler);
    }

    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        router.handleUpdate(context, request, handler);
    }
}
