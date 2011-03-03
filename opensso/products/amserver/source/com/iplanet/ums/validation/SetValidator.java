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
 * $Id: SetValidator.java,v 1.4 2008/06/25 05:41:48 qcheng Exp $
 *
 */

package com.iplanet.ums.validation;

import java.util.StringTokenizer;

/**
 * Validates if given value is in the set. This class implements IValidator
 * interface. Default constructor should be used to create this object. Pass the
 * string value to be validated and set of Stringvalues against which the value
 * should be validated to validate function, true is returned if the value is
 * present in the set.
 * @supported.all.api
 */
public class SetValidator implements IValidator {

    /**
     * Checks if the value is in the set. Validates the string value against the
     * rule, which is a set of elements in the form of a String, with each
     * elements separated by comma.
     * 
     * <pre>
     * Example: validate(&quot;A&quot;, &quot;A,B,C,D,F&quot;); // returns true
     * validate(&quot;408&quot;, &quot;415,650,408,510&quot;); // returns true
     * validate(&quot;770&quot;, &quot;415,650,408,510&quot;); // returns false
     * validate(408, &quot;415,650,408,510&quot;); // exception
     * </pre>
     * 
     * @param value Value to validate.
     * @param set Set used by the validation, in the form of a String with
     *            each elements separated by comma.
     * @return true if value is in the set
     */
    public boolean validate(String value, String set) {
        String[] range = stringToArray(normalize(set));
        for (int i = 0; i < range.length; i++) {
            if (value.equals(range[i])) {
                return true;
            }
        }
        return false;
    }

    private String[] stringToArray(String s) {
        if ((s == null) || (s.trim().length() == 0)) {
            return new String[0];
        }

        StringTokenizer st = new StringTokenizer(s, ",");
        String vals[] = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            vals[i++] = st.nextToken();
        }
        return vals;

    }

    private String normalize(String s) {
        String n = "";
        char[] array = s.toCharArray();
        for (int i = 0; i < array.length; i++) {
            if (!(array[i] == ' ')) {
                n = n + array[i];
            }
        }
        return n;
    }

}
