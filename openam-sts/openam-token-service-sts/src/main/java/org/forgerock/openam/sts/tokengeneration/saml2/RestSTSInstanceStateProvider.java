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

import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.publish.STSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceStateProvider
 * Caches state pulled from the persistent store for performance.
 * TODO: implement ServiceListener to invalidate cache entry if service updated. This will be done as part of the
 * work to persist published STS config state to the SMS.
 */
public class RestSTSInstanceStateProvider implements STSInstanceStateProvider<RestSTSInstanceState> {
    private final ConcurrentHashMap<String, RestSTSInstanceState> cachedRestInstanceConfigState;
    private final STSInstanceConfigPersister<RestSTSInstanceConfig> restStsInstanceConfigPersister;
    private final RestSTSInstanceStateFactory instanceStateFactory;
    private final Logger logger;

    @Inject
    RestSTSInstanceStateProvider(STSInstanceConfigPersister<RestSTSInstanceConfig> restStsInstancePersister,
                                 RestSTSInstanceStateFactory instanceStateFactory, Logger logger) {
        cachedRestInstanceConfigState = new ConcurrentHashMap<String, RestSTSInstanceState>();
        this.restStsInstanceConfigPersister = restStsInstancePersister;
        this.instanceStateFactory = instanceStateFactory;
        this.logger = logger;
    }

    public RestSTSInstanceState getSTSInstanceState(String instanceId, String realm) throws TokenCreationException, STSPublishException {
        RestSTSInstanceState cachedState = cachedRestInstanceConfigState.get(instanceId);
        if (cachedState == null) {
            RestSTSInstanceState createdState = createSTSInstanceState(instanceId, realm);
            RestSTSInstanceState concurrentlyCreatedState;
            if ((concurrentlyCreatedState = cachedRestInstanceConfigState.putIfAbsent(instanceId, createdState)) != null) {
                return concurrentlyCreatedState;
            } else {
                return createdState;
            }
        } else {
            return cachedState;
        }
    }

    private RestSTSInstanceState createSTSInstanceState(String instanceId, String realm) throws TokenCreationException, STSPublishException {
        logger.debug("Creating STSInstanceState for instanceId: " + instanceId);
        return instanceStateFactory.createRestSTSInstanceState(restStsInstanceConfigPersister.getSTSInstanceConfig(instanceId, realm));
    }
}
