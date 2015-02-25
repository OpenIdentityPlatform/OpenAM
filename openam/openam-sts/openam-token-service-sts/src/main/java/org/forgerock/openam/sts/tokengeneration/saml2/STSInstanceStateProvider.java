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

/**
 * Defines concern related to obtaining the STSInstanceConfig state corresponding to the sts instance identifier.
 * Allows a single token-generation-service to generate STS-instance-specific tokens.
 *
 * The generic type corresponds to either RestSTSInstanceState or SoapSTSInstanceState (latter class still pending).
 */
public interface STSInstanceStateProvider<T> {
    /**
     * Returns the instance type specified by the generic type. Implementations of this interface will cache returned
     * state, so that the persistent store (SMS) does not have to be consulted every time, as each token generation for
     * a particular sts instance will have to consult sts instance-specific state.
     * @param instanceId the sts instance id
     * @param realm the realm in which this instance is deployed
     * @return An instance of the generic type (currently RestSTSInstanceState)
     * @throws TokenCreationException If an instance of the Generic type cannot be created
     * @throws STSPublishException If the persistent store throws an exception
     */
    T getSTSInstanceState(String instanceId, String realm) throws TokenCreationException, STSPublishException;

    /**
     * Because implementations may cache sts instance state, it must be possible for persistent-store listeners
     * (currently ServiceListener implementations) to invalidate cached entries
     * @param instanceId the sts instance id
     */
    void invalidateCachedEntry(String instanceId);
}
