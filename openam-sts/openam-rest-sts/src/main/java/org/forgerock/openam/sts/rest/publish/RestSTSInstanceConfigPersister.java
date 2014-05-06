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

import org.forgerock.openam.sts.publish.STSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see org.forgerock.openam.sts.publish.STSInstanceConfigPersister
 *
 * It may be that this class will ultimately reference the SMS in OpenAM to persist(remove) the json corresponding to the
 * STSInstanceConfig. For now, the implementation remains empty. And it will likely only persist to the SMS, as the
 * publish service will only be deployed with OpenAM - so I don't have to worry about filesystem persistence for a
 * remote STS deployment. Thus some startup context associated with the REST-STS will simply perform a GET on the
 * REST STS Publish service, which will consult the STSInstancePersister, to pull all of the RestSTSInstanceConfigs out
 * of the SMS.
 *
 * TODO: right now, this class is just implementing an in-memory caching of RestSTSInstanceConfig state. This is just
 * so published Rest STS instances can be referenced by invocations against the token generation service. Soon it will
 * hit the SMS. At that point, some checked-exceptions will be thrown if entries cannot be found, etc.
 *
 * A distinct instance of this class will be bound in the rest-sts and in the token-gen service. Once they both
 * pull state from the SMS, that will be irrelevant, but now that the state is pulled from an in-memory map, this
 * reference has to be static to be visible across both instances. Cheesy, but temporary, to be replaced this class
 * persists to the SMS.
 *
 */
public class RestSTSInstanceConfigPersister implements STSInstanceConfigPersister<RestSTSInstanceConfig> {
    private static final ConcurrentHashMap<String, RestSTSInstanceConfig> configStore = new ConcurrentHashMap<String, RestSTSInstanceConfig>();
    private final Logger logger;

    @Inject
    public RestSTSInstanceConfigPersister(Logger logger)  {
        this.logger = logger;
    }

    public void persistSTSInstance(String key, RestSTSInstanceConfig instance) {
        /*
        Not worried about threading issues - this implementation is temporary.
         */
        if (configStore.get(key) != null) {
            throw new IllegalStateException("RestSTSInstanceConfig for key " + key + " already present!");
        }
        configStore.put(key, instance);
        logger.info("Persisted RestSTSInstanceConfig corresponding to key " + key);
    }

    public void removeSTSInstance(String key) {
        configStore.remove(key);
    }

    public RestSTSInstanceConfig getSTSInstanceConfig(String key) {
        RestSTSInstanceConfig config = configStore.get(key);
        if (config ==  null) {
            throw new IllegalStateException("No RestSTSInstanceConfig corresponding to key " + key);
        }
        return config;
    }

    public List<RestSTSInstanceConfig> getAllPublishedInstances() {
        return Collections.unmodifiableList(new ArrayList<RestSTSInstanceConfig>(configStore.values()));
    }
}
