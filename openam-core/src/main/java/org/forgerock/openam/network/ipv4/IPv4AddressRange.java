/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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
 *
 */
package org.forgerock.openam.network.ipv4;

import java.util.StringTokenizer;
import com.sun.identity.shared.debug.Debug;

public class IPv4AddressRange {
    private static Debug debug = Debug.getInstance("IPRange");
    private long low;
    private long high;

    /**
     * build a range with 2 ips
     */
    public IPv4AddressRange() {
        low = 0;
        high = 0;
    }

    public IPv4AddressRange(String range) {
        StringTokenizer st = null;
        String s1 = null;
        String s2 = null;

        if (range.contains(":")) {
            debug.message("IPRange found : ");
            st = new StringTokenizer(range, ":");
            if (st.hasMoreTokens()) {
                s1 = (String) st.nextToken();
                s2 = (String) st.nextToken();
            }

            low = parseOctets(s1);
            high = parseOctets(s2);

        } else if (range.contains("/")) {
            debug.message("IPRange found / ");
            st = new StringTokenizer(range, "/");
            if (st.hasMoreTokens()) {
                s1 = (String) st.nextToken();
                s2 = (String) st.nextToken();
            }

            low = parseOctets(s1);

            int netMask = Integer.parseInt(s2);

            long bmask = (long) Math.pow(2, 32 - netMask) - 1;

            low = low & (bmask ^ -1L);
            high = low | bmask;

        } else {
            debug.message("IPRange found single IP ");
            low = parseOctets(range);
            high = low;
        }
    }

    @Override
    public String toString() {
        String retVal = "";
        retVal = Long.toHexString(low) + ":" + Long.toHexString(high);
        return retVal;
    }

    public boolean inRange(String ip) {
        IPv4AddressRange theIP = new IPv4AddressRange(ip);
        return inRange(theIP);
    }

    public boolean inRange(IPv4AddressRange ipr) {
        return ((this.low <= ipr.low) && (this.high >= ipr.high));
    }

    private long parseOctets(String s1) {
        long ipv4 = 0;

        StringTokenizer st = new StringTokenizer(s1, ".");
        while (st.hasMoreTokens()) {
            String s = (String) st.nextToken();
            int octet = Integer.parseInt(s);
            if (octet > 255) {
                debug.message("IPRange.parseOctets found invalid octet: " + s);
                octet = 0;
            }
            ipv4 = ipv4 * 256 + octet;
        }
        return ipv4;
    }
}
