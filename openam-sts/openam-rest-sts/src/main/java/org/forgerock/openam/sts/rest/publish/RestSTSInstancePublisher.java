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

import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;
import org.forgerock.openam.sts.rest.RestSTS;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;

import java.util.List;

/**
 * Defines the interface consumed to publish a Rest STS instance, and to remove this instance once its functionality
 * should no longer be exposed.
 *
 * It may well be that this interface should be enhanced to handle a GET to the RestSTSPublishService, which will
 * return the RestSTSInstanceConfig instances corresponding to all published REST STS instances. This method would then
 * be consumed by a servlet-context-listener (or similar startup context) associated with the REST STS deployment. This
 * would allow REST STS instances to re-constitute themselves after a server restart regardless of whether they are deployed
 * locally (in the OpenAM .war) or remotely (in their own .war).
 */
public interface RestSTSInstancePublisher {
    void publishInstance(RestSTSInstanceConfig instanceConfig, RestSTS instance, String subPath) throws STSInitializationException;
    void removeInstance(String subPath) throws IllegalArgumentException;

    /**
     * Called to obtain the configuration elements corresponding to previously-published STS instances.
     * @return The STSInstanceConfig super-class (RestSTSInstanceConfig or SoapSTSInstanceConfig) instances corresponding
     * to published STS instances.
     */
    List<RestSTSInstanceConfig> getPublishedInstances();
}
