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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core.exceptions;

/**
 * An exception for when the resource requested requires a higher scope than the token supplied provides.
 * @see <a href="http://tools.ietf.org/html/rfc6750#section-3.1">RFC 6750, Bearer Token - Error Codes</a>
 */
public class InsufficientScopeException extends OAuth2Exception {

    public InsufficientScopeException(String requiredScope) {
        super(403, "insufficient_scope", "The resource requested requires scope: " + requiredScope);
    }

}
