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

import java.nio.charset.Charset;

/**
 * Class representing the structure of the Reply-Message attribute as specified in section 5.18 of RFC 2865 and able to
 * be instantiated from the on-the-wire bytes. This attribute is only included in access-requests.
 */
public class ReplyMessageAttribute extends Attribute {
    /**
     * The contained message.
     */
    private String msg = null;

    /**
     * Creates a ReplyMessageAttribute from the on-the-wire octets.
     *
     * @param octets
     *            the on-the-wire bytes.
     */
    public ReplyMessageAttribute(byte[] octets) {
        super(octets);
        msg = new String(octets, 2, octets.length - 2, Charset.forName("utf-8"));
    }

    /**
     * Creates a ReplyMessageAttribute to contain the given String message prior to sending in a packet. If the String
     * is greater than 255 bytes then it is trimmed down to below that length.
     *
     * @param message
     *            the message to be embedded.
     */
    public ReplyMessageAttribute(String message) {
        super(OctetUtils.toOctets(AttributeType.REPLY_MESSAGE, message));
        // ensure message too long gets trimmed
        this.msg = new String(super.getOctets(), 2, super.getOctets().length - 2);
    }

    /**
     * Returns the contained message.
     *
     * @return the contained message.
     */
    public String getMessage() {
        return msg;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    @Override
    public String toStringImpl() {
        return msg;
    }
}
