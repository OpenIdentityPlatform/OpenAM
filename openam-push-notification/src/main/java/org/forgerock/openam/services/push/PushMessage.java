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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.services.push;

import java.util.UUID;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.Reject;

/**
 * A class representing a message to be sent via a PushNotificationDelegate.
 */
public class PushMessage {

    /** Key for locating the message ID inside an OpenAM Push message. */
    public static final String MESSAGE_ID = "messageId";

    private final String recipient;
    private final String body;
    private final String subject;
    private final String messageId;

    /**
     * Create a new PushMessage.
     * @param recipient To whom the message will be addressed. May not be null.
     * @param body The message to contain within the message. May not be null.
     * @param subject The subject to contain within the message. May not be null.
     * @param messageId The messageId which defines the message's unique reference. If null, a random messageId will
     *                  be generated.
     */
    public PushMessage(String recipient, String body, String subject, String messageId) {
        Reject.ifNull(recipient);
        Reject.ifNull(body);
        Reject.ifNull(subject);

        this.recipient = recipient;
        this.body = body;
        this.subject = subject;

        if (messageId == null) {
            this.messageId = UUID.randomUUID().toString() + Time.currentTimeMillis();
        } else {
            this.messageId = messageId;
        }
    }

    /**
     * Create a new PushMessage with a default-generated messageId.
     * @param recipient To whom the message will be addressed. May not be null.
     * @param body The data to contain within the message. May not be null.
     * @param subject The subject to contain within the message. May not be null.
     */
    public PushMessage(String recipient, String body, String subject) {
        this(recipient, body, subject, null);
    }

    /**
     * Retrieve the recipient of this message.
     * @return The intended recipient of this message.
     */
    public String getRecipient() {
        return recipient;
    }

    /**
     * Retrieve the contents of this message.
     * @return The data stored within this message.
     */
    public String getBody() {
        return body;
    }

    /**
     * Retrieve the subject of this message.
     * @return The subject stored within this message.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Retrieve the message id of this message.
     *
     * @return The message id which was generated at creation of this message instance.
     */
    public String getMessageId() {
        return messageId;
    }
}
