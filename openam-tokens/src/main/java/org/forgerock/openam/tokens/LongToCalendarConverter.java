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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.tokens;

import static java.util.TimeZone.*;

import java.util.Calendar;

/**
 * A custom converter that converted {@code Long}s to {@code Calendar}s.
 *
 * @since 13.0.0
 */
public class LongToCalendarConverter implements Converter<Long, Calendar> {

    @Override
    public Calendar convertFrom(Long aLong) {
        if (aLong == null) {
            return null;
        }
        Calendar result = org.forgerock.openam.utils.Time.getCalendarInstance(getTimeZone("UTC"));
        result.setTimeInMillis(aLong);
        return result;
    }

    @Override
    public Long convertBack(Calendar calendar) {
        return calendar == null ? null : calendar.getTimeInMillis();
    }
}
