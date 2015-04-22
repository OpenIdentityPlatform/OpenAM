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
 * Copyright 2013-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator.disp;

import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenValidationException;

import java.net.URL;

/**
 * This interface defines the functionality to dispatch the REST AuthN request for a specific Token type.
 */
public interface TokenAuthenticationRequestDispatcher<T> {
    /**
     *
     * @param url The URL against which the request should be dispatched.
     * @param authTarget Necessary to access the Map<String, Object> which can contain necessary context information - e.g. configured
     *                   parameters for the targeted authN module. This reference can be null.
     * @param token The token which will be dispatched to the OpenAM authN context.
     * @return The state corresponding to a successful invocation. Produced by the OpenAM rest-authN context.
     * Includes the OpenAM session id.
     * @throws org.forgerock.openam.sts.TokenValidationException if an error occurred in the invocation, or if a non-200
     * result is returned.
     */
    String dispatch(URL url, AuthTargetMapping.AuthTarget authTarget, T token) throws TokenValidationException;
}
