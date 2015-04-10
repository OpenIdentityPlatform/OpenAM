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

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import java.util.Map;

/**
 * This interface defines and encapsulates the concerns of consuming the cxf runtime to programmatically publish and
 * destroy soap sts instances.
 */
public interface SoapSTSInstanceLifecycleManager {
    /**
     *
     * @param webServiceProperties The properties provided to the soap-sts instance. Obtained from the injector driven by
     *                             a SoapSTSInstanceConfig instance. Defines things like the crypto context, and the validators
     *                             plugged into the SecurityPolicy context.
     * @param securityTokenServiceProvider The actual implementation of the STS. Also obtained from the injector driven
     *                                     by SoapSTSInstanceConfig instance.
     * @param stsInstanceConfig The actual SoapSTSInstanceConfig corresponding to the to-be-published instance
     * @return the cxf.endpoint.Server instance returned from the cxf web-service create invocation. Must be passed to the
     * destorySTSInstance.
     * @throws STSPublishException If the operation cannot be successfully completed.
     */
    public Server exposeSTSInstanceAsWebService(Map<String, Object> webServiceProperties,
                                                 SecurityTokenServiceProvider securityTokenServiceProvider,
                                                 SoapSTSInstanceConfig stsInstanceConfig) throws STSPublishException;

    /**
     * Deletes the soap-sts web-service.
     * @param server the instance returned by the corresponding exposeSTSInstanceAsWebService invocation.
     * @throws STSPublishException If the operation cannot be successfully completed.
     */
    public void destroySTSInstance(Server server) throws STSPublishException;
}
