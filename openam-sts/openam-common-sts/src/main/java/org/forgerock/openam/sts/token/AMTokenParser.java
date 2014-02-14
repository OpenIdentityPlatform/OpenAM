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

package org.forgerock.openam.sts.token;

import org.forgerock.openam.sts.TokenValidationException;
import org.restlet.representation.Representation;

/**
 * This interface defines the ability to take the Representation result of a dispatched REST authN request and to
 * return a AM session id.
 */
public interface AMTokenParser {
    /**
     *
     * @param representation The value returned by a successful invocation of the ClientResource corresponding to the
     *                       authentication request.
     * @return Returns the string corresponding to the OpenAM session.
     * @throws TokenValidationException Thrown when authentication unsuccessful or the OpenAM session id could not be pulled from the response.
     */
    String getSessionFromAuthNResponse(Representation representation) throws TokenValidationException;
}
