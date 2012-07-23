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
 * $Id: BaseRecord.java,v 1.2 2008/06/25 05:43:27 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.ha.jmqdb.client;

import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

@Persistent
public class BaseRecord {

    // - primaryKey        => variable length
    // Primary Key must be unique in the database table.
    @PrimaryKey
    private String primaryKey = null;
    
    // - expdate           => 8 bytes
    // Secondary key is the expDate.
    @SecondaryKey(relate=Relationship.MANY_TO_ONE)
    private Long expDate;
    
    // - secondaryKey      => variable length
    @SecondaryKey(relate=Relationship.MANY_TO_ONE)
    private String secondaryKey  = null;
    
    // - auxData           => variable length
    //Auxilliary data stores additional field. 
    private String auxData = null;
    
    // -  state           => 4 bytes
    private int state;

    // -  blob            => variable length
    private byte[] blob = null;
    
    public void setPrimaryKey(String key) {
         primaryKey = key;
    }

    public void setExpDate(long date) {
        expDate = new Long(date);
    }
    
    public void setSecondaryKey(String key) {
        this.secondaryKey = key;
    }

    public void setAuxData(String aux) {
        auxData = aux;
    }

    public void setState(int ss) {
        state = ss;
    }

    public void setBlob(byte[] data) {
        blob = data;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }
    
    public Long getExpDate() {
        return expDate;
    }

    public String getSecondaryKey() {
        return secondaryKey;
    }

    public String getAuxData() {
        return auxData;
    }

    public int getState() {
        return state;
    }

    public byte[] getBlob() {
        return blob;
    }
}
