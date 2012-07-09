/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SearchFilter.java,v 1.2 2009/10/14 03:18:41 veiming Exp $
 */

package com.sun.identity.entitlement.util;

/**
 * This class encapsulates the information required for searching
 * of names.
 */
public class SearchFilter {
    public static enum Operator
        {EQUAL_OPERATOR, GREATER_THAN_OPERATOR, LESSER_THAN_OPERATOR};

    private String attrName;
    private String value;
    private long longValue;
    private Operator operator;

    /**
     * Constructor.
     *
     * @param attrName Attribute name.
     * <code>CREATED_BY_ATTRIBUTE, LAST_MODIFIED_BY_ATTRIBUTE,
     * CREATION_DATE_ATTRIBUTE, LAST_MODIFIED_DATE_ATTRIBUTE</code>.
     * @param value Search filter.
     */
    public SearchFilter(String attrName, String value) {
        this.attrName = attrName;
        this.value = value;
        this.operator = Operator.EQUAL_OPERATOR;
    }

    /**
     * Constructor.
     *
     * @param attrName Attribute name
     * <code>CREATED_BY_ATTRIBUTE, LAST_MODIFIED_BY_ATTRIBUTE,
     * CREATION_DATE_ATTRIBUTE, LAST_MODIFIED_DATE_ATTRIBUTE</code>.
     * @param value Search filter.
     * @param operator Operator. Can be one of these
     * <ul>
     * <li>SearchFilter.EQUAL_OPERATOR</li>
     * <li>SearchFilter.LESSER_THAN_OPERATOR</li>
     * <li>SearchFilter.GREATER_THAN_OPERATOR</li>
     * </ul>
     */
    public SearchFilter(String attrName, long value, Operator operator) {
        this.attrName = attrName;
        this.longValue = value;
        this.operator = operator;
    }

    /**
     * Returns filter name.
     * 
     * @return filter name.
     */
    public String getName() {
        return attrName;
    }

    /**
     * Returns filter value.
     *
     * @return filter value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns filter operator.
     *
     * @return filter operator.
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Returns filter numeric value.
     *
     * @return filter numeric value.
     */
    public long getNumericValue() {
        return longValue;
    }

    /**
     * Returns the LDAP search filter.
     * 
     * @return LDAP search filter.
     */
    public String getFilter() {
        if (value != null) {
            return "(ou=" + attrName + "=" + value +")";
        }

        String op = null;
        if (operator == Operator.LESSER_THAN_OPERATOR) {
            op = "<=";
        } else if (operator == Operator.GREATER_THAN_OPERATOR) {
            op = ">=|";
        } else {
            op = "=";
        }

        return "(ou" + op + Long.toString(longValue) + "=" + attrName + ")";
    }
}
