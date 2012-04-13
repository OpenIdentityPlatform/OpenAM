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
 * $Id: AMCrypt.java,v 1.2 2008/06/25 05:41:19 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.security.AccessController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceSchema;

// Just for code-review reference. Implementation has not changed.
/**
 * This class has methods to encrypt and decrypt password attributes.
 */
public class AMCrypt {

    public static Map encryptPasswords(Map attributes,
            ServiceSchema serviceSchema) {
        if (serviceSchema == null || attributes == null) {
            return attributes;
        }

        Iterator iter = attributes.keySet().iterator();
        while (iter.hasNext()) {
            String attributeName = (String) (iter.next());
            AttributeSchema as = serviceSchema
                    .getAttributeSchema(attributeName);
            if (as != null
                    && (as.getSyntax().equals(AttributeSchema.Syntax.PASSWORD) 
                            || as.getSyntax().equals(
                                    AttributeSchema.Syntax.ENCRYPTED_PASSWORD)))
            {
                Set valueSet = (Set) (attributes.get(attributeName));
                if (valueSet != null) {
                    HashSet tmpValueSet = new HashSet(valueSet);
                    valueSet.clear();
                    Iterator valIter = tmpValueSet.iterator();
                    while (valIter.hasNext()) {
                        String value = (String) valIter.next();
                        if (value != null) {
                            value = (String) AccessController
                                    .doPrivileged(new EncodeAction(value));
                        }
                        valueSet.add(value);
                    }
                }
            }
        }
        return attributes;
    }

    public static Map decryptPasswords(Map attributes,
            ServiceSchema serviceSchema) {
        if (serviceSchema == null || attributes == null) {
            return attributes;
        }

        Iterator iter = attributes.keySet().iterator();
        while (iter.hasNext()) {
            String attributeName = (String) (iter.next());
            AttributeSchema as = serviceSchema
                    .getAttributeSchema(attributeName);
            if (as != null
                    && (as.getSyntax().equals(AttributeSchema.Syntax.PASSWORD) 
                            || as.getSyntax().equals(
                                    AttributeSchema.Syntax.ENCRYPTED_PASSWORD)))
            {

                Set valueSet = (Set) (attributes.get(attributeName));
                if (valueSet != null) {
                    HashSet tmpValueSet = new HashSet(valueSet);
                    valueSet.clear();
                    Iterator valIter = tmpValueSet.iterator();
                    while (valIter.hasNext()) {
                        String value = (String) valIter.next();
                        if (value != null) {
                            value = (String) AccessController
                                    .doPrivileged(new DecodeAction(value));
                        }
                        valueSet.add(value);
                    }
                }
            }
        }
        return attributes;
    }

    public static Set decryptPasswords(Set values, String attributeName,
            ServiceSchema serviceSchema) {
        if (values == null || values.isEmpty()) {
            return values;
        }

        AttributeSchema as = serviceSchema.getAttributeSchema(attributeName);
        if (as == null
                || (!as.getSyntax().equals(AttributeSchema.Syntax.PASSWORD) && 
                        !as.getSyntax().equals(
                                AttributeSchema.Syntax.ENCRYPTED_PASSWORD))) {
            return values;
        }

        HashSet result = new HashSet();
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            String value = (String) iter.next();
            if (value != null) {
                value = (String) AccessController
                        .doPrivileged(new DecodeAction(value));
            }
            result.add(value);
        }
        return result;
    }
}
