
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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.exceptions;

import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.Token;

import java.text.MessageFormat;

/**
 * Base Core Token Service exception for all sub types.
 *
 * This base exception performs two key roles. First provides an umbrella
 * exception for all CTS based exceptions to simplify the API design.
 *
 * Second, provides some automatic formatting of the message to make it
 * more readable in log files.
 */
public class CoreTokenException extends Exception {
    public CoreTokenException(String error, Throwable cause) {
        super(wrapMessage(error), cause);
    }

    public CoreTokenException(String error) {
        super(wrapMessage(error));
    }

    public CoreTokenException(String error, Token token) {
        super(wrapMessage(error, token));
    }

    public CoreTokenException(String error, Token token, Throwable cause) {
        super(wrapMessage(error, token), cause);
    }

    private static String wrapMessage(String msg) {
        return MessageFormat.format(
                "\n{0}{1}",
                CoreTokenConstants.DEBUG_HEADER,
                msg);
    }

    private static String wrapMessage(String msg, Token token) {
        return MessageFormat.format(
                "\n{0}{1}\n{2}",
                CoreTokenConstants.DEBUG_HEADER,
                msg,
                token);
    }
}
