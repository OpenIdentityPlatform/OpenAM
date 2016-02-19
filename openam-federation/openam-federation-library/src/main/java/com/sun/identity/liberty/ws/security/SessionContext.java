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
 * $Id: SessionContext.java,v 1.2 2008/06/25 05:47:21 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.shared.DateUtils;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.AuthnContext;
import com.sun.identity.federation.message.common.FSMsgException;

import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLUtils;

import java.text.ParseException;

import java.util.Date;

import org.w3c.dom.Element; 
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** 
 * The <code>SessionContext</code> class represents session status of an entity
 * to another system entity. It is supplied to a relying party to support policy
 * enforcement.
 *
 * @supported.all.api
 */
public class SessionContext {

    protected SessionSubject _sessionSubject = null;
    protected AuthnContext _authnContext = null;
    protected String _providerID = null;
    protected Date _issueInstant = null;
    protected Date  _authenticationInstant = null;

    
    /**
     * Default constructor 
     */
    protected SessionContext() {
    }
    
    /**
     * Constructs a <code>SessionContext</code> object from a
     * <code>SessionSubject</code> object, a <code>AuthnContext</code>
     * object and a <code>String</code>.
     *
     * @param sessionSubject <code>SessionSubject</code> object.
     * @param authnContext authentication context object.
     * @param providerID provider ID.
     * @throws SAMLException if <code>sessionSubject</code> is null or
     *            <code>providerID</code> is null.
     */
    public SessionContext(SessionSubject sessionSubject,
			  AuthnContext authnContext,
			  String providerID) throws SAMLException {
	if ((sessionSubject == null) || (providerID == null)) {
	    SAMLUtils.debug.message("SessionContext: null input.");
	    throw new SAMLRequesterException(
		      SAMLUtils.bundle.getString("nullInput"));
	}
	_sessionSubject = sessionSubject;
	_authnContext = authnContext;
	_providerID = providerID;
	_issueInstant = new Date();
	_authenticationInstant = new Date();
    }

    /**
     * Returns the <code>SessionSubject</code> within the
     * <code>SessionContext</code> object.
     *
     * @return <code>SessionSubject</code> object.
     */
    public SessionSubject getSessionSubject() {
        return _sessionSubject;
    }

    /**
     * Sets the <code>SessionSubject</code> object.
     *
     * @param sub <code>SessionSubject</code> object.
     */
    public void setSessionSubject(SessionSubject sub) {
        _sessionSubject = sub;
    }
    
    /**
     * Returns the <code>AuthnContext</code> within the
     * <code>SessionContext</code> object.
     *
     * @return <code>AuthnContext</code> object.
     */
    public AuthnContext getAuthnContext() {
	return _authnContext;
    }

    /**
     * Returns the <code>ProviderID</code> in the <code>SessionContext</code>
     * object.
     *
     * @return <code>ProviderID</code> object
     */
    public String getProviderID() {
        return _providerID;
    }

    /**
     * Sets the <code>AuthnContext</code> in the <code>SessionContext</code>.
     *
     * @param authnContext <code>AuthnContext</code> to be set.
     * @return true if <code>AuthnContext</code> was set.
     */
    public boolean setAuthnContext(AuthnContext authnContext) {
	if (authnContext == null) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("SessionContext: " +
		    "setAuthnContext: Input is null.");
	    }
	    return false;
	}
	_authnContext = authnContext;
	return true;
    }

    /**
     * Constructs an <code>SessionContext</code> object from a DOM Element. 
     * 
     * @param element representing a DOM tree element.
     * @throws SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public SessionContext(Element element)throws SAMLException {
        // make sure input is not null
	if (element == null) {
	    SAMLUtils.debug.message("AttributeStatement: null input.");
	    throw new SAMLRequesterException(
		      SAMLUtils.bundle.getString("nullInput"));
	}

        // check if it's an ResourceAccessStatement
        boolean valid = SAMLUtils.checkStatement(element, "SessionContext");
        if (!valid) {
            SAMLUtils.debug.message("SessionContext: Wrong input.");
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("wrongInput"));
        }

	String authInstant = element.getAttribute("AuthenticationInstant");
	String issueInstant = element.getAttribute("AssertionIssueInstant");
	if ((authInstant == null) || (issueInstant == null)) {
	    SAMLUtils.debug.message("SessionContext: AuthenticationInstant " +
			"or AssertionIssueInstant is missing!");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("nullInput"));
	}
	try {
	    _issueInstant = DateUtils.stringToDate(issueInstant);
	    _authenticationInstant = DateUtils.stringToDate(authInstant);
	} catch (ParseException e) {
	    //TODO: handle exception
	}

        //Handle the children elements of SessionContext
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
                        SAMLUtils.debug.message("SessionContext: The tag name"+
                                      " or tag namespace of child element is" +
                                      " either null or empty.");
                        }
                        throw new SAMLRequesterException(
                                  SAMLUtils.bundle.getString("nullInput"));
                    }
                    if (tagName.equals("SessionSubject") &&
                        tagNS.equals("urn:liberty:sec:2003-08")) { //sec:
                        if (_sessionSubject != null) {
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("SessionContext:" +
                                   " should only contain one SessionSubject");
                            }
                            throw new SAMLRequesterException(
                                      SAMLUtils.bundle.getString("oneElement"));
                        } else {
			    try {
                                _sessionSubject =
				    new SessionSubject((Element) currentNode);
			    } catch (Exception e) {
				if (SAMLUtils.debug.messageEnabled()) {
				    SAMLUtils.debug.message("SessionContext:" +
						"could not new SessionSubject" +
						" object.");
				}
				throw new SAMLRequesterException(
					SAMLUtils.bundle.getString(
						"SessionSubject"));
			    }
                        }
                    } else if (tagName.equals("ProviderID") &&
                        tagNS.equals("urn:liberty:sec:2003-08")) { //sec
                        if (_providerID != null) {
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("SessionContext:"+
                                            " should at most contain one" +
                                            " ProviderID.");
                             }
                             throw new SAMLRequesterException(
                                      SAMLUtils.bundle.getString("oneElement"));
                        } else {
                             _providerID = currentNode.getChildNodes().item(0)
                                           .getNodeValue();
                        }
                    } else if (tagName.equals("AuthnContext") &&
                        tagNS.equals("urn:liberty:iff:2003-08")) { //lib
                        if (_authnContext != null) {
                            if (SAMLUtils.debug.messageEnabled()) {
                                SAMLUtils.debug.message("SessionContext: " +
                                            "should at most contain one " +
                                            "AuthnContext");
                            }
                            throw new SAMLRequesterException(
                                      SAMLUtils.bundle.getString("oneElement"));
                        } else {
			    try {
                                _authnContext = new AuthnContext((Element)
                                                    currentNode);
			    } catch (Exception e) {
				if (SAMLUtils.debug.messageEnabled()) {
				    SAMLUtils.debug.message("SessionContext:" +
						"could not new AuthnContext" +
						" object.", e);
				}
				throw new SAMLRequesterException(
					SAMLUtils.bundle.getString(
						"AuthnContext"));
			    }
                        }
                    } else {
                         if (SAMLUtils.debug.messageEnabled()) {
                             SAMLUtils.debug.message("SessionContext: "+
                                     "Wrong element " + tagName + " included.");
                         }
                         throw new SAMLRequesterException(
                                     SAMLUtils.bundle.getString("wrongInput"));
                    }
                } // end of if (currentNode.getNodeType() == Node.ELEMENT_NODE)
            } // end of for loop
        }  // end of if (nodeCount > 0)
        // check if the subject is null
        if ((_sessionSubject == null)||(_authnContext == null)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SessionContext should contain " +
					"one SessionSubject and one " +
					" one AuthnContext.");
            }
            throw new SAMLRequesterException(
                        SAMLUtils.bundle.getString("missingElement"));
        }
	
    }

    /**
     * Returns a String representation of the <code>SessionContext</code>
     * element.
     *
     * @return A string containing the valid XML for this element.
     *         By default name space name is prepended to the element name
     *         example <code>&lt;saml:Subject&gt;</code>.
     * @throws ParseException if could not convert String Date
     *            expression to Date object.
     * @throws FSMsgException if could not get <code>AuthnContext</code> XML
     *            String representation.
     */
    public String toXMLString() throws ParseException, FSMsgException {
	return toXMLString(true, false);
    }

    /**
     * Returns a String representation of the <code>&lt;SessionContext&gt;</code>
     * element.
     *
     * @param includeNS if true prepends all elements by their Namespace
     *        name <code>&lt;saml:Subject&gt;</code>.
     * @param declareNS if true includes the namespace within the
     *        generated XML.
     * @return A string containing the valid XML for this element. Return null
     *         if error happened.
     * @throws ParseException if could not convert String Date
     *            expression to Date object.
     * @throws FSMsgException if could not get <code>AuthnContext</code> XML
     *            String representation.
     **/
    public String toXMLString(boolean includeNS, boolean declareNS)
	throws ParseException, FSMsgException {

        SAMLConstants sc;
        StringBuffer xml = new StringBuffer(3000);
	String secprefix = "";
        String libprefix = "";
        String liburi = "";
	String secNS = "";

        if (includeNS) {
             libprefix = IFSConstants.LIB_PREFIX;
	     secprefix = WSSEConstants.TAG_SEC + ":";
        }
        if (declareNS) {
            liburi = IFSConstants.LIB_NAMESPACE_STRING;
	    secNS = " " + WSSEConstants.TAG_XMLNS + ":" +
		    WSSEConstants.TAG_SEC + "=\"" + WSSEConstants.NS_SEC +
		    "\"";
        }

        xml.append("<").append(secprefix).
            append(WSSEConstants.TAG_SESSIONCONTEXT).append(secNS).append(" ").
            append("AuthenticationInstant=").append("\"").
            append(DateUtils.toUTCDateFormat(_issueInstant)).append("\" ").
            append("AssertionIssueInstant=").append("\"").
            append(DateUtils.toUTCDateFormat(_authenticationInstant)).
	    append("\"").
            append(">");

        xml.append(_sessionSubject.toXMLString(includeNS, declareNS));

        xml.append("<").append(secprefix).append(WSSEConstants.TAG_PROVIDERID).
            append(">").append(_providerID).append("</").append(secprefix).
            append(WSSEConstants.TAG_PROVIDERID).append(">");

        if (_authnContext != null) {
            xml.append(_authnContext.toXMLString(includeNS, declareNS));
        }

        xml.append("</").append(secprefix).
            append(WSSEConstants.TAG_SESSIONCONTEXT).append(">");

        return xml.toString();
    }
}

