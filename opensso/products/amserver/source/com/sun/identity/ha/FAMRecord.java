/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMRecord.java,v 1.3 2008/08/01 22:24:45 hengming Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.ha;

import java.util.HashMap;
/**
 * FAMRecord stores all the data that the user would store in the database.
 */
public class FAMRecord 
{
    /** Represents JMS message read Operation */
    static public final String READ = "READ";

    /** Represents JMS write Operation */
    static public final String WRITE = "WRITE";

    /** Represents delete Operation */
    static public final String DELETE = "DELETE";

    /** Represents delete Date Operation */
    static public final String DELETEBYDATE = "DELETEBYDATE";

    /** Represents shut down Operation */
    static public final String SHUTDOWN = "SHUTDOWN";

    /** Represents the record count such as data record count*/
    static public final String GET_RECORD_COUNT = "GET_RECORD_COUNT";

    /** Represents JMS message read Operation with secondary key */
    static public final String READ_WITH_SEC_KEY = "READ_WITH_SEC_KEY";

    String service = null;
    String operation = null; 
    String primaryKey = null;
    long expDate = 0;
    String secondaryKey = null;
    int state = 0;
    String auxdata = null;
    byte[] data = null;    
    HashMap extraStringAttrs = null;
    HashMap extraByteAttrs = null; 
      
    /**
     * FAMRecord represents the data fields that the user would store in 
     * a persistent datastore. 
     * @param svc Service code such as session or saml2 
     * @param op  Operation such as read, write, delete, deletebydate.
     * @param pKey Primary Key
     * @param eDate Expiration Date 
     * @param secKey Secondary Key 
     * @param st state 
     * @param ax Additional Data 
     * @param blob Data Blob 
     */
    public FAMRecord ( String svc, String op, String pKey, long eDate,
        String secKey, int st, String ax, byte[] blob) {
        service = svc; 
        operation = op; 
        primaryKey = pKey; 
        expDate = eDate;
        secondaryKey = secKey; 
        state = st; 
        auxdata = ax; 
        data = blob; 
    }
    
    public void setString(String key, String val) {
       if (extraStringAttrs == null) {
           extraStringAttrs = new HashMap();
       }     
       extraStringAttrs.put(key, val);
    }
    
    public void setBytes(String key, byte[] bytes) {
       if (extraByteAttrs == null) {
           extraByteAttrs = new HashMap();
       }    
       extraByteAttrs.put(key, bytes);
    }
    
    public void setStringAttrs(HashMap map) {
        extraStringAttrs = map; 
    }
    
    public void setBlob(byte[] blob) {
        data = blob; 
    }
    
    public String getService(){
        return service;
    }
    
    public String getPrimaryKey() {
        return primaryKey;
    }
    
    public long getExpDate() {
        return expDate;
    }

    public String getSecondarykey() {
        return secondaryKey;
    }

    public String getAuxData() {
        return auxdata;
    }

    public int getState() {
        return state;
    }

    public byte[] getBlob() {
        return data;
    } 
    
    public String getOperation() {
        return operation; 
    }
    
    public String getString(String key) {
      return (String) extraStringAttrs.get(key);
    }
    
    public byte[] getBytes(String key) {
       return (byte[]) extraByteAttrs.get(key);
    }
    
    public HashMap getExtraStringAttributes() {
        return extraStringAttrs;
    }
    
    public HashMap getExtraByteAttributes() {
        return extraByteAttrs; 
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Service: ").append(service).append("\n");
        buffer.append("OP: ").append(operation).append("\n");
        buffer.append("PK: ").append(primaryKey).append("\n");
        buffer.append("SK: ").append(secondaryKey).append("\n");
        buffer.append("Expr Date: ").append(expDate).append("\n");
        buffer.append("State: ").append(state).append("\n");
        buffer.append("Aux Data: ").append(auxdata).append("\n");
        buffer.append("Data: ").append(new String(data)).append("\n");
        buffer.append("Extra String Attrs: ").append(extraStringAttrs).append("\n");
        buffer.append("Extra Byte Attrs: ").append(extraByteAttrs).append("\n");

        return buffer.toString();
    }
}