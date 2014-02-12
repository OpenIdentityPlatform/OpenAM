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

package org.forgerock.openam.license;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Loads required licenses from the classpath with well-known names.
 *
 * @see ClassLoader#getSystemResourceAsStream(String)
 * @since 12.0.0
 */
public class ClasspathLicenseLocator extends ResourceLoaderLicenseLocator {
    private final ClassLoader classLoader;

    /**
     * Constructs a classpath license locator with the given classloader, charset and list of license files to load.
     *
     * @param classLoader the classloader to use for locating licenses on the classpath.
     * @param charset the charset to use for decoding license files.
     * @param licenseFiles the list of license file names to load.
     */
    public ClasspathLicenseLocator(ClassLoader classLoader, Charset charset, String...licenseFiles) {
        super(charset, licenseFiles);
        if (classLoader == null) {
            throw new NullPointerException("ClassLoader is null");
        }

        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     *
     * Loads licenses from the configured classloader.
     *
     * @param resourceName the resource to load.
     * @return an input stream to the license file, or null if not found.
     */
    @Override
    protected InputStream getResourceAsStream(String resourceName) {
        return classLoader.getResourceAsStream(resourceName);
    }
}
