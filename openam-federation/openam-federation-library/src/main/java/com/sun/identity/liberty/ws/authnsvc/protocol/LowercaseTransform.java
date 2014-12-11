/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LowercaseTransform.java,v 1.2 2008/06/25 05:47:08 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.authnsvc.protocol;

import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;

/**
 * The <code>LowercaseTransform</code> class represents a
 * <code>Transform</code> that replaces all upprtcase characters with
 * lowercase characters.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class LowercaseTransform extends Transform {

    /**
     * This is the default constructor.
     */
    public LowercaseTransform() {
        name = LOWERCASE_URI;
    }

    /**
     * Transforms password.
     * @param password original password
     * @return transformed password
     */
    public String transform(String password)
    {
        if (AuthnSvcUtils.debug.messageEnabled()) {
            AuthnSvcUtils.debug.message("LowercaseTransform.transform");
        }

        if (password == null) {
            return null;
        }

        return password.toLowerCase();
    }
}
