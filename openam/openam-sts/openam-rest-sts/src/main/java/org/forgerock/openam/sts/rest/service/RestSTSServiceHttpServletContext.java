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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.service;

import org.forgerock.json.resource.AbstractContext;
import org.forgerock.json.resource.Context;

import javax.servlet.http.HttpServletRequest;

/**
 * An instance of this class will be returned by the RestSTSServiceHttpServletContextFactory class encapsulated in the
 * RestSTSServiceHttpServletContextFactoryProvider class, which is registered with CREST via the web.xml entry for the rest-sts,
 * and will be consulted as the HttpServletContextFactory to provide AbstractContext instances with HttpServletRequest
 * input state. This class is a replacement for the default org.forgerock.json.resource.SecurityContext implementation
 * provided by the default org.forgerock.json.resource.SecurityContextFactory, which is consulted when a
 * org.forgerock.json.resource.servlet.HttpServlet does not register a custom HttpServletContextFactory via the
 * context-factory-class init-param (see rest-sts servlet entry in web.xml for details on this entry).
 * This custom implementation is provided because the default SecurityContext
 * class did not reference the client-certs exposed via two-way tls, and adding this state to the SecurityContext seemed
 * semantically impure. See https://bugster.forgerock.org/jira/browse/CREST-173 for details.
 */
public class RestSTSServiceHttpServletContext extends AbstractContext {
    private static final String CONTEXT_NAME = "RestSTSServiceHttpServletContext";
    private final HttpServletRequest httpServletRequest;

    RestSTSServiceHttpServletContext(Context parent, HttpServletRequest httpServletRequest) {
        super(parent);
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public String getContextName() {
        return CONTEXT_NAME;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }
}
