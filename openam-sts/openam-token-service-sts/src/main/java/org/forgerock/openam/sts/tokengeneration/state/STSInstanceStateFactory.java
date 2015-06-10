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

import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.config.user.STSInstanceConfig;

/**
 * Defines the concerns of producing the STSInstanceState corresponding to a given rest/soap sts instance. The STSInstanceState
 * interface is an encapsulation of the STSInstanceConfig state, and a SAML2CryptoProvider and OpenIdConnectTokenPKIProvider
 * driven by this state.
 * This factory will be consulted to produce the instance state, which is then cached. Implementations of this interface
 * will, in turn, consume a STSInstanceConfigStore<T> instance to access the SMS, in order to obtain the STSInstanceConfig
 * necessary to produce an STSInstanceState instance.
 */
public interface STSInstanceStateFactory<S extends STSInstanceState, T extends STSInstanceConfig> {
    S createSTSInstanceState(T stsInstanceConfig) throws TokenCreationException;
}
