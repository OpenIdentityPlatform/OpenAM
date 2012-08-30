/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openam.session.model;

import java.io.Serializable;

/**
 * Provides Common Base OpenAM Root Entity Object for the Internal
 * Model of the OpenAM Session Object Stack.
 *
 * @author peter.major@forgerock.com
 * @author steve.ferris@forgerock.com
 * @author jeff.schenk@forgerock.com
 *
 */
public abstract class AMRootEntity implements Serializable {
    private static final long serialVersionUID = 101L;   //  10.1

    /** Represents Session Persistence Read Operation */
    static public final String READ = "READ";

    /** Represents Session Persistence Write Operation */
    static public final String WRITE = "WRITE";

    /** Represents Session Persistence Delete Operation */
    static public final String DELETE = "DELETE";

    /** Represents Session Persistence Delete Date Operation */
    static public final String DELETEBYDATE = "DELETEBYDATE";

    /** Represents Session Persistence  Shutdown Operation */
    static public final String SHUTDOWN = "SHUTDOWN";

    /** Represents the record count such as data record count*/
    static public final String GET_RECORD_COUNT = "GET_RECORD_COUNT";

    /** Represents Session Persistence  Read Operation with secondary key */
    static public final String READ_WITH_SEC_KEY = "READ_WITH_SEC_KEY";

    /**
     * New Session Persistence Operations can be Defined here, or within
     * extended persistence layer for specific Operations Types per
     * configured Session Persistence Backend Implementation.
     */

    /** Represents Session Persistence Flush All Session Operation */
    static public final String FLUSHALL = "FLUSHALL";

    /**
     * Common Properties
     */
    private String service = null;
    private String operation = null;
    private String primaryKey = null;
    private long expDate = 0;
    private String secondaryKey = null;
    private int state = 0;
    private String auxData = null;
    // byte[] encoded as Base64
    private String data = null;
    // Used During Serialization of InternalSession.
    private byte[] serializedInternalSessionBlob = null;


    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getAuxData() {
        return auxData;
    }

    public void setAuxData(String auxData) {
        this.auxData = auxData;
    }

    /**
     *  Secured Internal Session Data
     * @return byte[] Array of Serialized Data.
     */
    public byte[] getSerializedInternalSessionBlob() {
        return serializedInternalSessionBlob;
    }

    public void setSerializedInternalSessionBlob(byte[] serializedInternalSessionBlob) {
        this.serializedInternalSessionBlob = serializedInternalSessionBlob;
    }

    /**
     * Base64 Encoded Data.
     * @return
     */
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Session Record: ").append(this.getClass().getSimpleName()).append("\n");
        buffer.append("Service: ").append(service).append("\n");
        buffer.append("OP: ").append(operation).append("\n");
        buffer.append("PK: ").append(primaryKey).append("\n");
        buffer.append("SK: ").append(secondaryKey).append("\n");
        buffer.append("Expr Date: ").append(expDate).append("\n");
        buffer.append("State: ").append(state).append("\n");
        buffer.append("Aux Data: ").append(auxData).append("\n");
        if (this.getData() != null)
        {
            buffer.append("Data Byte Length: ").append(this.getData().length()).append("\n");
        }
        if (this.getSerializedInternalSessionBlob() == null)
        {
            buffer.append("IS Serialized Blob Byte Length: ").append(this.getSerializedInternalSessionBlob().length).append("\n");
        }
        return buffer.toString();
    }




}
