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

import org.forgerock.openam.cts.api.tokens.Token;

/**
 * Base Core Token Service exception for all sub types.
 *
 * @author robert.wapshott@forgerock.com
 */
public class CoreTokenException extends Exception {
    public CoreTokenException(String error, Throwable cause) {
        super(error, cause);
    }

    public CoreTokenException(String error) {
        super(error);
    }

    public CoreTokenException(String error, Token token) {
        super(error + "\n" + token);
    }

    public CoreTokenException(String error, Token token, Throwable cause) {
        super(error + "\n" + token, cause);
    }
}
