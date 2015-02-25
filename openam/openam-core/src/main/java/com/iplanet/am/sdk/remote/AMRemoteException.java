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
 * $Id: AMRemoteException.java,v 1.2 2008/06/25 05:41:26 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.remote;

public class AMRemoteException extends Exception {

    int ldapErrorCode = 0;

    String msg;

    String args[];

    String errorCode;

    public AMRemoteException(String msg, String errorCode, int ldapErrorCode,
            String args[]) {
        super(msg);
        this.ldapErrorCode = ldapErrorCode;
        this.msg = msg;
        this.args = args;
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String[] getMessageArgs() {
        return args;
    }

    public int getLDAPErrorCode() {
        return ldapErrorCode;
    }
}
