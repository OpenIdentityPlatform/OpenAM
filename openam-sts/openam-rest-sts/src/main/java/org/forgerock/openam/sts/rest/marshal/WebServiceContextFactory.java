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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.marshal;

import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;

import javax.xml.ws.WebServiceContext;

/**
 * This interface defines a factory for creating WebServiceContext implementations. The cxf-sts expects a reference
 * to a WebServiceContext instance in the TokenValidatorParameter and TokenProviderParameter instances passed to the
 * TokenValidator and TokenProvider validate/issue operations. Because the REST-STS does not run in a web-service
 * context, where the container creates this instance, a factory must provide this functionality.
 */
public interface WebServiceContextFactory {
    /**
     * Provides the WebServiceContext expected by the TokenValidatorParameter and TokenProviderParameter classes.
     * @param httpContext The http-protocol specific values
     * @param restSTSServiceHttpServletContext The rest-st encapsulation of the HttpServiceRequest
     * @return a WebServiceContext instance
     */
    public WebServiceContext getWebServiceContext(HttpContext httpContext,
                                                  RestSTSServiceHttpServletContext restSTSServiceHttpServletContext);
}
