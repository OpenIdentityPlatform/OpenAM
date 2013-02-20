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
 * $Id: AlphaValidator.java,v 1.3 2008/06/25 05:41:47 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums.validation;

/**
 * This class implements IValidator interface. Use the validate method of this
 * class to validate the string for alphabetic string. Pass the string to be
 * alphabeticvalidated and optional rule to validate function. The function
 * returns true value if the string is valid alphabetic string
 *
 * @supported.all.api
 */
public class AlphaValidator implements IValidator {

    /**
     * Determines whether the specified string is a valid
     * alphabetic string
     * 
     * @param value
     *            string value to validate
     * @param rule
     *            not used by this method
     * @return true if value is an alphabetic string
     */
    public boolean validate(String value, String rule) {
        return validate(value);

    }

    /**
     * Determines whether the specified string is a valid
     * alphabetic string
     * 
     * @param value
     *            string to test
     * @return true if value is an alphabetic string
     */
    public boolean validate(String value) {
        char aChar;

        StringBuilder buf = new StringBuilder(value);

        for (int aIndex = 0; aIndex < buf.length(); aIndex++) {
            aChar = buf.charAt(aIndex);

            if (!Character.isSpaceChar(aChar)) {
                if (!Character.isLetter(aChar)) {
                    return false;
                }
            }
        }

        return true;
    }
}
