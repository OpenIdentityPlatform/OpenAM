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
 * $Id: AuthI18n.java,v 1.2 2008/06/25 05:41:54 qcheng Exp $
 *
 */

package com.sun.identity.authentication.internal.util;

import com.iplanet.services.util.I18n;
import com.iplanet.ums.IUMSConstants;

public class AuthI18n {

    public static I18n authI18n = null;

    static {
        if (authI18n == null) {
            authI18n = I18n.getInstance(IUMSConstants.UMS_PKG);
        }
    }

    // public AuthI18n () {
    // if (authI18n == null) {
    // authI18n = new I18n (authComponentName);
    // }
    // }
    //
    // public static String getString(String str) {
    // if (authI18n == null) {
    // return null;
    // }
    // return authI18n.getString(str);
    // }
}
