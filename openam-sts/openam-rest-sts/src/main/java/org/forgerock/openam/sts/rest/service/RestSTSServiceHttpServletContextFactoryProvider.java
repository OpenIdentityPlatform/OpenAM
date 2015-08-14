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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.service;

import org.forgerock.http.Context;
import org.forgerock.http.context.RootContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.http.HttpContextFactory;

/**
 * This class name is specified under the context-factory-class param in the web.xml entry for the rest-sts servlet.
 * The default SecurityContextFactory produces an instance of the org.forgerock.json.resource.SecurityContext class,
 * which does not transfer the X509Certificate[] from the javax.servlet.request.X509Certificate attribute in the
 * HttpServletRequest to the SecurityContext. Adding this functionality to the default implementation introduces
 * semantic impurities, and because the rest-sts does not need the state provided by the default implementation, a
 * custom implementation seems the best course. See https://bugster.forgerock.org/jira/browse/CREST-173 for additional
 * details.
 *
 * The returned HttpServletContextFactory will be invoked to provide an AbstractContext with every CREST request.
 */
public class RestSTSServiceHttpServletContextFactoryProvider {
    static class RestSTSServiceHttpServletContextFactory implements HttpContextFactory {
        @Override
        public Context createContext(Context context, Request request) throws ResourceException {
            return new RestSTSServiceHttpServletContext(new RootContext(), request);
        }
    }
    public static HttpContextFactory getHttpServletContextFactory() {
        return new RestSTSServiceHttpServletContextFactory();
    }
}
