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
 * $Id: SelectTransform.java,v 1.2 2008/06/25 05:47:08 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.authnsvc.protocol;

import java.util.ArrayList;
import java.util.Iterator;

import com.sun.identity.liberty.ws.authnsvc.AuthnSvcUtils;

/**
 * The <code>SelectTransform</code> class represents a <code>Transform</code>
 * that removes all characters except those specified in the "allowed"
 * parameter.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class SelectTransform extends Transform {


    /**
     * This is the default constructor.
     */
    public SelectTransform() {
        name = SELECT_URI;
    }

    /**
     * Constructs  <code>SelectTransform</code> with allowed characters.
     * @param allowed all characters except specified in 'allowed' will be
     *                removed
     */
    public SelectTransform(String allowed) {
        name = SELECT_URI;
        Parameter parameter = new Parameter(Parameter.NAME_ALLOWED, allowed);
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
            AuthnSvcUtils.debug.message("SelectTransform.transform");
        }

        if (parameters == null || parameters.isEmpty()) {
            if (AuthnSvcUtils.debug.warningEnabled()) {
                AuthnSvcUtils.debug.warning("SelectTransform.transform: " +
                                            "no parameter found");
            }
            return password;
        }

        for(Iterator iter = parameters.iterator(); iter.hasNext(); ) {

            Parameter parameter = (Parameter)iter.next();
            if (parameter.getName().equals(Parameter.NAME_ALLOWED)) {

                String allowed = parameter.getValue();
                if (AuthnSvcUtils.debug.messageEnabled()) {
                    AuthnSvcUtils.debug.message("SelectTransform.transform: " +
                                                "allowed = " + allowed);
                }

                if (allowed == null || allowed.length() == 0) {
                    return "";
                }

                int pLen = password.length();
                StringBuffer resultSB = new StringBuffer(pLen);
                for(int i=0; i<pLen; i++) {
                    char c = password.charAt(i);
                    if (allowed.indexOf(c) != -1) {
                        resultSB.append(c);
                    }
                }

                return resultSB.toString();
            }
        }

        if (AuthnSvcUtils.debug.warningEnabled()) {
            AuthnSvcUtils.debug.warning("SelectTransform.transform: " +
                                        "parameter 'allowed' not found");
        }
        return password;
    }
}
