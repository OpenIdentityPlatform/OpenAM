/**
 * Licence (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (base64 @ miginfocom . com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
/**
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */
package com.sun.identity.shared.encode;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Base64 {

    public static byte[] encodeToByte(byte[] bytes, boolean lineSep) {
        return org.forgerock.util.encode.Base64.encodeToByte(bytes, lineSep);
    }

    public static char[] encodeToChar(byte[] bytes, boolean lineSep) {
        return org.forgerock.util.encode.Base64.encodeToChar(bytes, lineSep);
    }

    public static String encode(byte[] bytes, boolean lineSep) {
        return org.forgerock.util.encode.Base64.encode(bytes, lineSep);
    }

    public static String encode(byte[] bytes) {
        return org.forgerock.util.encode.Base64.encode(bytes);
    }

    public static byte[] decode(byte[] bytes) {
        return org.forgerock.util.encode.Base64.decode(bytes);
    }

    public static byte[] decode(char[] encoded) {
        return org.forgerock.util.encode.Base64.decode(encoded);
    }

    public static byte[] decode(String encoded) {
        return org.forgerock.util.encode.Base64.decode(encoded.toCharArray());
    }

    public static byte[] decodeFast(byte[] encoded) {
        return org.forgerock.util.encode.Base64.decodeFast(encoded);
    }

    public static byte[] decodeFast(char[] encoded) {
        return org.forgerock.util.encode.Base64.decodeFast(encoded);
    }

    public static byte[] decodeFast(String encoded) {
        return org.forgerock.util.encode.Base64.decodeFast(encoded.toCharArray());
    }

    /**
     * Decodes the supplied String into a UTF-8 String.
     *
     * @param s String to encode.
     */
    public static String decodeAsUTF8String(String s) {
        return new String(decode(s), Charset.forName("UTF-8"));
    }
}
