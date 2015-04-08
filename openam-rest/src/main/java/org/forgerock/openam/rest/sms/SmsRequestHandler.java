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

import static org.forgerock.openam.rest.sms.SmsRouteTree.*;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.configuration.ConfigurationBase;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSNotificationManager;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.guava.common.base.Function;
import org.forgerock.guava.common.collect.Maps;
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

    private static final String COT_CONFIG_SERVICE = "sunFMCOTConfigService";
    static final String IDFF_METADATA_SERVICE = "sunFMIDFFMetadataService";
    private static final String SAML2_METADATA_SERVICE = "sunFMSAML2MetadataService";
    private static final String WS_METADATA_SERVICE = "sunFMWSFederationMetadataService";

    private static final Function<String, Boolean> AUTHENTICATION_HANDLES_FUNCTION =
            new SingleServiceFunction(ISAuthConstants.AUTH_SERVICE_NAME);
    private static final Function<String, Boolean> CIRCLES_OF_TRUST_HANDLES_FUNCTION =
            new SingleServiceFunction(COT_CONFIG_SERVICE);
    private static final Function<String, Boolean> ENTITYPROVIDER_HANDLES_FUNCTION = new Function<String, Boolean>() {
        private final List<String> services =
                Arrays.asList(IDFF_METADATA_SERVICE, SAML2_METADATA_SERVICE, WS_METADATA_SERVICE);

        public Boolean apply(@Nullable String name) {
            return services.contains(name);
        }
    };

    private static final Function<String, Boolean> AUTHENTICATION_MODULE_HANDLES_FUNCTION = new Function<String, Boolean>() {
        @Override
        public Boolean apply(String serviceName) {
            return AMAuthenticationManager.getAuthenticationServiceNames().contains(serviceName);
        }
    };

    /**
     * Services are all the services not handled by other handlers for specific service schema types.
     */
    private static final Function<String, Boolean> SERVICES_HANDLES_FUNCTION = new Function<String, Boolean>() {
        private final List<Function<String, Boolean>> ALREADY_HANDLED = Arrays.asList(
                AUTHENTICATION_HANDLES_FUNCTION,
                AUTHENTICATION_MODULE_HANDLES_FUNCTION,
                CIRCLES_OF_TRUST_HANDLES_FUNCTION,
                ENTITYPROVIDER_HANDLES_FUNCTION
        );
        @Override
        public Boolean apply(String serviceName) {
            for (Function<String, Boolean> handled : ALREADY_HANDLED) {
                if (handled.apply(serviceName)) {
                    return false;
                }
            }
            return true;
        }
    };

    private static final List<Pattern> IGNORED_ROUTES = Arrays.asList(
            Pattern.compile("^platform/sites(/.*)?$")
    );
    private static final String DEFAULT_VERSION = "1.0";
    private static final String USE_PARENT_PATH = "USE-PARENT";
    private final SmsCollectionProviderFactory collectionProviderFactory;
    private final SmsSingletonProviderFactory singletonProviderFactory;
    private final SchemaType schemaType;
    private final Debug debug;
    private final Pattern schemaDnPattern;
    private final Collection<String> excludedServices;
    private Map<String, Map<SmsRouteTree, Set<Route>>> serviceRoutes = new HashMap<String, Map<SmsRouteTree, Set<Route>>>();
    private final SmsRouteTree routeTree;

    @Inject
    public SmsRequestHandler(@Assisted SchemaType type, SmsCollectionProviderFactory collectionProviderFactory,
            SmsSingletonProviderFactory singletonProviderFactory, @Named("frRest") Debug debug,
            @Named("excludedServices") Collection<String> excludedServices)
            throws SMSException, SSOException {
        this.schemaType = type;
        this.collectionProviderFactory = collectionProviderFactory;
        this.singletonProviderFactory = singletonProviderFactory;
        this.debug = debug;
        this.excludedServices = excludedServices;
        this.schemaDnPattern = Pattern.compile("^ou=([.0-9]+),ou=([^,]+)," +
                Pattern.quote(ServiceManager.getServiceDN()) + "$");
        routeTree = tree(
                branch("/authentication", leaf("/modules", AUTHENTICATION_MODULE_HANDLES_FUNCTION)),
                branch("/federation", CIRCLES_OF_TRUST_HANDLES_FUNCTION,
                        leaf("/entityproviders", ENTITYPROVIDER_HANDLES_FUNCTION)),
                leaf("/services", SERVICES_HANDLES_FUNCTION)
        );

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
                        serviceRoutes.put(svcName, addService(getServiceManager(), svcName, svcVersion));
                    }
                    break;
                case SMSObjectListener.MODIFY:
                    removeService(svcName);
                    serviceRoutes.put(svcName, addService(getServiceManager(), svcName, svcVersion));
                    if (ISAuthConstants.PLATFORM_SERVICE_NAME.equals(svcName)) {
                        addServersAndSitesRoutes(getServiceManager(), serviceRoutes);
                    }
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
        Map<String, Map<SmsRouteTree, Set<Route>>> serviceRoutes = new HashMap<String, Map<SmsRouteTree, Set<Route>>>();
        ServiceManager sm = getServiceManager();
        Set<String> serviceNames = sm.getServiceNames();
        for (String serviceName : serviceNames) {
            Map<SmsRouteTree, Set<Route>> routes = addService(sm, serviceName, DEFAULT_VERSION);
            if (routes != null) {
                serviceRoutes.put(serviceName, routes);
            }
        }
        if (schemaType == SchemaType.GLOBAL) {
            addServersAndSitesRoutes(sm, serviceRoutes);
        }
        this.serviceRoutes = serviceRoutes;
    }

    private void addServersAndSitesRoutes(ServiceManager sm, Map<String, Map<SmsRouteTree, Set<Route>>> serviceRoutes)
            throws SSOException, SMSException {
        ServiceSchemaManager ssm = sm.getSchemaManager(ISAuthConstants.PLATFORM_SERVICE_NAME, DEFAULT_VERSION);
        ServiceSchema parentSchema = ssm.getGlobalSchema().getSubSchema(ConfigurationBase.CONFIG_SITES);
        ServiceSchema schema = parentSchema.getSubSchema(ConfigurationBase.SUBSCHEMA_SITE);
        HashMap<SmsRouteTree, Set<Route>> routes = new HashMap<SmsRouteTree, Set<Route>>();
        addPaths("", new ArrayList<ServiceSchema>(Collections.singletonList(parentSchema)), schema, routes,
                Collections.<Pattern>emptyList(), routeTree);
        serviceRoutes.get(ISAuthConstants.PLATFORM_SERVICE_NAME).putAll(routes);
    }

    /**
     * Remove routes for the service name.
     */
    private void removeService(String name) {
        for (Map.Entry<SmsRouteTree, Set<Route>> routeEntry : serviceRoutes.get(name).entrySet()) {
            for (Route route : routeEntry.getValue()) {
                routeEntry.getKey().removeRoute(route);
            }
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
     * @return The routes that were configured.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     */
    private Map<SmsRouteTree, Set<Route>> addService(ServiceManager sm, String serviceName, String serviceVersion)
            throws SMSException, SSOException {
        if (excludedServices.contains(serviceName)) {
            debug.message("Excluding service from REST SMS: {}", serviceName);
            return null;
        }

        ServiceSchemaManager ssm = sm.getSchemaManager(serviceName, serviceVersion);
        String resourceName = ssm.getResourceName();
        Map<SmsRouteTree, Set<Route>> routes = new HashMap<SmsRouteTree, Set<Route>>();

        if (schemaType == SchemaType.GLOBAL) {
            ServiceSchema globalSchema = ssm.getGlobalSchema();
            if (globalSchema != null) {
                debug.message("Adding global schema REST SMS endpoints for service: {}", serviceName);
                addPaths(resourceName, new ArrayList<ServiceSchema>(), globalSchema, routes, IGNORED_ROUTES, null);
            }
        }
        ServiceSchema organizationSchema = ssm.getOrganizationSchema();
        if (organizationSchema != null) {
            debug.message("Adding realm schema REST SMS endpoints for service: {}", serviceName);
            addPaths(resourceName, new ArrayList<ServiceSchema>(), organizationSchema, routes, IGNORED_ROUTES, null);
        }
        return routes;
    }

    /**
     * Recursively adds routes for the schema paths found in the schema instance.
     * @param parentPath The parent route path to add new routes beneath.
     * @param schemaPath The path for schema that is built up as we navigate through the Schema and SubSchema
     *                   declarations for the service.
     * @param schema The Schema or SubSchema instance for this iteration of the method.
     * @param serviceRoutes Routes added for the service are added for later removal if needed.
     * @param ignoredRoutes Any routes to be ignored.
     * @param routeTree The tree to add routes to. If null, the root tree will be used to find the appropriate node.
     * @throws SMSException From downstream service manager layer.
     */
    private void addPaths(String parentPath, List<ServiceSchema> schemaPath, ServiceSchema schema,
            Map<SmsRouteTree, Set<Route>> serviceRoutes, List<Pattern> ignoredRoutes, SmsRouteTree routeTree)
            throws SMSException {
        String schemaName = schema.getResourceName();
        String path = parentPath;
        // Top-level schemas don't have a name and we don't want them in our schema path
        if (schemaName != null && schemaName.length() > 0) {
            schemaPath.add(schema);
            if (!USE_PARENT_PATH.equals(schemaName)) {
                path += "/" + schemaName;
            }
        }
        if (!schema.getAttributeSchemas().isEmpty() || schema.supportsMultipleConfigurations()) {
            if (schema.supportsMultipleConfigurations()) {
                RequestHandler handler = Resources.newCollection(collectionProviderFactory.create(
                        new SmsJsonConverter(schema), schema, schemaType, new ArrayList<ServiceSchema>(schemaPath),
                        parentPath, true));
                debug.message("Adding collection path {}", path);
                serviceRoutes.putAll(addRoute(schema, RoutingMode.STARTS_WITH, path, handler, ignoredRoutes, routeTree));
                parentPath = path + "/{" + schemaName + "}";
            } else {
                RequestHandler handler = singletonProviderFactory.create(
                         new SmsJsonConverter(schema), schema, schemaType, new ArrayList<ServiceSchema>(schemaPath),
                         parentPath, true);
                debug.message("Adding singleton path {}", path);
                serviceRoutes.putAll(addRoute(schema, RoutingMode.EQUALS, path, handler, ignoredRoutes, routeTree));
                parentPath = path;
            }
        }
        for (String subSchema : (Set<String>) schema.getSubSchemaNames()) {
            addPaths(parentPath, new ArrayList<ServiceSchema>(schemaPath), schema.getSubSchema(subSchema),
                    serviceRoutes, ignoredRoutes, routeTree);
        }
    }

    private Map<SmsRouteTree, Set<Route>> addRoute(ServiceSchema schema, RoutingMode mode, String path,
            RequestHandler handler, List<Pattern> ignoredRoutes, SmsRouteTree routeTree) {
        for (Pattern ignored : ignoredRoutes) {
            if (ignored.matcher(path).matches()) {
                return Collections.emptyMap();
            }
        }
        SmsRouteTree tree = routeTree == null ? this.routeTree.handles(schema.getServiceName()) : routeTree;
        Route route = tree.addRoute(mode, path, handler);
        return Maps.newHashMap(Collections.singletonMap(tree, asSet(route)));
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
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        routeTree.handleAction(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        routeTree.handleCreate(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        routeTree.handleDelete(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        routeTree.handlePatch(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        routeTree.handleQuery(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        routeTree.handleRead(context, request, handler);
    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        routeTree.handleUpdate(context, request, handler);
    }

    private static final class SingleServiceFunction implements Function<String, Boolean> {

        private final String serviceName;

        SingleServiceFunction(String serviceName) {
            this.serviceName = serviceName;
        }

        @Nullable
        @Override
        public Boolean apply(String name) {
            return serviceName.equals(name);
        }
    }
}
