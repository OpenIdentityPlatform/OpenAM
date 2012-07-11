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
 * $Id: ZoneData.java,v 1.1 2008/11/22 02:24:33 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.services.systeminfo.utils;

import java.io.Serializable;
import com.sun.identity.diagnostic.base.services.systeminfo.SystemInfoConstants;

public class ZoneData extends Object implements SystemInfoConstants, 
    Serializable {
    
    private String zoneId;
    private String zoneName;
    private String status;
    private String zonePath;

    public ZoneData() {
    }
  
    /**
     * Getter for property zoneId.
     *
     * @return Value of property zoneId.
     */
    public String getZoneId() {
        return this.zoneId;
    }

    /**
     * Setter for property zoneId.
     *
     * @param zoneId New value of property zoneId.
     */
    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    /**
     * Getter for property zoneName.
     *
     * @return Value of property zoneName.
     */
    public String getZoneName() {
        return this.zoneName;
    }

    /**
     * Setter for property zoneName.
     *
     * @param zoneName New value of property zoneName.
     */
    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    /**
     * Getter for property zonePath.
     *
     * @return Value of property zonePath.
     */
    public String getZonePath() {
        return this.zonePath;
    }

    /**
     * Setter for property zonePath.
     *
     * @param zonePath New value of property zonePath.
     */
    public void setZonePath(String zonePath) {
        this.zonePath = zonePath;
    }

    /**
     * Getter for property status.
     *
     * @return Value of property status.
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Setter for property status.
     *
     * @param status New value of property status.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Helper method to convert as string representation
     *
     * @return string representation of this object
     */
     public String toString() {
         StringBuilder result = new StringBuilder();
         String newLine = System.getProperty("line.separator");

         result.append("ID\tNAME\tSTATUS\tPATH ");
         result.append(newLine);
         result.append(SMALL_LINE);
         result.append(newLine);
         if (zoneId != null && zoneId.length() > 0) {
             result.append(zoneId).append("\t");
             if (zoneName != null && zoneName.length() > 0) {
                result.append(zoneName).append("\t");
             } else {
                result.append("\t");
             }
             if (status != null && status.length() > 0) {
                result.append(status).append("\t");
             } else {
                result.append("\t");
             }
             if (zonePath != null && zonePath.length() > 0) {
                result.append(zonePath).append("\t");
             } else {
                result.append("\t");
             }
         }
         return result.toString();
    }
}
