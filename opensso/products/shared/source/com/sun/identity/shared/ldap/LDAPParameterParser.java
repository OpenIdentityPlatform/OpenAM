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
 * $Id: LDAPParameterParser.java,v 1.1 2009/11/20 23:52:58 ww203982 Exp $
 */
package com.sun.identity.shared.ldap;

import com.sun.identity.shared.ldap.ber.stream.BERElement;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * @deprecated As of ForgeRock OpenAM 10.
 */
public class LDAPParameterParser {

    public static int getLengthOctets(InputStream stream, int[] bytesRead,
        int[] bytesProcessed) throws IOException {
        int contents_length = 0;
        int octet = stream.read();
        bytesRead[0]++;
        bytesProcessed[0]++;
        if (octet == 0x80) {
            /* Indefinite length */
            contents_length = -1;
        } else {
            if ((octet & 0x80) > 0) {
            /* Definite (long form) - num octets encoded in 7 rightmost bits */
                int num_length_octets = (octet & 0x7F);
                for (int i = 0; i < num_length_octets; i++) {
                    octet = stream.read();
                    bytesRead[0]++;
                    bytesProcessed[0]++;
                    contents_length = (contents_length<<8) + octet;
                }
            } else {
            /* Definite (short form) - one length octet.  Value encoded in   */
            /* 7 rightmost bits.                                             */
                contents_length = octet;
            }
        }
        return contents_length;
    }

    public static int getLengthOctets(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        int contentsLength = 0;        
        if (buffer[offset[0]] == 0x80) {
            /* Indefinite length */
            contentsLength = -1;
            offset[0]++;
            bytesProcessed[0]++;
        } else {
            if ((buffer[offset[0]] & 0x80) > 0) {
            /* Definite (long form) - num octets encoded in 7 rightmost bits */
                int num_length_octets = (buffer[offset[0]] & 0x7F);
                offset[0]++;
                bytesProcessed[0]++;
                for (int i = 0; i < num_length_octets; i++) {
                    contentsLength = (contentsLength << 8) |
                        (buffer[offset[0]] & 0x000000FF);
                    offset[0]++;
                    bytesProcessed[0]++;
                }
            } else {
            /* Definite (short form) - one length octet.  Value encoded in   */
            /* 7 rightmost bits.                                             */
                contentsLength = buffer[offset[0]];
                offset[0]++;
                bytesProcessed[0]++;
            }
        }
        return contentsLength;
    }

    public static boolean parseBoolean(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        offset[0]++;
        bytesProcessed[0]++; 
        if (buffer[offset[0]] > 0) {
            offset[0]++;
            bytesProcessed[0]++;
            return true;
        } else {
            offset[0]++;
            bytesProcessed[0]++;
            return false;
        }
    }

    public static int parseInt(InputStream stream, int[] bytesRead,
        int[] bytesProcessed) throws IOException {
        int contentsLength = LDAPParameterParser.getLengthOctets(stream,
            bytesRead, bytesProcessed);
        int m_value = 0;
        /* Definite length content octets string. */
        if (contentsLength > 0) {
            boolean negative = false;
            int octet = stream.read();
            bytesRead[0]++;            
            if ((octet & 0x80) > 0)  /* left-most bit is 1. */
                negative = true;

            for (int i = 0; i < contentsLength; i++) {
                if (i > 0) {
                    octet = stream.read();
                    bytesRead[0]++;
                }
                if (negative) {
                    m_value = (m_value<<8) + (int)(octet ^ 0xFF) & 0xFF;
                } else {
                    m_value = (m_value<<8) + (int)(octet & 0xFF);
                }
                bytesProcessed[0]++;
            }
            if (negative) {  /* convert to 2's complement */
                m_value = (m_value + 1) * -1;
            }
        }
        return m_value;
    }

    public static int parseInt(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        int contentsLength = LDAPParameterParser.getLengthOctets(buffer,
            offset, bytesProcessed);
        int m_value = 0;
        /* Definite length content octets string. */
        if (contentsLength > 0) {
            boolean negative = false;
            for (int i = 0; i < contentsLength; i++) {                
                if (i == 0) {
                    if ((buffer[offset[0]] & 0x80) > 0) {
                        negative = true;
                    }
                }
                if (negative) {
                    m_value = (m_value << 8) + (int) (buffer[offset[0]] ^
                        0xFF) & 0xFF;
                } else {
                    m_value = (m_value << 8) + (int) (buffer[offset[0]] &
                        0xFF);
                }
                offset[0]++;
                bytesProcessed[0]++;
            }
            if (negative) {  /* convert to 2's complement */
                m_value = (m_value + 1) * -1;
            }
        }
        return m_value;
    }

    public static byte[] parseOctetBytes(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        int contentsLength = LDAPParameterParser.getLengthOctets(buffer,
            offset, bytesProcessed);
        byte[] m_value = null;
        if (contentsLength > 0 ) {
            m_value = new byte[contentsLength];
            System.arraycopy(buffer, offset[0], m_value, 0,
                contentsLength);
            offset[0] += contentsLength;
            bytesProcessed[0] += contentsLength;
        }
        return m_value;
    }

    public static byte[] parseOctetBytesList(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        int contentsLength = LDAPParameterParser.getLengthOctets(buffer,
            offset, bytesProcessed);
        if (contentsLength == -1) {
            ByteArrayOutputStream m_value = new ByteArrayOutputStream();           
            while (true) {
                if (buffer[offset[0]] == BERElement.EOC) {
                    offset[0] += 2;
                    bytesProcessed[0] += 2;
                    break;
                } else {
                    if (buffer[offset[0]] == BERElement.OCTETSTRING) {
                        offset[0]++;
                        bytesProcessed[0]++;
                        try {
                            m_value.write(parseOctetBytes(buffer, offset,
                                bytesProcessed));
                        } catch (IOException wontHappen) {}
                    } else {
                        if (buffer[offset[0]] == (BERElement.OCTETSTRING |
                            BERElement.CONSTRUCTED)) {
                            offset[0]++;
                            bytesProcessed[0]++;
                            try {
                                m_value.write(parseOctetBytesList(buffer,
                                    offset, bytesProcessed));
                            } catch (IOException wontHappen) {}
                        }
                    }
                }
            }
            return m_value.toByteArray();
        } else {
            return parseOctetBytes(buffer, offset, bytesProcessed);
        }
    }

    public static String parseOctetString(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        int contentsLength = LDAPParameterParser.getLengthOctets(buffer,
            offset, bytesProcessed);
        String m_value = null;
        if (contentsLength > 0 ) {
            try {
                m_value = new String(buffer, offset[0], contentsLength,
                    "UTF8");
            } catch (Throwable ignored) {}
            offset[0] += contentsLength;
            bytesProcessed[0] += contentsLength;
        }
        return m_value;
    }

    public static String parseOctetStringList(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        int contentsLength = LDAPParameterParser.getLengthOctets(buffer,
            offset, bytesProcessed);
        if (contentsLength == -1) {    
            StringBuffer m_value = new StringBuffer();                
            while (true) {
                if (buffer[offset[0]] == BERElement.EOC) {
                    offset[0] += 2;
                    bytesProcessed[0] += 2;
                    break;
                } else {
                    if (buffer[offset[0]] == BERElement.OCTETSTRING) {
                        offset[0]++;
                        bytesProcessed[0]++;
                        m_value.append(parseOctetString(buffer, offset,
                            bytesProcessed));
                    } else {
                        if (buffer[offset[0]] == (BERElement.OCTETSTRING |
                            BERElement.CONSTRUCTED)) {
                            offset[0]++;
                            bytesProcessed[0]++;
                            m_value.append(parseOctetStringList(buffer, offset,
                                bytesProcessed));
                        }
                    }
                }
            }
            return m_value.toString();
        } else {
            return parseOctetString(buffer, offset, bytesProcessed);
        }
    }

    public static void parseNull(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        offset[0]++;
        bytesProcessed[0]++;
        return;
    }

    private static int readSubIdentifier(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        int octet;
        int sub_id = 0;
        do {            
            octet = buffer[offset[0]];
            sub_id = (sub_id << 7) | (octet & 0x7F);
            offset[0]++;
            bytesProcessed[0]++;
        } while ((octet & 0x80) > 0);
        return sub_id;
    }

    public static int[] parseObjectId(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        int length = LDAPParameterParser.getLengthOctets(buffer, offset,
            bytesProcessed);
        bytesProcessed[0] += length;
        int[]  contents_read = new int[1];
        ArrayList oid = new ArrayList(10);
        contents_read[0] = 0;
        int sub_id = readSubIdentifier(buffer, offset, contents_read);
        length -= contents_read[0];
        if (sub_id < 40) {
            oid.add(new Integer(0));
        } else {
            if (sub_id < 80) {
                oid.add(new Integer(1));
            } else {
                oid.add(new Integer(2));
            }
        }
        oid.add(new Integer(sub_id -
            (((Integer)oid.get(oid.size() - 1)).intValue() * 40)));
        while (length > 0) {
            contents_read[0] = 0;
            sub_id = readSubIdentifier(buffer, offset, contents_read);
            length -= contents_read[0];
            oid.add(new Integer(sub_id));
        }
        int[] m_value = new int[oid.size()];
        for (int i = 0; i < oid.size(); i++) {
            m_value[i] = ((Integer) oid.get(i)).intValue();
        }
        return m_value;
    }

    public static BitSet parseBitString(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        /* Primitive - definite length content octets string. */

        int octet;
        int contentsLength = LDAPParameterParser.getLengthOctets(buffer,
            offset, bytesProcessed);
        int length = contentsLength;

        /* First content octect doesn't encode any of
         * the string - it encodes the number of unused
         * bits in the final content octet.
         */
        int last_unused_bits = buffer[offset[0]];
        offset[0]++;
        length--;

        int m_value_num_bits = ((length - 1) * 8) + (8 - last_unused_bits);
        BitSet m_value = new BitSet();

        int bit_num = 0;
        for (int i = 0; i < length - 1; i++) {            
            octet = buffer[offset[0]];
            offset[0]++;
            int mask = 0x80;
            for (int j = 0; j < 8; j++) {
                if ((octet & mask) > 0) {
                    m_value.set(bit_num);
                } else {
                    m_value.clear(bit_num);
                }
                bit_num++;
                mask = mask / 2;
            }
        }
        octet = buffer[offset[0]];  /* last content octet */
        offset[0]++;
        int mask = 0x80;
        for (int j = 0; j < 8 - last_unused_bits; j++) {
            if ((octet & mask) > 0) {
                m_value.set(bit_num);
            } else {
                m_value.clear(bit_num);
            }
            bit_num++;
            mask = mask / 2;
        }
        bytesProcessed[0] += contentsLength;
        return m_value;
    }

    public static String parseString(byte[] buffer, int[] offset,
        int[] bytesProcessed) {
        String m_value = null;
        try {
            int contentsLength = LDAPParameterParser.getLengthOctets(buffer,
                offset, bytesProcessed);
            if (contentsLength > 0 ) {
                m_value = new String(buffer, offset[0], contentsLength,
                    "UTF8");
                offset[0] += contentsLength;
                bytesProcessed[0] += contentsLength;
            }
        } catch (Throwable x) {}
        return m_value;
    }
}
