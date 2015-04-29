/*
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
 * $Id: IDPSessionCopy.java,v 1.3 2009/05/06 19:48:34 madan_ranganath Exp $
 *
 * Portions copyright 2013-2015 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.common.SAML2Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a copy of a session in the identity provider side.
 * It keeps track of information that is needed for single sign on
 * as well as single log out.
 * The difference between IDPSession and IDPSessionCopy is IDPSessionCopy
 * only keeps SSOToken id so that it can be Serializable.
 */

public class IDPSessionCopy implements Serializable {

    private String ssoTokenID = null;
    private List<NameIDandSPpair> nameIDandSPpairs = null;
    private String pendingLogoutRequestID = null;
    private String originatingLogoutRequestBinding = null;
    private String originatingLogoutRequestID = null;
    private String originatingLogoutSPEntityID = null;
    private boolean doLogoutAll = false;
    private String metaAlias = null;

    /**
     * Default constructor for deserialization.
     */
    public IDPSessionCopy() {
    }

    /**
     * Constructor for a <code>IDPSessionCopy</code>
     *
     * @param ssoToken the <code>SSO Token</code> corresponding 
     *   to the <code>IDPSessionCopy</code>
     */ 
    public IDPSessionCopy(String ssoToken) {
        this.ssoTokenID = ssoToken;
        nameIDandSPpairs = new ArrayList<NameIDandSPpair>();
    }

    public IDPSessionCopy(IDPSession idpSession) {
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            ssoTokenID = sessionProvider.getSessionID(idpSession.getSession());
        } catch (SessionException se) {
            SAML2Utils.debug.error("Error retrieving session provider.", se);
        }
        nameIDandSPpairs = new ArrayList<NameIDandSPpair>(idpSession.getNameIDandSPpairs());
        pendingLogoutRequestID = idpSession.getPendingLogoutRequestID();
        originatingLogoutRequestBinding = idpSession.getOriginatingLogoutRequestBinding();
        originatingLogoutRequestID = idpSession.getOriginatingLogoutRequestID();
        originatingLogoutSPEntityID = idpSession.getOriginatingLogoutSPEntityID();
        doLogoutAll = idpSession.getLogoutAll();
        metaAlias = idpSession.getMetaAlias();
    }
    
    /**
     * Returns the <code>SSO Token</code> of the session
     *
     * @return the <code>SSO Token</code> of the session
     */ 
    public String getSSOToken() {
        return ssoTokenID;
    }
   
    /**
     * Returns the list of <code>NameID</code> and 
     *    <code>SPEntityID</code> pair of the session      
     *
     * @return the list of <code>NameID</code> and 
     *    <code>SPEntityID</code> pair of the session      
     */ 
    public List<NameIDandSPpair> getNameIDandSPpairs() {
        return nameIDandSPpairs;
    }
   
    /**
     * Sets the pending log out request id of the session
     *
     * @param id the request id
     */ 
    public void setPendingLogoutRequestID(String id) {
        pendingLogoutRequestID = id;
    }
    
    /**
     * Returns the pending log out request id of the session
     *
     * @return id the pending log out request id
     */ 
    public String getPendingLogoutRequestID() {
        return pendingLogoutRequestID;
    }

    /**
     * Sets the original logout request binding.
     *
     * @param originatingLogoutRequestBinding The original logout request binding.
     */
    public void setOriginatingLogoutRequestBinding(String originatingLogoutRequestBinding) {
        this.originatingLogoutRequestBinding = originatingLogoutRequestBinding;
    }

    /**
     * Returns the original logout request binding.
     *
     * @return The original logout request binding.
     */
    public String getOriginatingLogoutRequestBinding() {
        return originatingLogoutRequestBinding;
    }

    /**
     * Sets the original log out request id of the session
     *
     * @param id the request id
     */ 
    public void setOriginatingLogoutRequestID(String id) {
        originatingLogoutRequestID = id;
    }
    
    /**
     * Returns the original log out request id of the session
     *
     * @return the original log out request id
     */ 
    public String getOriginatingLogoutRequestID() {
        return originatingLogoutRequestID;
    }
    
    /**
     * Sets the original log out <code>SPEntityID</code> of the session
     *
     * @param id the <code>SPEntityID</code>
     */ 
    public void setOriginatingLogoutSPEntityID(String id) {
        originatingLogoutSPEntityID = id;
    }
    
    /**
     * Returns the original log out <code>SPEntityID</code> of the session
     *
     * @return the original log out <code>SPEntityID</code> 
     */ 
    public String getOriginatingLogoutSPEntityID() {
        return originatingLogoutSPEntityID;
    }

    /**
     * Sets the logoutAll property 
     *
     * @param enable true or false
     */ 
    public void setLogoutAll(boolean enable) {
        doLogoutAll = enable;
    }

    /**
     * Returnss the logoutAll property 
     */ 
    public boolean getLogoutAll() {
        return doLogoutAll;
    }

    /**
     * Sets the IDP Meta Alias.
     *
     * @param metaAlias Meta Alias
     */
    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
    }

    /**
     * Returns the IDP Meta Alias.
     *
     * @return the IDP Meta Alias
     */
    public String getMetaAlias() {
        return metaAlias;
    }
}

