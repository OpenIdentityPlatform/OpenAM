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
 * $Id: SAMLStatsAccessor.java,v 1.2 2008/06/25 05:47:30 qcheng Exp $
 *
 */


package com.sun.identity.saml;

/**
 *
 * @author  cmort
 */
public class SAMLStatsAccessor {
    
    private static SAMLStatsAccessor singletonInstance = null;
    
    /** Holds value of property totalAssertions. */
    private int totalAssertions;
    
    /** Holds value of property totalArtifacts. */
    private int totalArtifacts;
    
    /** Creates a new instance of SessionStatsAccessor - 
       private casue this is a Singleton Pattern */
    private SAMLStatsAccessor() {
    }
    
    /** Gets the singleton class */
    public static SAMLStatsAccessor getAccessor() {
        
        if ( singletonInstance == null ) {
            singletonInstance = new SAMLStatsAccessor();   
        }
        return singletonInstance;
        
    }
    
    /** Gets property totalAssertions.
     * @return Value of property totalAssertions.
     *
     */
    public int getTotalAssertions() {
        return this.totalAssertions;
    }
    
    /** Sets property totalAssertions.
     * @param totalAssertions New value of property totalAssertions.
     *
     */
    public void setTotalAssertions(int totalAssertions) {
        this.totalAssertions = totalAssertions;
    }
    
    /** Gets property totalArtifacts.
     * @return Value of property totalArtifacts.
     *
     */
    public int getTotalArtifacts() {
        return this.totalArtifacts;
    }
    
    /** Sets property totalArtifacts.
     * @param totalArtifacts New value of property totalArtifacts.
     *
     */
    public void setTotalArtifacts(int totalArtifacts) {
        this.totalArtifacts = totalArtifacts;
    }
}
