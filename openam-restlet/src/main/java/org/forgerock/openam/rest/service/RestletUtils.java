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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.service;

import org.forgerock.guice.core.InjectorHolder;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;

import com.google.inject.Key;

/**
 * Utility methods for dealing with Restlet resources.
 */
public class RestletUtils {

    private RestletUtils() {}

    /**
     * Creates a Finder instance that returns an injected instance of a resource. Prevents multiple instances of
     * the same resource class being created unnecessarily.
     * @param resource The type of the resource.
     * @return The injected instance.
     */
    public static Finder wrap(final Class<? extends ServerResource> resource) {
        return wrap(Key.get(resource));
    }

    /**
     * Creates a Finder instance that returns the specified instance of a resource. Prevents multiple instances of
     * the same resource class being created unnecessarily.
     * @param resource The resource.
     * @return The injected instance.
     */
    public static Finder wrap(final Key<? extends ServerResource> resource) {
        return new Finder() {
            @Override
            public ServerResource create(Request request, Response response) {
                return InjectorHolder.getInstance(resource);
            }
        };
    }

}
