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
import com.sun.identity.sm.ServiceListener;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Route;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.ServiceListenerRegistration;
import org.forgerock.openam.sts.rest.config.RestSTSInstanceModule;
import org.forgerock.openam.sts.rest.config.RestSTSModule;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.service.RestSTSService;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
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
 */
public class RestSTSInstancePublisherImpl implements RestSTSInstancePublisher {
    private final Router router;
    private final STSInstanceConfigStore<RestSTSInstanceConfig> persistentStore;
    private final Map<String, Route> publishedRoutes;
    private final ServiceListenerRegistration serviceListenerRegistration;
    private final ServiceListener serviceListener;
    private final Logger logger;

    @Inject
    RestSTSInstancePublisherImpl(Router router,
                                 STSInstanceConfigStore<RestSTSInstanceConfig> persistentStore,
                                 ServiceListenerRegistration serviceListenerRegistration,
                                 @Named(RestSTSModule.REST_STS_PUBLISH_LISTENER)ServiceListener serviceListener,
                                 Logger logger) {
        this.router = router;
        this.persistentStore = persistentStore;
        this.serviceListenerRegistration = serviceListenerRegistration;
        this.serviceListener = serviceListener;
        publishedRoutes = new HashMap<String, Route>();
        this.logger = logger;
    }

    /**
     * Publishes the rest STS instance at the specified relative path. This method will be invoked when the Rest STS instance
     * is initially published, and to re-constitute previously-published instances following a server restart.
     * Note on transaction semantics: publishing a rest-sts instance means modifying both the CREST router, and the SMS.
     * First the CREST router is modified, and then the SMS. If the SMS modifications fail, the CREST router modifications
     * will be rolled-back, so that a publish can be successfully re-tried.
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
        String deploymentSubPath = normalizeDeploymentSubPath(instanceConfig.getDeploymentSubPath());

        if (publishedRoutes.containsKey(normalizeDeploymentSubPathForRouteCache(deploymentSubPath))) {
            throw new STSPublishException(ResourceException.CONFLICT, "A rest-sts instance at sub-path " +
                    deploymentSubPath + " has already been published.");
        }
        Route route = router.addRoute(deploymentSubPath, new RestSTSService(restSTSInstance, logger));
        /*
        Need to persist the published Route instance as it is necessary for router removal.
         */
        publishedRoutes.put(normalizeDeploymentSubPathForRouteCache(deploymentSubPath), route);
        /*
        If this is a republish (i.e. re-constitute previously-published Rest STS instances following OpenAM restart),
        then the RestSTSInsanceConfig does not need to be persisted, as it was obtained from the SMS via a GET
        on the publish service by the RestSTSInstanceRepublishServlet.
         */
        if (!republish) {
            try {
                persistentStore.persistSTSInstance(deploymentSubPath, instanceConfig);
            } catch (STSPublishException e) {
                //roll-back the router mutation before throwing exception
                router.removeRoute(route);
                publishedRoutes.remove(normalizeDeploymentSubPathForRouteCache(deploymentSubPath));
                throw e;
            }
        }
        return deploymentSubPath;
    }

    /**
     * Removes the published rest-sts instance at the specified stsId. Note that when previously-published Rest STS
     * instances are reconstituted following an OpenAM restart, the publishInstance above will be called, which will
     * re-constitute the Route state in the Map, state necessary to remove the Route corresponding to the STS instance
     * from the Crest router. This is important because the Route has a package-private ctor.
     * Note on transaction semantics: The removal of a previously-published rest-sts instance involves removing the associated
     * route from the CREST router (first step), and then removing the instance state from the SMS (second step). This
     * method lacks the state to re-constitute CREST router state if the SMS modifications fail, and it may be a safe
     * assumption that a removal would want the instance unavailable for invocation first-and-foremost. Nevertheless, it
     * should be possible to invoke this method several times for the same rest-sts instance, in case the LDAP server was
     * down in previous invocations. That means that the lack of a CREST route for a previously-published instance should
     * not prevent the attempted removal of the associated configuration state from the SMS. Note that the consumption
     * of this service is limited to Administrators, so malicious consumption is prevented.
     * @param stsId the path, relative to the base rest-sts service, to the to-be-removed service. Note that this path
     *              includes the realm.
     * @param realm The realm of the STS instance
     * @param removeOnlyFromRouter Set to true when called by a ServiceListener in a site deployment to remove a rest-sts instance, deleted
     *                             on another server, and thus removed from the SMS, but requiring removal from the CREST router.
     *                             Set to false in all other cases.
     * @throws org.forgerock.openam.sts.STSPublishException if the entry in the SMS could not be removed, or if no
     * Route entry could be found in the Map corresponding to a previously-published instance.
     */
    public synchronized void removeInstance(String stsId, String realm, boolean removeOnlyFromRouter) throws STSPublishException {
        Route route = publishedRoutes.remove(normalizeDeploymentSubPathForRouteCache(stsId));
        if (route == null) {
            /*
            The route is null. Check to see if caller is attempting to delete a non-existent instance, or whether this is
            a deletion retry which previously failed due to an ldap issue. The presence of SMS config state will allow us
            to make that determination. The STSPublishException will be thrown if no config state is present in the SMS.
             */
            try {
                persistentStore.getSTSInstanceConfig(stsId, realm);
            } catch (STSPublishException e) {
                throw new STSPublishException(ResourceException.NOT_FOUND, "A rest sts instance with id " + stsId +
                        " in realm " + realm + " not found.");
            }
            /*
            No route was found in the route table, but state was found in the SMS. Thus we are dealing with either the
            retry of a previously-failed deletion, or an illegal state.
             */
            if (!removeOnlyFromRouter) {
                logger.warn("A previously published STS instance with id " + stsId + " in realm " + realm +
                        " found in SMS, but not in route table. This indicates a previously failed deletion attempt." +
                        " Will attempt to remove from SMS.");
            } else {
                logger.error("A previously published STS instance with id " + stsId + " in realm " + realm +
                        " not found in router, but found in SMS, and the removeOnlyFromRouter is true, indicating a " +
                        "ServiceListener invoked deletion triggered by a deletion on another site server. " +
                        "This is an illegal state.");
            }
        } else {
            router.removeRoute(route);
        }
        if (!removeOnlyFromRouter) {
            persistentStore.removeSTSInstance(stsId, realm);
        }
    }

    public List<RestSTSInstanceConfig> getPublishedInstances() throws STSPublishException{
        return persistentStore.getAllPublishedInstances();
    }

    public RestSTSInstanceConfig getPublishedInstance(String stsId, String realm) throws STSPublishException {
        return persistentStore.getSTSInstanceConfig(stsId, realm);
    }

    /**
     * This method is only to be called by the RestSTSSetupListener, which calls it only to re-publish
     * previously-published Rest STS instances during OpenAM startup.
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

    public boolean isInstanceExposedInCrest(String stsId) {
        return publishedRoutes.get(normalizeDeploymentSubPathForRouteCache(normalizeDeploymentSubPath(stsId))) != null;
    }

    public boolean isInstancePersistedInSMS(String stsId, String realm) throws STSPublishException {
        return persistentStore.isInstancePresent(normalizeDeploymentSubPath(stsId), realm);
    }

    /**
     * This method is only called by the RestSTSSetupListener, called during OpenAM startup by the AMSetupServlet. It
     * registers a ServiceListener to publish rest-sts instances published to servers in another site.
     */
    public void registerServiceListener() {
        try {
            serviceListenerRegistration.registerServiceListener(AMSTSConstants.REST_STS_SERVICE_NAME,
                    AMSTSConstants.REST_STS_SERVICE_VERSION, serviceListener);
            logger.debug("In RestSTSInstancePublisherImpl ctor, successfully added ServiceListener for service "
                    + AMSTSConstants.REST_STS_SERVICE_NAME);
        } catch (STSInitializationException e) {
            final String message = "Exception caught registering ServiceListener in " +
                    "RestSTSInstancePublisherImpl#registerServiceListener. This means that rest-sts-instances published " +
                    "to other site instances will not be propagated to the rest-sts-instnace CREST router on this server." + e;
            logger.error(message, e);
        }
    }

    private String normalizeDeploymentSubPath(String deploymentSubPath) {
        if (deploymentSubPath.endsWith(AMSTSConstants.FORWARD_SLASH)) {
            return deploymentSubPath.substring(0, deploymentSubPath.lastIndexOf(AMSTSConstants.FORWARD_SLASH));
        }

        if (deploymentSubPath.startsWith(AMSTSConstants.FORWARD_SLASH)) {
            return deploymentSubPath.substring(1, deploymentSubPath.length());
        }
        return deploymentSubPath;
    }

    /*
    In a site deployment, the RestSTSPublishServiceListener will need to listen for rest-sts instance deletion events,
    and remove the rest-sts instance from the CREST router on all site servers other than the site server where the instance
    was actually deleted. But the serviceComponent identifying the subconfig entry corresponding to the rest-sts-instance state,
    passed to ServiceListener#organizationConfigChanged, is always lower-case. Thus, when a rest-sts instance is deleted in
    a site deployment, and the RestSTSPublishServiceListener is called, it needs to be able to remove the route by referencing
    a lower-case rest-identifier. This method insures that all cached routes are cached with a lower-case id. Note that this
    does mean that multiple rest-sts instances with the same deploymentUrl except for the casing, cannot be published to the
    same realm.
     */
    private String normalizeDeploymentSubPathForRouteCache(String deploymentSubPath) {
        return deploymentSubPath.toLowerCase();
    }
}
