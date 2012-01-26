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
 * $Id: DerOutputStream.java,v 1.2 2008/06/25 05:52:44 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import sun.io.CharToByteConverter;
import sun.security.util.BitArray;

/**
 * Output stream marshaling DER-encoded data. This is eventually provided in the
 * form of a byte array; there is no advance limit on the size of that byte
 * array.
 * 
 * <P>
 * At this time, this class supports only a subset of the types of DER data
 * encodings which are defined. That subset is sufficient for generating most
 * X.509 certificates.
 */
public class DerOutputStream extends ByteArrayOutputStream implements
        DerEncoder {
    /**
     * Construct an DER output stream.
     * 
     * @param size
     *            how large a buffer to preallocate.
     */
    public DerOutputStream(int size) {
        super(size);
    }

    /**
     * Construct an DER output stream.
     */
    public DerOutputStream() {
    }

    /**
     * Writes tagged, pre-marshaled data. This calcuates and encodes the length,
     * so that the output data is the standard triple of { tag, length, data }
     * used by all DER values.
     * 
     * @param tag
     *            the DER value tag for the data, such as
     *            <em>DerValue.tag_Sequence</em>
     * @param buf
     *            buffered data, which must be DER-encoded
     */
    public void write(byte tag, byte[] buf) throws IOException {
        write(tag);
        putLength(buf.length);
        write(buf, 0, buf.length);
    }

    /**
     * Writes tagged data using buffer-to-buffer copy. As above, this writes a
     * standard DER record. This is often used when efficiently encapsulating
     * values in sequences.
     * 
     * @param tag
     *            the DER value tag for the data, such as
     *            <em>DerValue.tag_Sequence</em>
     * @param out
     *            buffered data
     */
    public void write(byte tag, DerOutputStream out) throws IOException {
        write(tag);
        putLength(out.count);
        write(out.buf, 0, out.count);
    }

    /**
     * Writes implicitly tagged data using buffer-to-buffer copy. As above, this
     * writes a standard DER record. This is often used when efficiently
     * encapsulating implicitly tagged values.
     * 
     * @param tag
     *            the DER value of the context-specific tag that replaces
     *            original tag of the value in the output , such as in
     * 
     * <pre>
     *                <em>
     *  &lt;field&gt; [N] IMPLICIT &lt;type&gt;
     * </em>
     * </pre>
     * 
     * For example, <em>FooLength [1] IMPLICIT INTEGER</em>, with
     *            value=4; would be encoded as "81 01 04" whereas in explicit
     *            tagging it would be encoded as "A1 03 02 01 04". Notice that
     *            the tag is A1 and not 81, this is because with explicit
     *            tagging the form is always constructed.
     * @param value
     *            original value being implicitly tagged
     */
    public void writeImplicit(byte tag, DerOutputStream value)
            throws IOException {
        write(tag);
        write(value.buf, 1, value.count - 1);
    }

    /**
     * Marshals pre-encoded DER value onto the output stream.
     */
    public void putDerValue(DerValue val) throws IOException {
        val.encode(this);
    }

    /*
     * PRIMITIVES -- these are "universal" ASN.1 simple types.
     * 
     * BOOLEAN, INTEGER, BIT STRING, OCTET STRING, NULL OBJECT IDENTIFIER,
     * SEQUENCE(OF), SET(OF) PrintableString, T61String, IA5String, UTCTime
     */

    /**
     * Marshals a DER boolean on the output stream.
     */
    public void putBoolean(boolean val) throws IOException {
        write(DerValue.tag_Boolean);
        putLength(1);
        if (val) {
            write(0xff);
        } else {
            write(0);
        }
    }

    /**
     * Marshals a DER unsigned integer on the output stream.
     */
    public void putInteger(BigInt i) throws IOException {
        putUnsignedInteger(i.toByteArray());
    }

    /**
     * Marshals a DER unsigned integer on the output stream.
     */
    public void putUnsignedInteger(byte[] integerBytes) throws IOException {

        write(DerValue.tag_Integer);
        if ((integerBytes[0] & 0x080) != 0) {
            /*
             * prepend zero so it's not read as a negative number
             */
            putLength(integerBytes.length + 1);
            write(0);
        } else
            putLength(integerBytes.length);
        write(integerBytes, 0, integerBytes.length);
    }

    /**
     * Marshals a DER enumerated value on the output stream.
     */
    public void putEnumerated(int i) throws IOException {
        write(DerValue.tag_Enumerated);

        int bytemask = 0xff000000;
        int signmask = 0x80000000;
        int length;
        if ((i & 0x80000000) != 0) {
            // negative case
            for (length = 4; length > 1; --length) {
                if ((i & bytemask) != bytemask)
                    break;
                bytemask = bytemask >>> 8;
                signmask = signmask >>> 8;
            }
            if ((i & signmask) == 0) {
                // ensure negative case
                putLength(length + 1);
                write(0xff);
            } else {
                putLength(length);
            }
            // unrolled loop
            switch (length) {
            case 4:
                write((byte) (i >>> 24));
            case 3:
                write((byte) (i >>> 16));
            case 2:
                write((byte) (i >>> 8));
            case 1:
                write((byte) i);
            }
        } else {
            // positive case
            for (length = 4; length > 0; --length) {
                if ((i & bytemask) != 0)
                    break;
                bytemask = bytemask >>> 8;
                signmask = signmask >>> 8;
            }
            if ((i & signmask) != 0) {
                // ensure posititive case
                putLength(length + 1);
                write(0x00);
            } else {
                putLength(length);
            }
            // unrolled loop
            switch (length) {
            case 4:
                write((byte) (i >>> 24));
            case 3:
                write((byte) (i >>> 16));
            case 2:
                write((byte) (i >>> 8));
            case 1:
                write((byte) i);
            }
        }
    }

    /**
     * Marshals a DER bit string on the output stream. The bit string must be
     * byte-aligned.
     * 
     * @param bits
     *            the bit string, MSB first
     */
    public void putBitString(byte[] bits) throws IOException {
        write(DerValue.tag_BitString);
        putLength(bits.length + 1);
        write(0); // all of last octet is used
        write(bits);
    }

    /**
     * Converts a boolean array to a BitArray. Trims trailing 0 bits in
     * accordance with DER encoding standard. We assume the input is not null.
     */
    private static BitArray toBitArray(boolean[] bitString) {
        if (bitString.length == 0) {
            return new BitArray(bitString);
        }

        // find index of last 1 bit. -1 if there aren't any
        int i;
        for (i = bitString.length - 1; i >= 0; i--) {
            if (bitString[i]) {
                break;
            }
        }
        int length = i + 1;

        // if length changed, copy to new appropriately-sized array
        if (length != bitString.length) {
            boolean[] newBitString = new boolean[length];
            System.arraycopy(bitString, 0, newBitString, 0, length);
            bitString = newBitString;
        }

        return new BitArray(bitString);
    }

    /**
     * Converts bit string to a BitArray, stripping off trailing 0 bits. We
     * assume that the bit string is not null.
     */
    private static BitArray toBitArray(byte[] bitString) {
        // compute length in bits of bit string
        int length, i;
        int maxIndex = 0;

        if (bitString.length == 0) {
            return new BitArray(0, bitString);
        }

        // find the index of the last byte with a 1 bit
        for (i = 0; i < bitString.length; i++) {
            if (bitString[i] != 0) {
                maxIndex = i;
            }
        }
        byte lastByte = bitString[maxIndex];
        length = (maxIndex + 1) * 8; // maximum, might reduce in next step

        // now find the last 1 bit in this last byte
        for (i = 1; i <= 0x80; i <<= 1) {
            if ((lastByte & i) == 0) {
                length--;
            } else {
                break;
            }
        }
        return new BitArray(length, bitString);
    }

    /**
     * Marshals a DER bit string on the output stream. The bit strings need not
     * be byte-aligned.
     * 
     * @param ba bitarray, MSB first
     */
    public void putUnalignedBitString(BitArray ba) throws IOException {
        byte[] bits = ba.toByteArray();

        write(DerValue.tag_BitString);
        putLength(bits.length + 1);
        write(bits.length * 8 - ba.length()); // excess bits in last octet
        write(bits);
    }

    /**
     * Marshals a DER bit string on the output stream. All trailing 0 bits will
     * be stripped off in accordance with DER encoding.
     * 
     * @param bitString the bit string, MSB first
     */
    public void putUnalignedBitString(byte[] bitString) throws IOException {
        putUnalignedBitString(toBitArray(bitString));
    }

    /**
     * Marshals a DER bit string on the output stream. All trailing 0 bits will
     * be stripped off in accordance with DER encoding.
     * 
     * @param bitString the bit string as an array of booleans.
     */
    public void putUnalignedBitString(boolean[] bitString) throws IOException {
        putUnalignedBitString(toBitArray(bitString));
    }

    /**
     * DER-encodes an ASN.1 OCTET STRING value on the output stream.
     * 
     * @param octets
     *            the octet string
     */
    public void putOctetString(byte[] octets) throws IOException {
        write(DerValue.tag_OctetString, octets);
    }

    /**
     * Marshals a DER "null" value on the output stream. These are often used to
     * indicate optional values which have been omitted.
     */
    public void putNull() throws IOException {
        write(DerValue.tag_Null);
        putLength(0);
    }

    /**
     * Marshals an object identifier (OID) on the output stream. Corresponds to
     * the ASN.1 "OBJECT IDENTIFIER" construct.
     */
    public void putOID(ObjectIdentifier oid) throws IOException {
        oid.encode(this);
    }

    /**
     * Marshals a sequence on the output stream. This supports both the ASN.1
     * "SEQUENCE" (zero to N values) and "SEQUENCE OF" (one to N values)
     * constructs.
     */
    public void putSequence(DerValue[] seq) throws IOException {
        DerOutputStream bytes = new DerOutputStream();
        int i;

        for (i = 0; i < seq.length; i++)
            seq[i].encode(bytes);

        write(DerValue.tag_Sequence, bytes);
    }

    /**
     * Marshals the contents of a set on the output stream without ordering the
     * elements. Ok for BER encoding, but not for DER encoding.
     * 
     * For DER encoding, use orderedPutSet() or orderedPutSetOf().
     */
    public void putSet(DerValue[] set) throws IOException {
        DerOutputStream bytes = new DerOutputStream();
        int i;

        for (i = 0; i < set.length; i++)
            set[i].encode(bytes);

        write(DerValue.tag_Set, bytes);
    }

    /**
     * NSCP : Like putOrderSetOf, except not sorted. This may defy DER encoding
     * but is needed for compatibility with communicator.
     */
    public void putSet(byte tag, DerEncoder[] set) throws IOException {
        putOrderedSet(tag, set, null);
    }

    /**
     * Marshals the contents of a set on the output stream. Sets are
     * semantically unordered, but DER requires that encodings of set elements
     * be sorted into ascending lexicographical order before being output. Hence
     * sets with the same tags and elements have the same DER encoding.
     * 
     * This method supports the ASN.1 "SET OF" construct, but not "SET", which
     * uses a different order.
     */
    public void putOrderedSetOf(byte tag, DerEncoder[] set) throws IOException {
        putOrderedSet(tag, set, lexOrder);
    }

    /**
     * Marshals the contents of a set on the output stream. Sets are
     * semantically unordered, but DER requires that encodings of set elements
     * be sorted into ascending tag order before being output. Hence sets with
     * the same tags and elements have the same DER encoding.
     * 
     * This method supports the ASN.1 "SET" construct, but not "SET OF", which
     * uses a different order.
     */
    public void putOrderedSet(byte tag, DerEncoder[] set) throws IOException {
        putOrderedSet(tag, set, tagOrder);
    }

    /**
     * Lexicographical order comparison on byte arrays, for ordering elements of
     * a SET OF objects in DER encoding.
     */
    private static ByteArrayLexOrder lexOrder = new ByteArrayLexOrder();

    /**
     * Tag order comparison on byte arrays, for ordering elements of SET objects
     * in DER encoding.
     */
    private static ByteArrayTagOrder tagOrder = new ByteArrayTagOrder();

    /**
     * Marshals a the contents of a set on the output stream with the encodings
     * of its sorted in increasing order.
     * 
     * @param order
     *            the order to use when sorting encodings of components.
     */
    private void putOrderedSet(byte tag, DerEncoder[] set, Comparator order)
            throws IOException {
        DerOutputStream[] streams = new DerOutputStream[set.length];

        for (int i = 0; i < set.length; i++) {
            streams[i] = new DerOutputStream();
            set[i].derEncode(streams[i]);
        }

        // order the element encodings
        byte[][] bufs = new byte[streams.length][];
        for (int i = 0; i < streams.length; i++) {
            bufs[i] = streams[i].toByteArray();
        }
        if (order != null) {
            Arrays.sort(bufs, order);
        }

        DerOutputStream bytes = new DerOutputStream();
        for (int i = 0; i < streams.length; i++) {
            bytes.write(bufs[i]);
        }
        write(tag, bytes);

    }

    /**
     * Converts string to printable and writes to der output stream.
     */
    public void putPrintableString(String s) throws IOException {
        putStringType(DerValue.tag_PrintableString, s);
    }

    public void putVisibleString(String s) throws IOException {
        putStringType(DerValue.tag_VisibleString, s);
    }

    /**
     * Marshals a string which is consists of BMP (unicode) characters
     */
    public void putBMPString(String s) throws IOException {
        putStringType(DerValue.tag_BMPString, s);
    }

    public void putUTF8String(String s) throws IOException {
        putStringType(DerValue.tag_UTF8String, s);
    }

    /**
     * Marshals a string which is consists of IA5(ASCII) characters
     */
    public void putIA5String(String s) throws IOException {
        putStringType(DerValue.tag_IA5String, s);
    }

    public void putStringType(byte tag, String s) throws IOException {
        int next_byte_index;
        CharToByteConverter cbc;
        byte buf[];
        try {
            cbc = ASN1CharStrConvMap.getDefault().getCBC(tag);
            if (cbc == null)
                throw new IOException("No character to byte converter for tag");
            buf = new byte[cbc.getMaxBytesPerChar() * s.length()];
            // Don't use convertAll() here b/c it does not throw
            // UnknownCharacterException.
            next_byte_index = cbc.convert(s.toCharArray(), 0, s.length(), buf,
                    0, buf.length);
        } catch (java.io.CharConversionException e) {
            throw new IOException("Not a valid string type " + tag);
        } catch (IllegalAccessException e) {
            throw new IOException("Cannot load CharToByteConverter class "
                    + "for DER tag " + tag);
        } catch (InstantiationException e) {
            throw new IOException("Cannot instantiate CharToByteConverter "
                    + "class for DER tag " + tag);
        }

        // next_byte_index = cbc.nextByteIndex();
        write(tag);

        // for BMPString, it generates 0xFE 0xFF at the beginning which are not
        // compatible with MS cert. Remove it here.
        int start = 0;

        if ((tag == DerValue.tag_BMPString) && (buf[0] == (byte) 0xFE)) {
            start = 2;
            next_byte_index -= 2;
        }

        putLength(next_byte_index);
        write(buf, start, next_byte_index);

    }

    private void put2DateBytes(byte[] buffer, int value, int offset) {
        int upper = value / 10;
        int lower = value % 10;
        buffer[offset] = (byte) ((byte) upper + (byte) '0');
        buffer[offset + 1] = (byte) ((byte) lower + (byte) '0');
    }

    private static Calendar GMTGregorianCalendar = null;

    private Calendar getGMTGregorianCalendar() {
        if (GMTGregorianCalendar == null) {
            TimeZone tz = TimeZone.getTimeZone("GMT");
            GMTGregorianCalendar = new GregorianCalendar(tz);
        }
        return (Calendar) GMTGregorianCalendar.clone();
    }

    public byte[] getDateBytes(Date d, boolean UTC) {

        byte[] datebytes;

        if (UTC) {
            datebytes = new byte[13];
        } else { // generalized time has 4 digits for yr
            datebytes = new byte[15];
        }

        Calendar cal = getGMTGregorianCalendar();
        cal.setTime(d);

        int i = 0;
        if (!UTC) {
            put2DateBytes(datebytes, cal.get(Calendar.YEAR) / 100, i);
            i += 2;
        }
        put2DateBytes(datebytes, cal.get(Calendar.YEAR) % 100, i);
        // Calendar's MONTH is zero-based
        i += 2;
        put2DateBytes(datebytes, cal.get(Calendar.MONTH) + 1, i);
        i += 2;
        put2DateBytes(datebytes, cal.get(Calendar.DAY_OF_MONTH), i);
        i += 2;
        put2DateBytes(datebytes, cal.get(Calendar.HOUR_OF_DAY), i);
        i += 2;
        put2DateBytes(datebytes, cal.get(Calendar.MINUTE), i);
        i += 2;
        put2DateBytes(datebytes, cal.get(Calendar.SECOND), i);
        i += 2;
        // datebytes[i] = 'Z';
        datebytes[i] = (byte) 'Z';

        return datebytes;
    }

    /**
     * Marshals a DER UTC time/date value.
     * 
     * <P>
     * YYMMDDhhmmss{Z|+hhmm|-hhmm} ... emits only using Zulu time and with
     * seconds (even if seconds=0) as per IETF-PKIX partI.
     */
    public void putUTCTime(Date d) throws IOException {
        /*
         * Format the date.
         */

        // This was the old code. Way too slow to be usable (stevep)
        // String pattern = "yyMMddHHmmss'Z'";
        // SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        // TimeZone tz = TimeZone.getTimeZone("GMT");
        // sdf.setTimeZone(tz);
        // byte[] utc = (sdf.format(d)).getBytes();
        byte[] datebytes = getDateBytes(d, true); // UTC = true

        /*
         * Write the formatted date.
         */
        write(DerValue.tag_UtcTime);
        putLength(datebytes.length);
        write(datebytes);
    }

    /**
     * Marshals a DER Generalized Time/date value.
     * 
     * <P>
     * YYYYMMDDhhmmss{Z|+hhmm|-hhmm} ... emits only using Zulu time and with
     * seconds (even if seconds=0) as per IETF-PKIX partI.
     */
    public void putGeneralizedTime(Date d) throws IOException {
        /*
         * Format the date.
         */
        TimeZone tz = TimeZone.getTimeZone("GMT");

        // This is way too slow to be usable (stevep)
        String pattern = "yyyyMMddHHmmss'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(tz);
        byte[] gt = (sdf.format(d)).getBytes();

        /*
         * Write the formatted date.
         */
        write(DerValue.tag_GeneralizedTime);
        putLength(gt.length);
        write(gt);
    }

    /**
     * Put the encoding of the length in the stream.
     * 
     * @param len
     *            the length of the attribute.
     * @exception IOException
     *                on writing errors.
     */
    public void putLength(int len) throws IOException {
        if (len < 128) {
            write((byte) len);

        } else if (len < (1 << 8)) {
            write((byte) 0x081);
            write((byte) len);

        } else if (len < (1 << 16)) {
            write((byte) 0x082);
            write((byte) (len >> 8));
            write((byte) len);

        } else if (len < (1 << 24)) {
            write((byte) 0x083);
            write((byte) (len >> 16));
            write((byte) (len >> 8));
            write((byte) len);

        } else {
            write((byte) 0x084);
            write((byte) (len >> 24));
            write((byte) (len >> 16));
            write((byte) (len >> 8));
            write((byte) len);
        }
    }

    /**
     * Put the tag of the attribute in the stream.
     * 
     * @param tagClass
     *            class the tag class type, one of UNIVERSAL, CONTEXT,
     *            APPLICATION or PRIVATE
     * @param form
     *            if true, the value is constructed, otherwise it is primitive.
     * @param val
     *            the tag value
     */
    public void putTag(byte tagClass, boolean form, byte val) {
        byte tag = (byte) (tagClass | val);
        if (form) {
            tag |= (byte) 0x20;
        }
        write(tag);
    }

    /**
     * Write the current contents of this <code>DerOutputStream</code> to an
     * <code>OutputStream</code>.
     * 
     * @exception IOException
     *                on output error.
     */
    public void derEncode(OutputStream out) throws IOException {
        out.write(toByteArray());
    }
}
