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

package org.forgerock.openam.rest.dashboard;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.inject.Inject;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Resource;

/**
 * REST resource for a user's trusted devices.
 *
 * @since 12.0.0
 */
public class TrustedDevicesResource extends UserDevicesResource<TrustedDevicesDao> {

    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateFormat DATE_FORMATTER =
            SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private static final String LAST_SELECTED_DATE_KEY = "lastSelectedDate";

    /**
     * Constructs a new TrustedDevicesResource.
     *
     * @param dao An instance of the {@code TrustedDevicesDao}.
     */
    @Inject
    public TrustedDevicesResource(TrustedDevicesDao dao) {
        super(dao);
    }

    protected Resource convertValue(JsonValue profile) throws ParseException {
        final JsonValue lastSelectedDateJson = profile.get(LAST_SELECTED_DATE_KEY);
        final Date lastSelectedDate;
        final String formatted;

        if (lastSelectedDateJson.isString()) {
            synchronized (DATE_PARSER) {
                lastSelectedDate = DATE_PARSER.parse(lastSelectedDateJson.asString());
            }
        } else {
            lastSelectedDate = new Date(lastSelectedDateJson.asLong());
        }

        synchronized (DATE_FORMATTER) {
            formatted = DATE_FORMATTER.format(lastSelectedDate);
        }

        profile.put(LAST_SELECTED_DATE_KEY, formatted);
        return new Resource(profile.get("name").asString(), profile.hashCode() + "", profile);
    }
}
