/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AttributeSchemaComparator.java,v 1.2 2008/06/25 05:43:09 qcheng Exp $
 *
 */

package com.sun.identity.console.property;

import com.sun.identity.sm.AttributeSchema;
import java.text.Collator;
import java.util.Comparator;

/**
 * This comparator compares the i18n key of two
 * <code>AttributeSchema</code> objects. Its supports Collator.
 */
public class AttributeSchemaComparator implements Comparator {
    private Collator collator = null;

    /**
     * Constructs of an attribute schema's i18n key comparator object
     *
     * @param collator for locale base sorting
     */
    public AttributeSchemaComparator(Collator collator) {
        this.collator = collator;
    }

    /**
     * Performs string comparison on attribute schema's i18n keys
     *
     * @param o1  <code>AttributeSchema</code> object
     * @param o2  <code>AttributeSchema</code> object
     * @return 0 if i18n key of o1 is equal to i18n key of o2; less than 0 if
     * i18n key of o1 is lexicographically less than i18n key of o2; greater
     * than 0 if i18n key of o1 is greater than i18n key of o2.
     */
    public int compare(Object o1, Object o2) {
        AttributeSchema attr1 = (AttributeSchema) o1;
        AttributeSchema attr2 = (AttributeSchema) o2;

        String key1 = attr1.getI18NKey();
        String key2 = attr2.getI18NKey();
        if (key1 != null && key2 != null) {
            if (collator != null) {
                return collator.compare(key1, key2);
            } else {
                return key1.compareTo(key2);
            } 
        }
        return -1;
    }
}
