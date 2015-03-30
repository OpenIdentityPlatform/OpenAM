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
 * @since 13.0.0
 */
public class SmsRequestHandler implements RequestHandler, SMSObjectListener {

    private static final List<String> EXCLUDED_SERVICES = Arrays.asList();
    private static final String DEFAULT_VERSION = "1.0";
    private final SmsCollectionProviderFactory collectionProviderFactory;
    private final SmsSingletonProviderFactory singletonProviderFactory;
    private final SchemaType schemaType;
    private final Debug debug;
    private final Pattern schemaDnPattern;
    private Map<String, Set<Route>> serviceRoutes;
    private Router router;

    @Inject
    public SmsRequestHandler(@Assisted SchemaType type, SmsCollectionProviderFactory collectionProviderFactory,
            SmsSingletonProviderFactory singletonProviderFactory, @Named("frRest") Debug debug)
            throws SMSException, SSOException {
        this.schemaType = type;
        this.collectionProviderFactory = collectionProviderFactory;
        this.singletonProviderFactory = singletonProviderFactory;
        this.debug = debug;
        this.schemaDnPattern = Pattern.compile("^ou=([.0-9]+),ou=([^,]+)," +
                Pattern.quote(ServiceManager.getServiceDN()) + "$");

        createServices();
        SMSNotificationManager.getInstance().registerCallbackHandler(this);
    }

    /**
     * Responds to changes in the SMS data layer - we only handle changes to the deployed services.
     * @param dn The DN of the SMS item that changed.
     * @param type The type of change - see {@link com.sun.identity.sm.SMSObjectListener}.
     */
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

    /**
     * When all SMS objects have changed, we reload all the routes.
     */
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

    /**
     * Creates a {@link Router} for all the registered services, and then assigns that router to the instance so that
     * it will be used for all future requests.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     */
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

    /**
     * Remove routes for the service name.
     */
    private void removeService(String name) {
        for (Route route : serviceRoutes.get(name)) {
            router.removeRoute(route);
        }
        serviceRoutes.remove(name);
    }

    /**
     * Adds routes for the specified service to the provided router. Realm schema routes are added if the
     * {@link #schemaType} is either {@link SchemaType#GLOBAL} (for default values) or
     * {@link SchemaType#ORGANIZATION}. Global schema routes are only added for {@link SchemaType#GLOBAL}.
     *
     * @param sm The ServiceManager instance to use.
     * @param serviceName The name of the service being added.
     * @param serviceVersion The version of the service being added.
     * @param router The router for routes to be added to.
     * @return The routes that were configured.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     */
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

    /**
     * Recursively adds routes for the schema paths found in the schema instance.
     * @param parentPath The parent route path to add new routes beneath.
     * @param schemaPath The path for schema that is built up as we navigate through the Schema and SubSchema
     *                   declarations for the service.
     * @param schema The Schema or SubSchema instance for this iteration of the method.
     * @param router The router to add routes to.
     * @param serviceRoutes Routes added for the service are added to this set for later removal if needed.
     * @throws SMSException From downstream service manager layer.
     */
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
                RequestHandler handler = Resources.newCollection(collectionProviderFactory.create(
                        new SmsJsonConverter(schema), schema, schemaType, new ArrayList<ServiceSchema>(schemaPath),
                        parentPath, true));
                debug.message("Adding collection path {}", path);
                serviceRoutes.add(router.addRoute(RoutingMode.EQUALS, path, handler));
                parentPath = path + "/{" + schemaName + "}";
            } else {
                RequestHandler handler = singletonProviderFactory.create(
                         new SmsJsonConverter(schema), schema, schemaType, new ArrayList<ServiceSchema>(schemaPath),
                         parentPath, true);
                debug.message("Adding singleton path {}", path);
                router.addRoute(RoutingMode.EQUALS, path, handler);
                parentPath = path;
            }
        }
        for (String subSchema : (Set<String>) schema.getSubSchemaNames()) {
            addPaths(parentPath, new ArrayList<ServiceSchema>(schemaPath), schema.getSubSchema(subSchema), router,
                    serviceRoutes);
        }
    }

    /**
     * Gets a ServiceManager instance for the admin SSOToken.
     * @return A newly-created instance.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     */
    private ServiceManager getServiceManager() throws SSOException, SMSException {
        SSOToken adminSSOToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
        return new ServiceManager(adminSSOToken);
    }

    /**
     * Delegates the request to the internal {@link #router} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        router.handleAction(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #router} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        router.handleCreate(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #router} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        router.handleDelete(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #router} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        router.handlePatch(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #router} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        router.handleQuery(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #router} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        router.handleRead(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #router} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        router.handleUpdate(context, request, handler);
    }
}
