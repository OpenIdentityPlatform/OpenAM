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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.identity.setup.AMSetupServlet;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.publish.STSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
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
 *
 * TODO: transaction semantics for publish and remove - if one of the mutations throws an exception, take steps to unwind
 * the other mutations!!!
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
     * Publishes the rest STS instance at the specified relative path. This method will be invoked when the Rest STS instance
     * is initially published, and to re-constitute previously-published instances following a server restart.
     * @param instanceConfig The RestSTSInstanceConfig which defined the guice bindings which specify the functionality of
     *                       the RestSTS instance. This RestSTSInstanceConfig will be persisted so that persisted instances
     *                       can be reconstituted in case of a server restart.
     * @param restSTSInstance The RestSTS instance defining the functionality of the rest STS service.
     * @param republish Indicates whether this is an original publish, or a republish following OpenAM restart.
     * @throws STSPublishException thrown in case a rest-sts instance has already been published at the specified
     * subPath, or in case other errors prevented a successful publish.
     */
    public synchronized String publishInstance(RestSTSInstanceConfig instanceConfig, RestSTS restSTSInstance,
                                             boolean republish) throws STSPublishException {
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
        String deploymentSubPath = instanceConfig.getDeploymentSubPath();
        if (deploymentSubPath.endsWith(AMSTSConstants.FORWARD_SLASH)) {
            deploymentSubPath = deploymentSubPath.substring(0, deploymentSubPath.lastIndexOf(AMSTSConstants.FORWARD_SLASH));
        }

        if (deploymentSubPath.startsWith(AMSTSConstants.FORWARD_SLASH)) {
            deploymentSubPath = deploymentSubPath.substring(1, deploymentSubPath.length());
        }

        if (publishedRoutes.containsKey(deploymentSubPath)) {
            throw new STSPublishException(ResourceException.BAD_REQUEST, "A rest-sts instance at sub-path " +
                    deploymentSubPath + " has already been published.");
        }
        Route route = router.addRoute(deploymentSubPath, new RestSTSService(restSTSInstance, logger));
        /*
        Need to persist the published Route instance as it is necessary for router removal.
         */
        publishedRoutes.put(deploymentSubPath, route);
        /*
        If this is a republish (i.e. re-constitute previously-published Rest STS instances following OpenAM restart),
        then the RestSTSInsanceConfig does not need to be persisted, as it was obtained from the SMS via a GET
        on the publish service by the RestSTSInstanceRepublishServlet.
         */
        if (!republish) {
            persistentStore.persistSTSInstance(deploymentSubPath, instanceConfig);
        }
        return deploymentSubPath;
    }

    /**
     * Removes the published rest-sts instance at the specified stsId. Note that when previously-published Rest STS
     * instances are reconstituted following an OpenAM restart, the publishInstance above will be called, which will
     * re-constitute the Route state in the Map, state necessary to remove the Route corresponding to the STS instance
     * from the Crest router. This is important because the Route has a package-private ctor.
     * @param stsId the path, relative to the base rest-sts service, to the to-be-removed service. Note that this path
     *              includes the realm.
     * @param realm The realm of the STS instance
     * @throws org.forgerock.openam.sts.STSPublishException if the entry in the SMS could not be removed, or if no
     * Route entry could be found in the Map corresponding to a previously-published instance.
     */
    public synchronized void removeInstance(String stsId, String realm) throws STSPublishException {
        Route route = publishedRoutes.remove(stsId);
        if (route == null) {
            throw new STSPublishException(ResourceException.NOT_FOUND, "No previously published STS instance with id "
                    + stsId + " in realm " + realm + " found!");
        }
        persistentStore.removeSTSInstance(stsId, realm);
        router.removeRoute(route);
    }

    public List<RestSTSInstanceConfig> getPublishedInstances() throws STSPublishException{
        return persistentStore.getAllPublishedInstances();
    }

    public RestSTSInstanceConfig getPublishedInstance(String stsId, String realm) throws STSPublishException {
        return persistentStore.getSTSInstanceConfig(stsId, realm);
    }

    /*
    This method is only to be called by the RestSTSInstanceRepublishServlet, which calls it only to re-publish
    previously-published Rest STS instances during OpenAM startup.
     */
    public void republishExistingInstances() throws STSPublishException {
        /*
        Do not trigger the republish if OpenAM is being installed or upgraded.
         */
        if (AMSetupServlet.isCurrentConfigurationValid()) {
            final List<RestSTSInstanceConfig> publishedInstances = getPublishedInstances();
            for (RestSTSInstanceConfig instanceConfig : publishedInstances) {
                Injector instanceInjector;
                try {
                    instanceInjector = Guice.createInjector(new RestSTSInstanceModule(instanceConfig));
                } catch (Exception e) {
                    logger.error("Exception caught creating the guice injector in republish corresponding to rest sts " +
                            "instance: " + instanceConfig.toJson() + ". This instance cannot be republished. Exception: " + e);
                    continue;
                }
                try {
                    publishInstance(instanceConfig, instanceInjector.getInstance(RestSTS.class), true);
                    logger.info("Republished Rest STS instance corresponding to config " + instanceConfig.toJson());
                } catch (STSPublishException e) {
                    logger.error("Exception caught publishing rest sts " +
                            "instance: " + instanceConfig.toJson() + ". This instance cannot be republished. Exception: " + e);
                    continue;
                }
            }
        }
    }
}
