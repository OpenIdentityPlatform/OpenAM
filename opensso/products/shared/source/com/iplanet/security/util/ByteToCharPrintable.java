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
 * $Id: ByteToCharPrintable.java,v 1.2 2008/06/25 05:52:43 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import sun.io.ByteToCharConverter;
import sun.io.ConversionBufferFullException;
import sun.io.MalformedInputException;
import sun.io.UnknownCharacterException;

/**
 * Converts bytes in ASN.1 Printable String character set to unicode characters.
 * 
 */
public class ByteToCharPrintable extends ByteToCharConverter {
    public String getCharacterEncoding() {
        return "ASN.1 Printable";
    }

    public int convert(byte[] input, int inStart, int inEnd, char[] output,
            int outStart, int outEnd) throws MalformedInputException,
            UnknownCharacterException, ConversionBufferFullException {
        int j = outStart;
        boolean hasNonPrintableChar = false;

        for (int i = inStart; i < inEnd; i++, j++) {
            if (j >= outEnd) {
                byteOff = i;
                charOff = j;
                throw new ConversionBufferFullException();
            }
            if (!subMode
                    && !CharToBytePrintable
                            .isPrintableChar((char) (input[i] & 0x7f))) {
                /*
                 * "bug" fix for 359010 byteOff = i; charOff = j; 
                 * badInputLength =
                 * 1; throw new UnknownCharacterException();
                 */
                j--;
                hasNonPrintableChar = true;
            } else
                output[j] = (char) (input[i] & 0x7f);
        }

        byteOff = inEnd;
        charOff = j;
        return j - outStart;
    }

    public int flush(char[] output, int outStart, int outEnd)
            throws MalformedInputException, ConversionBufferFullException {
        return 0;
    }

    public void reset() {
    }

}
