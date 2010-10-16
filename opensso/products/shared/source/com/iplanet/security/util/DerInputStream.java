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
 * $Id: DerInputStream.java,v 1.2 2008/06/25 05:52:44 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

import sun.security.util.BitArray;

/**
 * A DER input stream, used for parsing ASN.1 DER-encoded data such as that
 * found in X.509 certificates. DER is a subset of BER/1, which has the
 * advantage that it allows only a single encoding of primitive data. (High
 * level data such as dates still support many encodings.) That is, it uses the
 * "Definite" Encoding Rules (DER) not the "Basic" ones (BER).
 * 
 * <P>
 * Note that, like BER/1, DER streams are streams of explicitly tagged data
 * values. Accordingly, this programming interface does not expose any variant
 * of the java.io.InputStream interface, since that kind of input stream holds
 * untagged data values and using that I/O model could prevent correct parsing
 * of the DER data.
 * 
 * <P>
 * At this time, this class supports only a subset of the types of DER data
 * encodings which are defined. That subset is sufficient for parsing most X.509
 * certificates.
 * 
 */
public class DerInputStream {
    /*
     * This version only supports fully buffered DER. This is easy to work with,
     * though if large objects are manipulated DER becomes awkward to deal with.
     * That's where BER is useful, since BER handles streaming data relatively
     * well.
     */
    DerInputBuffer buffer;

    /**
     * Create a DER input stream from a data buffer. The buffer is not copied,
     * it is shared. Accordingly, the buffer should be treated as read-only.
     * 
     * @param data
     *            the buffer from which to create the string (CONSUMED)
     */
    public DerInputStream(byte[] data) {
        buffer = new DerInputBuffer(data);
        buffer.mark(Integer.MAX_VALUE);
    }

    /**
     * Create a DER input stream from part of a data buffer. The buffer is not
     * copied, it is shared. Accordingly, the buffer should be treated as
     * read-only.
     * 
     * @param data
     *            the buffer from which to create the string (CONSUMED)
     * @param offset
     *            the first index of <em>data</em> which will be read as DER
     *            input in the new stream
     * @param len
     *            how long a chunk of the buffer to use, starting at "offset"
     */
    public DerInputStream(byte[] data, int offset, int len) {
        buffer = new DerInputBuffer(data, offset, len);
        buffer.mark(Integer.MAX_VALUE);
    }

    DerInputStream(DerInputBuffer buf) {
        buffer = buf;
        buffer.mark(Integer.MAX_VALUE);
    }

    /**
     * Creates a new DER input stream from part of this input stream.
     * 
     * @param len
     *            how long a chunk of the current input stream to use, starting
     *            at the current position.
     * @param do_skip
     *            true if the existing data in the input stream should be
     *            skipped. If this value is false, the next data read on this
     *            stream and the newly created stream will be the same.
     */
    public DerInputStream subStream(int len, boolean do_skip)
            throws IOException {
        DerInputBuffer newbuf = buffer.dup();

        newbuf.truncate(len);
        if (do_skip)
            buffer.skip(len);
        return new DerInputStream(newbuf);
    }

    /**
     * Return what has been written to this DerInputStream as a byte array.
     * Useful for debugging.
     */
    public byte[] toByteArray() {
        return buffer.toByteArray();
    }

    /*
     * PRIMITIVES -- these are "universal" ASN.1 simple types.
     * 
     * INTEGER, BIT STRING, OCTET STRING, NULL OBJECT IDENTIFIER, SEQUENCE (OF),
     * SET (OF) PrintableString, T61String, IA5String, UTCTime
     */

    /**
     * Get an (unsigned) integer from the input stream.
     */
    public BigInt getInteger() throws IOException {
        if (buffer.read() != DerValue.tag_Integer)
            throw new IOException("DER input, Integer tag error");

        return buffer.getUnsigned(getLength(buffer));
    }

    /**
     * Get a bit string from the input stream. Only octet-aligned bitstrings
     * (multiples of eight bits in length) are handled by this method.
     */
    public byte[] getBitString() throws IOException {
        if (buffer.read() != DerValue.tag_BitString)
            throw new IOException("DER input not an bit string");
        int length = getLength(buffer);

        /*
         * This byte affects alignment and padding (for the last byte). Use
         * getUnalignedBitString() for none 8-bit aligned bit strings.
         */
        if (buffer.read() != 0)
            return null;
        length--;

        /*
         * Just read the data into an aligned, padded octet buffer.
         */
        byte[] retval = new byte[length];
        if (buffer.read(retval) != length)
            throw new IOException("short read of DER bit string");
        return retval;
    }

    /**
     * Get a bit string from the input stream. The bit string need not be
     * byte-aligned.
     */
    public BitArray getUnalignedBitString() throws IOException {
        if (buffer.read() != DerValue.tag_BitString)
            throw new IOException("DER input not a bit string");

        int length = getLength(buffer) - 1;

        /*
         * First byte = number of excess bits in the last octet of the
         * representation.
         */
        int validBits = length * 8 - buffer.read();

        byte[] repn = new byte[length];

        if (buffer.read(repn) != length)
            throw new IOException("short read of DER bit string");
        return new BitArray(validBits, repn);
    }

    /**
     * Returns an ASN.1 OCTET STRING from the input stream.
     */
    public byte[] getOctetString() throws IOException {
        if (buffer.read() != DerValue.tag_OctetString)
            throw new IOException("DER input not an octet string");

        int length = getLength(buffer);
        byte[] retval = new byte[length];
        if (buffer.read(retval) != length)
            throw new IOException("short read of DER octet string");

        return retval;
    }

    /**
     * Returns the asked number of bytes from the input stream.
     */
    public void getBytes(byte[] val) throws IOException {
        if (val.length != 0) {
            if (buffer.read(val) != val.length) {
                throw new IOException("short read of DER octet string");
            }
        }
    }

    /**
     * Reads an encoded null value from the input stream.
     */
    public void getNull() throws IOException {
        if (buffer.read() != DerValue.tag_Null || buffer.read() != 0)
            throw new IOException("getNull, bad data");
    }

    /**
     * Reads an X.200 style Object Identifier from the stream.
     */
    public ObjectIdentifier getOID() throws IOException {
        return new ObjectIdentifier(this);
    }

    /**
     * Return a sequence of encoded entities. ASN.1 sequences are ordered, and
     * they are often used, like a "struct" in C or C++, to group data values.
     * They may have optional or context specific values.
     * 
     * @param startLen
     *            guess about how long the sequence will be (used to initialize
     *            an auto-growing data structure)
     * @return array of the values in the sequence
     */
    public DerValue[] getSequence(int startLen) throws IOException {
        if (buffer.read() != DerValue.tag_Sequence)
            throw new IOException("Sequence tag error");
        return readVector(startLen);
    }

    /**
     * Return a set of encoded entities. ASN.1 sets are unordered, though DER
     * may specify an order for some kinds of sets (such as the attributes in an
     * X.500 relative distinguished name) to facilitate binary comparisons of
     * encoded values.
     * 
     * @param startLen
     *            guess about how large the set will be (used to initialize an
     *            auto-growing data structure)
     * @return array of the values in the sequence
     */
    public DerValue[] getSet(int startLen) throws IOException {
        if (buffer.read() != DerValue.tag_Set)
            throw new IOException("Set tag error");
        return readVector(startLen);
    }

    /**
     * Return a set of encoded entities. ASN.1 sets are unordered, though DER
     * may specify an order for some kinds of sets (such as the attributes in an
     * X.500 relative distinguished name) to facilitate binary comparisons of
     * encoded values.
     * 
     * @param startLen
     *            guess about how large the set will be (used to initialize an
     *            auto-growing data structure)
     * @param implicit
     *            if true tag is assumed implicit.
     * @return array of the values in the sequence
     */
    public DerValue[] getSet(int startLen, boolean implicit) throws IOException 
    {
        int tag = buffer.read();
        if (!implicit) {
            if (tag != DerValue.tag_Set) {
                throw new IOException("Set tag error");
            }
        }
        return (readVector(startLen));
    }

    /*
     * Read a "vector" of values ... set or sequence have the same encoding,
     * except for the initial tag, so both use this same helper routine.
     */
    protected DerValue[] readVector(int startLen) throws IOException {
        int len = getLength(buffer);
        DerInputStream newstr;

        if (len == 0)
            // return empty array instead of null, which should be
            // used only for missing optionals
            return new DerValue[0];

        /*
         * Create a temporary stream from which to read the data, unless it's
         * not really needed.
         */
        if (buffer.available() == len)
            newstr = this;
        else
            newstr = subStream(len, true);

        /*
         * Pull values out of the stream.
         */
        Vector vec = new Vector(startLen, 5);
        DerValue value;

        do {
            value = new DerValue(newstr.buffer);
            vec.addElement(value);
        } while (newstr.available() > 0);

        if (newstr.available() != 0)
            throw new IOException("extra data at end of vector");

        /*
         * Now stick them into the array we're returning.
         */
        int i, max = vec.size();
        DerValue[] retval = new DerValue[max];

        for (i = 0; i < max; i++)
            retval[i] = (DerValue) vec.elementAt(i);

        return retval;
    }

    /**
     * Get a single DER-encoded value from the input stream. It can often be
     * useful to pull a value from the stream and defer parsing it. For example,
     * you can pull a nested sequence out with one call, and only examine its
     * elements later when you really need to.
     */
    public DerValue getDerValue() throws IOException {
        return new DerValue(buffer);
    }

    public String getPrintableString() throws IOException {
        return (new DerValue(buffer)).getPrintableString();
    }

    public String getT61String() throws IOException {
        return (new DerValue(buffer)).getT61String();
    }

    public String getIA5String() throws IOException {
        return (new DerValue(buffer)).getIA5String();
    }

    public String getBMPString() throws IOException {
        return (new DerValue(buffer)).getBMPString();
    }

    public String getUniversalString() throws IOException {
        return (new DerValue(buffer)).getUniversalString();
    }

    /**
     * Get a UTC encoded time value from the input stream.
     */
    public Date getUTCTime() throws IOException {
        if (buffer.read() != DerValue.tag_UtcTime)
            throw new IOException("DER input, UTCtime tag invalid ");
        if (buffer.available() < 11)
            throw new IOException("DER input, UTCtime short input");

        int len = getLength(buffer);

        if (len < 11 || len > 17)
            throw new IOException("DER getUTCTime length error");

        /*
         * UTC time encoded as ASCII chars, YYMMDDhhmmss. If YY <= 50, we assume
         * 20YY; if YY > 50, we assume 19YY, as per IETF-PKIX part I.
         */
        int year, month, day, hour, minute, second;

        year = 10 * Character.digit((char) buffer.read(), 10);
        year += Character.digit((char) buffer.read(), 10);
        if (year <= 50) // origin 2000
            year += 2000;
        else
            year += 1900; // origin 1900

        month = 10 * Character.digit((char) buffer.read(), 10);
        month += Character.digit((char) buffer.read(), 10);
        month -= 1; // months are 0-11

        day = 10 * Character.digit((char) buffer.read(), 10);
        day += Character.digit((char) buffer.read(), 10);

        hour = 10 * Character.digit((char) buffer.read(), 10);
        hour += Character.digit((char) buffer.read(), 10);

        minute = 10 * Character.digit((char) buffer.read(), 10);
        minute += Character.digit((char) buffer.read(), 10);

        len -= 10;

        /**
         * We allow for non-encoded seconds, even though the IETF-PKIX
         * specification says that the seconds should always be encoded even if
         * it is zero.
         */

        if (len == 3 || len == 7) {
            second = 10 * Character.digit((char) buffer.read(), 10);
            second += Character.digit((char) buffer.read(), 10);
            len -= 2;
        } else
            second = 0;

        if (month < 0 || day <= 0 || month > 11 || day > 31 || hour >= 24
                || minute >= 60 || second >= 60)
            throw new IOException("Parse UTC time, invalid format");

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0); /* To clear millisecond field */
        cal.set(Calendar.ERA, GregorianCalendar.AD);
        Date readDate = cal.getTime();
        long utcTime = readDate.getTime();

        /*
         * Finally, "Z" or "+hhmm" or "-hhmm" ... offsets change hhmm
         */
        if (!(len == 1 || len == 5))
            throw new IOException("Parse UTC time, invalid offset");

        switch (buffer.read()) {
        case '+': {
            int Htmp = 10 * Character.digit((char) buffer.read(), 10);
            Htmp += Character.digit((char) buffer.read(), 10);
            int Mtmp = 10 * Character.digit((char) buffer.read(), 10);
            Mtmp += Character.digit((char) buffer.read(), 10);

            if (Htmp >= 24 || Mtmp >= 60)
                throw new IOException("Parse UTCtime, +hhmm");

            utcTime += ((Htmp * 60) + Mtmp) * 60 * 1000;
        }
            break;

        case '-': {
            int Htmp = 10 * Character.digit((char) buffer.read(), 10);
            Htmp += Character.digit((char) buffer.read(), 10);
            int Mtmp = 10 * Character.digit((char) buffer.read(), 10);
            Mtmp += Character.digit((char) buffer.read(), 10);

            if (Htmp >= 24 || Mtmp >= 60)
                throw new IOException("Parse UTCtime, -hhmm");

            utcTime -= ((Htmp * 60) + Mtmp) * 60 * 1000;
        }
            break;

        case 'Z':
            break;

        default:
            throw new IOException("Parse UTCtime, garbage offset");
        }
        readDate.setTime(utcTime);
        return readDate;
    }

    /**
     * Get a Generalized encoded time value from the input stream.
     */
    public Date getGeneralizedTime() throws IOException {
        if (buffer.read() != DerValue.tag_GeneralizedTime)
            throw new IOException("DER input, GeneralizedTime tag invalid ");

        if (buffer.available() < 13)
            throw new IOException("DER input, GeneralizedTime short input");

        int len = getLength(buffer);

        /*
         * Generalized time encoded as ASCII chars, YYYYMMDDhhmm[ss]
         */
        int year, month, day, hour, minute, second;

        year = 1000 * Character.digit((char) buffer.read(), 10);
        year += 100 * Character.digit((char) buffer.read(), 10);
        year += 10 * Character.digit((char) buffer.read(), 10);
        year += Character.digit((char) buffer.read(), 10);

        month = 10 * Character.digit((char) buffer.read(), 10);
        month += Character.digit((char) buffer.read(), 10);
        month -= 1; // Calendar months are 0-11

        day = 10 * Character.digit((char) buffer.read(), 10);
        day += Character.digit((char) buffer.read(), 10);

        hour = 10 * Character.digit((char) buffer.read(), 10);
        hour += Character.digit((char) buffer.read(), 10);

        minute = 10 * Character.digit((char) buffer.read(), 10);
        minute += Character.digit((char) buffer.read(), 10);

        len -= 12;

        /**
         * We allow for non-encoded seconds, even though the IETF-PKIX
         * specification says that the seconds should always be encoded even if
         * it is zero.
         */

        if (len == 3 || len == 7) {
            second = 10 * Character.digit((char) buffer.read(), 10);
            second += Character.digit((char) buffer.read(), 10);
            len -= 2;
        } else
            second = 0;

        if (month < 0 || day <= 0 || month > 11 || day > 31 || hour >= 24
                || minute >= 60 || second >= 60)
            throw new IOException("Parse Generalized time, invalid format");

        /*
         * Shouldn't this construct a Gregorian calendar directly??? We don't
         * really want locale dependant processing here
         */
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0); /* To clear millisecond field */
        cal.set(Calendar.ERA, GregorianCalendar.AD);
        Date readDate = cal.getTime();
        long utcTime = readDate.getTime();

        /*
         * Finally, "Z" or "+hhmm" or "-hhmm" ... offsets change hhmm
         */
        if (!(len == 1 || len == 5))
            throw new IOException("Parse Generalized time, invalid offset");

        switch (buffer.read()) {
        case '+': {
            int Htmp = 10 * Character.digit((char) buffer.read(), 10);
            Htmp += Character.digit((char) buffer.read(), 10);
            int Mtmp = 10 * Character.digit((char) buffer.read(), 10);
            Mtmp += Character.digit((char) buffer.read(), 10);

            if (Htmp >= 24 || Mtmp >= 60)
                throw new IOException("Parse GeneralizedTime, +hhmm");

            utcTime += ((Htmp * 60) + Mtmp) * 60 * 1000;
        }
            break;

        case '-': {
            int Htmp = 10 * Character.digit((char) buffer.read(), 10);
            Htmp += Character.digit((char) buffer.read(), 10);
            int Mtmp = 10 * Character.digit((char) buffer.read(), 10);
            Mtmp += Character.digit((char) buffer.read(), 10);

            if (Htmp >= 24 || Mtmp >= 60)
                throw new IOException("Parse GeneralizedTime, -hhmm");

            utcTime -= ((Htmp * 60) + Mtmp) * 60 * 1000;
        }
            break;

        case 'Z':
            break;

        default:
            throw new IOException("Parse GeneralizedTime, garbage offset");
        }
        readDate.setTime(utcTime);
        return readDate;
    }

    /*
     * Get a byte from the input stream.
     */
    // package private
    int getByte() throws IOException {
        return (0x00ff & buffer.read());
    }

    public int peekByte() throws IOException {
        return buffer.peek();
    }

    // package private
    int getLength() throws IOException {
        return getLength(buffer);
    }

    /*
     * Get a length from the input stream, allowing for at most 32 bits of
     * encoding to be used. (Not the same as getting a tagged integer!)
     */
    static int getLength(InputStream in) throws IOException {
        int value, tmp;

        tmp = in.read();
        if ((tmp & 0x080) == 0x00) { // 1 byte datum?
            value = tmp;
        } else { // no, more ...
            tmp &= 0x07f;

            /*
             * NOTE: tmp == 0 indicates BER encoded data. tmp > 4 indicates more
             * than 4Gb of data.
             */
            if (tmp <= 0 || tmp > 4)
                throw new IOException(
                        "DerInput.getLength(): lengthTag="
                                + tmp
                                + ", "
                                + ((tmp == 0) ? 
                                    "Indefinite length encoding not supported"
                                        + " or incorrect DER encoding."
                                        : "too big."));

            for (value = 0; tmp > 0; tmp--) {
                value <<= 8;
                value += 0x0ff & in.read();
            }
        }
        return value;
    }

    /**
     * Mark the current position in the buffer, so that a later call to
     * <code>reset</code> will return here.
     */
    public void mark(int value) {
        buffer.mark(value);
    }

    /**
     * Return to the position of the last <code>mark</code> call. A mark is
     * implicitly set at the beginning of the stream when it is created.
     */
    public void reset() {
        buffer.reset();
    }

    /**
     * Returns the number of bytes available for reading. This is most useful
     * for testing whether the stream is empty.
     */
    public int available() {
        return buffer.available();
    }
}
