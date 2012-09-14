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
 * $Id: PatchData.java,v 1.1 2008/11/22 02:24:33 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.services.systeminfo.utils;

import java.io.Serializable;
import com.sun.identity.diagnostic.base.services.systeminfo.SystemInfoConstants;

public class PatchData extends Object implements SystemInfoConstants, 
    Serializable {

    private String patch;
    private String obsoletes;
    private String requires;
    private String incompatibles;
    private String packages;
    
    public PatchData() {
    }
    
    /**
     * Getter for property patch.
     *
     * @return Value of property patch.
     */
    public String getPatchNumber() {
        return this.patch;
    }
    
    /**
     * Setter for property patch.
     *
     * @param patch New value of property patch.
     */
    public void setPatchNumber(String patch) {
        this.patch = patch;
    }
    
    /**
     * Getter for property obsoletes.
     *
     * @return Value of property obsoletes.
     */
    public String getObsoletes() {
        return this.obsoletes;
    }
    
    /**
     * Setter for property obsoletes.
     *
     * @param obsoletes New value of property obsoletes.
     */
    public void setObsoletes(String obsoletes) {
        this.obsoletes = obsoletes;
    }
    
    /**
     * Getter for property requires.
     *
     * @return Value of property requires.
     */
    public String getRequires() {
        return this.requires;
    }
    
    /**
     * Setter for property requires.
     *
     * @param requires New value of property requires.
     */
    public void setRequires(String requires) {
        this.requires = requires;
    }
    
    /**
     * Getter for property incompatibles.
     *
     * @return Value of property incompatibles.
     */
    public String getIncompatibles() {
        return this.incompatibles;
    }
    
    /**
     * Setter for property incompatibles.
     *
     * @param incompatibles New value of property incompatibles.
     */
    public void setIncompatibles(String incompatibles) {
        this.incompatibles = incompatibles;
    }
    
    /**
     * Getter for property packages.
     *
     * @return Value of property packages.
     */
    public String getPackages() {
        return this.packages;
    }
    
    /**
     * Setter for property packages.
     *
     * @param packages New value of property packages.
     */
    public void setPackages(String packages) {
        this.packages = packages;
    }
    
    /**
     * Helper method to convert object as string representation
     *
     * @return string value of this object.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        result.append(newLine);
        if (patch != null && patch.length() > 0 ) {
            result.append(PATCHKEY).append(patch).append(" ");
            if (obsoletes != null && obsoletes.length() > 0) {
                result.append(OBSOLETESKEY).append(obsoletes).append(" ");
            }
            if (requires != null && requires.length() > 0) {
                result.append(REQUIRESKEY).append(requires).append(" ");
            }
            if (incompatibles != null && incompatibles.length() > 0) {
                result.append(INCOMPATIBLESKEY)
                      .append(incompatibles).append(" ");
            }
            if (packages != null && packages.length() > 0) {
                result.append(PACKAGESKEY).append(packages).append(" ");
            }
        }
        return result.toString();
    }
}
