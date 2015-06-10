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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.tokengeneration.state;

import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.rest.ServiceListenerRegistration;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.tokengeneration.config.TokenGenerationModule;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @see STSInstanceStateProvider
 * @see STSInstanceStateProviderBase
 */
public class SoapSTSInstanceStateProvider extends STSInstanceStateProviderBase<SoapSTSInstanceConfig, SoapSTSInstanceState> {

    @Inject
    SoapSTSInstanceStateProvider(STSInstanceConfigStore<SoapSTSInstanceConfig> restStsInstanceStore,
                                 STSInstanceStateFactory<SoapSTSInstanceState, SoapSTSInstanceConfig> instanceStateFactory,
                                 ServiceListenerRegistration serviceListenerRegistration,
                                 @Named(TokenGenerationModule.SOAP_STS_INSTANCE_STATE_LISTENER)ServiceListener serviceListener,
                                 Logger logger) {
        super(restStsInstanceStore, instanceStateFactory, serviceListenerRegistration, serviceListener,
                AMSTSConstants.SOAP_STS_SERVICE_NAME, AMSTSConstants.SOAP_STS_SERVICE_VERSION, logger);
    }
}
