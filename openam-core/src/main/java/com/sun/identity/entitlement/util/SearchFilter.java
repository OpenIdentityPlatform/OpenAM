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
 *
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 */

package com.sun.identity.entitlement.util;

/**
 * This class encapsulates the information required for searching
 * of names.
 */
public class SearchFilter {

    public static enum Operator {
        LESS_THAN_OPERATOR,
        LESS_THAN_OR_EQUAL_OPERATOR,
        EQUALS_OPERATOR,
        GREATER_THAN_OPERATOR,
        GREATER_THAN_OR_EQUAL_OPERATOR
    }

    private SearchAttribute attribute;
    private String value;
    private long longValue;
    private Operator operator;

    /**
     * Constructor.
     *
     * @param attribute Attribute details.
     * <code>CREATED_BY_ATTRIBUTE, LAST_MODIFIED_BY_ATTRIBUTE,
     * CREATION_DATE_ATTRIBUTE, LAST_MODIFIED_DATE_ATTRIBUTE</code>.
     * @param value Search filter.
     */
    public SearchFilter(SearchAttribute attribute, String value) {
        this.attribute = attribute;
        this.value = value;
        this.operator = Operator.EQUALS_OPERATOR;
    }

    /**
     * Constructor.
     *
     * @param attribute Attribute details
     * <code>CREATED_BY_ATTRIBUTE, LAST_MODIFIED_BY_ATTRIBUTE,
     * CREATION_DATE_ATTRIBUTE, LAST_MODIFIED_DATE_ATTRIBUTE</code>.
     * @param value Search filter.
     * @param operator Operator. Can be one of these
     * <ul>
     * <li>SearchFilter.EQUALS_OPERATOR</li>
     * <li>SearchFilter.LESS_THAN_OPERATOR</li>
     * <li>SearchFilter.GREATER_THAN_OPERATOR</li>
     * </ul>
     */
    public SearchFilter(SearchAttribute attribute, long value, Operator operator) {
        this.attribute = attribute;
        this.longValue = value;
        this.operator = operator;
    }

    /**
     * Returns filter name.
     * 
     * @return filter name.
     */
    public String getName() {
        return attribute.getAttributeName();
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
            return "(" + attribute.toFilter("=") + "=" + value +")";
        }

        /*
         * We store numeric policy meta-data such as creationDate and lastModifiedDate as value-key pairs under 'ou'.
         * All entries are stored twice; once with a "|" prefix added to the value-key and once without it.
         * We use the pipe prefixed entry with >= and the non-prefixed entry with <=. Filtering fails to operate
         * correctly if we do not pair up the operator with the pipe filter prefix in this way.
         *
         * Also, since LDAP does not support < or > natively, we need to invert >= and <=.
         */

        String attrValue = Long.toString(longValue) + "=" + attribute.getAttributeName();

        switch (operator) {
            case LESS_THAN_OPERATOR:
                return "(!(" + attribute.getLdapAttribute() + ">=|" + attrValue + "))";
            case LESS_THAN_OR_EQUAL_OPERATOR:
                return "(" + attribute.getLdapAttribute() + "<=" + attrValue + ")";
            case GREATER_THAN_OPERATOR:
                return "(!(" + attribute.getLdapAttribute() + "<=" + attrValue + "))";
            case GREATER_THAN_OR_EQUAL_OPERATOR:
                return "(" + attribute.getLdapAttribute() + ">=|" + attrValue + ")";
            case EQUALS_OPERATOR:
            default:
                return "(" + attribute.getLdapAttribute() + "=" + attrValue + ")";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SearchFilter)) {
            return false;
        }

        SearchFilter that = (SearchFilter) o;

        return longValue == that.longValue
                && !(attribute != null ? !attribute.equals(that.attribute) : that.attribute != null)
                && operator == that.operator
                && !(value != null ? !value.equals(that.value) : that.value != null);

    }

    @Override
    public int hashCode() {
        int result = attribute != null ? attribute.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (int) (longValue ^ (longValue >>> 32));
        result = 31 * result + (operator != null ? operator.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SearchFilter " + getFilter();
    }
}
