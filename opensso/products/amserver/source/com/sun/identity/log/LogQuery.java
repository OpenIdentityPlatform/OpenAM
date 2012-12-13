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
 * $Id: LogQuery.java,v 1.4 2008/06/25 05:43:35 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log;

import java.util.ArrayList;

/**
 * LogQuery defines the query format that the reader api supports.
 * This class contains a list of individual query elements
 * and also stores information about whether all the query to
 * be satisfied or any one to be satisfied. It also allows
 * caller to set required number of most recent records and
 * to specify the <code>sortby</code> field name (optional).
 * @supported.all.api
 */

public class LogQuery {
    /**
     * matching condition, values of globalOperand.
     * All the queries to be applied successfully
     */
    static public final int MATCH_ALL_CONDITIONS = 1;

    /* Any one of the query must be true */
    static public final int MATCH_ANY_CONDITION  = 2;
    
    /**
     * when maximum records asked
     * Most recent maximum number of records to be collected.
     * Here maximum number will be as stored in the configuration.
     */
    static public final int MOST_RECENT_MAX_RECORDS = -1;

    /**
     * All the records that matches query criteria (if any)
     * will be retrieved. Maximum number of records as configured
     * will be ignored.
     */
    static public final int ALL_RECORDS = -2;
    
    // private attributes
    private int maxRecord;
    private int globalOperand;
    private ArrayList queries;  /* list of QueryElement object */
    private ArrayList columns;  /* columns requested */
    private String sortBy;
    
    /**
     * Default constructor
     * It creates the new object and assigns space to them.
     * It sets default values when applicable.
     **/
    public LogQuery() {
        this.maxRecord = LogQuery.MOST_RECENT_MAX_RECORDS;
        this.globalOperand = LogQuery.MATCH_ANY_CONDITION;
        this.queries = null;
        this.columns = null;
        this.sortBy = null;
    }
    
    /**
     * Customized constructor to set only <code>maxrecord</code>.
     *
     * @param max_record is maximum number of most recent records to be
     *        returned.
     */
    public LogQuery(int max_record) {
        this.maxRecord = max_record;
        this.globalOperand = LogQuery.MATCH_ANY_CONDITION;
        this.queries = null;
        this.columns = null;
        this.sortBy = null;
    }
    
    /**
     * Customized constructor.
     *
     * @param max_Record the maximum number of most recent records
     *        to be returned
     * @param matchCriteria whether all queries or any one to match.
     * @param sortingBy <code>fieldname</code> on which records to be sorted.
     * @throws IllegalArgumentException if any of the
     *         <code>max_Record/matchCriteria</code> is not valid.
     */
    public LogQuery(int max_Record, int matchCriteria, String sortingBy)
    throws IllegalArgumentException {
        if (max_Record < LogQuery.ALL_RECORDS) {
            throw new IllegalArgumentException(
            "max_Record should be greater than LogQuery.ALL_RECORDS");
        } else {
            this.maxRecord = max_Record;
        }
        if ((matchCriteria > LogQuery.MATCH_ANY_CONDITION) ||
            (matchCriteria < LogQuery.MATCH_ALL_CONDITIONS))
        {
            throw new IllegalArgumentException(
                "matchCriteria should be LogQuery.MATCH_ANY_CONDITION or " +
                "LogQuery.MATCH_ALL_CONDITIONS");
        } else {
            this.globalOperand = matchCriteria;
        }
        if (sortingBy != null) {
            this.sortBy = sortingBy;
        }
        this.queries = null;
        this.columns = null;
    }
    
    /**
     * Sets the <code>globalOperand</code> field to either any query criteria
     * match or to match all the criteria.
     *
     * @param no the value to set to <code>globalOperand</code>
     * @throws IllegalArgumentException when parameter is passed as
     *         neither all nor any match.
     */
    public void setGlobalOperand (int no)
        throws IllegalArgumentException
    {
        if (no > LogQuery.MATCH_ANY_CONDITION) {
            IllegalArgumentException ie = new IllegalArgumentException(
                "parameter should be LogQuery.MATCH_ANY_CONDITION or " +
                "LogQuery.MATCH_ALL_CONDITIONS");
            throw (ie);
        } else if (no < LogQuery.MATCH_ALL_CONDITIONS) {
            IllegalArgumentException ie = new IllegalArgumentException(
            "parameter should be LogQuery.MATCH_ANY_CONDITION or " +
            "LogQuery.MATCH_ALL_CONDITIONS");
            throw (ie);
        }
        this.globalOperand = no;
    }
    
    /*
     * Sets maxRecord i.e. maximum number of records to return to the
     * user specified value.
     * It checks whether it exceeds the configured maximum value or not.
     *
     * @param value the maximum number of records to return.
     */
    public void setMaxRecord(int value) {
        if (value < LogQuery.ALL_RECORDS) {
            return;
        }
        this.maxRecord = value;
    }
    
    /**
     * Adds a query element to the list present in <code>LogQuery</code>.
     *
     * @param qryElement the query to be added into the list
     */
    public void addQuery(QueryElement qryElement) {
        if (qryElement == null) {
            return;
        }
        if (this.queries == null) {
            this.queries = new ArrayList();
        }
        this.queries.add(qryElement);
    }
    
    /**Returns the full list of query
     *
     * @return full list of query
     **/
    public ArrayList getQueries() {
        return (this.queries);
    }
    
    /*
     * Returns max number of records asked for.
     *
     * @return max number of records asked for.
     */
    public int getNumRecordsWanted() {
        return (this.maxRecord);
    }
    
    /**
     * Returns the value of global operand set in the query.
     *
     * @return the value of global operand set in the query.
     */
    public int getGlobalOperand() {
        return (this.globalOperand);
    }
    
    /**
     * Set the field name on which records to be sorted.
     *
     * @param fieldName field name on which records to be sorted.
     */
    public void setSortingField(String fieldName) {
        if (fieldName == null) {
            return;
        }
        this.sortBy = fieldName;
    }
    
    /**
     * Returns the field name on which records to be sorted.
     *
     * @return the field name on which records to be sorted.
     */
    public String getSortingField() {
        return (this.sortBy);
    }

    /**
     * Set the columns to be selected.  This applies to flatfile
     * logging also; means "fields", rather than "columns" then.
     *
     * @param columns to request.
     */
    public void setColumns(ArrayList columns) {
        if ((columns == null) || (columns.isEmpty())) {
            return;
        }
        this.columns = columns;
    }
    
    /**
     * Returns the table column names selected.  This applies to flatfile
     * logging also; means "fields", rather than "columns" then.
     *
     * @return the ArrayList of columns specified.
     */
    public ArrayList getColumns() {
        return (this.columns);
    }


}
