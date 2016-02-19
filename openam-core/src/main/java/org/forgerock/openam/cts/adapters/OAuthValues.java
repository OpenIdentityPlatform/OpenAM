/**
 * Copyright 2013 ForgeRock, Inc.
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
package org.forgerock.openam.cts.adapters;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

/**
 * Responsible for converting data types used by OAuth Tokens into appropriate Core Token Service values.
 *
 * @author robert.wapshott@forgerock.com
 */
public class OAuthValues {
    // Separator for serialising collections of Strings.
    private static final String SEPARATOR = ",";

    /**
     * @param value A collection of Strings.
     * @return A single string which stores the collection with a separator.
     */
    public String getSingleValue(Collection<String> value) {
        return StringUtils.join(value, SEPARATOR);
    }

    /**
     * @param value A single String to split into a collection.
     * @return A non null, but possibly empty collection.
     */
    public Collection<String> fromSingleValue(String value) {
        Collection<String> values = new ArrayList<String>();
        for (String s : StringUtils.split(value, SEPARATOR)) {
            values.add(s);
        }
        return values;
    }

    /**
     * @param values A Collection containing a single timestamp to convert.
     *               The timestamp must be in milliseconds from the epoch.
     * @return A Calendar that represents this timestamp.
     */
    public Calendar getDateValue(Collection<String> values) {
        if (values == null || values.size() != 1) {
            throw new IllegalArgumentException();
        }
        String dateString = values.iterator().next();
        long timestamp = Long.parseLong(dateString);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        return calendar;
    }

    /**
     * @param timestamp Calendar representing the timestamp.
     * @return A non null non empty collection of a single timestamp.
     */
    public Collection<String> fromDateValue(Calendar timestamp) {
        return Arrays.asList(Long.toString(timestamp.getTimeInMillis()));
    }
}
