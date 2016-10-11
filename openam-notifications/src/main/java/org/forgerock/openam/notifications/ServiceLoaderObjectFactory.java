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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.notifications;

import java.util.ServiceLoader;

import org.forgerock.util.Reject;

/**
 * Uses service loader to get an instance of {@link ObjectFactory} and delegates out
 * to that instance to retrieve instances for the requested class type.
 *
 * @since 14.0.0
 */
public enum ServiceLoaderObjectFactory implements ObjectFactory {

    INSTANCE;

    private ObjectFactory factory;

    ServiceLoaderObjectFactory() {
        ServiceLoader<ObjectFactory> loader = ServiceLoader.load(ObjectFactory.class);

        for (ObjectFactory factory : loader) {
            Reject.rejectStateIfTrue(this.factory != null, "Only expected one object factory");
            this.factory = factory;
        }

        Reject.ifNull(factory, "Object factory should not be null");
    }

    @Override
    public <T> T get(Class<T> tClass) {
        Reject.ifNull(tClass, "Object factory should not be null");
        return factory.get(tClass);
    }

}
