/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */
package com.sun.identity.sm.model;

import org.forgerock.openam.session.model.AMRootEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * @author peter.major
 * @author steve
 */
public class AMRecord extends AMRootEntity {
    private static final long serialVersionUID = 101L;   //  10.1

    /**
     * AMRecord Specifics
     */
    @SuppressWarnings("UseOfObsoleteCollectionType")
    private Vector<String> records = null;

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
     * @param stringBlob Data Blob
     */
    public AMRecord(String svc, String op, String pKey, long eDate,
        String secKey, int st, String ax, String stringBlob) {
        this.setService(svc);
        this.setOperation(op);
        this.setPrimaryKey(pKey);
        this.setExpDate(eDate);
        this.setSecondaryKey(secKey);
        this.setState(st);
        this.setAuxData(ax);
        this.setData(stringBlob);
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
        buffer.append(super.toString());

        buffer.append("Extra String Attrs: ").append(extraStringAttrs).append("\n");
        buffer.append("Extra Byte Attrs: ").append(extraByteAttrs).append("\n");

        return buffer.toString();
    }
}
