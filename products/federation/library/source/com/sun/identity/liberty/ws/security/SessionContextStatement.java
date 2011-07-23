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
 * $Id: SessionContextStatement.java,v 1.2 2008/06/25 05:47:22 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;

import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectStatement;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLUtils;

import org.w3c.dom.Element; 
import org.w3c.dom.Node; 
import org.w3c.dom.NodeList;

/** 
 * The <code>SessionContextStatement</code> element conveys session status
 * of an entity to another system entity within the body of an
 * <code>&lt;saml:assertion&gt;</code> element.
 *
 * @supported.all.api
 */
public class SessionContextStatement extends SubjectStatement {
    /**
     * The Statement is an Session Context Statement.
     */
    public final static int SESSIONCONTEXT_STATEMENT = 5;
    protected ProxySubject _proxySubject = null;
    protected SessionContext _sessionContext = null;

    /**
     * Constructs a <code>SessionContextStatement</code> object from a
     * <code>SessionContext</code> object and a <code>Subject</code> object.
     *
     * @param sessionContext <code>SessionContext</code> object.
     * @param subject <code>Subject</code> object.
     * @throws SAMLException if <code>sessionContext</code> is null or subject
     *         is null.
     */
    public SessionContextStatement(SessionContext sessionContext,
            Subject subject)
            throws SAMLException {
        if ((sessionContext == null) || (subject ==null)) {
            SAMLUtils.debug.message("SessionContextStatement: " +
                    "SessionContext is null!");
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("nullInput"));
        }
        _sessionContext = sessionContext;
        this._subject = subject;
    }

    /**
     * Constructs a <code>SessionContextStatement</code> object from a
     * <code>SessionContext</code> object, a <code>proxySubject</code> and
     * a <code>Subject</code> object.
     *
     * @param sessionContext <code>SessionContext</code> object.
     * @param proxySubject <code>ProxySubject</code> object.
     * @param subject <code>Subject</code> object.
     * @throws SAMLException if <code>sessionContext</code> is null or
     *         subject is null.
     */
    public SessionContextStatement(SessionContext sessionContext,
            ProxySubject proxySubject,
            Subject subject)
            throws SAMLException {
        if ((sessionContext == null) || (subject ==null)) {
            SAMLUtils.debug.message("SessionContextStatement: " +
                    "SessionContext is null!");
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("nullInput"));
        }
        _sessionContext = sessionContext;
        _proxySubject = proxySubject;
        this._subject = subject;
    }

    /**
     * Constructs a <code>SessionContextStatement</code> object from a DOM
     * element. 
     *
     * @param element the Document Element
     * @throws SAMLException if there is an error in the sender or in
     *            the element definition.
     */
    public SessionContextStatement(Element element)throws SAMLException {
        // make sure input is not null
	if (element == null) {
	    SAMLUtils.debug.message("AttributeStatement: null input.");
	    throw new SAMLRequesterException(
		      SAMLUtils.bundle.getString("nullInput"));
	}
	// check if it's an SessionContextStatement
        boolean valid = SAMLUtils.checkStatement(element,
			"SessionContextStatement");
	if (!valid) {
	    SAMLUtils.debug.message("SessionContextStatement: Wrong input.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	//Handle the children elements of SessionContextStatement
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
			    SAMLUtils.debug.message("SessionContextStatement:"
				+ " The tag name or tag namespace of child"
				+ " element is either null or empty.");
			}
			throw new SAMLRequesterException(
				SAMLUtils.bundle.getString("nullInput"));
		    }
		    if (tagName.equals("Subject") &&
			tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
			if (this._subject != null) {
			    if (SAMLUtils.debug.messageEnabled()) {
				SAMLUtils.debug.message("SessionContext" +
					"Statement should only contain " +
					"one subject");
			    }
			    throw new SAMLRequesterException(
				      SAMLUtils.bundle.getString("oneElement"));

			} else {
			    this._subject = new Subject((Element) currentNode);
			}
		    } else if (tagName.equals("ProxySubject") &&
			tagNS.equals(WSSEConstants.NS_SEC)) {
			if (_proxySubject != null) {
			    if (SAMLUtils.debug.messageEnabled()) {
				SAMLUtils.debug.message("SessionContext" +
					"Statement should only contain " +
					"one ProxySubject");
			    }
			    throw new SAMLRequesterException(
				      SAMLUtils.bundle.getString("oneElement"));
			} else {
			    _proxySubject =
				new ProxySubject((Element) currentNode);
			}
		    } else if (tagName.equals("SessionContext") &&
			tagNS.equals(WSSEConstants.NS_SEC)) {
			if (_sessionContext != null) {
			    if (SAMLUtils.debug.messageEnabled()) {
				SAMLUtils.debug.message("SessionContext" +
				"Statement should only contain " +
				"one SessionContext");
			    }
			    throw new SAMLRequesterException(
				      SAMLUtils.bundle.getString("oneElement"));
			} else {
			    _sessionContext =
				new SessionContext((Element) currentNode);
			}
		    } else {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("SessionContextStatement:"
				+ "Wrong element " + tagName + " included.");
			}
			throw new SAMLRequesterException(
				  SAMLUtils.bundle.getString("wrongInput"));
		    }
		} // end of if (currentNode.getNodeType() == Node.ELEMENT_NODE)
	    } // end of for loop
	}  // end of if (nodeCount > 0)
    }

    /**
     * Return the <code>ProxySubject</code> in the
     * <code>SessionContextStatement</code>.
     *
     * @return <code>ProxySubject</code>.
     */
    public ProxySubject getProxySubject() {
        return _proxySubject;
    }

    /**
     * Returns the <code>SessionContext</code> in the
     * <code>SessionContextStatement</code>.
     *
     * @return <code>SessionContext</code>
     */
    public SessionContext getSessionContext() {
        return _sessionContext;
    }

    /**
     * Sets the <code>ProxySubject</code> for
     * <code>SessionContextStatement</code>.
     *
     * @param proxySubject the object to be set.
     * @return true if the operation is successful. 
     */
    public boolean setProxySubject(ProxySubject proxySubject) {
        if (proxySubject == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ResourceAccessStatement: " +
                                        "setResourceID:Input is null.");
            }
            return false;
        }
        _proxySubject = proxySubject;
        return true;
    }


    /**
     * Returns the real type of the Statement.
     *
     * @return An integer which represents <code>SessionContextStatement</code>
     *         internally.
     */
    public int getStatementType() {
        return SESSIONCONTEXT_STATEMENT;
    }

    /**
     * Returns  a String representation of <code>SessionContextStatement</code>.
     *
     * @return String representation of the <code>SessionContextStatement</code>.
     *         object.
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
     * @return String representation of the 
     *         <code>&lt;saml:ResourceAccessStatement&gt;</code> element.
     */
    public  String  toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(1000);
	String prefix = "";
        String secprefix = "";
        String libprefix = "";
        String uri = "";
        String securi = "";

        if (includeNS) {
             prefix = SAMLConstants.ASSERTION_PREFIX;
             libprefix = IFSConstants.LIB_PREFIX;
             secprefix = WSSEConstants.TAG_SEC + ":";
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
	    securi = " " + WSSEConstants.TAG_XMLNS + ":" +
		     WSSEConstants.TAG_SEC + "=" + "\"" +
		     WSSEConstants.NS_SEC + "\"";
        }

	try {
            xml.append("<").append(secprefix).
		append(WSSEConstants.TAG_SESSIONCONTEXTSTATEMENT).
		append(securi).append(">\n");
	    xml.append(this._subject.toString(includeNS, true));
	    if (_proxySubject != null) {
		xml.append(_proxySubject.toString(includeNS, declareNS));
	    }
	    xml.append(_sessionContext.toXMLString(includeNS, declareNS));
	    xml.append("</").append(secprefix).
		append("SessionContextStatement>");
	} catch (Exception e) {
	    return null;
	}

        return(xml.toString());
    }
}

