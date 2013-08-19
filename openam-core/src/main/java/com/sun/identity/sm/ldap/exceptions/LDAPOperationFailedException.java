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
import org.forgerock.opendj.ldap.responses.Result;

import java.text.MessageFormat;

/**
 * Indicates that an operation has failed. This is typically used to describe a failure whilst performing
 * some secondary processing and it wasn't clear what the original process was.
 *
 * @author robert.wapshott@forgerock.com
 */
public class LDAPOperationFailedException extends CoreTokenException {
    public LDAPOperationFailedException(Result result) {
        super(MessageFormat.format(
                    "\n" +
                    CoreTokenConstants.DEBUG_HEADER +
                    "Operation failed:\n" +
                    "Result Code: {0}\n" +
                    "Diagnostic Message: {1}\n" +
                    "Matched DN: {2}",
                    result.getResultCode(),
                    result.getDiagnosticMessage(),
                    result.getMatchedDN()),
                result.getCause());
    }

    public LDAPOperationFailedException(String error, Throwable cause) {
        super("\n" + CoreTokenConstants.DEBUG_HEADER + error, cause);
    }

    public LDAPOperationFailedException(String error) {
        super(error, (Throwable) null);
    }
}
