/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Base64.java,v 1.2 2008/06/25 05:51:28 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

public class Base64 {

    /**
     * Returns a String of base64-encoded characters to represent the passed
     * data array.
     * 
     * @param data
     *            the array of bytes to encode
     * @return String base64-coded characters
     */
    static public String encode(byte[] data) {
        char[] out = new char[((data.length + 2) / 3) * 4];

        //
        // 3 bytes encode to 4 chars. Output is always an even
        // multiple of 4 characters.
        //
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
            boolean quad = false;
            boolean trip = false;

            int val = (0xFF & (int) data[i]);
            val <<= 8;
            if ((i + 1) < data.length) {
                val |= (0xFF & (int) data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length) {
                val |= (0xFF & (int) data[i + 2]);
                quad = true;
            }
            out[index + 3] = alphabet[(quad ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 2] = alphabet[(trip ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 1] = alphabet[val & 0x3F];
            val >>= 6;
            out[index + 0] = alphabet[val & 0x3F];
        }
        return new String(out);
    }

    /**
     * Returns an array of decoded bytes which were encoded in the passed byte
     * array.
     * 
     * @param data
     *            the array of base64-encoded characters
     * @return byte[] the decoded data array
     */
    static public byte[] decode(String encdata) {
        // convert to a char[]
        char[] data = encdata.toCharArray();

        if (data.length <= 0)
            throw new RuntimeException("Invalid encoded data!");

        // check that length is a multiple of 4
        if (data.length % 4 != 0)
            throw new RuntimeException("Data is not Base64 encoded.");

        int len = ((data.length + 3) / 4) * 3;
        if (data[data.length - 1] == '=')
            --len;
        if (data[data.length - 2] == '=')
            --len;
        byte[] out = new byte[len];

        int shift = 0; // # of excess bits stored in accum
        int accum = 0; // excess bits
        int index = 0;

        for (int ix = 0; ix < data.length; ix++) {
            int value = codes[data[ix] & 0xFF]; // ignore high byte of char
            if (value >= 0) { // skip over non-code
                accum <<= 6; // bits shift up by 6 each time thru
                shift += 6; // loop, with new bits being put in
                accum |= value; // at the bottom.
                if (shift >= 8) { // whenever there are 8 or more shifted in,
                    shift -= 8; // write them out (from the top, leaving any
                    out[index++] = // excess at the bottom for next iteration.
                    (byte) ((accum >> shift) & 0xff);
                }
            } else {
                if (data[ix] != '=') {
                    throw new RuntimeException("Data is not Base64 encoded.");
                }
            }
        }
        if (index != out.length)
            throw new RuntimeException("Data length mismatch.");

        return out;
    }

    /**
     * code characters for values 0..63
     */
    static private char[] alphabet = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
        .toCharArray();

    /**
     * lookup table for converting base64 characters to value in range 0..63
     */
    static private byte[] codes = new byte[256];

    static {
        for (int i = 0; i < 256; i++)
            codes[i] = -1;
        for (int i = 'A'; i <= 'Z'; i++)
            codes[i] = (byte) (i - 'A');
        for (int i = 'a'; i <= 'z'; i++)
            codes[i] = (byte) (26 + i - 'a');
        for (int i = '0'; i <= '9'; i++)
            codes[i] = (byte) (52 + i - '0');
        codes['+'] = 62;
        codes['/'] = 63;
    }
}
