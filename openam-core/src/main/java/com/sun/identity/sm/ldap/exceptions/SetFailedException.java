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
package com.sun.identity.sm.ldap.exceptions;

import com.sun.identity.sm.ldap.api.CoreTokenConstants;
import com.sun.identity.sm.ldap.api.tokens.Token;
import org.forgerock.opendj.ldap.requests.ModifyRequest;

import java.text.MessageFormat;

/**
 * Represents a failure during the setting of a tokens value.
 *
 * @author robert.wapshott@forgerock.com
 */
public class SetFailedException extends CoreTokenException {
    public SetFailedException(Token token, ModifyRequest diff, Throwable e) {
        super(MessageFormat.format(
                    "\n" +
                    CoreTokenConstants.DEBUG_HEADER +
                    "Failed to set Token:\n" +
                    "{0}\n" +
                    "Modify Request:\n" +
                    "{1}",
                    token,
                    diff),
                e);
    }

    public SetFailedException(Token token, Throwable e) {
        super(MessageFormat.format(
                    "\n" +
                    CoreTokenConstants.DEBUG_HEADER +
                    "Failed to set Token:\n" +
                    "{0}", token),
                e);
    }
}
