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
 * $Id: TruncationTransform.java,v 1.2 2008/06/25 05:47:08 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */


package com.sun.identity.liberty.ws.authnsvc.protocol;

import java.util.ArrayList;
import java.util.Iterator;

import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;

/**
 * The <code>TruncationTransform</code> class represents a
 * <code>Transform</code> that remove all subsequent characters after a given
 * number of characters have been obtained.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class TruncationTransform extends Transform {


    /**
     * This is the default constructor.
     */
    public TruncationTransform() {
        name = TRUNCATION_URI;
    }

    /**
     * Constructs <code>TruncationTransform</code> object with length.
     * @param length all subsequent characters after the length will be
     *               removed
     */
    public TruncationTransform(int length) {
        name = TRUNCATION_URI;
        Parameter parameter = new Parameter(Parameter.NAME_LENGTH, "" +length);
        parameters = new ArrayList();
        parameters.add(parameter);
    }

    /**
     * Transforms password.
     * @param password original password
     * @return transformed password
     */
    public String transform(String password)
    {
        if (AuthnSvcUtils.debug.messageEnabled()) {
            AuthnSvcUtils.debug.message("TruncationTransform.transform");
        }

        if (password == null) {
            return null;
        }

        if (parameters == null || parameters.isEmpty()) {
            if (AuthnSvcUtils.debug.warningEnabled()) {
                AuthnSvcUtils.debug.warning("TruncationTransform.transform: " +
                                            "no parameter found");
            }
            return password;
        }

        for(Iterator iter = parameters.iterator(); iter.hasNext(); ) {

            Parameter parameter = (Parameter)iter.next();
            if (parameter.getName().equals(Parameter.NAME_LENGTH)) {

                try {
                    int length = Integer.parseInt(parameter.getValue());
                    if (length < password.length() && length >= 0) {
                        return password.substring(0, length);
                    } else {
                        if (AuthnSvcUtils.debug.messageEnabled()) {
                            AuthnSvcUtils.debug.message(
                               "TruncationTransform.transform: parameter " +
                               "length value isn't less than password length");
                        }
                    }
                } catch (Exception ex) {
                    if (AuthnSvcUtils.debug.warningEnabled()) {
                        AuthnSvcUtils.debug.warning(
                                "TruncationTransform.transform: " +
                                "parameter value is not integer", ex);
                    }
                }

                return password;
            }
        }

        if (AuthnSvcUtils.debug.warningEnabled()) {
            AuthnSvcUtils.debug.warning("TruncationTransform.transform: " +
                                        "parameter 'name' not found");
        }
        return password;
    }
}
