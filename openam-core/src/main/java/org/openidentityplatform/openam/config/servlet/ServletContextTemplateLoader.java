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
 * Copyright 2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import jakarta.servlet.ServletContext;

import freemarker.cache.TemplateLoader;

/**
 * Loads {@code .ftl} sources from the webapp's {@code ServletContext} using
 * {@code jakarta.servlet}. FreeMarker's own {@code freemarker.cache.WebappTemplateLoader} cannot
 * be used here: even in the latest FreeMarker release it is hard-wired to {@code javax.servlet
 * .ServletContext} and was never ported to Jakarta EE 9, so it doesn't compile against this
 * project's servlet API. The now-removed vendored Click fork hit the identical problem with
 * Velocity Tools' {@code WebappResourceLoader} and carried its own Jakarta-clean copy for the
 * same reason; this loader is the FreeMarker equivalent, kept intentionally minimal since only
 * {@code getResource}/{@code getResourceAsStream} are needed (unlike that Velocity copy, no
 * macro/webapp-attribute plumbing is required).
 */
public class ServletContextTemplateLoader implements TemplateLoader {

    private final ServletContext servletContext;
    private final String basePath;

    public ServletContextTemplateLoader(ServletContext servletContext, String basePath) {
        this.servletContext = servletContext;
        this.basePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        String fullPath = basePath + "/" + name;
        URL url = servletContext.getResource(fullPath);
        return url == null ? null : fullPath;
    }

    @Override
    public long getLastModified(Object templateSource) {
        // Unknown; FreeMarker's Configuration#setTemplateUpdateDelay controls re-check frequency.
        return -1L;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        String path = (String) templateSource;
        InputStream stream = servletContext.getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Could not find template: " + path);
        }
        return new InputStreamReader(stream, encoding);
    }

    @Override
    public void closeTemplateSource(Object templateSource) {
        // No persistent handle is kept open between findTemplateSource and getReader.
    }
}
