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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.tokens;

/**
 * Responsible for defining the available token types in the Core Token Service.
 *
 * If new tokens are added, this enum must be updated via APPENDING to the end of the enum list.
 *
 * Existing operations MUST STAY in the order they are defined. This is validated by TokenTypeTest.
 */
public enum TokenType {

    /**
     * Session token type.
     */
    SESSION,
    /**
     * SAML2 token type.
     */
    SAML2,
    /**
     * OAuth token type.
     */
    OAUTH,
    /**
     * REST token type.
     */
    REST,
    /**
     * Generatic token type.
     */
    GENERIC,
    /**
     * OAuth Resource set token type.
     */
    RESOURCE_SET,
    /**
     * UMA Permission ticket token type.
     */
    PERMISSION_TICKET,
    /**
     * UMA Requesting party token type.
     */
    REQUESTING_PARTY,
    /**
     * UMA Audit entry token type.
     */
    UMA_AUDIT_ENTRY,
    /**
     * Session blacklist token type.
     */
    SESSION_BLACKLIST,
    /**
     * UMA Pending request token type.
     */
    UMA_PENDING_REQUEST,
    /**
     * STS token type.
     */
    STS
}
