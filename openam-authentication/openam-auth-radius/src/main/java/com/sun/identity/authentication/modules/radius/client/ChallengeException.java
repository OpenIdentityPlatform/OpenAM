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
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 * Portions Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package com.sun.identity.authentication.modules.radius.client;

import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.ReplyMessageAttribute;
import org.forgerock.openam.radius.common.StateAttribute;

/**
 * Used for handling received challenge responses from radius servers.
 */
public class ChallengeException extends Exception {
    /**
     * The received packet.
     */
    private final AccessChallenge challenge;

    /**
     * Constructs an instance from the received response packet.
     *
     * @param res the response packet.
     */
    public ChallengeException(AccessChallenge res) {
        challenge = res;
    }

    /**
     * Returns the set of attributes in this packet.
     *
     * @return the set of attributes.
     */
    public AttributeSet getAttributeSet() {
        return challenge.getAttributeSet();
    }

    /**
     * Returns the state value for the packet as contained in the nested state attribute.
     *
     * @return the state value.
     */
    public String getState() {
        return ((StateAttribute) (challenge.getAttributeSet().
                getAttributeByType(AttributeType.STATE))).getState();
    }

    /**
     * Returns the value of the first incurred reply message attribute contained in the packet.
     *
     * @return the message string or the empty string if not found
     */
    public String getReplyMessage() {
        ReplyMessageAttribute a = ((ReplyMessageAttribute) (challenge.getAttributeSet().
                getAttributeByType(AttributeType.REPLY_MESSAGE)));
        if (a == null) {
            return "";
        }
        return a.getMessage();
    }
}
