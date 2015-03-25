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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.forgerock.json.resource.Route;
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
import com.sun.identity.sm.SMSNotificationManager;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * A CREST routing request handler that creates collection and singleton resource providers for
 * the SMS configuration services. Uses the {@link ServiceManager} to get a list of all registered
 * services and examines the service schema structure to work out URI patterns.
 * <p>
 * Instances of this class also register themselves as listeners with the
 * {@link SMSNotificationManager} and when schemas are added/modified/removed, the URI structure
 * and resource providers are updated accordingly.
 */
public class SmsRequestHandler implements RequestHandler, SMSObjectListener {

    private static final List<String> EXCLUDED_SERVICES = Arrays.asList();
    private static final String DEFAULT_VERSION = "1.0";
    private final SmsCollectionProviderFactory collectionProviderFactory;
    private final SchemaType schemaType;
    private final Debug debug;
    private final Pattern schemaDnPattern;
    private Map<String, Set<Route>> serviceRoutes;
    private Router router;

    @Inject
    public SmsRequestHandler(@Assisted SchemaType type, SmsCollectionProviderFactory collectionProviderFactory,
            @Named("frRest") Debug debug)
            throws SMSException, SSOException {
        this.schemaType = type;
        this.collectionProviderFactory = collectionProviderFactory;
        this.debug = debug;
        this.schemaDnPattern = Pattern.compile("^ou=([.0-9]+),ou=([^,]+)," +
                Pattern.quote(ServiceManager.getServiceDN()) + "$");

        createServices();
        SMSNotificationManager.getInstance().registerCallbackHandler(this);
    }

    @Override
    public void objectChanged(String dn, int type) {
        Matcher matcher = schemaDnPattern.matcher(dn);
        if (!matcher.matches()) {
            return;
        }

        String svcName = matcher.group(2);
        String svcVersion = matcher.group(1);
        try {
            switch (type) {
                case SMSObjectListener.DELETE:
                    if (serviceRoutes.containsKey(svcName)) {
                        removeService(svcName);
                    }
                    break;
                case SMSObjectListener.ADD:
                    if (!serviceRoutes.containsKey(svcName)) {
                        serviceRoutes.put(svcName, addService(getServiceManager(), svcName, svcVersion, this.router));
                    }
                    break;
                case SMSObjectListener.MODIFY:
                    removeService(svcName);
                    serviceRoutes.put(svcName, addService(getServiceManager(), svcName, svcVersion, this.router));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown modification type: " + type);
            }
        } catch (SSOException e) {
            debug.error("Could not update SMS REST services for change to " + svcName, e);
        } catch (SMSException e) {
            debug.error("Could not update SMS REST services for change to " + svcName, e);
        }
    }

    private void removeService(String name) {
        for (Route route : serviceRoutes.get(name)) {
            router.removeRoute(route);
        }
        serviceRoutes.remove(name);
    }

    @Override
    public void allObjectsChanged() {
        try {
            createServices();
        } catch (SSOException e) {
            debug.error("Could not recreate SMS REST services", e);
        } catch (SMSException e) {
            debug.error("Could not recreate SMS REST services", e);
        }
    }

    private void createServices() throws SSOException, SMSException {
        Router router = new Router();
        Map<String, Set<Route>> serviceRoutes = new HashMap<String, Set<Route>>();
        ServiceManager sm = getServiceManager();
        Set<String> serviceNames = sm.getServiceNames();
        for (String serviceName : serviceNames) {
            Set<Route> routes = addService(sm, serviceName, DEFAULT_VERSION, router);
            if (routes != null) {
                serviceRoutes.put(serviceName, routes);
            }
        }
        this.router = router;
        this.serviceRoutes = serviceRoutes;
    }

    private ServiceManager getServiceManager() throws SSOException, SMSException {
        SSOToken adminSSOToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        return new ServiceManager(adminSSOToken);
    }

    private Set<Route> addService(ServiceManager sm, String serviceName, String serviceVersion, Router router)
            throws SMSException, SSOException {
        if (EXCLUDED_SERVICES.contains(serviceName)) {
            debug.message("Excluding service from REST SMS: {}", serviceName);
            return null;
        }

        ServiceSchemaManager ssm = sm.getSchemaManager(serviceName, serviceVersion);
        String resourceName = ssm.getResourceName();
        Set<Route> routes = new HashSet<Route>();

        if (schemaType == SchemaType.GLOBAL) {
            ServiceSchema globalSchema = ssm.getGlobalSchema();
            if (globalSchema != null) {
                debug.message("Adding global schema REST SMS endpoints for service: {}", serviceName);
                addPaths(resourceName, new ArrayList<ServiceSchema>(), globalSchema, router, routes);
            }
        }
        ServiceSchema organizationSchema = ssm.getOrganizationSchema();
        if (organizationSchema != null) {
            debug.message("Adding realm schema REST SMS endpoints for service: {}", serviceName);
            addPaths(resourceName, new ArrayList<ServiceSchema>(), organizationSchema, router, routes);
        }
        return routes;
    }

    private void addPaths(String parentPath, List<ServiceSchema> schemaPath, ServiceSchema schema, Router router,
            Set<Route> serviceRoutes) throws SMSException {
        String schemaName = schema.getResourceName();
        String path = parentPath;
        // Top-level schemas don't have a name and we don't want them in our schema path
        if (schemaName != null && schemaName.length() > 0) {
            schemaPath.add(schema);
            path += "/" + schemaName;
        }
        if (!schema.getAttributeSchemas().isEmpty() || schema.supportsMultipleConfigurations()) {
            if (schema.supportsMultipleConfigurations()) {
                RequestHandler collectionHandler = Resources.newCollection(collectionProviderFactory.create(
                        new SmsJsonConverter(schema), schema, schemaType, new ArrayList<ServiceSchema>(schemaPath),
                        parentPath, true));
                debug.message("Adding collection path {}", path);
                serviceRoutes.add(router.addRoute(RoutingMode.EQUALS, path, collectionHandler));
                parentPath = path + "/{" + schemaName + "}";
            } else {
                debug.message("Adding singleton path {}", path);
                // router.addRoute(RoutingMode.EQUALS, path, collectionHandler);
                parentPath = path;
            }
        }
        for (String subSchema : (Set<String>) schema.getSubSchemaNames()) {
            addPaths(parentPath, new ArrayList<ServiceSchema>(schemaPath), schema.getSubSchema(subSchema), router,
                    serviceRoutes);
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
