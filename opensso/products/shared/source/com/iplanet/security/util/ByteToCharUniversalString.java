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
 * $Id: ByteToCharUniversalString.java,v 1.2 2008/06/25 05:52:43 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import sun.io.ByteToCharConverter;
import sun.io.ConversionBufferFullException;
import sun.io.UnknownCharacterException;

/**
 * Converts bytes in ASN.1 UniversalString character set to unicode characters.
 * 
 */
public class ByteToCharUniversalString extends ByteToCharConverter {
    public String getCharacterEncoding() {
        return "ASN.1 UniversalString";
    }

    public int convert(byte[] input, int inStart, int inEnd, char[] output,
            int outStart, int outEnd) throws ConversionBufferFullException,
            UnknownCharacterException {
        int j = outStart;

        int i = inStart;
        while (i < inEnd) {
            // XXX we do not know what to do with truly UCS-4 characters here
            // we also assumed network byte order

            if (i + 3 >= inEnd
                    || (!((input[i] == 0 && input[i + 1] == 0) 
                            || (input[i + 2] == 0 && input[i + 3] == 0)))) {
                byteOff = i;
                charOff = j;
                throw new UnknownCharacterException();
            }
            if (input[i + 2] == 0 && input[i + 3] == 0) {
                // Try to be a bit forgiving. If the byte order is
                // reversed, we still try handle it.

                // Sample Date Set (1):
                // 0000000 f 0 \0 \0 213 0 \0 \0 S 0 \0 \0
                // 0000014

                // Sample Date Set (2):
                // 0000000 w \0 \0 \0 w \0 \0 \0 w \0 \0 \0 . \0 \0 \0
                // 0000020 ( \0 \0 \0 t \0 \0 \0 o \0 \0 \0 b \0 \0 \0
                // 0000040 e \0 \0 \0 | \0 \0 \0 n \0 \0 \0 o \0 \0 \0
                // 0000060 t \0 \0 \0 t \0 \0 \0 o \0 \0 \0 b \0 \0 \0
                // 0000100 e \0 \0 \0 ) \0 \0 \0 . \0 \0 \0 c \0 \0 \0
                // 0000120 o \0 \0 \0 m \0 \0 \0
                // 0000130
                output[j] = (char) (((input[i + 1] << 8) & 0xff00) 
                        + (input[i] & 0x00ff));
            } else {
                // This should be the right order.
                //
                // 0000000 0000 00c4 0000 0064 0000 006d 0000 0069
                // 0000020 0000 006e 0000 0020 0000 0051 0000 0041
                // 0000040

                // (input[i] == 0 && input[i+1] == 0)
                output[j] = (char) (((input[i + 2] << 8) & 0xff00) 
                        + (input[i + 3] & 0x00ff));
            }
            j++;
            i += 4;
        }
        byteOff = inEnd;
        charOff = j;
        return j - outStart;
    }

    public int flush(char[] output, int outStart, int outEnd) {
        return 0;
    }

    public void reset() {
    }

}
