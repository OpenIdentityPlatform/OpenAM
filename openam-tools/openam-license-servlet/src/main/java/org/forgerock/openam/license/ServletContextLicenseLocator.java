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

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Locates license files using the {@link javax.servlet.ServletContext#getResourceAsStream(String)} method to locate
 * licenses within a web archive. This allows more flexibility than the {@link ClasspathLicenseLocator} in a web
 * application as the license may be located anywhere within the .war file and does not need to be on the classpath.
 *
 * @since 12.0.0
 */
public class ServletContextLicenseLocator extends ResourceLoaderLicenseLocator {
    private static final String[] DEFAULT_LICENSE_FILES = { "WEB-INF/legal-notices/license.txt" };
    private final ServletContext servletContext;

    /**
     * Constructs the license locator with the given servlet context, character set and license file names.
     *
     * @param servletContext the servlet context to use for locating license files.
     * @param charset the character set to use to decode license files.
     * @param licenseFiles the set of license file names to load.
     * @see ServletContext#getResourceAsStream(String)
     */
    public ServletContextLicenseLocator(ServletContext servletContext, Charset charset, String...licenseFiles) {
        super(charset, licenseFiles);

        if (servletContext == null) {
            throw new NullPointerException("ServletContext is null");
        }
        this.servletContext = servletContext;
    }

    /**
     * Constructs the license locator with the given servlet context, the platform default character set, and the
     * default license file name (WEB-INF/legal-notices/license.txt).
     *
     * @param servletContext the servlet context to use for locating license files.
     * @see ServletContext#getResourceAsStream(String)
     * @see Charset#defaultCharset()
     */
    public ServletContextLicenseLocator(ServletContext servletContext) {
        this(servletContext, Charset.defaultCharset(), DEFAULT_LICENSE_FILES);
    }

    /**
     * {@inheritDoc}
     *
     * Loads required licenses from the servlet context.
     *
     * @param resourceName the resource to load.
     * @return an input stream to the required license or null if not found.
     */
    @Override
    protected InputStream getResourceAsStream(String resourceName) {
        return servletContext.getResourceAsStream(resourceName);
    }
}
