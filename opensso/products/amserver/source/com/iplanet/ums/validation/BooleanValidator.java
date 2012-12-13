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
 * $Id: BooleanValidator.java,v 1.3 2008/06/25 05:41:48 qcheng Exp $
 *
 */

package com.iplanet.ums.validation;

/**
 * @supported.all.api
 */
public class BooleanValidator implements IValidator {

    /**
     * Determines whether the specified string is a valid
     * boolean value
     * 
     * @param value
     *            string value to validate
     * @param rule
     *            not used by this method
     * @return true if value is an boolean value
     */
    public boolean validate(String value, String rule) {
        // return validate( value );
        return (value.equalsIgnoreCase(rule));

    }

    /**
     * Determines whether the specified string is a valid
     * boolean value
     * 
     * @param value
     *            string to test
     * @return true if value is an boolean value
     */
    public boolean validate(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")
                || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("no"))
            return true;
        return false;
    }
}
