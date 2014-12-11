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
 * $Id: FSAccountFedInfo.java,v 1.4 2008/06/25 05:46:39 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.accountmgmt;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.saml.assertion.NameIdentifier;

/**
 * This class handles the information of federated user account.
 * @supported.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSAccountFedInfo {

    /**
     * Specifies provider's (SP/IDP) ID.
     * It will always be a remote provider's ID.
     */
    private String providerID = "";
    
    /**
     * Contains NameIdentifier sent to other side in federation process.
     */
    private NameIdentifier localNameIdentifier = null;
    
    /**
     * Contains NameIdentifier received from other side in federation process.
     */
    private NameIdentifier remoteNameIdentifier = null;
    
    /**
     * Represents user's federation status (Active/Inactive).
     */
    private boolean isActive = true;

    /**
     * Represents the federation type
     */ 
    private boolean isAffiliationFed = false;
    
    /*
     * Represents that in a specific federation remote deployement 
     * participated as SP or IDP.
     */
    private boolean isRoleIDP = true;
    
    /**
     * Default Constructor.
     */
    FSAccountFedInfo() {
    }

    /**
     * Constructor.
     * @param providerID  Specifies the provider's (SP/IDP) ID.
     * @param nameIdentifier  Contains NameIdentifier sent/received 
     *     in federation process.
     * @param nameIdentifierType indicates if IdentifierType is of type 
     *     LOCAL or REMOTE
     * @param isRoleIDP Represents that in a specific federation remote
     *     deployement participated as SP or IDP.
     * @throws FSAccountMgmtException if illegal argument passed.
     */
    public FSAccountFedInfo(
        String providerID, 
        NameIdentifier nameIdentifier,
        int nameIdentifierType, 
        boolean isRoleIDP)
        throws FSAccountMgmtException
    {
        if (nameIdentifierType == IFSConstants.LOCAL_NAME_IDENTIFIER) {
            init(providerID, 
                 nameIdentifier, 
                 null, 
                 isRoleIDP);
        }  else if (nameIdentifierType == IFSConstants.REMOTE_NAME_IDENTIFIER) {
            init(providerID, 
                 null, 
                 nameIdentifier, 
                 isRoleIDP);
        } else {
            FSUtils.debug.error("FSAccountFedInfo.Constructor() : Invalid" +
                " Argument : Invalid Name Identifier Type");
            throw new FSAccountMgmtException(
                IFSConstants.INVALID_NAME_IDENTIFIER_TYPE, null);
        }
    }
    
    /**
     * Constructor.
     * @param providerID Specifies provider's (SP/IDP) ID.
     * @param localNameIdentifier Contains NameIdentifier sent to other side 
     *     in federation process.
     * @param remoteNameIdentifier Contains NameIdentifier received from
     *     other side in federation process.
     * @param isRoleIDP Represents that in a specific federation remote
     *     deployement participated as SP or IDP.
     * @throws FSAccountMgmtException if illegal argument passed.
     */
    public FSAccountFedInfo(
        String providerID, 
        NameIdentifier localNameIdentifier,
        NameIdentifier remoteNameIdentifier, 
        boolean isRoleIDP)
        throws FSAccountMgmtException
    {
        init(providerID, 
            localNameIdentifier, 
            remoteNameIdentifier, 
            isRoleIDP);
    }
    
    /**
     * Initializes the account federation information object. 
     * @param providerID Specifies provider's (SP/IDP) ID.
     *  Always Remote provider.
     * @param localNameIdentifier Contains NameIdentifier sent to other side 
     *  in federation process.
     * @param remoteNameIdentifier Contains NameIdentifier received from
     *  other side in federation process.
     * @param isRoleIDP Represents that in a specific federation remote
     *  deployement participated as SP or IDP.
     * @throws FSAccountMgmtException if illegal argument passed.
     */
    private void init(
        String providerID, 
        NameIdentifier localNameIdentifier,
        NameIdentifier remoteNameIdentifier, 
        boolean isRoleIDP)
        throws FSAccountMgmtException
    {
        if ((providerID == null) || (providerID.length() <= 0)) {
            FSUtils.debug.error(
                "FSAccountFedInfo.init(): Invalid Argument: providerID is " +
                providerID);
            throw new
                FSAccountMgmtException(IFSConstants.NULL_PROVIDER_ID, null);
        }
        
        if (localNameIdentifier == null && remoteNameIdentifier == null) {
            FSUtils.debug.error("FSAccountFedInfo.Constructor(): Invalid " +
                "Argument: both NameIdentifiers are null");
            throw new FSAccountMgmtException(
                IFSConstants.NULL_NAME_IDENTIFIER, null);
        }
        
        this.providerID = providerID;
        this.localNameIdentifier = localNameIdentifier;
        this.remoteNameIdentifier = remoteNameIdentifier;
        this.isRoleIDP = isRoleIDP;
        this.isActive = true;
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAccountFedInfo.init() : " + 
                "providerID :: " + this.providerID +
                ", isRoleIDP :: " + this.isRoleIDP);
            if (localNameIdentifier != null ) {
                FSUtils.debug.message(
                    "FSAccountFedInfo.init() : localNameIdentifier" +
                    this.localNameIdentifier.toString());
            }
            if (remoteNameIdentifier != null ) {
                FSUtils.debug.message(
                    "FSAccountFedInfo.init() : remoteNameIdentifier" +
                    this.remoteNameIdentifier.toString());
            }
        }
    }
    
    /**
     * Returns provider's (SP/IDP) ID.
     * @return remote provider's id
     * @supported.api
     */
    public String getProviderID() {
        return this.providerID;
    }
    
    /**
     * Sets provider's ID.
     * @param providerID - remote provider's id
     */
    void setProviderID(String providerID) {
        this.providerID = providerID;
    }
    
    /**
     * Sets value in local field.
     * @param localNameIdentifier Contains NameIdentifier sent to other  
     *  side in federation process.
     */
    public void setLocalNameIdentifier(
        NameIdentifier localNameIdentifier)
    {
        this.localNameIdentifier = localNameIdentifier;
    }
    
    /**
     * Returns local NameIdentifier sent to other side(SP/IDP).
     * @return local NameIdentifier sent to other side
     * @supported.api
     */
    public NameIdentifier getLocalNameIdentifier() {
        return this.localNameIdentifier;
    }
    
    /**
     * Sets value in local field.
     * @param remoteNameIdentifier Contains NameIdentifier received from
     *  other side in federation process.
     */
    public void setRemoteNameIdentifier(
        NameIdentifier remoteNameIdentifier) 
    {
        this.remoteNameIdentifier = remoteNameIdentifier;
    }
    
    /**
     * Returns remote NameIdentifier received from other side(SP/IDP).
     * @return remote NameIdentifier received from other side
     * @supported.api
     */
    public NameIdentifier getRemoteNameIdentifier() {
        return this.remoteNameIdentifier;
    }
    
    /**
     * Sets Federation Status as active.
     */
    public void activateFedStatus() {
        this.isActive = true;
    }
    
    /**
     * Sets Federation Status as Inactive.
     */
    public void deActivateFedStatus() {
        this.isActive = false;
    }
    
    /**
     * Returns true/false if Federation Status is Active/Inactive.
     * @return true/false if Federation Status is Active/Inactive.
     */
    public boolean isFedStatusActive() {
        return this.isActive;
    }
    
    /**
     * Represents that in a specific federation remote
     * deployement participated as SP or IDP.
     * @return true if in a specific federation remote
     * deployement participated as IDP.
     * And returns false if as SP.
     * @supported.api
     */    
    public boolean isRoleIDP() {
        return this.isRoleIDP;
    }

    /** 
     * Represents that in a specific federation remote
     * deployement participated as SP or IDP.
     * @param isRoleIDP  Represents that in a specific federation remote
     * deployement participated as SP or IDP.
     */    
    void setRole(boolean isRoleIDP) {
        this.isRoleIDP = isRoleIDP;
    }

    /**
     * Sets the affiliation flag.
     * @param isAffiliationFed true if the federation is affiliation type.
     */ 
    public void setAffiliation(boolean isAffiliationFed) {
        this.isAffiliationFed = isAffiliationFed;
    }

    /**
     * Gets the affiliation federation type.
     * @return true if the federation is of affiliation type.
     * @supported.api
     */ 
    public boolean getAffiliation() {
        return isAffiliationFed;
    }
}
