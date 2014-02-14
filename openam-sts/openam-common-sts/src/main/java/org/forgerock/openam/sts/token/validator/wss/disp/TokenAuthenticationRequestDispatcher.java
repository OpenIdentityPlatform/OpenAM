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

package org.forgerock.openam.sts.token.validator.wss.disp;

import org.restlet.representation.Representation;

import java.net.URI;
/**
 * This interface defines the functionality to dispatch the REST AuthN request for a specific Token type.
 */
public interface TokenAuthenticationRequestDispatcher<T> {
    /**
     *
     * @param uri The URI against which the request should be dispatched. It is effectively a URL, but the Restlet ClientResource
     *            expects a URI, not a URL instance.
     * @param token The token which will be dispatched to the OpenAM authN context.
     * @return The state corresponding to a successful invocation.
     * The Restlet ClientResource class throws a ResourceException - should probably put checked exception in signature.
     * Also should be clear on the semantics of this throw - is it for every non-200 response? TODO
     */
    Representation dispatch(URI uri, T token);
}
