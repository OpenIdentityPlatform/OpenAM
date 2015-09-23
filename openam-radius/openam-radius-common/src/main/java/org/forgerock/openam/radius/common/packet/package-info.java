/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions Copyrighted 2011 ForgeRock AS
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */

/**
 * This package in large part contains the objects that model the RADIUS protocol specified in RFC 2865. The objects
 * that implement the protocol include the four packet types, authenticators for request and response packet types,
 * and all attribute fields. All attribute fields support two constructors. To be clear RFC 2865 refers to the
 * on-the-wire bytes that represent a RADIUS object as the octets of that object where an octet is a single eight bit
 * byte. One constructor for each attribute takes the octets and is responsible for extracting the model type
 * represented by the value of that attribute. The other constructor takes the model object represented by the value
 * of that attribute and generates the octets that can we sent on the wire for that attribute.
 *
 * When a RADIUS packet is received either in the RADIUS authentication module acting as a RADIUS client or in
 * the RADIUS server, the on-the-wire octets are passed to the
 * {@link org.forgerock.openam.radius.common.PacketFactory} which splits instantiates the
 * appropriate packet type with injected id, authenticator, and attributes found in the received octets.
 *
 * When preparing a packet from model objects in order to send it, the constructor on the packet type is used and
 * then the attributes to be sent within the packet are then injected in. Once the packet is reacy its getOctets
 * method will return the on-the-wire representation of the packet.
 *
 */
package org.forgerock.openam.radius.common.packet;
