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

package org.forgerock.openam.sts;

/**
 * The CXF-STS engine throws only one exception type - the org.apache.cxf.ws.security.sts.provider.STSException, which
 * is a RuntimeException. I cannot throw one of my standard exceptions (e.g. TokenValidationException or TokenCreationException)
 * from the implementation of STS interfaces (e.g. TokenProvider, TokenValidator), as no checked exceptions are supported.
 * Yet I occasionally need to throw an exception, which I want to distinguish from the STSException - hence this class.
 */
public class AMSTSRuntimeException  extends RuntimeException {
    private final int errorCode;
    public AMSTSRuntimeException(int code, String message) {
        super(message);
        errorCode = code;
    }

    public AMSTSRuntimeException(int code, String message, Throwable cause) {
        super(message, cause);
        errorCode = code;

    }

    public int getCode() { return errorCode; }
}
