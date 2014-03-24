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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

/**
 * Represents a general OAuth2 Exception. The specific subtypes should be used where appropriate.
 *
 * @since 12.0.0
 */
public class OAuth2Exception extends Exception {

    public OAuth2Exception() {
        super();
    }

    public OAuth2Exception(final String message) {
        super(message);
    }

    public OAuth2Exception(final String message, final Throwable cause) {
        super(message, cause);
    }

    public OAuth2Exception(final Throwable cause) {
        super(cause);
    }
}
