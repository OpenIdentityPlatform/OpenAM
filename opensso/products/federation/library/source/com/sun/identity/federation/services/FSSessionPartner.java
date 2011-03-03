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
 * $Id: FSSessionPartner.java,v 1.2 2008/06/25 05:46:57 qcheng Exp $
 *
 */


package com.sun.identity.federation.services;

/**
 * Class that encapsulates session partner.
 */
public class FSSessionPartner{
    private boolean isRoleIDP = false;
    private String sessionPartner = null;
    
    /**
     * Constructs a new <code>FSSessionPartner</code> object.
     * @param sessionPartner session partner's provider ID
     * @param isRoleIDP if the session partner's role is IDP
     */
    public FSSessionPartner (String sessionPartner, boolean isRoleIDP) {
        this.sessionPartner = sessionPartner;
        this.isRoleIDP = isRoleIDP;
    }
    
    /**
     * Returns session partner's provider ID.
     * @return session partner's provider ID
     */
    public String getPartner () {
        return sessionPartner;
    }
    
    /**
     * Sets session partner's provider ID.
     * @param sessionPartner session partner's provider ID
     */
    public void setPartner (String sessionPartner) {
        this.sessionPartner = sessionPartner;
    }
    
    /**
     * Returns the role of the session partner.
     * @return <code>true</code> if the role of the session partner is
     *  <code>IDP</code>; <code>false</code> otherwise.
     */
    public boolean getIsRoleIDP () {
        return isRoleIDP;
    }
    
    /**
     * Sets the role of the session partner.
     * @param roleIDP <code>true</code> if the role of the session partner is
     *  <code>IDP</code>; <code>false</code> otherwise.
     */
    public void setIsRoleIDP (boolean roleIDP) {
        this.isRoleIDP = roleIDP;
    }
    
    /**
     * Checks if the session partner's provider ID equals to the one with this
     * object.
     * @param partnerID session partner's provider ID to compare to
     * @return <code>true</code> if the two session partner's provider IDs
     *  are the same; <code>false</code> otherwise.
     */
    public boolean isEquals(String partnerID) {
        return this.sessionPartner.equals(partnerID);
    }

    /**
     * Checks if input partner is equal to this object.
     * @param partner session partner to compare to
     * @return <code>true</code> the two objects are equal; <code>false</code>
     *  otherwise.
     */
    boolean equals(FSSessionPartner partner) {
        if (this.sessionPartner.equals(partner.getPartner()) && 
            (this.isRoleIDP == partner.getIsRoleIDP()))
        {
            return true;
        }
        return false;
    }

    /**
     * Returns a hash code for this object.
     * @return a hash code value for this object.
     */
    public int hashCode() {
        if (isRoleIDP) {
            return sessionPartner.hashCode() + 1;
        } else {
            return sessionPartner.hashCode() + 0;
        }
    }
}
