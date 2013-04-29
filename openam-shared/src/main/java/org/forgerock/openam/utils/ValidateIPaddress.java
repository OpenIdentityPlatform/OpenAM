/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openam.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author alin.brici@forgerock.com
 */
public class ValidateIPaddress {
    private static Pattern IPv4Pattern = Pattern.compile("^(25[0-5]{1}\\.|"            // 250-255
            + "2[0-4]{1}[0-9]{1}\\.|"    // 200-249
            + "1[0-9]{2}\\.|"            // 100-199
            + "[1-9]{1}[0-9]{1}\\.|"        // 10-99
            + "[0-9]{1}\\.){3}"            // 0-9
            + "(25[0-5]{1}|"
            + "2[0-4]{1}[0-9]{1}|"
            + "1[0-9]{2}|"
            + "[1-9]{1}[0-9]{1}|"
            + "[0-9]{1}){1}$");

    private static Pattern IPv6Pattern = Pattern.compile(
            "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}"
                    + "(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
                    + "(.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){5}"
                    + "(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
                    + "(.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){4}"
                    + "(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
                    + "(.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}"
                    + "(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
                    + "(.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}"
                    + "(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
                    + "(.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}"
                    + "(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
                    + "(.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})"
                    + "|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])"
                    + "(.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:)))(%.+)?\\s*$");

    public ValidateIPaddress() {

    }

    /**
     * Determines if IP address is IP version 4
     * @param ipAddress is the IP address that is being verified
     * @return true if IP address is IPv4, false if it is not IPv4
     */
    public static boolean isIPv4(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        Matcher IPv4Matcher = IPv4Pattern.matcher(ipAddress);
        if (IPv4Matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if IP address is IP version 6
     * @param ipAddress is the IP address that is being verified
     * @return true if IP address is IPv4, false if it is not IPv4
     */
    public static boolean isIPv6(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        Matcher IPv6Matcher = IPv6Pattern.matcher(ipAddress);
        if (IPv6Matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines if an IP address is valid
     * @param ipAddress is the IP address that is being verified
     * @return true is the IP address is valid, false if it is not valid
     */
    public static boolean isValidIP(String ipAddress){
        if(ValidateIPaddress.isIPv4(ipAddress)) { // check if IPv4
            return true;
        } else if(ValidateIPaddress.isIPv6(ipAddress)){ // check if IPv6
            return true;
        } else { // not an IPv4 or IPv6
            return false;
        }
    }


}
