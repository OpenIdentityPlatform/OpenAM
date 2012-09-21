/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FileNameDecoder.java,v 1.2 2008/06/25 05:44:08 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.flatfile;

import java.io.UnsupportedEncodingException;

/**
 * Same as URLDecoder except '+' is not decoded into a ' '.
 * 
 * @see com.sun.identity.sm.flatfile.FileNameEncoder
 */

public class FileNameDecoder {

    /**
     * Decodes a <code>x-www-form-urlencoded</code> string. Except '+' is not
     * decoded to a ' ' as described at the top of this class. UTF-8 encoding is
     * used to determine what characters are represented by any consecutive
     * sequences of the form "<code>%<i>xy</i></code>".
     * 
     * @param s
     *            the <code>String</code> to decode
     * @return the newly decoded <code>String</code>
     */
    public static String decode(String s) {

        String str = null;

        try {
            str = decode(s, "UTF8");
        } catch (UnsupportedEncodingException e) {
            // The system should always have the platform default
        }

        return str;
    }

    /**
     * Decodes a <code>application/x-www-form-urlencoded</code> string using a
     * specific encoding scheme. The supplied encoding is used to determine what
     * characters are represented by any consecutive sequences of the form 
     * "<code>%<i>xy</i></code>".
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilites.</em>
     * 
     * @param s
     *            the <code>String</code> to decode
     * @param enc
     *            The name of a supported <a
     *            href="../lang/package-summary.html#charenc">
     *            character encoding</a>.
     * @return the newly decoded <code>String</code>
     * @exception UnsupportedEncodingException
     *                If character encoding needs to be consulted, but named
     *                character encoding is not supported
     * @see FileNameEncoder#encode(java.lang.String, java.lang.String)
     */
    public static String decode(String s, String enc)
            throws UnsupportedEncodingException {

        boolean needToChange = false;
        int numChars = s.length();
        StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2
                : numChars);
        int i = 0;

        if (enc.length() == 0) {
            throw new UnsupportedEncodingException(
                    "FileNameDecoder: empty string enc parameter");
        }

        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
            case '%':
                /*
                 * Starting with this instance of %, process all consecutive
                 * substrings of the form %xy. Each substring %xy will yield a
                 * byte. Convert all consecutive bytes obtained this way to
                 * whatever character(s) they represent in the provided
                 * encoding.
                 */

                try {

                    // (numChars-i)/3 is an upper bound for the number
                    // of remaining bytes
                    if (bytes == null)
                        bytes = new byte[(numChars - i) / 3];
                    int pos = 0;

                    while (((i + 2) < numChars) && (c == '%')) {
                        bytes[pos++] = (byte) Integer.parseInt(s.substring(
                                i + 1, i + 3), 16);
                        i += 3;
                        if (i < numChars)
                            c = s.charAt(i);
                    }

                    // A trailing, incomplete byte encoding such as
                    // "%x" will cause an exception to be thrown

                    if ((i < numChars) && (c == '%'))
                        throw new IllegalArgumentException("FileNameDecoder: "
                                + "Incomplete trailing escape (%) pattern");

                    sb.append(new String(bytes, 0, pos, enc));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("FileNameDecoder: "
                            + "Illegal hex characters in escape (%) pattern - "
                            + e.getMessage());
                }
                needToChange = true;
                break;
            default:
                sb.append(c);
                i++;
                break;
            }
        }

        return (needToChange ? sb.toString() : s);
    }
}
