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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.publish;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.publish.STSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.service.RestSTSService;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @see org.forgerock.openam.sts.rest.publish.RestSTSInstancePublisher
 *
 * The publishInstance and removeInstance methods are synchronized because I want state consistency around the various operations
 * related to publishing and removing a rest-sts instance. In other words, I could use a ConcurrentHashMap for the
 * publishedRoutes, and call putIfAbsent, but that would require first adding the Route to the router, and rolling-back
 * these operations in the case of failure (i.e. removing the route from the router). Because publishing rest-sts instances
 * will occur infrequently, synchronizing both of these actions seemed a better trade-off to insure consistent, non-interleaved
 * mutations of all stateful elements involved with publishing a rest-sts instance.
 */
public class RestSTSInstancePublisherImpl implements RestSTSInstancePublisher {
    private final Router router;
    private final STSInstanceConfigPersister<RestSTSInstanceConfig> persistentStore;
    private final Map<String, Route> publishedRoutes;
    private final Logger logger;

    @Inject
    RestSTSInstancePublisherImpl(Router router, STSInstanceConfigPersister<RestSTSInstanceConfig> persistentStore, Logger logger) {
        this.router = router;
        this.persistentStore = persistentStore;
        publishedRoutes = new HashMap<String, Route>();
        this.logger = logger;
    }

    /**
     * Publishes the rest STS instance at the specified relative path
     * @param instanceConfig The RestSTSInstanceConfig which defined the guice bindings which specify the functionality of
     *                       the RestSTS instance. This RestSTSInstanceConfig will be persisted so that persisted instances
     *                       can be reconstituted in case of a server restart.
     * @param restSTSInstance The RestSTS instance defining the functionality of the rest STS service.
     * @param subPath the path, relative to the base rest-sts service, to the to-be-exposed rest-sts service.
     * @throws STSInitializationException thrown in case a rest-sts instance has already been published at the specified
     * subPath.
     */
    @Override
    public synchronized void publishInstance(RestSTSInstanceConfig instanceConfig, RestSTS restSTSInstance, String subPath) throws STSInitializationException {
        /*
        Exclude the possibility that a rest-sts instance has already been added at the sub-path.

        Looking at the router.add code to determine what happens if route already exists: it looks like it amounts to a no-op -
        the backing route collection is a CopyOnWriteArraySet, which will just return false if the add did not work, but
        because this value is not returned, we don't know what the outcome of the operation was. But because the Route class
        is contained in the CoWAS, and it does not over-ride the equals method, every new reference to a Route will be added -
        i.e. equality is defined as reference equality. So it seems possible to add the same path several times, and the
        specific SingletonResourceProvider chosen to service a given request is a function of the RouteMatcher.

        But because it should not be possible to add a new rest-sts instance at the same path, I can avoid all of these
        complexities by checking my HashMap if an entry is present, as I need to maintain a reference to all
        published Routes in order to be able to remove them.
         */
        if (publishedRoutes.containsKey(subPath)) {
            throw new STSInitializationException(ResourceException.BAD_REQUEST, "A rest-sts instance at sub-path " + subPath + " has already been published.");
        }
        Route route = router.addRoute(subPath, new RestSTSService(restSTSInstance, logger));
        /*
        Need to persist the published Route instance as it is necessary for router removal.
         */
        publishedRoutes.put(subPath, route);
        /*
        TODO: it is not clear that the deployment config element will be unique across both rest and soap sts instances.
        Safest will be to generate a uuid in the STSInstanceConfig ctor, and reference this value as an indexed ldap
        attribute when persisting the STSInstanceConfig to the SMS.
         */
        persistentStore.persistSTSInstance(instanceConfig.getDeploymentConfig().getUriElement(), instanceConfig);
    }

    /**
     * Removes the published rest-sts instance at the specified subPath.
     * @param subPath the path, relative to the base rest-sts service, to the to-be-removed service.
     * @throws IllegalArgumentException if no rest-sts instance has been published at this relative path.
     */
    @Override
    public synchronized void removeInstance(String subPath) throws IllegalArgumentException {
        Route route = publishedRoutes.remove(subPath);
        if (route == null) {
            throw new IllegalArgumentException("No published Rest STS instance at path " + subPath);
        }
        persistentStore.removeSTSInstance(subPath);
        router.removeRoute(route);
    }

    public List<RestSTSInstanceConfig> getPublishedInstances() {
        return persistentStore.getAllPublishedInstances();
    }
}
