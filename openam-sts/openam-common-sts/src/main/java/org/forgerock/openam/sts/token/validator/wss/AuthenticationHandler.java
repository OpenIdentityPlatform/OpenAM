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

package org.forgerock.openam.sts.token.validator.wss;

import org.apache.ws.security.handler.RequestData;
import org.forgerock.openam.sts.TokenValidationException;

/**
 * Defines the authentication interface which hooks the wss4j token authenticators into the OpenAM authentication context.
 * @param <T> The to-be-authenticated token.
 */
public interface AuthenticationHandler<T> {
    void authenticate(RequestData data, T token) throws TokenValidationException;
}
