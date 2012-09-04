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
 * $Id: FileNameEncoder.java,v 1.3 2008/06/25 05:44:08 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.flatfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;

/**
 * Same as URLEncoder except '*' and ' ' are also encoded since they are not
 * acceptable as file names on windows. ' ' is acctually only not acceptable on
 * windows if it's the only character in a file name but we encode it anyway to
 * make things easier. Also, ' ' is not encoded to '+' as it would in URL
 * encoding, and '+' is not encoded. Lastly, '=' is also not encoded since it
 * doesn't need to be and is contained many times in a dn.
 */
public class FileNameEncoder {
    static BitSet dontNeedEncoding;

    static final int caseDiff = ('a' - 'A');

    static {
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('+');
        dontNeedEncoding.set('=');
    }

    /**
     * You can't call the constructor.
     */
    private FileNameEncoder() {
    }

    /**
     * Translates a string into <code>x-www-form-urlencoded</code> format with
     * some differences as described at the top of this class. This method uses
     * UTF-8 as the encoding scheme to obtain the bytes for unsafe characters.
     * 
     * @param s <code>String</code> to be translated.
     * @return the translated <code>String</code>.
     */
    public static String encode(String s) {

        String str = null;

        try {
            str = encode(s, "UTF8");
        } catch (UnsupportedEncodingException e) {
            // The system should always have the platform default
        }

        return str;
    }

    /**
     * Translates a string into <code>application/x-www-form-urlencoded</code>
     * format using a specific encoding scheme with some differences as
     * described at the top of this class. This method uses the supplied
     * encoding scheme to obtain the bytes for unsafe characters.
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilites.</em>
     * 
     * @param s
     *            <code>String</code> to be translated.
     * @param enc
     *            The name of a supported <a
     *            href="../lang/package-summary.html#charenc">
     *            character encoding</a>.
     * @return the translated <code>String</code>.
     * @exception UnsupportedEncodingException
     *                If the named encoding is not supported
     * @see FileNameDecoder#decode(java.lang.String, java.lang.String)
     */
    public static String encode(String s, String enc)
            throws UnsupportedEncodingException {

        boolean needToChange = false;
        boolean wroteUnencodedChar = false;
        int maxBytesPerChar = 10; // rather arbitrary limit, but safe for now
        StringBuilder out = new StringBuilder(s.length());
        ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);

        OutputStreamWriter writer = new OutputStreamWriter(buf, enc);

        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            // System.out.println("Examining character: " + c);
            if (dontNeedEncoding.get(c)) {
                out.append((char) c);
                wroteUnencodedChar = true;
            } else {
                // convert to external encoding before hex conversion
                try {
                    if (wroteUnencodedChar) { // Fix for 4407610
                        writer = new OutputStreamWriter(buf, enc);
                        wroteUnencodedChar = false;
                    }
                    writer.write(c);
                    /*
                     * If this character represents the start of a Unicode
                     * surrogate pair, then pass in two characters. It's not
                     * clear what should be done if a bytes reserved in the
                     * surrogate pairs range occurs outside of a legal surrogate
                     * pair. For now, just treat it as if it were any other
                     * character.
                     */
                    if (c >= 0xD800 && c <= 0xDBFF) {
                        if ((i + 1) < s.length()) {
                            int d = s.charAt(i + 1);
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                writer.write(d);
                                i++;
                            }
                        }
                    }
                    writer.flush();
                } catch (IOException e) {
                    buf.reset();
                    continue;
                }
                byte[] ba = buf.toByteArray();
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                buf.reset();
                needToChange = true;
            }
        }

        return (needToChange ? out.toString() : s);
    }
}
