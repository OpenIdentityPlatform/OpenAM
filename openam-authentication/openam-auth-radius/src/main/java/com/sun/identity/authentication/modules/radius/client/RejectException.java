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

import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.ReplyMessageAttribute;

/**
 * Exception used to pass a received AccessReject to calling code.
 */
public class RejectException extends Exception {
    /**
     * The AccessReject packet.
     */
    private AccessReject reject = null;

    /**
     * Construct a new instance with the received AccessReject response packet.
     *
     * @param res the AccessReject packet received.
     */
    public RejectException(AccessReject res) {
        reject = res;
    }

    /**
     * Returns the attribute set of the contained AccessReject response packet.
     *
     * @return the attribute set.
     */
    public AttributeSet getAttributeSet() {
        return reject.getAttributeSet();
    }

    /**
     * Returns the reply message if any send with the AccessReject packet.
     *
     * @return the reply message or null if not included.
     */
    public String getReplyMessage() {
        return ((ReplyMessageAttribute) (reject.getAttributeSet().
                getAttributeByType(AttributeType.REPLY_MESSAGE))).getMessage();
    }
}
