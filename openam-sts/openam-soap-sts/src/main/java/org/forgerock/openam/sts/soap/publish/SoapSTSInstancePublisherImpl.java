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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.publish;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.apache.cxf.endpoint.Server;
import org.forgerock.guava.common.collect.MapDifference;
import org.forgerock.guava.common.collect.Maps;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.soap.config.SoapSTSInjectorHolder;
import org.forgerock.openam.sts.soap.config.SoapSTSInstanceModule;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @see org.forgerock.openam.sts.soap.publish.SoapSTSInstancePublisher
 *
 */
public class SoapSTSInstancePublisherImpl implements SoapSTSInstancePublisher {
    /*
    This class encapsulates the SoapSTSInstanceConfig corresponding to a published instance, and the cxf.endpoint.Server
    instance associated with the exposed web-service, which is necessary to shut-down a previously-published soap
    web-service.

    This class will delegate equals and hashCode to the encapsulated soapSTSInstanceConfig, so that it can be provided to
    guava's Map difference algorithm, which is necessary to determine the set of added, removed, and modified soap-sts
    instances.
     */
    static final class ConfigAndServerHolder {
        private final SoapSTSInstanceConfig soapSTSInstanceConfig;
        // can be null
        private final Server server;
        ConfigAndServerHolder(SoapSTSInstanceConfig soapSTSInstanceConfig, Server server) {
            this.soapSTSInstanceConfig = soapSTSInstanceConfig;
            this.server = server;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ConfigAndServerHolder) {
                return soapSTSInstanceConfig.equals(((ConfigAndServerHolder)other).soapSTSInstanceConfig);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return soapSTSInstanceConfig.hashCode();
        }
    }

    /*
    Note that this map does not have to be concurrent as this class will only ever run in a single-threaded ScheduledExecutor.
     */
    private final Map<String, ConfigAndServerHolder> publishedAndExposedInstances;
    private final SoapSTSInstanceLifecycleManager soapSTSInstanceLifecycleManager;
    private final PublishServiceConsumer publishServiceConsumer;
    private final Logger logger;

    @Inject
    public SoapSTSInstancePublisherImpl(SoapSTSInstanceLifecycleManager soapSTSInstanceLifecycleManager,
                                        PublishServiceConsumer publishServiceConsumer,
                                        Logger logger) {
        publishedAndExposedInstances = new HashMap<String, ConfigAndServerHolder>();
        this.soapSTSInstanceLifecycleManager = soapSTSInstanceLifecycleManager;
        this.publishServiceConsumer = publishServiceConsumer;
        this.logger = logger;
    }

    @Override
    public void run() {
        //run method will be in a global try/catch(Exception) block because an uncaught exception will terminate subsequent runs
        try {
            Set<SoapSTSInstanceConfig> serverProvidedConfigurations;
            try {
                /*
                When the ScheduledExecutorService#shutdownNow is called, running threads will be interrupted. Respond ASAP.
                */
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                serverProvidedConfigurations = publishServiceConsumer.getPublishedInstances();
            } catch (ResourceException e) {
                /*
                Don't log to error - many log messages will be generated as this Runnable runs periodically, and this
                exception is thrown if the OpenAM mothership is down.
                */
                logger.warn("Soap sts instances published on the home OpenAM server cannot be exposed as a " +
                        "web-service because their configuration state could not be obtained from the home server. " +
                        "Exception: " + e, e);
                return;
            }
            /*
            When the ScheduledExecutorService#shutdownNow is called, running threads will be interrupted. Respond ASAP.
            */
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            /*
            Three cases must be handled:
            1. a new instance has been published
            2. an instances has been removed
            3. an instance has been updated
            The guava Maps#difference method will make this easy - first marshal the returned instances to map view
            equivalent to that maintained in publishedAndExposedInstances.
            */
            Map<String, ConfigAndServerHolder> serverProvidedConfigurationMap =
                    marshalPublishedConfigToCommonFormat(serverProvidedConfigurations);
            MapDifference<String, ConfigAndServerHolder> diff = Maps.difference(publishedAndExposedInstances, serverProvidedConfigurationMap);

            exposeNewInstances(diff.entriesOnlyOnRight().values());
            removeDeletedInstances(diff.entriesOnlyOnLeft().values());
            updateInstances(diff.entriesDiffering().values());
        } catch (Exception e) {
            logger.error("Unexpected exception caught in SoapSTSInstancePublisherImpl#run: " + e, e);
        }
    }

    /*
    The guava Diff library should operation on maps of the same types. This method marshals the collection of SoapSTSInstanceConfig
    instances provided by the OpenAM home server to the ConfigAndServerHolder construct used to determine which instances
    should be removed, which should be added, and which should be updated.
     */
    private Map<String, ConfigAndServerHolder> marshalPublishedConfigToCommonFormat(Set<SoapSTSInstanceConfig> publishedInstances) {
        HashMap<String, ConfigAndServerHolder> marshaledPublishedInstances = new HashMap<String, ConfigAndServerHolder>();
        for (SoapSTSInstanceConfig instanceConfig : publishedInstances) {
            marshaledPublishedInstances.put(instanceConfig.getDeploymentSubPath(), new ConfigAndServerHolder(instanceConfig, null));
        }
        return marshaledPublishedInstances;
    }

    private void exposeNewInstances(Collection<ConfigAndServerHolder> newlyPublishedInstances) {
        for (ConfigAndServerHolder entry : newlyPublishedInstances) {
            try {
                exposeNewInstance(entry);
            } catch (STSPublishException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private void exposeNewInstance(ConfigAndServerHolder configAndServerHolder) throws STSPublishException {
        publishInstance(configAndServerHolder.soapSTSInstanceConfig);
    }

    private void removeDeletedInstances(Collection<ConfigAndServerHolder> removedInstances) {
        for (ConfigAndServerHolder entry : removedInstances) {
            try {
                removeDeletedInstance(entry);
            } catch (STSPublishException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private void removeDeletedInstance(ConfigAndServerHolder configAndServerHolder) throws STSPublishException {
        try {
            soapSTSInstanceLifecycleManager.destroySTSInstance(configAndServerHolder.server);
        } catch (STSPublishException e) {
            /*
             Yes, we are catching and re-throwing a STSPublishException because the SoapSTSInstanceLifecycleManager does not
             have the state to create a specific-enough exception.
             */
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, "Could not remove soap sts instance: "
                    + configAndServerHolder.soapSTSInstanceConfig.getDeploymentSubPath() + "; Exception: " + e, e);
        }
        final String instanceId = configAndServerHolder.soapSTSInstanceConfig.getDeploymentSubPath();
        publishedAndExposedInstances.remove(instanceId);
        logger.info("Destroyed soap-sts instance " + instanceId);
    }

    private void updateInstances(Collection<MapDifference.ValueDifference<ConfigAndServerHolder>> updatedInstances) {
        for (MapDifference.ValueDifference<ConfigAndServerHolder> difference : updatedInstances) {
            try {
                removeDeletedInstance(difference.leftValue());
            } catch (STSPublishException e) {
                logger.error("Exception caught unpublishing to-be-updated instance " +
                        difference.leftValue().soapSTSInstanceConfig.getDeploymentSubPath() +
                        "; This means the updated instance cannot be exposed. The exception: " + e);
                continue;
            }
            try {
                exposeNewInstance(difference.rightValue());
            } catch (STSPublishException e) {
                logger.error("Exception caught exposing to-be-updated sts instance " +
                        difference.rightValue().soapSTSInstanceConfig.getDeploymentSubPath() +
                        ". This means that this instance is not available. Exception: " + e);
            }
        }
    }

    private void publishInstance(SoapSTSInstanceConfig instanceConfig) throws STSPublishException {
        Injector injector;
        try {
            injector = SoapSTSInjectorHolder.getInstance(Key.get(Injector.class))
                    .createChildInjector(new SoapSTSInstanceModule(instanceConfig));
            final Server server = soapSTSInstanceLifecycleManager.exposeSTSInstanceAsWebService(
                    injector.getInstance(Key.get(new TypeLiteral<Map<String, Object>>() {},
                            Names.named(AMSTSConstants.STS_WEB_SERVICE_PROPERTIES))),
                    injector.getInstance(SecurityTokenServiceProvider.class),
                    instanceConfig);
            publishedAndExposedInstances.put(instanceConfig.getDeploymentSubPath(),
                    new ConfigAndServerHolder(instanceConfig, server));
            //TODO: add the sts element, or whatever is exposed in web.xml to the log message?
            logger.info("The following soap-sts instance has been successfully exposed at "
                    + instanceConfig.getDeploymentSubPath() + ":\n" + instanceConfig);
        } catch (Exception e) {
            throw new STSPublishException(ResourceException.INTERNAL_ERROR, "Could not create injector corresponding to the " +
                    "to-be-published instance " + instanceConfig.getDeploymentSubPath() + "; The exception: " + e, e);
        }
    }
}
