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
* Portions Copyrighted 2024 3A Systems LLC.
*/
package org.forgerock.openam.cors;

import org.forgerock.guice.core.InjectorHolder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A Servlet Filter implementation of the CORS Service, initialized using a FilterConfig.
 *
 * This Filter forces requests to abide by the CORS standard using the {@link CORSService}.
 */
public class CORSFilter implements Filter {

    /**
     * Required default constructor
     */
    public CORSFilter() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse res = (HttpServletResponse) response;

        CORSConfigListener corsConfigListener = InjectorHolder.getInstance(CORSConfigListener.class);

        if (corsConfigListener.getCorsService().handleRequest(req, res)) {
            chain.doFilter(req, res);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {

    }
}
