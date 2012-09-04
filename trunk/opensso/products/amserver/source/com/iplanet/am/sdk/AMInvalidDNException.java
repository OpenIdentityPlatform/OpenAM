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
 * $Id: AMInvalidDNException.java,v 1.4 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.ums.UMSException;

/**
 * The <code>AMInvalidDNException</code> is thrown to indicate that an invalid
 * DN was used.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */

public class AMInvalidDNException extends AMException {
    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     */

    public AMInvalidDNException(String msg, String errorCode) {
        super(msg, errorCode);
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     */

    protected AMInvalidDNException(
            String msg, String errorCode, UMSException ue) {
        super(msg, errorCode, ue);
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param args
     *            Array of arguments to replace in the error message
     */
    public AMInvalidDNException(String msg, String errorCode, Object args[]) {
        super(msg, errorCode, args);
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     */
    protected AMInvalidDNException(String msg, String errorCode, Object args[],
            UMSException ue) {
        super(msg, errorCode, args, ue);
    }

}
