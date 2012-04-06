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
 * $Id: LogRecordSorter.java,v 1.3 2008/06/25 05:43:41 qcheng Exp $
 *
 */

package com.sun.identity.log.util;

import java.util.ArrayList;

/**
 * LogRecordSorter class provides mechanism to sort log records on any field.
 * It only sorts in ascending order on selected field. All the fields should
 * be String only.
 **/

public class LogRecordSorter {
    /**
     * This is the list of records with header to be sorted 
     */
    public ArrayList unSortedRecords;
    /**
     * Contains the 2D array in sorted order
     */
    public String [][] sortedArray; 
    String     sortingField;
    int        sortingFieldPos = -1;
    
    /**
     * Constructor.
     *
     * @param fieldName specifies a valid fieldname of the record.
     * @param allRecs contains the ArrayList of records.
     * @throws NoSuchFieldException if sort by field name is absent.
     **/
    public LogRecordSorter (String fieldName, ArrayList allRecs)
        throws NoSuchFieldException
    {
        this.sortingField = fieldName;
        this.unSortedRecords = allRecs;
        String [] fields = (String [])this.unSortedRecords.get(0);
        int len = fields.length;
        sortedArray = new String[unSortedRecords.size()][];
        boolean fieldPresent = false;
        sortedArray[0] = (String [])this.unSortedRecords.get(0);
        for (int i = 0 ; i < len; i++) {
            if (fieldName.compareToIgnoreCase(fields[i]) == 0) {
                fieldPresent = true;
                this.sortingFieldPos = i;
                break;
            }
        }
        if (fieldPresent == false) {
            String errorMsg = "no such field named " + fieldName + " ";
            throw new NoSuchFieldException(errorMsg);
        }
    }
    
    /**
     * This method should be called after creating the object, to get the
     * sorted 2D array.
     * @throws IllegalArgumentException if sort by field name is absent.
     * @throws RuntimeException if it fails to sort records.
     * @return returns the sorted records in 2D array.
     **/
    public String [][] getSortedRecords()
        throws IllegalArgumentException, RuntimeException
    {
        if (this.sortingFieldPos < 0) {
            throw new IllegalArgumentException("wrong sort by fieldname");
        }
        int recordSize = this.unSortedRecords.size();
        for (int i = 1; i < recordSize; i ++) {
            String [] debug = (String [])unSortedRecords.get(i);
            try {
                insert(i,(String[])this.unSortedRecords.get(i));
            } catch (RuntimeException e) {
                throw new RuntimeException("Problem while sorting");
            }
        }
        return (sortedArray);
    }
    
    /* method that puts record in sorted array after sorting */
    private void insert(int length, String [] record) {
        if (length == 1) {
            sortedArray[1] = record;
        } else if (length == 2) {
            String [] strArr = sortedArray[1];
            String str = strArr[sortingFieldPos];
            if (str.compareTo(record[sortingFieldPos]) > 0) {
                sortedArray[2] = sortedArray[1];
                sortedArray[1] = record;
            } else {
                sortedArray[2] = record;
            }
        } else if (length == 3) {
            String [] strArr = sortedArray[1];
            String [] str1Arr = sortedArray[2];
            String str = strArr[sortingFieldPos];
            String str1= str1Arr[sortingFieldPos];
            if (str.compareTo(record[sortingFieldPos]) > 0) {
                push(1);
                sortedArray[1] = record;
            } else if (str1.compareTo(record[sortingFieldPos]) > 0) {
                push(2);
                sortedArray[2] = record;
            } else {
                push(3);
                sortedArray[3] = record;
            }
        } else {
            String grp = null;
            int start = 1;
            int end = length-1;
            int diff = end - start;
            int mid = 0;
            if (diff % 2 == 0) {
                mid = diff / 2;
            } else {
                mid = (diff - 1) / 2;
            }
            String [] strArr = sortedArray[mid];
            String str = strArr[sortingFieldPos];
            while (true) {
                if (str.compareTo(record[sortingFieldPos]) > 0) {
                    grp = "Left";
                    end = mid;
                    diff = end - start;
                    if (diff == 0) {
                        break;
                    } else if (diff%2 == 0) {
                        mid = start + diff/2;
                    } else {
                        mid = start + (diff-1)/2;
                    }
                    strArr = sortedArray[mid];
                    str = strArr[sortingFieldPos];
                } else {
                    grp = "Right";
                    start = mid + 1;
                    diff = end - start;
                    if (diff == 0) {
                        break;
                    } else if (diff%2 == 0) {
                        mid = start + diff/2;
                    } else {
                        mid = start + (diff - 1)/2;
                    }
                    strArr = sortedArray[mid];
                    str = strArr[sortingFieldPos];
                }
            }
            if (grp.equalsIgnoreCase("Right") && (diff == 0)) {
                int index = end;
                String [] str1Arr = sortedArray[index - 1];
                String [] str2Arr = sortedArray[index];
                String str1 = str1Arr[sortingFieldPos];
                String str2 = str2Arr[sortingFieldPos];
                
                if (str1.compareTo(record[sortingFieldPos]) > 0) {
                    push(index-1);
                    sortedArray[index - 1] = record;
                } else if (str2.compareTo(record[sortingFieldPos]) > 0) {
                    push(index);
                    sortedArray[index] = record;
                } else {
                    push(index+1);
                    sortedArray[index + 1] = record;
                }
            } else if (grp.equalsIgnoreCase("Left") && (diff == 0)) {
                int index = start;
                String [] str1Arr = sortedArray[index];
                String [] str2Arr = sortedArray[index + 1];
                String str1 = str1Arr[sortingFieldPos];
                String str2 = str2Arr[sortingFieldPos];
                
                if (str1.compareTo(record[sortingFieldPos]) > 0) {
                    push(index);
                    sortedArray[index] = record;
                } else if (str2.compareTo(record[sortingFieldPos]) > 0) {
                    push(index+1);
                    sortedArray[index + 1] = record;
                } else {
                    push(index+2);
                    sortedArray[index + 2] = record;
                }
            }
        }
    }
    
    /**
     * method that pushes the records from the specified position towards end
     */
    void push( int from) {
        int len = sortedArray.length;
        --len;
        if (len == 0) {
            return;
        }
        while (from < len) {
            sortedArray[len] = sortedArray[len - 1];
            --len;
        }
    }
}
