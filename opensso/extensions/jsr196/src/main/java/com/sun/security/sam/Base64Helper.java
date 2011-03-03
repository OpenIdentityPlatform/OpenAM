/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: Base64Helper.java,v 1.1 2008/10/27 14:17:21 monzillo Exp $
 */
package com.sun.security.sam;

import java.util.HashMap;

/**
 * @author monzillo
 */
public class Base64Helper {

    private static final byte PAD_BYTE = (byte) '=';
    private static final int PAD_VALUE = 64;
    private static final byte[] base64Alphabet = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };
    private static final HashMap<Byte, Integer> base64DecoderMap =
            new HashMap<Byte, Integer>();
    

    static {
        for (int i = 0; i < base64Alphabet.length; i++) {
            base64DecoderMap.put(new Byte(base64Alphabet[i]), new Integer(i));
        }
    }

    public static byte[] decode(byte[] data) {

        int padding = 0;
        if (data[data.length - 2] == PAD_BYTE) {
            padding = 2;
        } else if (data[data.length - 1] == PAD_BYTE) {
            padding = 1;
        }
        int size = ((data.length / 4) * 3) - padding;

        byte[] rvalue = new byte[size];
        byte carry = 0;

        for (int i = 0, cycle = 0; i < data.length; i++) {

            byte bite;

            if (data[i] == PAD_BYTE && i >= data.length - 2) {
                bite = -1;
            } else {

                Byte key = new Byte(data[i]);

                Integer value = base64DecoderMap.get(key);

                if (value == null) {
                    throw new IllegalArgumentException("invalid Base64 char at data[" + i + "]");
                }
                bite = value.byteValue();
            }

            int chunk = i % 4;
            switch (chunk) {
                case 0:
                    carry = (byte) ((bite << 2) & 252);
                    break;
                case 1:
                    rvalue[cycle * 3] = (byte) (carry | ((bite >> 4) & 3));
                    carry = (byte) ((bite << 4) & 240);
                    break;
                case 2:
                    if (bite >= 0) {
                        rvalue[(cycle * 3) + 1] = (byte) (carry | ((bite >> 2) & 15));
                        carry = (byte) ((bite << 6) & 192);
                    }
                    break;
                case 3:
                    if (bite >= 0) {
                        rvalue[(cycle * 3) + 2] = (byte) (carry | bite);
                        cycle += 1;
                    }
                    break;
            }

            if (bite < 0) {
                break;
            }

        }

        return rvalue;
    }

    public static byte[] encode(byte[] data) {

        int size = ((data.length + 2) / 3) * 4;

        byte[] rvalue = new byte[size];
        byte carry = 0;

        for (int i = 0, cycle = 0; i < data.length; i++) {

            byte bite = data[i];

            int chunk = i % 3;

            switch (chunk) {
                case 0:
                    rvalue[(cycle * 4) + chunk] = base64Alphabet[(bite >> 2) & 63];
                    carry = (byte) ((bite << 4) & 48);
                    break;
                case 1:
                    rvalue[(cycle * 4) + chunk] = base64Alphabet[carry | ((bite >> 4) & 15)];
                    carry = (byte) ((bite << 2) & 60);
                    break;
                case 2:
                    rvalue[(cycle * 4) + chunk] = base64Alphabet[carry | ((bite >> 6) & 3)];
                    rvalue[(cycle * 4) + chunk + 1] = base64Alphabet[bite & 63];
                    break;
            }

            if (i == data.length - 1 && (chunk != 2)) {
                rvalue[(cycle * 4) + chunk + 1] = base64Alphabet[carry];
                rvalue[(cycle * 4) + chunk + 2] = PAD_BYTE;
                if (chunk == 0) {
                    rvalue[(cycle * 4) + chunk + 3] = PAD_BYTE;
                }
            } else if (chunk == 2) {
                cycle += 1;
            }

        }

        return rvalue;
    }
    
    private static final String[] testData = {
        "===",
        "abc",
        "abcdef=",
        "no   padding",
        "a",
        "abcd",
        "abcdefg",
        "ab==c==",
        "two padding char",
        "ab",
        "==",
        "1 padding char",
    };

    public static void main(int argc, String[] argv) {

        for (String s : testData) {

            byte[] encoded = encode(s.getBytes());

            byte[] decoded = decode(encoded);

            if (!s.equals(new String(decoded))) {
                System.out.println("s: " + s +
                        " encoded: " + new String(encoded) +
                        " decoded: " + new String(decoded));
            }
        }
    }
}
