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
 * Portions Copyrighted [2011] [ForgeRock AS]
 * Portions Copyrighted [2015] [Intellectual Reserve, Inc (IRI)]
 */
package org.forgerock.openam.radius.common.packet;

import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.OctetUtils;

/**
 * Class representing the structure of the Framed-Compression attribute as specified in section 5.13 of RFC 2865.
 */
public class FramedCompressionAttribute extends Attribute {
    /**
     * This compression value indicates no compression.
     */
    public static final int NONE = 0;
    /**
     * This compression value indicates VJ TCP/IP header compression.
     */
    public static final int VJ_TCP_IP_HEADER = 1;
    /**
     * This compression value indicates IPX header compression.
     */
    public static final int IPX_HEADER = 2;
    /**
     * This compression value indicates Stac-LZS compression.
     */
    public static final int STAC_LZS = 3;

    /**
     * The compression type.
     */
    private int compression = 0;

    /**
     * Construct a new instance from the compression type.
     *
     * @param compression the compression type that should be applied
     */
    public FramedCompressionAttribute(int compression) {
        super(OctetUtils.toOctets(AttributeType.FRAMED_COMPRESSION, compression));
        this.compression = compression;
    }

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public FramedCompressionAttribute(byte[] octets) {
        super(octets);
        this.compression = OctetUtils.toIntVal(octets);
    }

    /**
     * Returns the desired compression indicator.
     *
     * @return the compression indicator.
     */
    public int getCompression() {
        return compression;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {

        return new StringBuilder().append(compression).toString();
    }
}
