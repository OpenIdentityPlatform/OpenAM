/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AccessAccept.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Represents a RADIUS packet and all contained attribute fields.
 */
public abstract class Packet {
    /**
     * The packet type.
     */
    protected PacketType type = null;
    /**
     * The id of the packet.
     */
    protected short id = 0;
    /**
     * The authenticator field of the packet.
     */
    protected Authenticator authenticator = null;
    /**
     * The attribute instances held by this packet with ordering preserved.
     */
    protected AttributeSet attrs = new AttributeSet();

    /**
     * Constructs a new Packet of the specified type in preparation for generating its on-the-wire representation and
     * sending it.
     *
     * @param packetType
     *            The type of the packet.
     */
    public Packet(PacketType packetType) {
        type = packetType;
    }

    /**
     * Constructs a new Packet of the specified type, with the indicated identifier and authenticator .
     *
     * @param packetType
     *            The type of the packet.
     * @param id
     *            The identifier of the packet.
     * @param auth
     *            The authenticator of the packet.
     */
    public Packet(PacketType packetType, short id, Authenticator auth) {
        this(packetType);
        this.id = id;
        authenticator = auth;
    }

    /**
     * Returns the packet type.
     *
     * @return the packet type.
     */
    public PacketType getType() {
        return type;
    }

    /**
     * Returns the identifier of this packet.
     *
     * @return the identifier of this packet.
     */
    public short getIdentifier() {
        return id;
    }

    /**
     * Sets the identifier of this packet.
     *
     * @param id
     *            The identifier.
     */
    public void setIdentifier(short id) {
        this.id = id;
    }

    /**
     * Returns the authenticator for this packet.
     *
     * @return the authenticator instance for this packet.
     */
    public Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * Adds the passed-in attribute to the set of attributes for this packet preserving insertion order.
     *
     * @param attr
     *            the attribute instance to be added.
     */
    public void addAttribute(Attribute attr) {
        if (attr != null) {
            attrs.addAttribute(attr);
        }
    }

    /**
     * Returns the object that holds the attributes for the packet and maintains insertion order.
     *
     * @return the attribute set for this packet.
     */
    public AttributeSet getAttributeSet() {
        return attrs;
    }

    /**
     * Returns the attribute instance at the indicated insertion order or location order on the wire.
     *
     * @param pos
     *            the index of the attribute
     * @return the attribute instance.
     * @throws ArrayIndexOutOfBoundsException
     *             if in index is specified beyond the set of existing attributes.
     */
    public Attribute getAttributeAt(int pos) {
        return attrs.getAttributeAt(pos);
    }

    /**
     * Returns a representation of the packet. Used to log packets and their contents if logging is enabled for the
     * RADIUS server functionality.
     *
     * @return a representation of the packet and its contents.
     */
    @Override
    public String toString() {
        return "Packet [code=" + type.getTypeCode() + ",id=" + (id & 0xFF) + "]";
    }

    /**
     * Sets the authenticator instance for this packet.
     *
     * @param authenticator
     *            the authenticator.
     */
    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Get the on-the-wire octet sequence for this packet conforming to rfc 2865.
     *
     * @return the octet bytes for streaming across the wire.
     */
    public byte[] getOctets() {
        final ByteArrayOutputStream s = new ByteArrayOutputStream();
        byte[] bytes = null;
        s.write(type.getTypeCode());
        s.write(id);

        // insert two length bytes to be filled in later
        try {
            s.write(new byte[] { 0, 0 }); // two octets of length
        } catch (final IOException e) {
            // won't happen with ByteArrayOutputStream
        }

        // insert the bytes for the authenticator of this packet
        bytes = authenticator.getOctets();

        try {
            s.write(bytes);
        } catch (final IOException e) {
            // won't happen with ByteArrayOutputStream
        }

        // insert all attributes in the order in which they were inserted
        for (int i = 0; i < attrs.size(); i++) {
            final Attribute a = attrs.getAttributeAt(i);
            bytes = a.getOctets();
            try {
                s.write(bytes);
            } catch (final IOException e) {
                // won't happen with ByteArrayOutputStream
            }
        }
        final byte[] res = s.toByteArray();
        // now poke length in - in big endian - network byte order
        res[2] = ((byte) ((res.length >> 8) & 0xFF));
        res[3] = ((byte) (res.length & 0xFF));
        return res;
    }
}
