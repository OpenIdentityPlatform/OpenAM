/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Base64.java,v 1.2 2009/06/11 05:29:46 superpat7 Exp $
 */

package com.sun.javafx.oauth;

import java.io.IOException;

/**
 * This class contains a Base64 encoder and decoder. It cannot be
 * instantiated. The encoder and decoder are based on RFC 1521, except that
 * encoder do not break up lines and the decoder will treat a line break as
 * as invalid characters. This done since MIDP JAD's can not have line breaks
 * with in an attribute. Also different RFC's use different lengths for
 * base64 strings such as; 76 for 1521, 64 for 1421, and no breaks for 2228.
 * <p>
 * For example to decode: 
 * <pre>
 *      String  encodedData;
 *      byte    binaryData[];
 *
 *      binaryData = Base64.decode(encodedData);
 * </pre>
 * This will decode the String in <i>encodedData</i> and give you an array 
 * of bytes in the array <i>binaryData</i>. 
 *
 * On errors, this class throws an IOException with the following detail
 * strings:
 * <pre>
 *    "Base64 string not a mulitple of 4"
 *    "Invalid character in Base64 string"
 * </pre>
 */

public class Base64 {
    /** prevents anyone from instantiating this class */
    private Base64() {
    }

    /**
     * This character array provides the alphabet map from RFC1521.
     */
    private final static char ALPHABET[] = {
	//       0    1    2    3    4    5    6    7
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',  // 0
		'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',  // 1
		'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',  // 2
		'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',  // 3
		'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',  // 4
		'o', 'p', 'q', 'r', 's', 't', 'u', 'v',  // 5
		'w', 'x', 'y', 'z', '0', '1', '2', '3',  // 6
		'4', '5', '6', '7', '8', '9', '+', '/'  // 7
	};

    /**
     * Decodes a 7 bit Base64 character into its binary value.
     */
    private static int valueDecoding[] = new int[128];

    /**
     * initializes the value decoding array from the 
     * character map
     */
    static {
	for (int i = 0; i < valueDecoding.length; i++) {
	    valueDecoding[i] = -1;
	}

	for (int i = 0; i < ALPHABET.length; i++) {
	    valueDecoding[ALPHABET[i]] = i;
	}
    }

    /**
     * Converts a byte array into a Base64 encoded string.
     * @param data bytes to encode
     * @return base64 encoding of data; 4 chars for every 3 bytes
     */
    public static String encode(byte[] data) {
        return encode(data, 0, data.length);
    }

    /**
     * Converts a byte array into a Base64 encoded string.
     * @param data bytes to encode
     * @param offset which byte to start at
     * @param length how many bytes to encode; padding will be added if needed
     * @return base64 encoding of data; 4 chars for every 3 bytes
     */
    public static String encode(byte[] data, int offset, int length) {
        int i;
        int encodedLen;
        char[] encoded;

        // 4 chars for 3 bytes, run input up to a multiple of 3
        encodedLen = (length + 2) / 3 * 4;
        encoded = new char [encodedLen];

        for (i = 0, encodedLen = 0; encodedLen < encoded.length;
             i += 3, encodedLen += 4) {
            encodeQuantum(data, offset + i, length - i, encoded, encodedLen);
        }
               
        return new String(encoded);
    }

    /**
     * Encodes 1, 2, or 3 bytes of data as 4 Base64 chars.
     *
     * @param in buffer of bytes to encode
     * @param inOffset where the first byte to encode is
     * @param len how many bytes to encode
     * @param out buffer to put the output in
     * @param outOffset where in the output buffer to put the chars
     */
    private static void encodeQuantum(byte in[], int inOffset, int len,
                                      char out[], int outOffset) {
	byte a = 0, b = 0, c = 0;

        a = in[inOffset];
        out[outOffset] = ALPHABET[(a >>> 2) & 0x3F];

        if (len > 2) {
            b = in[inOffset + 1];
            c = in[inOffset + 2];
            out[outOffset + 1] = ALPHABET[((a << 4) & 0x30) +
                                         ((b >>> 4) & 0xf)];
	    out[outOffset + 2] = ALPHABET[((b << 2) & 0x3c) +
                                          ((c >>> 6) & 0x3)];
	    out[outOffset + 3] = ALPHABET[c & 0x3F];
        } else if (len > 1) {
            b = in[inOffset + 1];
            out[outOffset + 1] = ALPHABET[((a << 4) & 0x30) +
                                         ((b >>> 4) & 0xf)];
	    out[outOffset + 2] =  ALPHABET[((b << 2) & 0x3c) +
                                          ((c >>> 6) & 0x3)];
	    out[outOffset + 3] = '=';
        } else {
            out[outOffset + 1] = ALPHABET[((a << 4) & 0x30) +
                                         ((b >>> 4) & 0xf)];
	    out[outOffset + 2] = '=';
	    out[outOffset + 3] = '=';
        }
    }

    /**
     * Converts a Base64 encoded string to a byte array.
     * @param encoded Base64 encoded data
     * @return decode binary data; 3 bytes for every 4 chars - minus padding
     * @exception IOException is thrown, if an I/O error occurs reading the data
     */
    public static byte[] decode(String encoded)
            throws IOException {
        return decode(encoded, 0, encoded.length());
    }

    /**
     * Converts an embedded Base64 encoded string to a byte array.
     * @param encoded a String with Base64 data embedded in it
     * @param offset which char of the String to start at
     * @param length how many chars to decode; must be a multiple of 4
     * @return decode binary data; 3 bytes for every 4 chars - minus padding
     * @exception IOException is thrown, if an I/O error occurs reading the data
     */
    public static byte[] decode(String encoded, int offset, int length)
            throws IOException {
        int i;
        int decodedLen;
        byte[] decoded;

        // the input must be a multiple of 4
        if (length % 4 != 0) {
            throw new IOException(
                "Base64 string length is not multiple of 4");
        }

        // 4 chars for 3 bytes, but there may have been pad bytes
        decodedLen = length / 4 * 3;
        if (encoded.charAt(offset + length - 1) == '=') {
            decodedLen--;
            if (encoded.charAt(offset + length - 2) == '=') {
                decodedLen--;
            }
        }

        decoded = new byte [decodedLen];
        for (i = 0, decodedLen = 0; i < length; i += 4, decodedLen += 3) {
            decodeQuantum(encoded.charAt(offset + i),
                          encoded.charAt(offset + i + 1),
                          encoded.charAt(offset + i + 2),
                          encoded.charAt(offset + i + 3),
                          decoded, decodedLen);
        }
               
        return decoded;
    }

    /**
     * Decode 4 Base64 chars as 1, 2, or 3 bytes of data.
     *
     * @param in1 first char of quantum to decode
     * @param in2 second char of quantum to decode
     * @param in3 third char of quantum to decode
     * @param in4 forth char of quantum to decode
     * @param out buffer to put the output in
     * @param outOffset where in the output buffer to put the bytes
     */
    private static void decodeQuantum(char in1, char in2, char in3, char in4, 
                                byte[] out, int outOffset)
	throws IOException {
	int a = 0, b = 0, c = 0, d = 0;
        int pad = 0;

        a = valueDecoding[in1 & 127];
        b = valueDecoding[in2 & 127];

        if (in4 == '=') {
            pad++;
            if (in3 == '=') {
                pad++;
            } else {
                c = valueDecoding[in3 & 127];
            }
        } else {
            c = valueDecoding[in3 & 127];
            d = valueDecoding[in4 & 127];
        }

	if (a < 0 || b < 0 || c < 0 || d < 0) {
            throw new IOException("Invalid character in Base64 string");
        }

        // the first byte is the 6 bits of a and 2 bits of b
        out[outOffset] = (byte)(((a << 2) & 0xfc) | ((b >>> 4) & 3));

        if (pad < 2) {
            // the second byte is 4 bits of b and 4 bits of c
            out[outOffset + 1] = (byte)(((b << 4) & 0xf0) | ((c >>> 2) & 0xf));

            if (pad < 1) {
                // the third byte is 2 bits of c and 4 bits of d
                out[outOffset + 2] =
                    (byte)(((c << 6) & 0xc0) | (d  & 0x3f));
            }
        }
    }
}
