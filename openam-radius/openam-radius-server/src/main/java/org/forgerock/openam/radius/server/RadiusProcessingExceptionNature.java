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

package org.forgerock.openam.radius.server;

/**
 * Enum that describes the nature of the exception.
 */
public enum RadiusProcessingExceptionNature {
    /**
     * Indicates that the exception is unrecoverable and no further responses will be sent. The likely course of
     * action on this kind of exception is to stop the RADIUS server.
     */
    CATASTROPHIC,

    /**
     * Indicates that this response is invalid and is unable to be sent. The likely course of action on this kind of
     * exception is to log the error and terminate the authentication from the client.
     */
    INVALID_RESPONSE,

    /**
     * Indicates that the exception is temporary and the response should be re-tried. This is likely due to a
     * network issue that may be resolved in the future. The likely course of action for this kind of exception is
     * to try again later.
     */
    TEMPORARY_FAILURE,
}