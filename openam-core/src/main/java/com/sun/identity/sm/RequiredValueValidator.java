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
 * $Id: RequiredValueValidator.java,v 1.2 2008/06/25 05:44:04 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import java.util.Iterator;
import java.util.Set;

/**
 * This validator checks if Set is empty or not.
 */
public class RequiredValueValidator implements ServiceAttributeValidator {
    public RequiredValueValidator() {
    }

    /**
     * Returns true if values is not empty and does not contain empty string.
     * 
     * @param values
     *            the set of values to be validated
     * @return true if values is not empty and does not contain empty string.
     */
    public boolean validate(Set values) {
        boolean valid = (values.size() > 0);
        if (valid) {
            valid = false;
            for (Iterator i = values.iterator(); (i.hasNext() && !valid);) {
                String str = (String) i.next();
                /*
                 * 20050502 Dennis Seah Note: valid even if str is " "
                 */
                valid = (str != null) && (str.length() > 0);
            }
        }
        return valid;
    }
}
