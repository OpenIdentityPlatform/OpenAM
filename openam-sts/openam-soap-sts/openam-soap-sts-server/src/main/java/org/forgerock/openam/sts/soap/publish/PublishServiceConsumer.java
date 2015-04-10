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

import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import java.util.Set;

/**
 * Defines the concerns of performing a GET on the sts-publish service to obtain SoapSTSInstanceConfig instances
 * corresponding to published soap-sts instances.
 */
public interface PublishServiceConsumer {
    /**
     *
     * @return a non-null list containing the published instances (empty if no instances have been published)
     */
    Set<SoapSTSInstanceConfig> getPublishedInstances() throws ResourceException;
}
