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
 * $Id: ResourceResults.java,v 1.2 2008/06/25 05:43:45 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/** 
 * Class that encapsulates a set of <code>ResourceResult</code>
 * objects
 */
public class ResourceResults {

    private Set resourceResults = new HashSet();

    /** 
     * Constructs a <code>ResourceResults</code> object 
     */
    public ResourceResults() {
    }

    /** 
     * Constructs a <code>ResourceResults</code> object given a set of 
     * <code>ResourceResult</code> objects
     * @param resourceResults set of <code>ResourceResult</code> 
     * objects that would  be contained in this 
     * <code>ResourceResults</code> object
     */
    public ResourceResults(Set resourceResults) {
        if (resourceResults != null) {
            this.resourceResults = resourceResults;
        }
    }

    /**
     * Returns <code>ResourceResult</code> objects contained in this object 
     * @return <code>ResourceResult</code> objects contained in this object
     *
     *
     */
    public Set getResourceResults() {
        return resourceResults;
    }

    /**
     * Sets response decisions at the top level <code>PolicyDecision</code> in 
     * each contained <code>ResourceResult</code>
     * @param responseDecisions a <code>Map</code> of key/values, 
     * that would be typically
     * exposed as http headers by policy agents
     * 
     */
    public void setResponseDecisions(Map responseDecisions) {
        Iterator iter = resourceResults.iterator();
        while (iter.hasNext()) {
            ResourceResult rr = (ResourceResult)iter.next();
            PolicyDecision pd = rr.getPolicyDecision();
            if (pd != null) {
                pd.setResponseDecisions(responseDecisions);
            }
            rr.setPolicyDecision(pd);
        }
    }

    /**
     * Returns XML representation of this object
     * @return XML representation of this object
     *
     *
     */
    public String toXML() {
        StringBuilder sb = new StringBuilder();
        Iterator iter = resourceResults.iterator();
        while (iter.hasNext()) {
            ResourceResult rr = (ResourceResult)iter.next();
            sb.append(rr.toXML());
        }
        return sb.toString();
    }

    /**
     * Returns string representation of this object
     * @return string representation of this object
     *
     *
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator iter = resourceResults.iterator();
        while (iter.hasNext()) {
            ResourceResult rr = (ResourceResult)iter.next();
            sb.append(rr.toString());
        }
        return sb.toString();
    }

}
