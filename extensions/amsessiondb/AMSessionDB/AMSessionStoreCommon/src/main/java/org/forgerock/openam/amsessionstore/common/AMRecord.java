/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.amsessionstore.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * @author peter.major
 * @author steve
 */
public class AMRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
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
    
    private String service = null;
    private String operation = null; 
    private String primaryKey = null;
    private long expDate = 0;
    private String secondaryKey = null;
    private int state = 0;
    private String auxdata = null;
    @SuppressWarnings("UseOfObsoleteCollectionType")
    private Vector<String> records = null;
    
    // byte[] encoded as Base64
    private String data = null;    
    private Map<String, String> extraStringAttrs = null;
    // value is byte[] encoded as Base64
    private Map<String, String> extraByteAttrs = null; 
    
    /**
     * AMRecord represents the data fields that the user would store in 
     * a persistent data store. 
     * 
     * @param svc Service code such as session or saml2 
     * @param op  Operation such as read, write, delete, deletebydate.
     * @param pKey Primary Key
     * @param eDate Expiration Date 
     * @param secKey Secondary Key 
     * @param st state 
     * @param ax Additional Data 
     * @param blob Data Blob 
     */
    public AMRecord(String svc, String op, String pKey, long eDate,
        String secKey, int st, String ax, String blob) {
        service = svc; 
        operation = op; 
        primaryKey = pKey; 
        expDate = eDate;
        secondaryKey = secKey; 
        state = st; 
        auxdata = ax; 
        data = blob; 
    }
    
    public AMRecord() {
        // do nothing
    }
     
    public void setExtraByteAttrs(String key, String bytes) {
       if (extraByteAttrs == null) {
           extraByteAttrs = new HashMap<String, String>();
       }    
       extraByteAttrs.put(key, bytes);
    }
    
    public void setExtraByteAttrs(Map<String, String> map) {   
       extraByteAttrs = map;
    }
    
    public Map<String, String> getExtraByteAttributes() {
        return extraByteAttrs; 
    }
    
    public Map<String, String> getExtraStringAttributes() {
        return extraStringAttrs;
    }
    
    public void setExtraStringAttrs(Map<String, String> map) {
        extraStringAttrs = map; 
    }
    
    public void setExtraStringAttrs(String key, String value) {
       if (extraStringAttrs == null) {
           extraStringAttrs = new HashMap<String, String>();
       }    
       extraStringAttrs.put(key, value); 
    }
    
    public void setData(String blob) {
        data = blob; 
    }
    
    public String getData() {
        return data;
    }
    
    public String getService(){
        return service;
    }
    
    public void setService(String service){
        this.service = service;
    }
    
    public String getPrimaryKey() {
        return primaryKey;
    }
    
    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }
    
    public long getExpDate() {
        return expDate;
    }
    
    public void setExpDate(long expDate) {
        this.expDate = expDate;
    }

    public String getSecondaryKey() {
        return secondaryKey;
    }
    
    public void setSecondaryKey(String secondaryKey) {
        this.secondaryKey = secondaryKey;
    }

    public String getAuxdata() {
        return auxdata;
    }
    
    public void setAuxdata(String auxdata) {
        this.auxdata = auxdata;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
     
    public String getOperation() {
        return operation; 
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public String getString(String key) {
      return extraStringAttrs.get(key);
    }
    
    public String getBytes(String key) {
       return extraByteAttrs.get(key);
    }
    
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<String> getRecords() {
        return records;
    }
    
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public void setRecords(Vector<String> records) {
        this.records = records;
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
        
        if (data != null) {
            buffer.append("Data: ").append(data).append("\n");
        }
        
        buffer.append("Extra String Attrs: ").append(extraStringAttrs).append("\n");
        buffer.append("Extra Byte Attrs: ").append(extraByteAttrs).append("\n");

        return buffer.toString();
    }
}
