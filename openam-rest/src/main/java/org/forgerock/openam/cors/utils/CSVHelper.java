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
package org.forgerock.openam.cors.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Used to switch CSVs between Strings and Lists, while trimming spaces.
 */
public class CSVHelper {

    /**
     * Converts a String with CSV values in it to a set of strings,
     * having trimmed any whitespace from the CSVs.
     *
     * @param csv Comma-seperated value string
     * @param lowerCase true if we should put all values to lower case
     * @return a list wherein each element is a trimmed, single member of the CSV
     */
    public List<String> csvStringToList(final String csv, final boolean lowerCase) {

        if (csv == null) {
            return new ArrayList<String>();
        }

        final String[] split = csv.split(",");

        for(int i = 0; i < split.length; i++) {
            if (lowerCase) {
                split[i] = split[i].toLowerCase().trim();
            } else {
                split[i] = split[i].trim();
            }
        }

        return new ArrayList<String>(Arrays.asList(split));
    }

    /**
     * Converts a List of strings into a CSV string.
     *
     * @param list The set of strings to concatenate into a CSV
     * @return A CSV string containing the values in the supplied list
     */
    public String listToCSVString(final List<String> list) {

        final StringBuilder sb = new StringBuilder();

        if (list == null) {
            return sb.toString();
        }

        for(int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));

            if (i != list.size() - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }
}
