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
 * $Id: IPAddressValidator.java,v 1.2 2008/06/25 05:44:04 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import org.forgerock.openam.network.ValidateIPaddress;

import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * The class <code>IPAddressValidator</code> is used to check if the IP
 * address is syntactically correct.
 */
public class IPAddressValidator implements ServiceAttributeValidator {

    /**
     * Default Constructor.
     */
    public IPAddressValidator() {
    }

    /**
     * Validates a set of string IP address.
     * 
     * @param values
     *            the set of IP address to validate
     * @return true if all of the IP addresses are valid; false otherwise
     */
    public boolean validate(Set values) {

        Iterator it = values.iterator();
        while (it.hasNext()) {
            String value = (String) it.next();
            if (!validate(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates an IP address.
     * 
     * @param value
     *            the IP address to validate
     * @return true if the IP address is valid; false otherwise
     */
    public boolean validate(String value) {
        return ValidateIPaddress.isValidIP(value);
    }

}
