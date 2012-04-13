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
 * $Id: DNValidator.java,v 1.4 2009/01/28 05:34:52 ww203982 Exp $
 *
 */

package com.iplanet.ums.validation;

import com.sun.identity.shared.ldap.util.DN;

/**
 *
 * @supported.all.api
 */
public class DNValidator implements IValidator {

    /**
     * Determines whether the specified string is a valid
     * DN
     * 
     * @param value
     *            string value to validate
     * @param rule
     *            not used by this method
     * @return true if value is an valid DN, else return false
     */
    public boolean validate(String value, String rule) {
        return validate(value);
    }

    /**
     * Determines whether the specified string is a valid
     * DN
     * 
     * @param value
     *            string to test
     * @return true if value is an DN
     */
    public boolean validate(String value) {
        if (DN.isDN(value)) {
            return true;
        } else {
            return false;
        }
    }
}
