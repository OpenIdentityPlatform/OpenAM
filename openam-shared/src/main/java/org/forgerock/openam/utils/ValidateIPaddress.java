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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates IP V4 and V6 addresses.
 */
public final class ValidateIPaddress {

    private static final Pattern IP_V4_PATTERN = Pattern.compile(
              "^(25[0-5]{1}\\.|"         // 250-255
            + "2[0-4]{1}[0-9]{1}\\.|"    // 200-249
            + "1[0-9]{2}\\.|"            // 100-199
            + "[1-9]{1}[0-9]{1}\\.|"     // 10-99
            + "[0-9]{1}\\.){3}"          // 0-9
            + "(25[0-5]{1}|"
            + "2[0-4]{1}[0-9]{1}|"
            + "1[0-9]{2}|"
            + "[1-9]{1}[0-9]{1}|"
            + "[0-9]{1}){1}$");

    private static final Pattern IP_V6_PATTERN = Pattern.compile(
              "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}"
            + "(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
            + "(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){5}"
            + "(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
            + "(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){4}"
            + "(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
            + "(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}"
            + "(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
            + "(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}"
            + "(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
            + "(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}"
            + "(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
            + "(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})"
            + "|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
            + "(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:)))(%\\.+)?\\s*$");

    private ValidateIPaddress() {
    }

    /**
     * Determines if IP address is IP version 4.
     * @param ipAddress is the IP address that is being verified
     * @return true if IP address is IPv4, false if it is not IPv4
     */
    public static boolean isIPv4(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        Matcher ipv4Matcher = IP_V4_PATTERN.matcher(ipAddress);
        return ipv4Matcher.find();
    }

    /**
     * Determines if IP address is IP version 6.
     * @param ipAddress is the IP address that is being verified
     * @return true if IP address is IPv4, false if it is not IPv4
     */
    public static boolean isIPv6(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        Matcher ipv6Matcher = IP_V6_PATTERN.matcher(ipAddress);
        return ipv6Matcher.find();
    }

    /**
     * Determines if an IP address is valid.
     * @param ipAddress is the IP address that is being verified
     * @return true is the IP address is valid, false if it is not valid
     */
    public static boolean isValidIP(String ipAddress) {
        return ValidateIPaddress.isIPv4(ipAddress) || ValidateIPaddress.isIPv6(ipAddress);
    }
}
