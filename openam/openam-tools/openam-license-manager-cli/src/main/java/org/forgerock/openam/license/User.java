/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.license;

/**
 * Simple interface for interacting with a user during click-through licensing. Allows for easy mocking of user
 * interaction for unit testing.
 *
 * @since 12.0.0
 */
public interface User {

    /**
     * Displays the given message to the user, localised to their language/locale.
     *
     * @param messageId the id of the localised message to display.
     */
    void tell(String messageId);

    /**
     * Directly shows a non-localised (or already localised) message to the user.
     *
     * @param message the message to display verbatim.
     */
    void show(String message);

    /**
     * Asks the user the given question and returns their answer as a string.
     *
     * @param questionId the message id of a question to ask the user.
     * @return the response as a string, or null if the user has disconnected.
     */
    String ask(String questionId);

    /**
     * Gets the name of the user.
     *
     * @return the name of the user.
     */
    String getName();

    /**
     * Get a localised message for display to this user.
     * @param messageId the id of the message to get.
     * @return the localised message.
     */
    String getMessage(String messageId);
}
