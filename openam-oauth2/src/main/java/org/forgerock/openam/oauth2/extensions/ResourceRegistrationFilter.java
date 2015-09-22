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

package org.forgerock.openam.oauth2.extensions;

import org.forgerock.oauth2.resources.ResourceSetDescription;

/**
 * Extension filter that will be called before and after resource sets are
 * registered.
 *
 * <p>Implementations of this interface can use the Guice setter based injection.</p>
 *
 * @since 13.0.0
 */
public interface ResourceRegistrationFilter extends Comparable<ResourceRegistrationFilter> {

    /**
     * Invoked before a resource set is registered in the backend.
     *
     * <p>Changes made to the {@literal resourceSet} object will be
     * persisted.</p>
     *
     * @param resourceSet The resource set that will be registered.
     */
    void beforeResourceRegistration(ResourceSetDescription resourceSet);

    /**
     * Invoked after a resource set is registered in the backend.
     *
     * <p>Changes made to the {@literal resourceSet} object will
     * <strong>not</strong> be persisted.</p>
     *
     * @param resourceSet The resource set that was registered.
     */
    void afterResourceRegistration(ResourceSetDescription resourceSet);
}
