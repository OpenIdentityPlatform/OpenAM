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
 * $Id: ByteToCharUnicode.java,v 1.2 2008/06/25 05:52:43 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import sun.io.ByteToCharUnicodeBig;
import sun.io.ByteToCharUnicodeLittle;
import sun.io.ConversionBufferFullException;
import sun.io.MalformedInputException;

/**
 * Convert byte arrays containing Unicode characters into arrays of actual
 * Unicode characters, sensing the byte order automatically. To force a
 * particular byte order, use either the "UnicodeBig" or the "UnicodeLittle"
 * encoding.
 * 
 * If the first character is a byte order mark, it will be interpreted and
 * discarded. Otherwise, the byte order is assumed to be BigEndian. Either way,
 * the byte order is decided by the first character. Later byte order marks will
 * be passed through as characters (if they indicate the same byte order) or
 * will cause an error (if they indicate the other byte order).
 * 
 * @see ByteToCharUnicodeLittle
 * @see ByteToCharUnicodeBig
 */
public class ByteToCharUnicode extends sun.io.ByteToCharConverter {

    static final char BYTE_ORDER_MARK = (char) 0xfeff;

    static final char REVERSED_MARK = (char) 0xfffe;

    static final int AUTO = 0;

    static final int BIG = 1;

    static final int LITTLE = 2;

    int byteOrder;

    public ByteToCharUnicode() {
        byteOrder = AUTO;
    }

    public String getCharacterEncoding() {
        switch (byteOrder) {
        case BIG:
            return "UnicodeBig";
        case LITTLE:
            return "UnicodeLittle";
        default:
            return "Unicode";
        }
    }

    boolean started = false;

    int leftOverByte;

    boolean leftOver = false;

    public int convert(byte[] in, int inOff, int inEnd, char[] out, int outOff,
            int outEnd) throws ConversionBufferFullException,
            MalformedInputException {
        byteOff = inOff;
        charOff = outOff;

        if (inOff >= inEnd)
            return 0;

        int b1, b2;
        int bc = 0;
        int inI = inOff, outI = outOff;

        if (leftOver) {
            b1 = leftOverByte & 0xff;
            leftOver = false;
        } else
            b1 = in[inI++] & 0xff;
        bc = 1;

        if (!started) { /* Read possible initial byte-order mark */
            if (inI < inEnd) {
                b2 = in[inI++] & 0xff;
                bc = 2;

                char c = (char) ((b1 << 8) | b2);
                int bo = AUTO;

                if (c == BYTE_ORDER_MARK)
                    bo = BIG;
                else if (c == REVERSED_MARK)
                    bo = LITTLE;

                if (byteOrder == AUTO) {
                    if (bo == AUTO) {
                        bo = BIG; // BigEndian by default
                    }
                    byteOrder = bo;
                    if (inI < inEnd) {
                        b1 = in[inI++] & 0xff;
                        bc = 1;
                    }
                } else if (bo == AUTO) {
                    inI--;
                    bc = 1;
                } else if (byteOrder == bo) {
                    if (inI < inEnd) {
                        b1 = in[inI++] & 0xff;
                        bc = 1;
                    }
                } else {
                    badInputLength = bc;
                    throw new MalformedInputException(
                            "Incorrect byte-order mark");
                }

                started = true;
            }
        }

        /* Loop invariant: (b1 contains the next input byte) && (bc == 1) */
        while (inI < inEnd) {
            b2 = in[inI++] & 0xff;
            bc = 2;

            char c;
            if (byteOrder == BIG)
                c = (char) ((b1 << 8) | b2);
            else
                c = (char) ((b2 << 8) | b1);

            if (c == REVERSED_MARK)
                throw new MalformedInputException("Reversed byte-order mark");

            if (outI >= outEnd)
                throw new ConversionBufferFullException();
            out[outI++] = c;
            byteOff = inI;
            charOff = outI;

            if (inI < inEnd) {
                b1 = in[inI++] & 0xff;
                bc = 1;
            }
        }

        if (bc == 1) {
            leftOverByte = b1;
            leftOver = true;
        }

        return outI - outOff;
    }

    public void reset() {
        leftOver = false;
        byteOff = charOff = 0;
    }

    public int flush(char buf[], int off, int len)
            throws MalformedInputException {
        if (leftOver) {
            reset();
            throw new MalformedInputException();
        }
        byteOff = charOff = 0;
        return 0;
    }

}
