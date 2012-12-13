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
 * $Id: DerValue.java,v 1.2 2008/06/25 05:52:44 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import sun.io.ByteToCharConverter;
import sun.security.util.BitArray;

import com.iplanet.security.x509.AVAValueConverter;
import com.iplanet.security.x509.GenericValueConverter;

/**
 * Represents a single DER-encoded value. DER encoding rules are a subset of the
 * "Basic" Encoding Rules (BER), but they only support a single way ("Definite"
 * encoding) to encode any given value.
 * 
 * <P>
 * All DER-encoded data are triples <em>{type, length, data}</em>. This class
 * represents such tagged values as they have been read (or constructed), and
 * provides structured access to the encoded data.
 * </P>
 * <P>
 * At this time, this class supports only a subset of the types of DER data
 * encodings which are defined. That subset is sufficient for parsing most X.509
 * certificates, and working with selected additional formats (such as PKCS #10
 * certificate requests, and some kinds of PKCS #7 data).
 * </P>
 */
public class DerValue {
    /** The tag class types */
    public static final byte TAG_UNIVERSAL = (byte) 0x000;

    public static final byte TAG_APPLICATION = (byte) 0x040;

    public static final byte TAG_CONTEXT = (byte) 0x080;

    public static final byte TAG_PRIVATE = (byte) 0x0c0;

    /** The DER tag of the value; one of the tag_ constants. */
    public byte tag;

    protected DerInputBuffer buffer;

    /**
     * The DER-encoded data of the value.
     */
    public DerInputStream data;

    private int length;

    /*
     * The type starts at the first byte of the encoding, and is one of these
     * tag_* values. That may be all the type data that is needed.
     */

    /*
     * These tags are the "universal" tags ... they mean the same in all
     * contexts. (Mask with 0x1f -- five bits.)
     */

    /** Tag value indicating an ASN.1 "BOOLEAN" value. */
    public final static byte tag_Boolean = 0x01;

    /** Tag value indicating an ASN.1 "INTEGER" value. */
    public final static byte tag_Integer = 0x02;

    /** Tag value indicating an ASN.1 "BIT STRING" value. */
    public final static byte tag_BitString = 0x03;

    /** Tag value indicating an ASN.1 "OCTET STRING" value. */
    public final static byte tag_OctetString = 0x04;

    /** Tag value indicating an ASN.1 "NULL" value. */
    public final static byte tag_Null = 0x05;

    /** Tag value indicating an ASN.1 "OBJECT IDENTIFIER" value. */
    public final static byte tag_ObjectId = 0x06;

    /** Tag value including an ASN.1 "ENUMERATED" value */
    public final static byte tag_Enumerated = 0x0A;

    /** Tag value including a "printable" string */
    public final static byte tag_PrintableString = 0x13;

    public final static byte tag_VisibleString = 0x1A;

    /** Tag value including a "teletype" string */
    public final static byte tag_T61String = 0x14;

    /** Tag value including an ASCII string */
    public final static byte tag_IA5String = 0x16;

    /** Tag value indicating an ASN.1 "UTCTime" value. */
    public final static byte tag_UtcTime = 0x17;

    /** Tag value indicating an ASN.1 "GeneralizedTime" value. */
    public final static byte tag_GeneralizedTime = 0x18;

    /** Tag value indicating an ASN.1 "BMPString" value. */
    public final static byte tag_BMPString = 0x1E;

    /** Tag value indicating an ASN.1 "UniversalString" value. */
    public final static byte tag_UniversalString = 0x1C;

    /** Tag value indicating an ASN.1 "UTF8String" value. (since 1998) */
    public final static byte tag_UTF8String = 0x0C;

    // CONSTRUCTED seq/set

    /**
     * Tag value indicating an ASN.1 "SEQUENCE" (zero to N elements, order is
     * significant).
     */
    public final static byte tag_Sequence = 0x30;

    /**
     * Tag value indicating an ASN.1 "SEQUENCE OF" (one to N elements, order is
     * significant).
     */
    public final static byte tag_SequenceOf = 0x30;

    /**
     * Tag value indicating an ASN.1 "SET" (zero to N members, order does not
     * matter).
     */
    public final static byte tag_Set = 0x31;

    /**
     * Tag value indicating an ASN.1 "SET OF" (one to N members, order does not
     * matter).
     */
    public final static byte tag_SetOf = 0x31;

    /*
     * These values are the high order bits for the other kinds of tags.
     */
    boolean isUniversal() {
        return ((tag & 0x0c0) == 0x000);
    }

    boolean isApplication() {
        return ((tag & 0x0c0) == 0x040);
    }

    /**
     * Returns true iff the CONTEXT SPECIFIC bit is set in the type tag. This is
     * associated with the ASN.1 "DEFINED BY" syntax.
     */
    public boolean isContextSpecific() {
        return ((tag & 0x0c0) == 0x080);
    }

    /**
     * Returns true iff the CONTEXT SPECIFIC TAG matches the passed tag.
     */
    public boolean isContextSpecific(byte cntxtTag) {
        if (!isContextSpecific()) {
            return false;
        }
        return ((tag & 0x01f) == cntxtTag);
    }

    boolean isPrivate() {
        return ((tag & 0x0c0) == 0x0c0);
    }

    /** Returns true iff the CONSTRUCTED bit is set in the type tag. */
    public boolean isConstructed() {
        return ((tag & 0x020) == 0x020);
    }

    /**
     * Creates a DER value from a string using a generic way of determining the
     * proper tag for the string. Assumes the string is a Generic attribute
     * value and uses the converter for generic string values to convert to the
     * Der Value.
     */
    public DerValue(String value) throws IOException {
        AVAValueConverter genericValue = new GenericValueConverter();
        DerValue val;

        val = genericValue.getValue(value);
        tag = val.tag;
        buffer = val.buffer;
        length = val.length;
        data = val.data;
        data.mark(Integer.MAX_VALUE);
    }

    /**
     * Creates a DerValue from a tag and some DER-encoded data.
     * 
     * @param tag
     *            the DER type tag
     * @param data
     *            the DER-encoded data
     */
    public DerValue(byte tag, byte[] data) {
        this.tag = tag;
        buffer = new DerInputBuffer((byte[]) data.clone());
        length = data.length;
        this.data = new DerInputStream(buffer);
        this.data.mark(Integer.MAX_VALUE);
    }

    /*
     * package private
     */
    DerValue(DerInputBuffer in) throws IOException {
        // NOTE: This must handle the special value used
        // to terminate BER indefinite encodings (tag and
        // length are both zero)

        // XXX must also parse BER-encoded constructed
        // values such as sequences, sets...

        tag = (byte) in.read();
        length = DerInputStream.getLength(in);

        buffer = in.dup();
        buffer.truncate(length);
        data = new DerInputStream(buffer);

        in.skip(length);
    }

    /**
     * Get an ASN.1/DER encoded datum from a buffer. The entire buffer must hold
     * exactly one datum, including its tag and length.
     * 
     * @param buf
     *            buffer holding a single DER-encoded datum.
     */
    public DerValue(byte[] buf) throws IOException {
        init(true, new ByteArrayInputStream(buf));
    }

    /**
     * Get an ASN.1/DER encoded datum from part of a buffer. That part of the
     * buffer must hold exactly one datum, including its tag and length.
     * 
     * @param buf
     *            the buffer
     * @param offset
     *            start point of the single DER-encoded dataum
     * @param len
     *            how many bytes are in the encoded datum
     */
    public DerValue(byte[] buf, int offset, int len) throws IOException {
        init(true, new ByteArrayInputStream(buf, offset, len));
    }

    /**
     * Get an ASN1/DER encoded datum from an input stream. The stream may have
     * additional data following the encoded datum.
     * 
     * @param in
     *            the input stream holding a single DER datum, which may be
     *            followed by additional data
     */
    public DerValue(InputStream in) throws IOException {
        init(false, in);
    }

    /*
     * helper routine
     */
    private void init(boolean fullyBuffered, InputStream in) throws IOException 
    {
        byte[] bytes;

        tag = (byte) in.read();
        length = DerInputStream.getLength(in);

        /*
         * if (length == 0) return;
         */

        if (fullyBuffered && in.available() != length)
            throw new IOException("extra DER value data (constructor)");

        bytes = new byte[length];

        // n.b. readFully not needed in normal fullyBuffered case
        DataInputStream dis = new DataInputStream(in);

        dis.readFully(bytes);
        buffer = new DerInputBuffer(bytes);
        data = new DerInputStream(buffer);
    }

    /**
     * Encode an ASN1/DER encoded datum onto a DER output stream.
     */
    public void encode(DerOutputStream out) throws IOException {
        out.write(tag);
        out.putLength(length);
        buffer.dump(out, length);

    }

    /**
     * Returns an ASN.1 BOOLEAN
     * 
     * @return the boolean held in this DER value
     */
    public boolean getBoolean() throws IOException {
        if (tag != tag_Boolean) {
            throw new IOException("DerValue.getBoolean, not a BOOLEAN " + tag);
        }
        if (length != 1) {
            throw new IOException("DerValue.getBoolean, invalid length "
                    + length);
        }
        if (buffer.read() != 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns an ASN.1 OBJECT IDENTIFIER.
     * 
     * @return the OID held in this DER value
     */
    public ObjectIdentifier getOID() throws IOException {
        if (tag != tag_ObjectId)
            throw new IOException("DerValue.getOID, not an OID " + tag);
        return new ObjectIdentifier(buffer);
    }

    /**
     * Returns an ASN.1 OCTET STRING
     * 
     * @return the octet string held in this DER value
     */
    public byte[] getOctetString() throws IOException {
        if (tag != tag_OctetString)
            throw new IOException(
                    "DerValue.getOctetString, not an Octet String: " + tag);

        byte[] bytes = new byte[length];

        if (buffer.read(bytes) != length)
            throw new IOException("short read on DerValue buffer");
        return bytes;
    }

    /**
     * Returns an ASN.1 unsigned integer value of enumerated value.
     * 
     * @return the (unsigned) integer held in this DER value
     */
    public int getEnumerated() throws IOException {
        if (tag != tag_Enumerated)
            throw new IOException("DerValue.getEnumerated, not an ENUMERATED "
                    + tag);
        if (length == 0)
            return 0;
        if (length > 4 || length < 1)
            throw new IOException("DerValue.getEnumerated, invalid length "
                    + length + "(must be between 1 and 4)");

        int value = 0;
        int nextbyte = buffer.read();
        if (nextbyte == -1)
            throw new IOException("short read on DerValue buffer");
        // perform sign extension
        value = (byte) nextbyte;

        for (int i = length - 1; i > 0; --i) {
            nextbyte = buffer.read();
            if (nextbyte == -1)
                throw new IOException("short read on DerValue buffer");
            value = 256 * value + nextbyte;
        }
        return value;
    }

    /**
     * Returns an ASN.1 unsigned INTEGER value.
     * 
     * @return the (unsigned) integer held in this DER value
     */
    public BigInt getInteger() throws IOException {
        if (tag != tag_Integer)
            throw new IOException("DerValue.getInteger, not an int " + tag);
        return buffer.getUnsigned(data.available());
    }

    /**
     * Returns an ASN.1 unsigned INTEGER value, the parameter determining if the
     * tag is implicit.
     * 
     * @param tagImplicit
     *            if true, ignores the tag value as it is assumed implicit.
     * @return the (unsigned) integer held in this DER value
     */
    public BigInt getInteger(boolean tagImplicit) throws IOException {
        if (!tagImplicit) {
            if (tag != tag_Integer) {
                throw new IOException("DerValue.getInteger, not an int " + tag);
            }
        }
        return buffer.getUnsigned(data.available());
    }

    /**
     * Returns an ASN.1 BIT STRING value. The bit string must be byte-aligned.
     * 
     * @return the bit string held in this value
     */
    public byte[] getBitString() throws IOException {
        if (tag != tag_BitString)
            throw new IOException("DerValue.getBitString, not a bit string "
                    + tag);

        return buffer.getBitString();
    }

    /**
     * Returns an ASN.1 BIT STRING value that need not be byte-aligned.
     * 
     * @return a BitArray representing the bit string held in this value
     */
    public BitArray getUnalignedBitString() throws IOException {
        if (tag != tag_BitString)
            throw new IOException("DerValue.getBitString, not a bit string "
                    + tag);

        return buffer.getUnalignedBitString();
    }

    /**
     * Returns the name component as a Java string, regardless of its encoding
     * restrictions (ASCII, T61, Printable, etc).
     */
    public String getAsString() throws IOException {
        AVAValueConverter genericValue = new GenericValueConverter();
        return genericValue.getAsString(this);
    }

    /**
     * Returns an ASN.1 BIT STRING value, with the tag assumed implicit based on
     * the parameter. The bit string must be byte-aligned.
     * 
     * @param tagImplicit
     *            if true, the tag is assumed implicit.
     * @return the bit string held in this value
     */
    public byte[] getBitString(boolean tagImplicit) throws IOException {
        if (!tagImplicit) {
            if (tag != tag_BitString)
                throw new IOException(
                        "DerValue.getBitString, not a bit string " + tag);
        }
        return buffer.getBitString();
    }

    /**
     * Returns an ASN.1 BIT STRING value, with the tag assumed implicit based on
     * the parameter. The bit string need not be byte-aligned.
     * 
     * @param tagImplicit
     *            if true, the tag is assumed implicit.
     * @return the bit string held in this value
     */
    public BitArray getUnalignedBitString(boolean tagImplicit)
            throws IOException {
        if (!tagImplicit) {
            if (tag != tag_BitString)
                throw new IOException(
                        "DerValue.getBitString, not a bit string " + tag);
        }
        return buffer.getUnalignedBitString();
    }

    /**
     * Returns an ASN.1 STRING value
     * 
     * @return the printable string held in this value
     */
    public String getPrintableString() throws IOException {
        if (tag != tag_PrintableString)
            throw new IOException("DerValue.getPrintableString, not a string "
                    + tag);

        return getASN1CharString();
    }

    /*
     * Internal utility ... returns a string regardless of what restrictions
     * have been placed on its encoding.
     */
    private String simpleGetString() throws IOException {
        StringBuffer s = new StringBuffer(length);
        try {
            int temp = length;

            data.reset();
            while (temp-- > 0)
                s.append((char) data.getByte());
        } catch (IOException e) {
            return null;
        }
        return new String(s);
    }

    /*
     * @eturns a string if the DerValue is a ASN.1 character string type and if
     * there is a ByteToChar converter for the type. Returns null otherwise.
     */
    public String getASN1CharString() throws IOException {
        ByteToCharConverter bcc;
        int ret;
        byte buf[];
        char cbuf[];

        try {
            bcc = ASN1CharStrConvMap.getDefault().getBCC(tag);
            if (bcc == null)
                return null;

            buf = new byte[length];
            cbuf = new char[bcc.getMaxCharsPerByte() * length];
            data.reset();
            data.getBytes(buf);
            ret = bcc.convert(buf, 0, buf.length, cbuf, 0, cbuf.length);
        } catch (java.io.CharConversionException e) {
            throw new IOException("Misformed DER value");
        } catch (IllegalAccessException e) {
            throw new IOException("Illegal Access loading ByteToCharConverter");
        } catch (InstantiationException e) {
            throw new IOException("Cannot instantiate ByteToCharConverter");
        }
        return new String(cbuf, 0, ret);
    }

    /**
     * Returns an ASN.1 T61 (Teletype) STRING value
     * 
     * @return the teletype string held in this value
     */
    public String getT61String() throws IOException {
        if (tag != tag_T61String)
            throw new IOException("DerValue.getT61String, not T61 " + tag);

        return getASN1CharString();
    }

    /**
     * Returns an ASN.1 IA5 (ASCII) STRING value
     * 
     * @return the ASCII string held in this value
     */
    public String getIA5String() throws IOException {
        if (tag != tag_IA5String)
            throw new IOException("DerValue.getIA5String, not IA5 " + tag);

        return getASN1CharString();
    }

    public String getBMPString() throws IOException {
        if (tag != tag_BMPString)
            throw new IOException("DerValue.getBMPString, not BMP " + tag);

        return getASN1CharString();
    }

    public String getUniversalString() throws IOException {
        if (tag != tag_UniversalString)
            throw new IOException(
                    "DerValue.getUniversalString, not UniversalString " + tag);

        return getASN1CharString();
    }

    public String getUTF8String() throws IOException {
        if (tag != tag_UTF8String)
            throw new IOException("DerValue.getUTF8String, not UTF8String "
                    + tag);

        return getASN1CharString();
    }

    /**
     * Returns true iff the other object is a DER value which is bitwise equal
     * to this one.
     * 
     * @param other
     *            the object being compared with this one
     */
    public boolean equals(Object other) {
        if (other instanceof DerValue)
            return equals((DerValue) other);
        else
            return false;
    }

    /**
     * Bitwise equality comparison. DER encoded values have a single encoding,
     * so that bitwise equality of the encoded values is an efficient way to
     * establish equivalence of the unencoded values.
     * 
     * @param other
     *            the object being compared with this one
     */
    public boolean equals(DerValue other) {
        data.reset();
        other.data.reset();
        if (this == other)
            return true;
        else if (tag != other.tag) {
            return false;
        } else {
            return buffer.equals(other.buffer);
        }
    }

    /**
     * Returns a printable representation of the value.
     * 
     * @return printable representation of the value
     */
    public String toString() {
        try {
            String s = getAsString();
            if (s != null)
                return s;
            if (tag == tag_Null)
                return "[DerValue, null]";
            if (tag == tag_ObjectId)
                return "OID." + getOID();

            // integers
            else
                return "[DerValue, tag = " + tag + ", length = " + length + "]";
        } catch (IOException e) {
            throw new IllegalArgumentException("misformatted DER value");
        }
    }

    /**
     * Returns a DER-encoded value, such that if it's passed to the DerValue
     * constructor, a value equivalent to "this" is returned.
     * 
     * @return DER-encoded value, including tag and length.
     */
    public byte[] toByteArray() throws IOException {
        DerOutputStream out = new DerOutputStream();

        encode(out);
        data.reset();
        return out.toByteArray();
    }

    /**
     * For "set" and "sequence" types, this function may be used to return a DER
     * stream of the members of the set or sequence. This operation is not
     * supported for primitive types such as integers or bit strings.
     */
    public DerInputStream toDerInputStream() throws IOException {
        if (tag == tag_Sequence || tag == tag_Set)
            return new DerInputStream(buffer);
        throw new IOException("toDerInputStream rejects tag type " + tag);
    }

    /**
     * Get the length of the encoded value.
     */
    public int length() {
        return length;
    }

    /**
     * Create the tag of the attribute.
     * 
     * @param tagClass
     *            class the tag class type, one of UNIVERSAL, CONTEXT,
     *            APPLICATION or PRIVATE
     * @param form
     *            if true, the value is constructed, otherwise it is primitive.
     * @param val
     *            the tag value.
     */
    public static byte createTag(byte tagClass, boolean form, byte val) {
        byte tag = (byte) (tagClass | val);
        if (form) {
            tag |= (byte) 0x20;
        }
        return (tag);
    }

    /**
     * Set the tag of the attribute. Commonly used to reset the tag value used
     * for IMPLICIT encodings.
     * 
     * @param tag
     *            the tag value
     */
    public void resetTag(byte tag) {
        this.tag = tag;
    }
}
