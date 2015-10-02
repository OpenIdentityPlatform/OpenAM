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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */

/**
 * Implementations of handlers and related context object for receiving and processing radius server traffic including:
 *
 * <pre>
 *     An accept-all handler that always returns an Access-Accept packet for testing purposes only. See
 *     {@link org.forgerock.openam.radius.server.spi.handlers.AcceptAllHandler}.
 *
 *     A reject-all handler that always returns an Access-Reject packet for testing purposes only. See
 *     {@link org.forgerock.openam.radius.server.spi.handlers.RejectAllHandler}.
 *
 *     A handler that expects client configuration from the admin console to indicate the realm and chain to be used
 *     for authenticating users. It uses openAM's {@link com.sun.identity.authentication.AuthContext} to perform the
 *     authentication conversation and translates each callback into an Access-Challenge response which triggers a
 *     subsequent Access-Requests that contains the user entered answer for a callback. The handler then injects
 *     all such answers into the callbacks and submits them back to the
 *     {@link com.sun.identity.authentication.AuthContext} and repeats this pattern until all callback sets have been
 *     exhausted and authentication fails or succeeds.
 *     See {@link org.forgerock.openam.radius.server.spi.handlers.OpenAMAuthHandler}.
 *
 * </pre>
 *
 * Created by boydmr on 6/4/15.
 */
package org.forgerock.openam.radius.server.spi.handlers;

