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
package org.forgerock.openam.utils;

import javax.xml.transform.TransformerFactory;
import org.forgerock.util.Reject;

/**
 * A {@link TransformerFactoryProvider} that caches TransformerFactory instances in per-thread storage. To avoid using
 * an actual thread-local (and polluting any thread pools that may touch this), we instead maintain our own map from
 * thread id to TransformerFactory instances. As this is just a normal map, it can be garbage collected as normal so
 * there is no memory leak on hot-redeploy.
 *
 * @since 12.0.0
 */
class PerThreadTransformerFactoryProvider implements TransformerFactoryProvider {

    private final PerThreadCache<TransformerFactory, RuntimeException> transformerFactoryCache;

    PerThreadTransformerFactoryProvider(final int maxSize) {
        Reject.ifTrue(maxSize <= 0, "maxSize must be positive");
        transformerFactoryCache = new PerThreadCache<TransformerFactory, RuntimeException>(maxSize) {

            @Override
            protected TransformerFactory initialValue() {
                return TransformerFactory.newInstance();
            }
        };
    }

    @Override
    public TransformerFactory getTransformerFactory() {
        return transformerFactoryCache.getInstanceForCurrentThread();
    }
}