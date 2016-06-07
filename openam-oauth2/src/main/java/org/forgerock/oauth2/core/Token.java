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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.audit.AuditConstants;

import java.util.Map;

/**
 * Models a OAuth2 token.
 *
 * @since 12.0.0
 * @supported.all.api
 */
public interface Token {

    /**
     * Gets the token's identifier.
     *
     * @return The token's id.
     * @throws ServerException If any internal server error occurs.
     */
    String getTokenId() throws ServerException;

    /**
     * Gets the token's name.
     *
     * @return The token's name.
     */
    String getTokenName();

    /**
     * Converts the token into a {@code Map} of its key data.
     *
     * @return A {@code Map} of the token's key data.
     * @throws ServerException If any internal server error occurs.
     */
    Map<String, Object> toMap() throws ServerException;

    /**
     * Gets the token's information.
     *
     * @return A {@code Map} of the token's information.
     */
    Map<String, Object> getTokenInfo();

    /**
     * Gets the {@link JsonValue} representation of the token.
     *
     * @return The {@link JsonValue} representation of the token.
     */
    JsonValue toJsonValue();

    /**
     * Get the audit tracking ID for this token.
     * @return The tracking ID.
     */
    String getAuditTrackingId();

    /**
     * Get the audit tracking ID key for this token.
     * @return The tracking ID key.
     */
    AuditConstants.TrackingIdKey getAuditTrackingIdKey();
}
