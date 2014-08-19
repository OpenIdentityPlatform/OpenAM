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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.router;

import org.forgerock.openam.rest.DefaultVersionBehaviour;

/**
 * Represents a rest router that has version behaviour.
 * @param <T> The router type, which will be returned for chaining purposes.
 */
public interface VersionedRouter<T> {
    /**
     * Sets the behaviour of the version routing process when the requested version is {@code null}.
     *
     * @see org.forgerock.openam.rest.service.VersionRouter#defaultToLatest()
     * @see org.forgerock.json.resource.VersionSelector#defaultToLatest()
     */
    T setVersioning(DefaultVersionBehaviour behaviour);

    /**
     * Sets whether or not to include a warning heading during the routing process when the
     * API version is not included in the request.
     *
     * @param warningEnabled Indicating whether to include the header or not.
     * @return The router of type T.
     */
    T setHeaderWarningEnabled(boolean warningEnabled);

}
