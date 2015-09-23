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
 * Class representing the structure of the Service-Type field as specified in section 5.6 of RFC 2865 and able to be
 * instantiated from the on-the-wire bytes or model objects.
 */
public class ServiceTypeAttribute extends Attribute {
    /**
     * The Login type.
     */
    public static final int LOGIN = 1;
    /**
     * The Framed type.
     */
    public static final int FRAMED = 2;
    /**
     * The Callback Login type.
     */
    public static final int CALLBACK_LOGIN = 3;
    /**
     * The Callback Framed type.
     */
    public static final int CALLBACK_FRAMED = 4;
    /**
     * The Outbound type.
     */
    public static final int OUTBOUND = 5;
    /**
     * The Administrative type.
     */
    public static final int ADMINSITRATIVE = 6;
    /**
     * The NAS Prompt type.
     */
    public static final int NAS_PROMPT = 7;
    /**
     * The Authenticate Only type.
     */
    public static final int AUTHENTICATE_ONLY = 8;
    /**
     * The Callback NAS Prompt type.
     */
    public static final int CALLBACK_NAS_PROMPT = 9;
    /**
     * The Call Check type.
     */
    public static final int CALL_CHECK = 10;
    /**
     * The Callback Administrative type.
     */
    public static final int CALLBACK_ADMINISTRATIVE = 11;

    /**
     * The type of service the user has requested, or the type of service to be provided.
     */
    private int type = 0;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public ServiceTypeAttribute(byte[] octets) {
        super(octets);
        type = OctetUtils.toIntVal(octets);
    }

    /**
     * Create an instance to contain the indicated type.
     *
     * @param type the service type.
     */
    public ServiceTypeAttribute(int type) {
        super(OctetUtils.toOctets(AttributeType.SERVICE_TYPE, type));
        this.type = type;
    }

    /**
     * Return the contained service type value.
     *
     * @return the service type value.
     */
    public int getServiceType() {
        return type;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return new StringBuilder().append(type).toString();
    }
}
