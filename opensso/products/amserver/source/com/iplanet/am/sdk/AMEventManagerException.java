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
 * $Id: AMEventManagerException.java,v 1.3 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

// Imports
import com.iplanet.ums.UMSException;

/**
 * This is a specific typed exception used to indicate some sort of Dpro Event
 * Manager exception. This class is a subclass of the AMException class.
 * 
 * @see AMException
 * @see java.lang.Exception
 * @see java.lang.Throwable
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public class AMEventManagerException extends AMException {
    public AMEventManagerException(String msg, String errorCode) {
        super(msg, errorCode);
    }

    public AMEventManagerException(
            String msg, String errorCode, UMSException ue) {
        super(msg, errorCode, ue);
    }

    public AMEventManagerException(
            String msg, String errorCode, Object args[]) {
        super(msg, errorCode, args);
    }

    public AMEventManagerException(
            String msg, String errorCode, Object args[], UMSException ue) {
        super(msg, errorCode, args, ue);
    }

}
