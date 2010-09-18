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
 * $Id: ObjectIdentifier.java,v 1.2 2008/06/25 05:52:44 qcheng Exp $
 *
 */

package com.iplanet.security.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Represent an ISO Object Identifier.
 * 
 * <P>
 * Object Identifiers are arbitrary length hierarchical identifiers. The
 * individual components are numbers, and they define paths from the root of an
 * ISO-managed identifier space. You will sometimes see a string name used
 * instead of (or in addition to) the numerical id. These are synonyms for the
 * numerical IDs, but are not widely used since most sites do not know all the
 * requisite strings, while all sites can parse the numeric forms.
 * </P>
 * 
 * <P>
 * So for example, JavaSoft has the sole authority to assign the meaning to
 * identifiers below the 1.3.6.1.4.42.2.17 node in the hierarchy, and other
 * organizations can easily acquire the ability to assign such unique
 * identifiers.
 * </P> 
 */
final public class ObjectIdentifier implements Serializable {
    /** use serialVersionUID from JDK 1.1. for interoperability */
    private static final long serialVersionUID = 8697030238860181294L;

    /**
     * Constructs an object identifier from a string. This string should be of
     * the form 1.23.34.45.56 etc.
     */
    public ObjectIdentifier(String oid) {
        if (oid == null)
            return;

        int ch = '.';
        int start = 0;
        int end = 0;

        // Calculate length of oid
        componentLen = 0;
        while ((end = oid.indexOf(ch, start)) != -1) {
            start = end + 1;
            componentLen += 1;
        }
        componentLen += 1;
        components = new int[componentLen];

        start = 0;
        int i = 0;
        String comp = null;
        while ((end = oid.indexOf(ch, start)) != -1) {
            comp = oid.substring(start, end);
            components[i++] = Integer.valueOf(comp).intValue();
            start = end + 1;
        }
        comp = oid.substring(start);
        components[i] = Integer.valueOf(comp).intValue();
    }

    /**
     * Constructs an object ID from an array of integers. This is used to
     * construct constant object IDs.
     */
    public ObjectIdentifier(int values[]) {
        try {
            components = (int[]) values.clone();
            componentLen = values.length;
        } catch (Throwable t) {
            System.out.println("X509.ObjectIdentifier(), no cloning!");
        }
    }

    /**
     * Constructs an object ID from an ASN.1 encoded input stream. The encoding
     * of the ID in the stream uses "DER", a BER/1 subset. In this case, that
     * means a triple { typeId, length, data }.
     * 
     * <P>
     * <STRONG>NOTE:</STRONG> When an exception is thrown, the input stream has
     * not been returned to its "initial" state.
     * 
     * @param in
     *            DER-encoded data holding an object ID
     * @exception IOException
     *                indicates a decoding error
     */
    public ObjectIdentifier(DerInputStream in) throws IOException {
        byte type_id;
        int bufferEnd;

        /*
         * Object IDs are a "universal" type, and their tag needs only one byte
         * of encoding. Verify that the tag of this datum is that of an object
         * ID.
         * 
         * Then get and check the length of the ID's encoding. We set up so that
         * we can use in.available() to check for the end of this value in the
         * data stream.
         */
        type_id = (byte) in.getByte();
        if (type_id != DerValue.tag_ObjectId)
            throw new IOException(
                    "X509.ObjectIdentifier() -- data isn't an object ID"
                            + " (tag = " + type_id + ")");

        bufferEnd = in.available() - in.getLength() - 1;
        if (bufferEnd < 0)
            throw new IOException("X509.ObjectIdentifier() -- not enough data");

        initFromEncoding(in, bufferEnd);
    }

    /*
     * Build the OID from the rest of a DER input buffer; the tag and length
     * have been removed/verified
     */
    ObjectIdentifier(DerInputBuffer buf) throws IOException {
        initFromEncoding(new DerInputStream(buf), 0);
    }

    /*
     * Helper function -- get the OID from a stream, after tag and length are
     * verified.
     */
    private void initFromEncoding(DerInputStream in, int bufferEnd)
            throws IOException {

        /*
         * Now get the components ("sub IDs") one at a time. We fill a temporary
         * buffer, resizing it as needed.
         */
        int component;
        boolean first_subid = true;

        for (components = new int[allocationQuantum], componentLen = 0; in
                .available() > bufferEnd;) {
            component = getComponent(in);

            if (first_subid) {
                int X, Y;

                /*
                 * The ISO root has three children (0, 1, 2) and those nodes
                 * aren't allowed to assign IDs larger than 39. These rules are
                 * memorialized by some special casing in the BER encoding of
                 * object IDs ... or maybe it's vice versa.
                 * 
                 * NOTE: the allocation quantum is large enough that we know we
                 * don't have to reallocate here!
                 */
                if (component < 40)
                    X = 0;
                else if (component < 80)
                    X = 1;
                else
                    X = 2;
                Y = component - (X * 40);

                components[0] = X;
                components[1] = Y;
                componentLen = 2;

                first_subid = false;

            } else {

                /*
                 * Other components are encoded less exotically. The only
                 * potential trouble is the need to grow the array.
                 */
                if (componentLen >= components.length) {
                    int tmp_components[];

                    tmp_components = new int[components.length
                            + allocationQuantum];
                    System.arraycopy(components, 0, tmp_components, 0,
                            components.length);
                    components = tmp_components;
                }
                components[componentLen++] = component;
            }
        }

        /*
         * Final sanity check -- if we didn't use exactly the number of bytes
         * specified, something's quite wrong.
         */
        if (in.available() != bufferEnd) {
            throw new IOException(
                    "X509.ObjectIdentifier() -- malformed input data");
        }
    }

    /*
     * n.b. the only public interface is DerOutputStream.putOID()
     */
    void encode(DerOutputStream out) throws IOException {
        DerOutputStream bytes = new DerOutputStream();
        int i;

        bytes.write((components[0] * 40) + components[1]);
        for (i = 2; i < componentLen; i++)
            putComponent(bytes, components[i]);

        /*
         * Now that we've constructed the component, encode it in the stream we
         * were given.
         */
        out.write(DerValue.tag_ObjectId, bytes);
    }

    /*
     * Tricky OID component parsing technique ... note that one bit per octet is
     * lost, this returns at most 28 bits of component. Also, notice this parses
     * in big-endian format.
     */
    private static int getComponent(DerInputStream in) throws IOException {
        int retval, i, tmp;

        for (i = 0, retval = 0; i < 4; i++) {
            retval <<= 7;
            tmp = in.getByte();
            retval |= (tmp & 0x07f);
            if ((tmp & 0x080) == 0)
                return retval;
        }

        throw new IOException("X509.OID, component value too big");
    }

    /*
     * Reverse of the above routine. Notice it needs to emit in big-endian form,
     * so it buffers the output until it's ready. (Minimum length encoding is a
     * DER requirement.)
     */
    private static void putComponent(DerOutputStream out, int val)
            throws IOException {
        int i;
        byte buf[] = new byte[4];

        for (i = 0; i < 4; i++) {
            buf[i] = (byte) (val & 0x07f);
            val >>>= 7;
            if (val == 0)
                break;
        }
        for (; i > 0; --i)
            out.write(buf[i] | 0x080);
        out.write(buf[0]);
    }

    // XXX this API should probably facilitate the JDK sort utility

    /**
     * Compares this identifier with another, for sorting purposes. An
     * identifier does not precede itself.
     * 
     * @param other
     *            identifer that may precede this one.
     * @return true iff <em>other</em> precedes this one in a particular
     *         sorting order.
     */
    public boolean precedes(ObjectIdentifier other) {
        int i;

        // shorter IDs go first
        if (other == this || componentLen < other.componentLen)
            return false;
        if (other.componentLen < componentLen)
            return true;

        // for each component, the lesser component goes first
        for (i = 0; i < componentLen; i++) {
            if (other.components[i] < components[i])
                return true;
        }

        // identical IDs don't precede each other
        return false;
    }

    public boolean equals(Object other) {
        if (other instanceof ObjectIdentifier)
            return equals((ObjectIdentifier) other);
        else
            return false;
    }

    /**
     * Compares this identifier with another, for equality.
     * 
     * @return true iff the names are identical.
     */
    public boolean equals(ObjectIdentifier other) {
        int i;

        if (other == this)
            return true;
        if (componentLen != other.componentLen)
            return false;
        for (i = 0; i < componentLen; i++) {
            if (components[i] != other.components[i])
                return false;
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        int oflow = 0;

        for (int i = 0; i < componentLen; i++) {
            oflow = (h & 0xff800000) >> 23;
            h <<= 9;
            h += components[i];
            h ^= oflow;
        }
        return h;
    }

    /**
     * Returns a string form of the object ID. The format is the conventional
     * "dot" notation for such IDs, without any user-friendly descriptive
     * strings, since those strings will not be understood everywhere.
     */
    public String toString() {
        String retval;
        int i;

        for (i = 0, retval = ""; i < componentLen; i++) {
            if (i != 0)
                retval += ".";
            retval += components[i];
        }
        return retval;
    }

    /*
     * To simplify, we assume no individual component of an object ID is larger
     * than 32 bits. Then we represent the path from the root as an array that's
     * (usually) only filled at the beginning.
     */
    private int components[]; // path from root

    private int componentLen; // how much is used.

    private static final int allocationQuantum = 5; // >= 2

    /**
     * Netscape Enhancement: This function implements a object identifier
     * factory. It should help reduces in-memory Object Identifier object. This
     * function also provide additional checking on the OID. A valid OID should
     * start with 0, 1, or 2.
     * 
     * Notes: This function never returns null. IOException is raised in error
     * conditions.
     */
    public static java.util.Hashtable mOIDs = new java.util.Hashtable();

    public static ObjectIdentifier getObjectIdentifier(String oid)
            throws IOException {
        int value;

        if (oid == null)
            throw new IOException("empty object identifier");

        oid.trim();

        ObjectIdentifier thisOID = (ObjectIdentifier) mOIDs.get(oid);
        if (thisOID != null)
            return thisOID;

        StringTokenizer token = new StringTokenizer(oid, ".");
        value = new Integer(token.nextToken()).intValue();
        /* First token should be 0, 1, 2 */
        if (value >= 0 && value <= 2) {
            value = new Integer(token.nextToken()).intValue();
            /* Second token should be 0 <= && >= 39 */
            if (value >= 0 && value <= 39) {
                thisOID = new ObjectIdentifier(oid);
                if (thisOID.toString().equals(oid)) {
                    mOIDs.put(oid, thisOID);
                    return thisOID;
                }
                throw new IOException("invalid oid " + oid);
            } else
                throw new IOException("invalid oid " + oid);
        } else
            throw new IOException("invalid oid " + oid);
    }

    public static ObjectIdentifier getObjectIdentifier(int values[])
            throws IOException {
        String retval;
        int i;

        for (i = 0, retval = ""; i < values.length; i++) {
            if (i != 0)
                retval += ".";
            retval += values[i];
        }
        return getObjectIdentifier(retval);
    }
}
