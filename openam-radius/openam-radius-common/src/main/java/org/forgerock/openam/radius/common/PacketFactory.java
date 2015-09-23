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

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides unmarshalling from the on-the-wire radius byte stream to the corresponding java objects. The old byte array
 * mechanism is supported that was in the original radius authentication module. Additionally, the newer nio
 * {@link java.nio.ByteBuffer} added for radius server support is also available.
 */
public final class PacketFactory {
    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(PacketFactory.class.getName());

    /**
     * Make constructor private so that this utility class can not be instantiated.
     */
    private PacketFactory() {

    }

    /**
     * Unmarshalls from an array the octet format of rfc 2865 and creates the corresponding package and instantiates its
     * embedded authenticator and attributes.
     *
     * @param octets
     *            the array containing the on-the-wire bytes received for the packet.
     * @return the packet type that represents the received bytes
     */
    public static Packet toPacket(byte[] octets) {
        // for old byte array approach we may have a array longer than packet so trim ByteBuffer down to just
        // packet length to prevent attribute parsing below from running off the end of the packet and onto unrelated
        // octets. length is 3rd/4th octets in big endian, network byte order.
        int packetLen = octets[3] & 0xFF;
        packetLen |= ((octets[2] << 8) & 0xFF00);

        return toPacket(ByteBuffer.wrap(octets, 0, packetLen));
    }

    /**
     * Unmarshalls from an ByteBuffer the octet format of rfc 2865 and creates the corresponding package and
     * instantiates its embedded authenticator and attributes.
     *
     * @param data
     *            ByteBuffer containing the octets for the packet in on-the-wire-format.
     * @return the packet type representing the packet found in the buffer octets
     */
    public static Packet toPacket(ByteBuffer data) {
        // pull off the single octet packet type code field
        final byte code = data.get();
        // pull out the single octet packet id field, convert to unsigned solely for presentation when logging so
        // ids are all positive.
        final short id = (short) ((data.get()) & 0xFF);
        // pull out the two octet packet length field that indicates the total number of octest for the entire packet
        // ie: (code, id, length, authenticator, and attribute fields)
        final short datalen = data.getShort();

        // read 16 octet authenticator field
        final byte[] authData = new byte[16];
        data.get(authData);

        // now instantiate the corresponding packet object
        final PacketType type = PacketType.getPacketType(code);
        Packet pkt = null;

        switch (type) {
        case ACCESS_ACCEPT:
            pkt = new AccessAccept();
            pkt.setAuthenticator(new ResponseAuthenticator(authData));
            break;
        case ACCESS_CHALLENGE:
            pkt = new AccessChallenge();
            pkt.setAuthenticator(new ResponseAuthenticator(authData));
            break;
        case ACCESS_REJECT:
            pkt = new AccessReject();
            pkt.setAuthenticator(new ResponseAuthenticator(authData));
            break;
        case ACCESS_REQUEST:
            pkt = new AccessRequest();
            pkt.setAuthenticator(new RequestAuthenticator(authData));
            break;
        case UNKNOWN:
        default:
            LOG.log(Level.WARNING, "Unsupported packet type code '" + code + "' received. Unable to handle packet.");
            return null;
        }
        pkt.setIdentifier(id);

        // now pull out the set of octets for each attribute and instantiate the corresponding attribute type and
        // inject into the packet object
        Attribute a = null;
        while ((a = PacketFactory.nextAttribute(data)) != null) {
            pkt.addAttribute(a);
        }
        return pkt;
    }

    /**
     * Reads the next attribute out of the buffer or null if there is no more content.
     *
     * @param bfr
     *            the buffer containing the remaining unconsumed octets for the packet
     * @return an attribute instance representing the next attribute octet set pulled from the buffer
     */
    public static Attribute nextAttribute(ByteBuffer bfr) {
        // requires that ByteBuffer only contains a full radius packet and ends where the attributes end without
        // additional unrelated octets.
        if (!bfr.hasRemaining()) {
            return null;
        }
        /*
         * for AttributeFactory to create attribute objects it must receive the full on-the-wire octets for each
         * attribute. Therefore, we mark the buffer, pull out the type octet to get at the length octet so that we can
         * instantiate a byte array of the correct length. Then we reset the buffer to the mark and read the full set of
         * octets for the attribute.
         */
        bfr.mark();
        bfr.get(); // discard the attribute type octet

        // get the attribute's octet length, byte is signed so we need to convert to unsigned byte
        final byte len = bfr.get();
        final int length = (len) & 0xFF; // may have gotten sign extension so trim down to one byte

        // now reset the buffer and pull out the full set of octets including the prefixing type and length octets
        final byte[] attrData = new byte[length];
        bfr.reset();
        bfr.get(attrData); // reads the type, length, and payload

        return AttributeFactory.createAttribute(attrData);

    }
}
