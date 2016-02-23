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

import org.forgerock.json.JsonValue;
import org.forgerock.util.Reject;

/**
 * A class representing a message to be sent via a PushNotificationDelegate.
 */
public class PushMessage {

    private String recipient;
    private JsonValue data;

    /**
     * Create a new PushMessage.
     * @param recipient To whom the message will be addressed. May not be null.
     * @param data The data to contain within the message. May not be null.
     */
    public PushMessage(String recipient, JsonValue data) {
        Reject.ifNull(recipient);
        Reject.ifNull(data);

        this.recipient = recipient;
        this.data = data;
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
    public JsonValue getData() {
        return data;
    }
}
