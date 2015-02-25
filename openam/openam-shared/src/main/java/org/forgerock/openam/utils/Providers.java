/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.utils;

/**
 * Static factory methods for getting pre-configured provider instances. Acts as a poor-man's Guice for use in non-Guice
 * aware code.
 *
 * @since 12.0.0
 */
public final class Providers {
    private Providers() {}

    /**
     * Gets a pre-configured Cipher provider that caches ciphers in a per-thread LRU cache.
     *
     * @param transformation the cipher transformation algorithm.
     * @param preferredProvider the preferred cipher provider.
     * @param maxSize the maximum size of the per-thread cipher cache. Should be sized to match thread pool size.
     * @return a per-thread caching JCE cipher provider.
     */
    public static CipherProvider cipherProvider(String transformation, String preferredProvider, int maxSize) {
        return new PerThreadCipherProvider(new JCECipherProvider(transformation, preferredProvider), maxSize);
    }

    /**
     * Gets a pre-configured DocumentBuilder provider that caches document builder instances in a per-thread LRU
     * cache. The created document builders are configured to avoid various entity expansion and remote DTD attacks.
     *
     * @param maxSize the maximum size of the per-thread cache. Should be sized to match thread pool size.
     * @return a per-thread caching, safe document builder provider.
     */
    public static DocumentBuilderProvider documentBuilderProvider(int maxSize) {
        return new PerThreadDocumentBuilderProvider(new SafeDocumentBuilderProvider(), maxSize);
    }

    /**
     * Gets a pre-configured SAXParser provider that caches parser instances in a per-thread LRU cache. The created
     * parsers are configured to avoid various entity expansion and remote DTD attacks.
     *
     * @param maxSize the maximum size of the per-thread cache. Should be sized to match thread pool size.
     * @return a per-thread caching, safe SAX parser provider.
     */
    public static SAXParserProvider saxParserProvider(int maxSize) {
        return new PerThreadSAXParserProvider(new SafeSAXParserProvider(), maxSize);
    }

    /**
     * Gets a basic TransformerFactory instance that can be used to create new Transformer objects.
     *
     * @param maxSize The maximum size of the per-thread cache. Should be sized to match thread pool size.
     * @return A per-thread caching, TransformerFactory provider.
     */
    public static TransformerFactoryProvider transformerFactoryProvider(final int maxSize) {
        return new PerThreadTransformerFactoryProvider(maxSize);
    }
}
