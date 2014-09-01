/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SubjectQueryAbstractImpl.java,v 1.2 2008/06/25 05:48:01 qcheng Exp $
 *
 */

package com.sun.identity.saml2.protocol.impl;

import org.w3c.dom.Element;

import java.util.ListIterator;
import java.util.Set;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.SubjectQueryAbstract;
import com.sun.identity.saml2.protocol.ProtocolFactory;

abstract public class SubjectQueryAbstractImpl 
   extends RequestAbstractImpl implements SubjectQueryAbstract {

    protected Subject subject;

    /** 
     * Returns the <code>Subject</code> object. 
     *
     * @return the <code>Subject</code> object. 
     * @see #setSubject(Subject)
     */
    public Subject getSubject() {
	return subject;
    }
    
    /** 
     * Sets the <code>Subject</code> object. 
     *
     * @param subject the new <code>Subject</code> object. 
     * @throws SAML2Exception if the object is immutable.
     * @see #getSubject
     */
    public void setSubject(Subject subject) throws SAML2Exception {
         if (!isMutable) {
	    throw new SAML2Exception(
		    SAML2SDKUtils.bundle.getString("objectImmutable"));
	}
	this.subject = subject;
    }

    protected void getXMLString(Set namespaces, StringBuffer attrs,
        StringBuffer childElements, boolean includeNSPrefix, boolean declareNS)
        throws SAML2Exception {

        validateData();

        super.getXMLString(namespaces, attrs, childElements, includeNSPrefix,
            declareNS);
        childElements.append(subject.toXMLString(includeNSPrefix,
            declareNS)).append(SAML2Constants.NEWLINE);
    }

    protected void validateData() throws SAML2Exception {
        if (subject == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("SubjectQueryAbstractImpl." +
                "getXMLString: Subject is expected");
            }

           throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("schemaViolation"));
        }
    }

    /** 
     * Parses attributes of the Docuemnt Element for this object.
     * 
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMAttributes(Element element) throws SAML2Exception {
        super.parseDOMAttributes(element);
    }

    /** 
     * Parses child elements of the Docuemnt Element for this object.
     * 
     * @param iter the child elements iterator.
     * @throws SAML2Exception if error parsing the Document Element.
     */ 
    protected void parseDOMChileElements(ListIterator iter)
        throws SAML2Exception {
        super.parseDOMChileElements(iter);

        if (iter.hasNext()) {
            Element childElement = (Element)iter.next();
            String localName = childElement.getLocalName() ;
            if (SAML2Constants.SUBJECT.equals(localName)) {
                subject =
                    AssertionFactory.getInstance().createSubject(childElement);
                return;
            }
        }

        if (SAML2SDKUtils.debug.messageEnabled()) {
            SAML2SDKUtils.debug.message("SubjectQueryAbstractImpl." +
                "parseDOMChileElements: Subject is expected");
        }
        throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("schemaViolation"));
    }
}
