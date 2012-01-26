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
 * $Id: RemoveEntry.java,v 1.2 2008/06/25 05:47:10 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.disco;

import com.sun.identity.liberty.ws.disco.common.DiscoConstants;

/**
 * The class <code>RemoveEntry</code> represents a remove entry element for
 * the discovery modify request.
 * <p>The following schema fragment specifies the expected content 
 * within the <code>RemoveEntry</code> object.
 * <p>
 * <pre>
 * &lt;xs:element name="RemoveEntry" type="RemoveEntryType">
 * &lt;complexType name="RemoveEntryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="entryID" type="{urn:liberty:disco:2003-08}IDReferenceType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public class RemoveEntry {

    private String entryID = null;

    /**
     * Constructor.
     * @param entryID entry ID to be removed
     */
    public RemoveEntry (String entryID) {
        this.entryID = entryID;
    }

    /**
     * Gets entry ID to be removed.
     *
     * @return entry ID to be removed.
     * @see #setEntryID(String)
     */
    public String getEntryID() {
        return entryID;
    }

    /**
     * Sets entry ID to be removed.
     *
     * @param entryID entry ID to be removed.
     * @see #getEntryID()
     */
    public void setEntryID(String entryID) {
        this.entryID = entryID;
    }

    /**
     * Returns string format.
     *
     * @return formatted string.
     */ 
    public String toString() {
        StringBuffer sb = new StringBuffer(200);
        sb.append("<RemoveEntry xmlns=\"").
            append(DiscoConstants.DISCO_NS).append("\"");
        if (entryID != null) {
            sb.append(" entryID=\"").append(entryID).append("\"");
        }
        sb.append("></RemoveEntry>");
        return sb.toString();
    }
}
