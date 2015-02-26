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

package com.sun.identity.sm.model;

import org.forgerock.openam.session.model.AMRootEntity;

import java.util.HashMap;
/**
 * FAMRecord stores all the data that the user would store in the database.
 */
public class FAMRecord extends AMRootEntity {
    private static final long serialVersionUID = 101L;   //  10.1

    /**
     * FAMRecord Specifics
     */
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
     * @param serializedInternalSessionBlob  Binary Blob
     */
    public FAMRecord ( String svc, String op, String pKey, long eDate,
        String secKey, int st, String ax, byte[] serializedInternalSessionBlob) {
        this.setService(svc);
        this.setOperation(op);
        this.setPrimaryKey(pKey);
        this.setExpDate(eDate);
        this.setSecondaryKey(secKey);
        this.setState(st);
        this.setAuxData(ax);
        this.setSerializedInternalSessionBlob(serializedInternalSessionBlob);
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
        buffer.append(super.toString());
        buffer.append("Extra String Attrs: ").append(extraStringAttrs).append("\n");
        buffer.append("Extra Byte Attrs: ").append(extraByteAttrs).append("\n");

        return buffer.toString();
    }
}
