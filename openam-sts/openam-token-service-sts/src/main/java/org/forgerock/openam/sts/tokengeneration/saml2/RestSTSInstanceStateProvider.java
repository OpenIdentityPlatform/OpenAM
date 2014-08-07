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

package org.forgerock.openam.sts.tokengeneration.saml2;

import com.sun.identity.sm.ServiceListener;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AMSTSRuntimeException;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.rest.ServiceListenerRegistration;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.tokengeneration.config.TokenGenerationModule;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceStateProvider
 * Caches state pulled from the persistent store for performance. Registers a ServiceListener in the ctor so that
 * cache entries can be invalidated when the corresponding service is updated.
 */
public class RestSTSInstanceStateProvider implements STSInstanceStateProvider<RestSTSInstanceState> {
    private final ConcurrentHashMap<String, RestSTSInstanceState> cachedRestInstanceConfigState;
    private final STSInstanceConfigStore<RestSTSInstanceConfig> restStsInstanceConfigStore;
    private final RestSTSInstanceStateFactory instanceStateFactory;
    private final Logger logger;

    @Inject
    RestSTSInstanceStateProvider(STSInstanceConfigStore<RestSTSInstanceConfig> restStsInstanceStore,
                                 RestSTSInstanceStateFactory instanceStateFactory,
                                 ServiceListenerRegistration serviceListenerRegistration,
                                 @Named(TokenGenerationModule.REST_STS_INSTANCE_STATE_LISTENER)ServiceListener serviceListener,
                                 Logger logger) {
        cachedRestInstanceConfigState = new ConcurrentHashMap<String, RestSTSInstanceState>();
        this.restStsInstanceConfigStore = restStsInstanceStore;
        this.instanceStateFactory = instanceStateFactory;
        this.logger = logger;
        /*
        Add the ServiceListener when the caching layer is initialized - i.e. in this ctor.
         */
        try {
            serviceListenerRegistration.registerServiceListener(AMSTSConstants.REST_STS_SERVICE_NAME,
                    AMSTSConstants.REST_STS_SERVICE_VERSION, serviceListener);
            logger.debug("In RestSTSInstanceStateProvider ctor, successfully added ServiceListener for service "
                    + AMSTSConstants.REST_STS_SERVICE_NAME);
        } catch (STSInitializationException e) {
            final String message = "Exception caught registering ServiceListener in the RestSTSInstanceStatePersister: " + e;
            logger.error(message, e);
            throw new AMSTSRuntimeException(ResourceException.INTERNAL_ERROR, message, e);
        }
    }

    public RestSTSInstanceState getSTSInstanceState(String instanceId, String realm) throws TokenCreationException, STSPublishException {
        /*
        The instanceId can include upper-case characters. Yet the id of the rest-sts instance, as indicated to registered
        ServiceListeners, is all lower-case. So the entry in the map should be always be lower-case.
         */
        final String lowerCaseInstanceId = instanceId.toLowerCase();
        RestSTSInstanceState cachedState = cachedRestInstanceConfigState.get(lowerCaseInstanceId);
        if (cachedState == null) {
            RestSTSInstanceState createdState = createSTSInstanceState(instanceId, realm);
            RestSTSInstanceState concurrentlyCreatedState;
            if ((concurrentlyCreatedState = cachedRestInstanceConfigState.putIfAbsent(lowerCaseInstanceId, createdState)) != null) {
                return concurrentlyCreatedState;
            } else {
                return createdState;
            }
        } else {
            return cachedState;
        }
    }

    public void invalidateCachedEntry(String instanceId) {
        if (cachedRestInstanceConfigState.remove(instanceId) != null) {
            logger.debug("In RestSTSInstanceStateProvider, removed cached instance state for instance " + instanceId);
        }
    }

    private RestSTSInstanceState createSTSInstanceState(String instanceId, String realm) throws TokenCreationException, STSPublishException {
        logger.debug("Creating STSInstanceState for instanceId: " + instanceId);
        return instanceStateFactory.createRestSTSInstanceState(restStsInstanceConfigStore.getSTSInstanceConfig(instanceId, realm));
    }
}
