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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common;

/**
 * Holder of patterns found in RFC 3865 and used in unit tests.
 */
public final class Rfc2865Examples {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Rfc2865Examples() {

    }

    /**
     * A shared secret.
     */
    public static String secret = "xyzzy5461";
    /**
     * A user password.
     */
    public static String password = "arctangent";

    /**
     * Example 7.1 packet in spaced hexadecimal form.
     */
    public static class Ex71 {
        public static String spacedHex = "01 00 00 38 0f 40 3f 94 73 97 80 57 bd 83 d5 cb"
                + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d" + "93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8"
                + "01 10 05 06 00 00 00 03";
    }

}
