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
 * $Id: ResourceAccessStatement.java,v 1.2 2008/06/25 05:47:20 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.liberty.ws.disco.EncryptedResourceID;
import com.sun.identity.liberty.ws.disco.ResourceID;

import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectStatement;

import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>ResourceAccessStatement</code> class conveys information regarding
 * the accessing entities and the resource for which access is being attempted.
 *
 * @supported.all.api
 */
public class ResourceAccessStatement extends SubjectStatement {
    /**
     * The Statement is an Resource Access Statement.
     */
    public final static int RESOURCEACCESS_STATEMENT = 4;
    private ResourceID _resourceID = null;
    private EncryptedResourceID _encryptedResourceID = null;
    
    protected ProxySubject _proxySubject = null;
    protected SessionContext _sessionContext = null;
    
    /**
     * Constructs an <code>ResourceAccessStatement</code> object from a DOM
     * Element.
     *
     * @param element representing a DOM tree element
     * @throws SAMLException if there is an error in the sender or in the
     *         element definition.
     */
    public ResourceAccessStatement(Element element) throws SAMLException {
        // make sure input is not null
        if (element == null) {
            SAMLUtils.debug.message("ResourceAccessStatement: null input.");
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("nullInput"));
        }
        // check if it's an ResourceAccessStatement
        boolean valid = SAMLUtils.checkStatement(element,
                "ResourceAccessStatement");
        if (!valid) {
            SAMLUtils.debug.message("ResourceAccessStatement: Wrong input.");
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("wrongInput"));
        }
        
        //Handle the children elements of ResourceAccessStatement
        NodeList nodes = element.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount > 0) {
            for (int i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    String tagName = currentNode.getLocalName();
                    String tagNS = currentNode.getNamespaceURI();
                    if ((tagName == null) || tagName.length() == 0 ||
                            tagNS == null || tagNS.length() == 0) {
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("ResourceAccessStatement:" +
                                    "The tag name or tag namespace of child" +
                                    " element is either null or empty.");
                        }
                        throw new SAMLRequesterException(
                                SAMLUtils.bundle.getString("nullInput"));
                    }
                    if (tagName.equals("Subject") &&
                            tagNS.equals(
                                    SAMLConstants.assertionSAMLNameSpaceURI)) {
                        if (this._subject != null) {
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("ResourceAccess" +
                                   "Statement:should only contain one subject");
                            }
                            throw new SAMLRequesterException(
                                    SAMLUtils.bundle.getString("oneElement"));
                        } else {
                            this._subject = new Subject((Element) currentNode);
                        }
                    } else if (tagName.equals("ResourceID") &&
                            tagNS.equals(WSSEConstants.NS_DISCO)) {
                        if (_resourceID != null ||
                                _encryptedResourceID != null) {
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("ResourceAccess"+
                                        "Statement: should at most " +
                                        "contain one ResourceIDGroup.");
                            }
                            throw new SAMLRequesterException(
                                    SAMLUtils.bundle.getString("oneElement"));
                        }
                        try {
                            _resourceID = new ResourceID((Element)currentNode);
                        } catch (Exception ex) {
                            throw new SAMLRequesterException(ex.getMessage());
                        }
                    } else if (tagName.equals("EncryptedResourceID") &&
                            tagNS.equals(WSSEConstants.NS_DISCO)) {
                        if (_resourceID != null ||
                                _encryptedResourceID != null) {
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("ResourceAccess"+
                                        "Statement: should at most " +
                                        "contain one ResourceIDGroup.");
                            }
                            throw new SAMLRequesterException(
                                    SAMLUtils.bundle.getString("oneElement"));
                        }
                        try {
                            _encryptedResourceID =
                                 new EncryptedResourceID((Element)currentNode);
                        } catch (Exception ex) {
                            throw new SAMLRequesterException(ex.getMessage());
                        }
                    } else if (tagName.equals("ProxySubject") &&
                            tagNS.equals(WSSEConstants.NS_SEC)) {
                        if (_proxySubject != null) {
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("ResourceAccess"+
                                        "Statement: should at most " +
                                        "contain one ProxySubject.");
                            }
                            throw new SAMLRequesterException(
                                    SAMLUtils.bundle.getString("oneElement"));
                        } else {
                            _proxySubject = new ProxySubject((Element)
                            currentNode);
                        }
                    } else if (tagName.equals("SessionContext") &&
                            tagNS.equals(WSSEConstants.NS_SEC)) {
                        if (_sessionContext != null) {
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("ResourceAccess"+
                                        "Statement: should at most " +
                                        "contain one SessionContext.");
                            }
                            throw new SAMLRequesterException(
                                    SAMLUtils.bundle.getString("oneElement"));
                        } else {
                            _sessionContext = new SessionContext((Element)
                            currentNode);
                        }
                    } else {
                        if (SAMLUtils.debug.messageEnabled()) {
                            SAMLUtils.debug.message("ResourceAccessStatement:"+
                                    "Wrong element " + tagName + "included.");
                        }
                        throw new SAMLRequesterException(
                                SAMLUtils.bundle.getString("wrongInput"));
                    }
                } // end of if (currentNode.getNodeType() == Node.ELEMENT_NODE)
            } // end of for loop
        }  // end of if (nodeCount > 0)
        
        // check if the subject is null
        if (this._subject == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement should " +
                        "contain one subject.");
            }
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("missingElement"));
        }
    }
    
    /**
     * Constructs a <code>ResourceAccessStatement</code> object from a
     * <code>String</code> object and a <code>Subject</code>.
     *
     * @param resourceID <code>String</code>.
     * @param subject <code>Subject</code> object.
     * @throws SAMLException if subject is null.
     */
    public ResourceAccessStatement(String resourceID,
            Subject subject) throws SAMLException {
        // check if the subject is null
        if (subject == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement: should" +
                        " contain one subject.");
            }
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("oneElement"));
        } else {
            this._subject = subject;
        }
        _resourceID = new ResourceID(resourceID);
    }
    
    /**
     * Constructs a <code>ResourceAccessStatement</code> object from a
     * <code>String</code> object, <code>ProxySubject</code> object and
     * a <code>Subject</code>.
     *
     * @param resourceID <code>String</code>.
     * @param proxySubject <code>ProxySubject</code> object.
     * @param subject <code>Subject</code> object.
     * @throws SAMLException if subject is null.
     */
    public ResourceAccessStatement(String resourceID,
            ProxySubject proxySubject,
            Subject subject) throws SAMLException {
        if (subject == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement: should" +
                        " contain one subject.");
            }
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("oneElement"));
        } else {
            this._subject = subject;
        }
        _resourceID = new ResourceID(resourceID);
        _proxySubject = proxySubject;
    }
    
    /**
     * Constructs a <code>ResourceAccessStatement</code> object from a
     * <code>String</code> object, <code>ProxySubject</code> object, a
     * <code>SessionContext</code> object and a <code>Subject</code>.
     *
     * @param resourceID resource ID.
     * @param proxySubject <code>ProxySubject</code> object.
     * @param sessionContext <code>SessionContext</code> object.
     * @param subject <code>Subject</code> object.
     * @throws SAMLException if subject is null.
     */
    public ResourceAccessStatement(String resourceID,
            ProxySubject proxySubject,
            SessionContext sessionContext,
            Subject subject) throws SAMLException {
        // check if the subject is null
        if (subject == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement: should" +
                        " contain one subject.");
            }
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("oneElement"));
        } else {
            this._subject = subject;
        }
        _resourceID = new ResourceID(resourceID);
        _proxySubject = proxySubject;
        _sessionContext = sessionContext;
        
    }
    
    
    /**
     * Constructs a <code>ResourceAccessStatement</code> object from a
     * <code>ResourceID</code> object, <code>ProxySubject</code> object, a
     * <code>SessionContext</code> object and a <code>Subject</code>.
     *
     * @param resourceID resource ID.
     * @param proxySubject <code>ProxySubject</code> object.
     * @param sessionContext <code>SessionContext</code> object.
     * @param subject <code>Subject</code> object.
     * @throws SAMLException if subject is null.
     */
    public ResourceAccessStatement(ResourceID resourceID,
            ProxySubject proxySubject,
            SessionContext sessionContext,
            Subject subject) throws SAMLException {
        // check if the subject is null
        if (subject == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement: should" +
                        " contain one subject.");
            }
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("oneElement"));
        } else {
            this._subject = subject;
        }
        _resourceID = resourceID;
        _proxySubject = proxySubject;
        _sessionContext = sessionContext;
        
    }
    
    /**
     * Constructs a <code>ResourceAccessStatement</code> object from a
     * <code>EncryptedResourceID</code> object, <code>ProxySubject</code>
     * object, a <code>SessionContext</code> object and a <code>Subject</code>.
     *
     * @param encryptedResourceID the encrypted resource ID.
     * @param proxySubject <code>ProxySubject</code> object.
     * @param sessionContext <code>SessionContext</code> object.
     * @param subject <code>Subject</code> object.
     * @throws SAMLException if subject is null.
     */
    public ResourceAccessStatement(EncryptedResourceID encryptedResourceID,
            ProxySubject proxySubject,
            SessionContext sessionContext,
            Subject subject) throws SAMLException {
        // check if the subject is null
        if (subject == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement: should" +
                        " contain one subject.");
            }
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("oneElement"));
        } else {
            this._subject = subject;
        }
        _encryptedResourceID = encryptedResourceID;
        _proxySubject = proxySubject;
        _sessionContext = sessionContext;
        
    }
    
    
    /**
     * Gets the <code>ResourceID</code> from this
     * <code>ResourceAccessStatement</code> object.
     * @return resource ID
     */
    public String getResourceID() {
        return _resourceID == null ? null : _resourceID.getResourceID();
    }
    
    /**
     * Gets the <code>ResourceID</code> object from this
     * <code>ResourceAccessStatement</code> object.
     * @return resource ID
     */
    public ResourceID getResourceIDObject() {
        return _resourceID;
    }
    
    /**
     * Gets the <code>EncryptedResourceID</code> object from this
     * <code>ResourceAccessStatement</code> object.
     * @return encrypted resource ID
     */
    public EncryptedResourceID getEncryptedResourceID() {
        return _encryptedResourceID;
    }
    
    
    /**
     * Sets the <code>ResourceID</code> for this
     * <code>ResourceAccessStatement</code> object.
     *
     * @param resourceID Resource ID.
     * @return true if the operation is successful. Otherwise return false.
     */
    public boolean setResourceID(String resourceID) {
        if (resourceID == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement: " +
                        "setResourceID:Input is null.");
            }
            return false;
        }
        _resourceID = new ResourceID(resourceID);
        return true;
    }
    
    
    /**
     * Sets the <code>ResourceID</code> for this
     * <code>ResourceAccessStatement</code> object.
     *
     * @param resourceID Resource ID.
     */
    public void setResourceID(ResourceID resourceID) {
        _resourceID = resourceID;
    }
    
    /**
     * Sets the <code>EncryptedResourceID</code> for this
     * <code>ResourceAccessStatement</code> object.
     *
     * @param resourceID encrypted Resource ID.
     */
    public void setEncryptedResourceID(EncryptedResourceID resourceID) {
        _encryptedResourceID = resourceID;
    }
    
    /**
     * Returns the type of the Statement.
     *
     * @return An integer which represents <code>ResourceAccessStatement</code>
     * internally.
     */
    public int getStatementType() {
        return RESOURCEACCESS_STATEMENT;
    }
    
    /**
     * Sets the <code>SessionContext</code> for this
     * <code>ResourceAccessStatement</code> object.
     *
     * @param sessionContext Session context object
     * @return true if the operation is successful. Otherwise return false.
     */
    public boolean setSessionContext(SessionContext sessionContext) {
        if (sessionContext == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement: " +
                        "setSessionContext: Input is null.");
            }
            return false;
        }
        _sessionContext = sessionContext;
        return true;
    }
    
    /**
     * Gets the <code>SessionContext</code> from this
     * <code>ResourceAccessStatement</code> object.
     *
     * @return <code>SessionContext</code>.
     */
    public SessionContext getSessionContext() {
        return _sessionContext;
    }
    
    /**
     * Returns the <code>ProxySubject</code> in the
     * <code>ResourceAccessStatement</code>.
     *
     * @return <code>ProxySubject</code>.
     */
    public ProxySubject getProxySubject() {
        return _proxySubject;
    }

    /**
     * Returns a String representation of the
     * <code>ResourceAccessStatement</code>.
     *
     * @return A String representation of the
     *         <code>ResourceAccessStatement</code> element.
     */
    public String toString()  {
        return toString(true, false);
    }
    
    /**
     * Returns a String representation of the
     * <code>ResourceAccessStatement</code>.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *        prepended  to the Element when converted.
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string representation of the
     *         <code>ResourceAccessStatement</code> element.
     */
    public  String  toString(boolean includeNS, boolean declareNS)  {
        
        StringBuffer result = new StringBuffer(1000);
        result.append("<").append(WSSEConstants.TAG_SEC + ":").
                append(WSSEConstants.TAG_RESOURCEACCESSSTATEMENT).append(" ").
                append(WSSEConstants.TAG_XML_SEC).append("=").
                append("\"").append(WSSEConstants.NS_SEC).append("\"");
        
        result.append(">\n").append(this._subject.toString(includeNS, true));
        
        if (_resourceID != null) {
            result.append(_resourceID);
        } else if (_encryptedResourceID != null) {
            result.append(_encryptedResourceID);
        }
        
        if (_proxySubject!=null) {
            result.append(_proxySubject.toString(includeNS, true));
            if (_sessionContext!=null) {
                try {
                    result.append(
                            _sessionContext.toXMLString(includeNS, true));
                } catch (Exception e) {
                }
            }
        }
        
        result.append("</").append(WSSEConstants.TAG_SEC + ":").
                append(WSSEConstants.TAG_RESOURCEACCESSSTATEMENT).append(">\n");
        return(result.toString());
    }
}
