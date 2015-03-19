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

package org.forgerock.openam.rest.sms;

import java.util.Map;

import org.forgerock.json.fluent.JsonValue;

import com.sun.identity.sm.ServiceSchema;

// Will be replaced by output from AME-6083
public class SmsJsonConverter {

    SmsJsonConverter(ServiceSchema schema) {}

    JsonValue toJson(Map<String, Object> data) {
        return JsonValue.json(data);
    }

    Map<String, Object> fromJson(JsonValue value) {
        return value.asMap();
    }
}
