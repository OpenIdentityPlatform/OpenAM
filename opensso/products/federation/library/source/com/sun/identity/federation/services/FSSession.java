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
 * $Id: FSSession.java,v 1.3 2008/06/25 05:46:56 qcheng Exp $
 *
 */

package com.sun.identity.federation.services;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.federation.common.*;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.liberty.ws.disco.common.DiscoConstants;
import com.sun.identity.saml.assertion.AttributeStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

/**
 * Class that encasulates federation session.
 */
public class FSSession{
    private List sessionPartners = null;
    private String sessionID = null;
    private String sessionIndex = null;
    private Map extraSessionAttributes = null;
    private String authnContext = null;
    private AttributeStatement statement = null;
    private NodeList resourceOfferings = null;
    private List securityAssertions = null;
    private boolean oneTimeFederation = false;
    private FSAccountFedInfo fedInfo = null;
    private String userID = null;
    private List attrStatements = null;
    private AttributeStatement _autoFedStatement = null;

    /**
     * Constructs a new <code>FSSession</code>.
     * @param sessionID authentication session ID
     * @param sessionPartners list of session partners
     */
    public FSSession(
        String sessionID, 
        List sessionPartners
    )
    {
        this.sessionID = sessionID;
        this.extraSessionAttributes = new HashMap();
        if (sessionPartners != null){
            this.sessionPartners = sessionPartners;
        } else {
            this.sessionPartners = new ArrayList();
        }
    }
    
    /**
     * Constructs a new <code>FSSession</code> object.
     * @param sessionID authentication session ID
     */
    public FSSession(String sessionID) {
        this( sessionID, null );
    }
    
    /**
     * Returns authentication context.
     * @return authentication context
     * @see #setAuthnContext(String)
     */
    public String getAuthnContext() {
        return authnContext;
    }
    
    /**
     * Sets authentication context.
     * @param authnContext authentication context to be set
     * @see #getAuthnContext()
     */
    public void setAuthnContext(String authnContext) {
        this.authnContext = authnContext;
    }
    
    /**
     * Returns session index.
     * @return session index.
     * @see #setSessionIndex(String)
     */
    public String getSessionIndex() {
        return sessionIndex;
    }
    
    /**
     * Sets session index.
     * @param sessionIndex session index to be set
     * @see #getSessionIndex()
     */
    public void setSessionIndex(String sessionIndex) {
        this.sessionIndex = sessionIndex;
    }
    
    /**
     * Returns list of session partners.
     * @return list of session partners
     */
    public List getSessionPartners() {
        return sessionPartners;
    }
    
    /**
     * Adds a session partner.
     * @param sessionPartner session partner to be added
     */
    public void addSessionPartner(FSSessionPartner sessionPartner) {
        Iterator i = sessionPartners.iterator();
        while (i.hasNext()) {
            if (((FSSessionPartner)i.next()).equals(sessionPartner)){
                return;
            }
        }
        sessionPartners.add(sessionPartner);
    }
    
    /**
     * Returns the first session partner of the list of session partners.
     * @return the first session partner of the list
     */
    public FSSessionPartner getCurrentSessionPartner() {
        Iterator i = sessionPartners.iterator();
        if (i.hasNext()) {
            return (FSSessionPartner)i.next();
        }
        return null;
    }
    
    /**
     * Removes a session partner.
     * @param sessionPartner session partner to be removed
     */
    public void removeSessionPartner(String sessionPartner) {
        Iterator i = sessionPartners.iterator();
        while (i.hasNext()) {
            FSSessionPartner oldSessionPartner =(FSSessionPartner)i.next();
            if (oldSessionPartner.isEquals(sessionPartner)) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSSession.removeSessionPartner : Removing " 
                        + sessionPartner);
                }
                i.remove();
            }
        }
    }
    
    /**
     * Returns session ID.
     * @return authentication session ID
     */
    public String getSessionID() {
        return sessionID;
    }
    
    /**
     * Adds attribute to the session.
     * @param key name of the attribute
     * @param value value of the attribute
     */
    public void addAttribute(String key, String value) {
        extraSessionAttributes.put(key, value);
    }
    
    /**
     * Returns value of an attribute from the session.
     * @param key name of the attribute
     * @return value of the attribute
     */
    public String getAttribute(String key) {
        return(String)extraSessionAttributes.get(key);
    }
    
    boolean isEquals(String sessionId) {
        return this.sessionID.equals(sessionId);
    }
    
    boolean equals(FSSession session) {
        return this.sessionID.equals(session.getSessionID());
    }

    /**
     * Returns a hash code for this session. The value is the hash code of
     * the session ID.
     * @return a hash code value for this object
     */
    public int hashCode() {
        return sessionID.hashCode();
    }

    /**
     * Sets the boot strap attribute statement.
     * @param statement Boot strap attribute statement
     * @see #getBootStrapAttributeStatement()
     */
    public void setBootStrapAttributeStatement(AttributeStatement statement) {
        this.statement = statement;
        try {
            Document doc = XMLUtils.toDOMDocument(
                statement.toString(true, true), FSUtils.debug);
            org.w3c.dom.Element element = doc.getDocumentElement();
            resourceOfferings = element.getElementsByTagNameNS(
                DiscoConstants.DISCO_NS, "ResourceOffering");
        } catch(Exception ex) {
            FSUtils.debug.error("FSSession.setBootStrapAttributeStatement" +
                "Can not parse the attribute statement:" , ex);
        }
    }

    /**
     * Gets the boot strap attribute statement.
     * @return Boot strap attribute statement
     * @see #setBootStrapAttributeStatement(AttributeStatement)
     */
    public AttributeStatement getBootStrapAttributeStatement() {
        return statement;
    }

    /**
     * Gets Discovery Service Boot strap resource offerings.
     * @return Bootstrap resoource offerings
     */
    public NodeList getBootStrapResourceOfferings() {
        return resourceOfferings;
    }

    /**
     * Sets the bootstrap security credential assertion.
     * @param assertions List of bootstrap security assertions.
     * @see #getBootStrapCredential()
     */
    public void setBootStrapCredential(List assertions) {
        securityAssertions = assertions;
    } 

    /**
     * Gets the boot strap security credential assertion.
     * @return List of Boot strap security assertion
     * @see #setBootStrapCredential(List)
     */
    public List getBootStrapCredential() {
        return securityAssertions;
    }

    /**
     * Sets AutoFederate Statement.
     * @param autoFedStmt Auto Federate Statement.
     * @see #getAutoFedStatement()
     */
    public void setAutoFedStatement(AttributeStatement autoFedStmt) {
        _autoFedStatement = autoFedStmt;
    }

    /**
     * Gets Auto Federate Statement.
     * @return Auto Federate Statement.
     * @see #setAutoFedStatement(AttributeStatement)
     */
    public AttributeStatement getAutoFedStatement() {
        return _autoFedStatement;
    }

    /**
     * Sets Attribute Statements.
     * @param attrStatements List of Attribute Statements.
     * @see #getAttributeStatements()
     */
    public void setAttributeStatements(List attrStatements) {
        this.attrStatements = attrStatements;
    }

    /**
     * Gets Attribute Statements.
     * @return List of Attribute Statements.
     * @see #setAttributeStatements(List)
     */
    public List getAttributeStatements() {
        return attrStatements;
    }

    /**
     * Sets the flag for one time federation.
     * @param oneTimeFlag flag for one time federation
     * @see #getOneTime()
     */
    public void setOneTime(boolean oneTimeFlag) {
        oneTimeFederation = oneTimeFlag;
    }
    
    /**
     * Returns the flag for one time federation.
     * @return <code>true</code> if the session is for one time federation;
     *  <code>false</code> otherwise.
     */
    public boolean getOneTime() {
        return oneTimeFederation;
    }

    /**
     * Sets account federation info.
     * @param fedInfo account federation info to be set
     * @see #getAccountFedInfo()
     */
    public void setAccountFedInfo(FSAccountFedInfo fedInfo) {
        this.fedInfo = fedInfo;
    }

    /**
     * Returns account federation info.
     * @return <code>FSAccountFedInfo</code> object
     * @see #setAccountFedInfo(FSAccountFedInfo)
     */
    public FSAccountFedInfo getAccountFedInfo() {
        return fedInfo;
    }

    /**
     * Sets user ID.
     * @param userID user ID to be set
     * @see #getUserID()
     */
    public void setUserID(String userID) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSSession.setUserID : " + userID);
        }
        this.userID = userID;
    }

    /**
     * Returns user ID.
     * @return user ID
     * @see #setUserID(String)
     */
    public String getUserID() {
        return userID;
    }
}
