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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.identity.idm;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentityRepository;

/**
 * Factory for creating {@code AMIdentityRepository} instances.
 */
public interface AMIdentityRepositoryFactory {

    /**
     * Creates a {@code AMIdentityRepository} instance.
     *
     * @param token The {@code SSOToken}.
     * @param realm The realm to create the instance in.
     * @return The {@code AMIdentityRepository} instance.
     */
    AMIdentityRepository create(SSOToken token, String realm);
}
