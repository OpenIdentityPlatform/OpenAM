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
 * $Id: TelephoneValidator.java,v 1.3 2008/06/25 05:41:48 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums.validation;

/**
 * Validates telephone number The object of this class is constructed using
 * default constructor and validate function is used to validate telephone
 * number passing the telephone number as argument to the function. Upon
 * validation the validate function returns boolean value.
 * @supported.all.api
 */
public class TelephoneValidator implements IValidator {

    /**
     * Determines if the value is a valid "basic" telephone number string
     * 
     * @param value
     *            string value to validate
     * @param rule
     *            not used by this method
     * @return true if the value represents a valid telephone number string
     */
    public boolean validate(String value, String rule) {
        return validate(value);
    }

    /**
     * Determines whether the specified string is a valid "basic" telephone
     * number string
     * 
     * @param telephone
     *            string telephone to validate
     * @return true if telephone is a valid telephone number string
     */
    public boolean validate(String telephone) {
        char aChar;

        StringBuilder buf = new StringBuilder(telephone);

        for (int aIndex = 0; aIndex < buf.length(); aIndex++) {
            aChar = buf.charAt(aIndex);

            if (!Character.isSpaceChar(aChar)) {
                if (!Character.isDigit(aChar) && !isValidTelephoneChars(aChar)) 
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Determines whether the specified string is a valid telephone character
     * like: ()-.
     */
    private boolean isValidTelephoneChars(char theChar) {
        int aVal = theChar;

        // check if value is
        // '()-.' in ascii
        if ((aVal == 40) || (aVal == 41) || (aVal == 45) || (aVal == 46)) {
            return true;
        } else {
            return false;
        }
    }

}
