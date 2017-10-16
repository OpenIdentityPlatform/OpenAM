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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

import static com.google.common.collect.Sets.newHashSet;
import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.json.resource.Requests.newApiRequest;
import static org.forgerock.json.resource.Resources.newAnnotatedRequestHandler;
import static org.forgerock.json.resource.Resources.newCollection;
import static org.forgerock.json.resource.Resources.newHandler;
import static org.forgerock.openam.core.rest.sms.SmsRealmProvider.REALMS_PATH;
import static org.forgerock.openam.core.rest.sms.tree.SmsRouteTreeBuilder.branch;
import static org.forgerock.openam.core.rest.sms.tree.SmsRouteTreeBuilder.filter;
import static org.forgerock.openam.core.rest.sms.tree.SmsRouteTreeBuilder.leaf;
import static org.forgerock.openam.core.rest.sms.tree.SmsRouteTreeBuilder.tree;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import com.google.common.base.Predicate;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.sms.tree.SmsRouteTree;
import org.forgerock.openam.forgerockrest.utils.MatchingResourcePath;
import org.forgerock.openam.rest.RealmRoutingFactory;
import org.forgerock.openam.rest.authz.CrestPrivilegeAuthzModule;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.services.routing.RouteMatcher;
import org.forgerock.util.promise.Promise;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSNotificationManager;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
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
public class SmsRequestHandler implements RequestHandler, SMSObjectListener, ServiceListener,
        Describable<ApiDescription, Request> {

    private static final Pattern DEFAULT_IGNORED_ROUTES =
            Pattern.compile("services/platform/(\\{sites\\}|sites|servers|secondary-urls)(/.*)?$");
    private static final String DEFAULT_VERSION = "1.0";
    static final String USE_PARENT_PATH = "USE-PARENT";
    private static final String EMPTY_PATH = "EMPTY";
    private final SmsCollectionProviderFactory collectionProviderFactory;
    private final SmsSingletonProviderFactory singletonProviderFactory;
    private final SmsGlobalSingletonProviderFactory globalSingletonProviderFactory;
    private final SchemaType schemaType;
    private final Debug debug;
    private final Pattern schemaDnPattern;
    private final Collection<String> excludedServices;
    private final RealmRoutingFactory realmRoutingFactory;
    private final Map<SchemaType, Collection<Predicate<String>>> excludedServiceSingletons = new HashMap<>();
    private final Map<SchemaType, Collection<Predicate<String>>> excludedServiceCollections = new HashMap<>();
    private final RealmNormaliser realmNormaliser;
    private final ServicesRealmSmsHandler servicesRealmSmsHandler;
    private Map<String, Map<SmsRouteTree, Set<RouteMatcher<Request>>>> serviceRoutes = new HashMap<>();
    private final SessionCache sessionCache;
    private final CoreWrapper coreWrapper;
    private final SmsServiceHandlerFunction smsServiceHandlerFunction;
    private final SitesResourceProvider sitesResourceProvider;
    private final ServersResourceProvider serversResourceProvider;
    private final List<Describable.Listener> apiListeners = new ArrayList<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock read = lock.readLock();
    private final Lock write = lock.writeLock();
    // Guarded by lock.
    private final SmsRouteTree routeTree;
    private ApiDescription api;

    @Inject
    public SmsRequestHandler(@Assisted SchemaType type, SmsCollectionProviderFactory collectionProviderFactory,
            SmsSingletonProviderFactory singletonProviderFactory,
            SmsGlobalSingletonProviderFactory globalSingletonProviderFactory, @Named("frRest") Debug debug,
            ExcludedServicesFactory excludedServicesFactory, AuthenticationChainsFilter authenticationChainsFilter,
            RealmRoutingFactory realmRoutingFactory, SessionCache sessionCache, CoreWrapper coreWrapper,
            RealmNormaliser realmNormaliser, Map<MatchingResourcePath, CrestAuthorizationModule> globalAuthzModules,
            CrestPrivilegeAuthzModule privilegeAuthzModule, SmsServiceHandlerFunction smsServiceHandlerFunction,
            PrivilegedAction<SSOToken> adminTokenAction, ServicesRealmSmsHandler servicesRealmSmsHandler,
            SitesResourceProvider sitesResourceProvider, ServersResourceProvider serversResourceProvider)
            throws SMSException, SSOException {
        this.schemaType = type;
        this.collectionProviderFactory = collectionProviderFactory;
        this.singletonProviderFactory = singletonProviderFactory;
        this.globalSingletonProviderFactory = globalSingletonProviderFactory;
        this.debug = debug;
        this.sessionCache = sessionCache;
        this.coreWrapper = coreWrapper;
        this.realmNormaliser = realmNormaliser;
        this.excludedServices = excludedServicesFactory.get(type);
        this.realmRoutingFactory = realmRoutingFactory;
        this.servicesRealmSmsHandler = servicesRealmSmsHandler;
        this.schemaDnPattern = Pattern.compile("^ou=([.0-9]+),ou=([^,]+)," +
                Pattern.quote(ServiceManager.getServiceDN()) + "$");
        Map<MatchingResourcePath, CrestAuthorizationModule> authzModules = type.equals(SchemaType.GLOBAL)
                ? globalAuthzModules
                : Collections.<MatchingResourcePath, CrestAuthorizationModule>emptyMap();
        routeTree = tree(authzModules, privilegeAuthzModule,
                branch("/authentication", smsServiceHandlerFunction.AUTHENTICATION_HANDLES_FUNCTION,
                        leaf("/modules", smsServiceHandlerFunction.AUTHENTICATION_MODULE_HANDLES_FUNCTION, true),
                        filter("/chains", smsServiceHandlerFunction.AUTHENTICATION_CHAINS_HANDLES_FUNCTION,
                                authenticationChainsFilter)),
                branch("/federation", smsServiceHandlerFunction.CIRCLES_OF_TRUST_HANDLES_FUNCTION,
                        leaf("/entityproviders", smsServiceHandlerFunction.ENTITYPROVIDER_HANDLES_FUNCTION, false)),
                leaf("/agents",smsServiceHandlerFunction.AGENTS_MODULE_HANDLES_FUNCTION, true),
                leaf("/services", smsServiceHandlerFunction, true)
        );

        this.smsServiceHandlerFunction = smsServiceHandlerFunction;
        this.sitesResourceProvider = sitesResourceProvider;
        this.serversResourceProvider = serversResourceProvider;

        addExcludedServiceProviders();
        createServices();
        addSpecialCaseRoutes();
        SMSNotificationManager.getInstance().registerCallbackHandler(this);
        registerServiceListener(adminTokenAction);
    }

    private void registerServiceListener(PrivilegedAction<SSOToken> adminTokenAction) throws SSOException, SMSException {
        ServiceConfigManager serviceConfigManager = new ServiceConfigManager(ISAuthConstants.AUTH_SERVICE_NAME,
                adminTokenAction.run());
        if (serviceConfigManager.addListener(this) == null) {
            debug.error("Could not add listener to ServiceConfigManager instance. Auth Module " +
                    "changes will not be dynamically updated");
        }
    }

    private void addSpecialCaseRoutes() throws SMSException, SSOException {
        addServiceInstancesQueryHandler();
        addAgentServiceQueryHandler();
        addAuthenticationHandlers();
        addRealmHandler();
        addCommonTasksHandler();
        addSitesHandler();
        addServersHandler();
    }

    //hard-coded authentication routes
    private void addAuthenticationHandlers() {
        // realm-config/authentication/modules -> realm module collection handler
        if (SchemaType.ORGANIZATION.equals(schemaType)) {
            getAuthenticationModuleRouter().addRoute(EQUALS, "",
                    newAnnotatedRequestHandler(InjectorHolder.getInstance(AuthenticationModuleRealmSmsHandler.class)));
        }
    }

    //hard-coded services routes
    private void addServiceInstancesQueryHandler() throws SSOException, SMSException {
        // realm-config/services -> service realm collection
        if (SchemaType.ORGANIZATION.equals(schemaType)) {
            SmsRouteTree serviceInstanceRouter = getServiceInstanceRouter();
            servicesRealmSmsHandler.setSmsRouteTree(serviceInstanceRouter);
            serviceInstanceRouter.addRoute(EQUALS, "", newAnnotatedRequestHandler(servicesRealmSmsHandler));
        }
    }

    //hard-coded aggregating agent query routes
    private void addAgentServiceQueryHandler() throws SSOException, SMSException {
        // realm-config/agents -> service realm collection
        if (SchemaType.ORGANIZATION.equals(schemaType)) {
            ServiceManager sm = getServiceManager();
            ServiceSchemaManager ssm = sm.getSchemaManager(ISAuthConstants.AGENT_SERVICE_NAME, DEFAULT_VERSION);
            ServiceSchema agentServiceSchema = ssm.getOrganizationSchema();
            SmsRouteTree serviceInstanceRouter = routeTree.handles(ISAuthConstants.AGENT_SERVICE_NAME);
            serviceInstanceRouter.addRoute(EQUALS, "", newAnnotatedRequestHandler(
                    new SmsAggregatingAgentsQueryHandler(agentServiceSchema, debug)));
        }
    }

    //hard-coded realm-config/commontasks route
    private void addCommonTasksHandler() {
        if (SchemaType.ORGANIZATION.equals(schemaType)) {
            routeTree.addRoute(STARTS_WITH, "/commontasks",
                    newCollection(InjectorHolder.getInstance(CommonTasksResource.class)));
        }
    }

    //routes under global-config/realms/{realms} route
    private void addRealmHandler() {
        if (SchemaType.GLOBAL.equals(schemaType)) {
            routeTree.addRoute(RoutingMode.STARTS_WITH, REALMS_PATH,
                    newHandler(new SmsRealmProvider(sessionCache, coreWrapper, realmNormaliser)));
        }
    }

    //routes under global-config/sites
    private void addSitesHandler() {
        if (SchemaType.GLOBAL.equals(schemaType)) {
            routeTree.addRoute(STARTS_WITH, "sites", newCollection(sitesResourceProvider));
        }
    }

    //routes under global-config/servers
    private void addServersHandler() {
        if (SchemaType.GLOBAL.equals(schemaType)) {
            routeTree.addRoute(STARTS_WITH, "servers", newCollection(serversResourceProvider));
        }
    }

    /**
     * Identifies the first node in the SMS tree which isn't explicitly handled by another handler.
     */
    private SmsRouteTree getServiceInstanceRouter() throws SMSException, SSOException {

        final Set<String> serviceNames = getServiceManager().getServiceNames();

        for (String serviceName : serviceNames) {
            if (smsServiceHandlerFunction.apply(serviceName)) {
                return routeTree.handles(serviceName);
            }
        }

        throw new IllegalStateException("Services SmsRouteTree could not be located");
    }

    private SmsRouteTree getAuthenticationModuleRouter() {
        return routeTree.handles(new ArrayList<>(AMAuthenticationManager.getAuthenticationServiceNames()).get(0));
    }

    private void addExcludedServiceProviders() {
        excludedServiceSingletons.put(SchemaType.GLOBAL, CollectionUtils.<Predicate<String>>asSet());
        excludedServiceSingletons.put(SchemaType.ORGANIZATION,
                asSet(smsServiceHandlerFunction.AUTHENTICATION_MODULE_HANDLES_FUNCTION));
        excludedServiceCollections.put(SchemaType.GLOBAL,
                asSet(smsServiceHandlerFunction.AUTHENTICATION_MODULE_HANDLES_FUNCTION));
        excludedServiceCollections.put(SchemaType.ORGANIZATION, CollectionUtils.<Predicate<String>>asSet());
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

        refreshServiceRoute(type, matcher.group(2), matcher.group(1));
    }

    /**
     * Applies the event type to the service routes.
     *
     * @param type Type of event as defined in {@link SMSObjectListener}
     * @param svcName Non null name of the service.
     * @param svcVersion Non null version of the service.
     */
    private void refreshServiceRoute(int type, String svcName, String svcVersion) {
        try {
            ServiceManager svcMgr = getServiceManager();
            switch (type) {
                case SMSObjectListener.DELETE:
                    if (serviceRoutes.containsKey(svcName)) {
                        removeService(svcName);
                        notifyDescriptorChange();
                    }
                    break;
                case SMSObjectListener.ADD:
                    if (!serviceRoutes.containsKey(svcName)) {
                        serviceRoutes.put(svcName, addService(svcMgr, svcName, svcVersion));
                        notifyDescriptorChange();
                    }
                    break;
                case SMSObjectListener.MODIFY:
                    try {
                        write.lock();
                        removeService(svcName);
                        serviceRoutes.put(svcName, addService(getServiceManager(), svcName, svcVersion));
                        notifyDescriptorChange();
                    } finally {
                        write.unlock();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown modification type: " + type);
            }
        } catch (SSOException | SMSException e) {
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

    @Override
    public void globalConfigChanged(String serviceName, String version, String groupName, String serviceComponent,
            int type) {
        if (ISAuthConstants.AUTH_SERVICE_NAME.equals(serviceName)) {
            SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
            refreshServiceRoute(SMSObjectListener.MODIFY, serviceName, version);
            for (String authModuleService : AMAuthenticationManager.getAuthenticationServiceNames()) {
                try {
                    refreshServiceRoute(SMSObjectListener.MODIFY, authModuleService,
                            new ServiceSchemaManager(authModuleService, token).getVersion());
                } catch (SMSException | SSOException e) {
                    debug.warning("Could not refresh service: {} as could not work out its version", authModuleService);
                }
            }
        }
    }

    /**
     * Creates a {@link Router} for all the registered services, and then assigns that router to the instance so that
     * it will be used for all future requests.
     * @throws SMSException From downstream service manager layer.
     * @throws SSOException From downstream service manager layer.
     */
    private synchronized void createServices() throws SSOException, SMSException {
        Map<String, Map<SmsRouteTree, Set<RouteMatcher<Request>>>> serviceRoutes = new HashMap<>();
        ServiceManager sm = getServiceManager();
        Set<String> serviceNames = sm.getServiceNames();
        for (String serviceName : serviceNames) {
            Map<SmsRouteTree, Set<RouteMatcher<Request>>> routes = addService(sm, serviceName, DEFAULT_VERSION);
            if (routes != null) {
                serviceRoutes.put(serviceName, routes);
            }
        }

        this.serviceRoutes = serviceRoutes;
    }

    /**
     * Remove routes for the service name.
     */
    private void removeService(String name) {
        if (serviceRoutes.get(name) == null) {
            return;
        }
        for (Map.Entry<SmsRouteTree, Set<RouteMatcher<Request>>> routeEntry : serviceRoutes.get(name).entrySet()) {
            for (RouteMatcher<Request> route : routeEntry.getValue()) {
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
    private Map<SmsRouteTree, Set<RouteMatcher<Request>>> addService(ServiceManager sm, String serviceName,
            String serviceVersion)
            throws SMSException, SSOException {
        if (excludedServices.contains(serviceName)) {
            debug.message("Excluding service from REST SMS: {}", serviceName);
            return new HashMap<>();
        }

        ServiceSchemaManager ssm = sm.getSchemaManager(serviceName, serviceVersion);
        Map<SmsRouteTree, Set<RouteMatcher<Request>>> routes = new HashMap<>();

        ServiceSchema organizationSchema = ssm.getOrganizationSchema();
        ServiceSchema dynamicSchema = ssm.getDynamicSchema();
        if (schemaType == SchemaType.GLOBAL) {
            ServiceSchema globalSchema = ssm.getGlobalSchema();
            if (hasGlobalSchema(globalSchema)) {
                debug.message("Adding global schema REST SMS endpoints for service: {}", serviceName);
                addGlobalPaths("", new ArrayList<ServiceSchema>(), globalSchema, organizationSchema,
                        dynamicSchema, routes, DEFAULT_IGNORED_ROUTES, null);
            } else if (organizationSchema != null) {
                debug.message("Adding global schema REST SMS endpoints for service: {}", serviceName);
                addGlobalPaths("", new ArrayList<ServiceSchema>(), organizationSchema, organizationSchema,
                        dynamicSchema, routes, DEFAULT_IGNORED_ROUTES, null);
            }
        } else {
            if (organizationSchema != null) {
                debug.message("Adding realm schema REST SMS endpoints for service: {}", serviceName);
                addPaths("", new ArrayList<ServiceSchema>(), organizationSchema, dynamicSchema, routes,
                        DEFAULT_IGNORED_ROUTES, null);
            } else if (dynamicSchema != null) {
                debug.message("Adding realm schema REST SMS endpoints for service: {}", serviceName);
                addPaths("", new ArrayList<ServiceSchema>(), dynamicSchema, dynamicSchema, routes,
                        DEFAULT_IGNORED_ROUTES, null);
            }
        }
        return routes;
    }

    private boolean hasGlobalSchema(ServiceSchema globalSchema) throws SMSException {
        return globalSchema != null
                && (!globalSchema.getAttributeSchemaNames().isEmpty()
                        || !globalSchema.getSubSchemaNames().isEmpty());
    }

    /**
     * Recursively adds global routes for the schema paths found in the schema instance.
     *
     * @param parentPath The parent route path to add new routes beneath.
     * @param schemaPath The path for schema that is built up as we navigate through the Schema and SubSchema
     *                   declarations for the service.
     * @param globalSchema The Global Schema instance.
     * @param organizationSchema The Organization Schema instance, or {@code null} if no organization schema is defined.
     * @param dynamicSchema The Dynamic Schema instance, or {@code null} if no dynamic schema is defined.
     * @param serviceRoutes Routes added for the service are added for later removal if needed.
     * @param ignoredRoutes Any routes to be ignored.
     * @param routeTree The tree to add routes to. If null, the root tree will be used to find the appropriate node.
     * @throws SMSException From downstream service manager layer.
     */
    private void addGlobalPaths(String parentPath, List<ServiceSchema> schemaPath, ServiceSchema globalSchema,
            ServiceSchema organizationSchema, ServiceSchema dynamicSchema, Map<SmsRouteTree,
            Set<RouteMatcher<Request>>> serviceRoutes, Pattern ignoredRoutes, SmsRouteTree routeTree)
            throws SMSException {
        String schemaName = globalSchema.getResourceName();
        String path = updatePaths(parentPath, schemaName, schemaPath, globalSchema);

        SmsGlobalSingletonProvider handler = globalSingletonProviderFactory.create(new SmsJsonConverter(globalSchema),
                globalSchema, organizationSchema, dynamicSchema, schemaType, new ArrayList<>(schemaPath),
                parentPath, true);
        debug.message("Adding singleton path {}", path);
        routeTree = addRoute(globalSchema, EQUALS, path, newAnnotatedRequestHandler(handler), ignoredRoutes, routeTree,
                serviceRoutes);

        if (globalSchema != organizationSchema) {
            addPaths("", schemaPath, globalSchema, serviceRoutes, ignoredRoutes, routeTree);
        }
    }

    private boolean excludeSingleton(String serviceName) {
        return exclude(excludedServiceSingletons.get(schemaType), serviceName);
    }

    private boolean excludeCollection(String serviceName) {
        return exclude(excludedServiceCollections.get(schemaType), serviceName);
    }

    private boolean exclude(Collection<Predicate<String>> excludeFunctions, String serviceName) {
        for (Predicate<String> f : excludeFunctions) {
            if (f.apply(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively adds routes for the schema paths found in the schema instance.
     *
     * @param parentPath The parent route path to add new routes beneath.
     * @param schemaPath The path for schema that is built up as we navigate through the Schema and SubSchema
     *                   declarations for the service.
     * @param schema The Schema or SubSchema instance for this iteration of the method.
     * @param dynamicSchema The dynamic Schema instance, or {@code null} if no dynamic schema is defined.
     * @param serviceRoutes Routes added for the service are added for later removal if needed.
     * @param ignoredRoutes Any routes to be ignored.
     * @param routeTree The tree to add routes to. If null, the root tree will be used to find the appropriate node.
     * @throws SMSException From downstream service manager layer.
     */
    private void addPaths(String parentPath, List<ServiceSchema> schemaPath, ServiceSchema schema,
            ServiceSchema dynamicSchema, Map<SmsRouteTree, Set<RouteMatcher<Request>>> serviceRoutes,
            Pattern ignoredRoutes, SmsRouteTree routeTree) throws SMSException {
        String schemaName = schema.getResourceName();
        String path = updatePaths(parentPath, schemaName, schemaPath, schema);
        SmsRouteTree subtree = routeTree;
        if (!schema.getAttributeSchemas().isEmpty() || schema.supportsMultipleConfigurations()) {
            if (schema.supportsMultipleConfigurations() && !excludeCollection(schema.getServiceName())) {
                RequestHandler handler = newCollection(collectionProviderFactory.create(
                        new SmsJsonConverter(schema), schema, schemaType, new ArrayList<>(schemaPath),
                        parentPath, true));
                debug.message("Adding collection path {}", path);
                subtree = addRoute(schema, STARTS_WITH, path, handler, ignoredRoutes, routeTree, serviceRoutes);
                parentPath = "/{" + schemaName + "}";
            } else if (!excludeSingleton(schema.getServiceName())) {
                RequestHandler handler = newAnnotatedRequestHandler(singletonProviderFactory.create(
                        new SmsJsonConverter(schema), schema, dynamicSchema, schemaType, new ArrayList<>(schemaPath),
                        parentPath, true));
                debug.message("Adding singleton path {}", path);
                subtree = addRoute(schema, EQUALS, path, handler, ignoredRoutes, routeTree, serviceRoutes);
                parentPath = "";
            }
        }

        addPaths(parentPath, schemaPath, schema, serviceRoutes, ignoredRoutes, subtree);
    }

    private void addPaths(String parentPath, List<ServiceSchema> schemaPath, ServiceSchema schema,
            Map<SmsRouteTree, Set<RouteMatcher<Request>>> serviceRoutes, Pattern ignoredRoutes,
            SmsRouteTree routeTree)
            throws SMSException {
        for (String subSchema : schema.getSubSchemaNames()) {
            ServiceSchema subServiceSchema = schema.getSubSchema(subSchema);
            if (USE_PARENT_PATH.equals(subServiceSchema.getResourceName()) && "".equals(parentPath)) {
                parentPath = schema.getResourceName();
            }
            addPaths(parentPath, new ArrayList<>(schemaPath), subServiceSchema, null, serviceRoutes,
                    ignoredRoutes, routeTree);
        }
    }

    private String updatePaths(String parentPath, String schemaName, List<ServiceSchema> schemaPath,
            ServiceSchema schema) {
        String path = parentPath;
        // Top-level schemas don't have a name and we don't want them in our schema path
        if (schema.getName() != null) {
            schemaPath.add(schema);
        }
        if (EMPTY_PATH.equals(schemaName) || (USE_PARENT_PATH.equals(schemaName) && EMPTY_PATH.equals(parentPath))) {
            return "";
        }
        if (!USE_PARENT_PATH.equals(schemaName)) {
            path += (path.equals("") ? "" : "/") + schemaName;
        }
        return path;
    }

    private SmsRouteTree addRoute(ServiceSchema schema, RoutingMode mode, String path,
            RequestHandler handler, Pattern ignoredRoutes, SmsRouteTree routeTree,
            Map<SmsRouteTree, Set<RouteMatcher<Request>>> serviceRoutes) {
        String parentPath = routeTree == null ? "" : routeTree.getPath() + (path.startsWith("/") ? "" : "/");
        if (ignoredRoutes.matcher(parentPath + path).matches()) {
            return routeTree;
        }
        SmsRouteTree tree = routeTree == null ? this.routeTree.handles(schema.getServiceName()) : routeTree;
        SmsRouteTree.Route route = tree.addRoute(mode, path, handler, schema.isHiddenInConfigUI());
        if (serviceRoutes.containsKey(tree)) {
            serviceRoutes.get(tree).add(route.matcher);
        } else {
            serviceRoutes.put(tree, newHashSet(route.matcher));
        }
        return route.tree;
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
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            read.lock();
            return routeTree.handleAction(context, request);
        } finally {
            read.unlock();
        }
    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     *
     * Thread Safety: Synchronized to protect access to {@link #routeTree} during modifications.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
        try {
            read.lock();
            return routeTree.handleCreate(context, request);
        } finally {
            read.unlock();
        }

    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
        try {
            read.lock();
            return routeTree.handleDelete(context, request);
        } finally {
            read.unlock();
        }

    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
        try {
            read.lock();
            return routeTree.handlePatch(context, request);
        } finally {
            read.unlock();
        }

    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     */
    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
            QueryResourceHandler handler) {
        try {
            read.lock();
            return routeTree.handleQuery(context, request, handler);
        } finally {
            read.unlock();
        }

    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            read.lock();
            return routeTree.handleRead(context, request);
        } finally {
            read.unlock();
        }
    }

    /**
     * Delegates the request to the internal {@link #routeTree} for SMS requests.
     * {@inheritDoc}
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     */
    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
        try {
            read.lock();
            return routeTree.handleUpdate(context, request);
        } finally {
            read.unlock();
        }
    }

    @Override
    public void schemaChanged(String serviceName, String version) {
        // no-op
    }

    @Override
    public void organizationConfigChanged(String serviceName, String version, String orgName, String groupName,
            String serviceComponent, int type) {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     *
     * @param apiProducer {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
        try {
            read.lock();
            return routeTree.api(apiProducer);
        } finally {
            read.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public ApiDescription handleApiRequest(Context context, Request request) {
        try {
            read.lock();
            return routeTree.handleApiRequest(context, request);
        } finally {
            read.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     *
     * @param listener {@inheritDoc}
     */
    @Override
    public void addDescriptorListener(Listener listener) {
        apiListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     *
     * Thread Safety: Read/Write locking to protect access to {@link #routeTree} during modifications.
     *
     * @param listener {@inheritDoc}
     */
    @Override
    public void removeDescriptorListener(Listener listener) {
        apiListeners.remove(listener);
    }

    private synchronized void notifyDescriptorChange() {
        ApiDescription oldApi = this.api;
        this.api = routeTree.handleApiRequest(
                new UriRouterContext(new RootContext(), "", "", Collections.<String, String>emptyMap()),
                newApiRequest(ResourcePath.empty()));
        if (!oldApi.equals(api)) {
            for (Listener listener : apiListeners) {
                listener.notifyDescriptorChange();
            }
        }
    }
}
