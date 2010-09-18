/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: QueryElement.java,v 1.3 2008/06/25 05:43:35 qcheng Exp $
 *
 */

package com.sun.identity.log;

/**
 * This class defines each individual query format.
 * It stores field name, value and relationship between them.
 * Currently it supports Equal, Not Equal, Greater Than, Lesser Than,
 * Greater Than Or Equal and Lesser Than Or Equal relationships.
 * @supported.all.api
 */
public class QueryElement {
    /**
     * Invalid Relationship.
     */
    public static final int NV = 0;

    /**
     * minimum relationship. set to "smallest";
     * "GT" in this case.
     */
    private static final int MIN_REL = 1;

    /**
     * Greater Than Relationship.
     */
    public static final int GT = 1;

    /**
     * Lesser Than Relationship.
     */
    public static final int LT = 2;

    /**
     * Equal Relationship.
     */
    public static final int EQ = 3;

    /**
     * Not Equal Relationship.
     */
    public static final int NE = 4;

    /**
     * Greater Than or Equal Relationship.
     */
    public static final int GE = 5;

    /**
     * Lesser Than or Equal Relationship.
     */
    public static final int LE = 6;
    
    /**
     * Contains Relationship.
     */
    public static final int CN = 7;
    
    /**
     * Starts With Relationship.
     */
    public static final int SW = 8;
    
    /**
     * Ends With Relationship.
     */
    public static final int EW = 9;
    
    /**
     * maximum relationship. set to "largest",
     * "EW" in this case
     */
    private static final int MAX_REL = 9;

    /* private fields of the class */
    private String fieldName;
    private String fieldValue;
    private int relation;
    
    /**Constructor.
     * @param fld name of the field to be set.
     * @param val value of the field to be set.
     * @param rel relation between field and value to be checked.
     **/
    public QueryElement(String fld, String val, int rel)
    throws IllegalArgumentException {
        this.fieldName = fld;
        this.fieldValue = val;
        if ((rel >= QueryElement.MIN_REL) && (rel <= QueryElement.MAX_REL)) {
            this.relation = rel;
        } else {
            throw new IllegalArgumentException(
            "rel param should be >= QueryElement.GT and <= QueryElement.EW");
        }
    }
    
    /**Default constructor.
     * Allocates memory for respective items.
     * All the fields to be set before use.
     */
    public QueryElement() {
        this.fieldName = new String();
        this.fieldValue = new String();
        this.relation = QueryElement.EQ;
    }
    
    /**
     * Returns the field name on which query to be applied
     *
     * @return field name present in this query element.
     */
    public String getFieldName() {
        return (fieldName);
    }
    
    /**
     * Returns the value of the field to be compared as stored in the query
     * element.
     *
     * @return value the field to be queried.
     */
    public String getFieldValue() {
        return (fieldValue);
    }
    
    /**
     * Returns relation to be applied in between field and value
     * as stored in the query element.
     *
     * @return relation the relation between the field and value
     *         to be checked.
     */
    public int getRelation() {
        return (relation);
    }
    
    /**
     * Sets the field name for this query element.
     *
     * @param field field or column name of the log record
     */
    public void setFieldName(String field) {
        this.fieldName = field;
    }
    
    /**
     * Sets the value for the field name in this query element.
     *
     * @param value field or column value of the log record
     */
    public void setFieldValue(String value) {
        this.fieldValue = value;
    }
    
    /**
     * This method modifies/sets the relation between the field
     * name and value in this query element.
     *
     * @param value relation between field and value to be matched.
     * @throws IllegalArgumentException if relation is invalid.
     */
    public void setRelation(int value)
    throws IllegalArgumentException {
        if ((value >= QueryElement.MIN_REL) && (value <= QueryElement.MAX_REL))
        {
            this.relation = value;
        } else {
            throw new IllegalArgumentException(
                "value should be >= QueryElement.GT and <= QueryElement.EW");
        }
        this.relation = value;
    }
}
