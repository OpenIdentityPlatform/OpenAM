/**
 * Copyright 2013 ForgeRock, Inc.
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
package org.forgerock.openam.cts.exceptions;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.Token;

import java.text.MessageFormat;

/**
 * Represents a failure to set the contents of an existing Token.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SetFailedException extends CoreTokenException {
    /**
     * Failed to set a Token.
     * @param token Non null Token being modified.
     * @param e Cause for the error.
     */
    public SetFailedException(Token token, Throwable e) {
        super(MessageFormat.format(
                    "\n" +
                    CoreTokenConstants.DEBUG_HEADER +
                    "Failed to set Token:\n" +
                    "{0}", token),
                e);
    }
}
