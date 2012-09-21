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
 * $Id: CharToBytePrintable.java,v 1.2 2008/06/25 05:52:44 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import sun.io.CharToByteConverter;
import sun.io.ConversionBufferFullException;
import sun.io.MalformedInputException;
import sun.io.UnknownCharacterException;

/**
 * Converts a string of ASN.1 PrintableString characters to PrintableString
 * bytes.
 * 
 */
public class CharToBytePrintable extends CharToByteConverter {
    /*
     * returns the character set id for the conversion. @return the character
     * set id.
     */
    public String getCharacterEncoding() {
        return "ASN.1 Printable";
    }

    public static boolean isPrintableChar(char c) {
        if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')
                && (c < '0' || c > '9') && (c != ' ') && (c != '\'')
                && (c != '(') && (c != ')') && (c != '+') && (c != ',')
                && (c != '-') && (c != '.') && (c != '/') && (c != ':')
                && (c != '=') && (c != '?')) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Converts an array of Unicode characters into an array of Printable String
     * bytes and returns the total number of characters converted. If conversion
     * cannot be done, UnknownCharacterException is thrown. The character and
     * byte offset will be set to the point of the unknown character. @param
     * input character array to convert. @param inStart offset from which to
     * start the conversion. @param inEnd where to end the conversion. @param
     * output byte array to store converted bytes. @param outStart starting
     * offset in the output byte array. @param outEnd ending offset in the
     * output byte array. @return the number of characters converted.
     */
    public int convert(char[] input, int inStart, int inEnd, byte[] output,
            int outStart, int outEnd) throws MalformedInputException,
            UnknownCharacterException, ConversionBufferFullException {
        int j = outStart;
        int i;
        for (i = inStart; i < inEnd; i++, j++) {
            if (j >= outEnd) {
                charOff = i;
                byteOff = j;
                throw new ConversionBufferFullException();
            }
            if (!subMode && !isPrintableChar(input[i])) {
                charOff = i;
                byteOff = j;
                badInputLength = 1;
                throw new UnknownCharacterException();
            }
            output[j] = (byte) (input[i] & 0x7f);
        }
        charOff = i;
        byteOff = j;
        return j - outStart;
    }

    public int flush(byte[] output, int outStart, int outEnd)
            throws MalformedInputException, ConversionBufferFullException {
        return 0;
    }

    public void reset() {
    }

    public int getMaxBytesPerChar() {
        return 1;
    }

}
