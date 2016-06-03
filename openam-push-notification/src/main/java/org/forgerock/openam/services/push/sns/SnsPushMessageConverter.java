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
package org.forgerock.openam.services.push.sns;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.services.push.PushMessage.*;

import com.google.inject.Singleton;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushMessageConverter;

/**
 * Converts a basic message String into a format appropriate for passing into the SNS.
 * This ensures that the priority is set correctly in GCM-land, and configures the appropriate
 * settings for everything else.
 */
@Singleton
public class SnsPushMessageConverter implements PushMessageConverter {

    private final static String GCM = "GCM";
    private final static String GCM_PRIORITY = "PRIORITY";
    private final static String GCM_HIGH_PRIORITY = "high";
    private final static String GCM_DATA = "data";
    private final static String GCM_MESSAGE = "message";

    private final static String APNS = "APNS";
    private final static String APNS_SANDBOX = "APNS_SANDBOX";
    private final static String APNS_APS = "aps";
    private final static String APNS_ALERT = "alert";
    private final static String APNS_CONTENT_AVAILABLE = "content-available";
    private final static String APNS_CONTENT_AVAILABLE_TRUE = "1";
    private final static String APNS_DATA = "data";
    private final static String APNS_SOUND = "sound";
    private final static String APNS_DEFAULT_SOUND = "default";

    private final static String DEFAULT = "default";

    @Override
    public String toTransferFormat(PushMessage message) {

        JsonValue gcm = json(object(
                field(GCM_PRIORITY, GCM_HIGH_PRIORITY),
                field(GCM_DATA, object(
                        field(GCM_MESSAGE, message.getBody()),
                        field(MESSAGE_ID, message.getMessageId())))));

        JsonValue apple = json(object(
                field(APNS_APS, object(
                        field(APNS_ALERT, message.getSubject()),
                        field(APNS_SOUND, APNS_DEFAULT_SOUND),
                        field(MESSAGE_ID, message.getMessageId()),
                        field(APNS_DATA, message.getBody()),
                        field(APNS_CONTENT_AVAILABLE, APNS_CONTENT_AVAILABLE_TRUE)))));

        JsonValue toSend = json(object(
                field(DEFAULT, message.getSubject()),
                field(GCM, gcm.toString()),
                field(APNS, apple.toString()),
                field(APNS_SANDBOX, apple.toString())
        ));

        return toSend.toString();
    }
}
