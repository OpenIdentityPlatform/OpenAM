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
 * $Id: AbstractResponse.java,v 1.2 2008/06/25 05:47:36 qcheng Exp $
 *
 */

 
package com.sun.identity.saml.protocol;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;

/**
 * This <code>AbstractResponse</code> class is an abstract base class for all
 * SAML Response in <code>samlp</code> namespace. It corresponds to
 * <code>ResponseAbstractType</code> in SAML protocol schema.
 *
 * @supported.all.api
 */
public abstract class AbstractResponse {

    protected String	responseID	= null;
    protected String	inResponseTo	= null;
    protected int	majorVersion	= SAMLConstants.PROTOCOL_MAJOR_VERSION;
    protected int	minorVersion	= SAMLConstants.PROTOCOL_MINOR_VERSION;
    protected Element	signature	= null;
    protected Date	issueInstant	= null;
    protected String	recipient	= null;
    protected boolean	signed		= false;
    protected boolean	valid		= true;
    protected boolean validationDone    = false;

    /**
     * Default constructor.
     */
    protected AbstractResponse() {
    }

    /**
     * Return whether the object is signed or not.
     * @return true if the object is signed; false otherwise.
     */
    public boolean isSigned() {
	return signed;
    }

    /**
     * Returns whether the signature on the object is valid or not.
     *
     * @return boolean true if the signature is valid; false otherwise.
     */
    public boolean isSignatureValid() {
	return valid;
    }

    /**
     * An abstract method that signs the object.
     *
     * @exception SAMLException if could not sign the object.
     */
    public abstract void signXML() throws SAMLException;

    /**
     * Gets the <code>ResponseID</code> of the Response.
     *
     * @return the <code>ResponseID</code> of the Response.
     */
    public String getResponseID() {
	return responseID;
    }

    /**
     * Set the <code>ResponseID</code> of the Response.
     *
     * @param responseID A String that is the <code>ResponseID</code> attribute
     *        of the Response.
     * @return true if the operation is successful.
     */
    public boolean setResponseID(String responseID) {
	if (signed) {
	    return false;
	}
	if ((responseID == null) || (responseID.length() == 0)) {
	    return false;
	} 
	this.responseID = responseID;
	return true;
    }

    /**
     * Gets the <code>InResponseTo</code> of the Response.
     * @return the <code>InResponseTo</code> of the Response.
     */
    public String getInResponseTo() {
	return inResponseTo;
    }

    /**
     * Set the <code>InResponseTo</code> of the Response.
     * @param inResponseTo The <code>InResponseTo</code> attribute of the
     *        Response.
     * @return true if the operation is successful.
     */
    public boolean setInResponseTo(String inResponseTo) {
	if (signed) {
	    return false;
	}
	if ((inResponseTo == null) || (inResponseTo.length() == 0)) {
	    return false;
	}
	this.inResponseTo = inResponseTo;
	return true;
    }

    /**
     * Gets the <code>MajorVersion</code> of the Response.
     * @return The <code>MajorVersion</code> of the Response.
     */
    public int getMajorVersion() {
	return majorVersion;
    }

    /**
     * Gets the <code>MinorVersion</code> of the Response.
     * @return The <code>MinorVersion</code> of the SAML response.
     */
    public int getMinorVersion() {
	return minorVersion;
    }
    
    /**
     * Sets the <code>MajorVersion</code> of the Response.
     * @param majorVersion the intended major version of SAML response.
     */
    public void setMajorVersion(int majorVersion) {
	this.majorVersion = majorVersion;
    }

    /**
     * Sets the <code>MinorVersion</code> of the Response.
     * @param minorVersion the intended minor version of SAML response.
     */
    public void setMinorVersion(int minorVersion) {
	this.minorVersion = minorVersion;
    }

    /**
     * Gets the signature of the Response.
     * @return The signature element of the Response.
     *		null is returned if the Response has no ds:Signature.
     */
    public Element getSignature() {
	return signature;
    }

    /**
     * Set the signature for the Response.
     *
     * @param elem <code>ds:Signature</code> element
     * @return true if the operation succeeds.
     */
    public boolean setSignature(Element elem) {
        if (signed) {
	    return false;
	}
	if (elem == null) {
	    return false;
	} else {
	    signature = elem;
	    signed = true;
	    return true;
	}
    }

    /**
     * Returns the <code>IssueInstant</code> of the Response.
     *
     * @return the <code>IssueInstant</code> of the Response.
     */
    public Date getIssueInstant() {
	return issueInstant;
    }

    /**
     * Set the <code>IssueInstant</code> of the Response.
     *
     * @param issueInstant a Date object representing the time when the Response
     *          is issued.
     * @return true if the operation succeeds.
     */
    public boolean setIssueInstant(Date issueInstant) {
	if (signed) {
	    return false;
	}
	if (issueInstant == null) {
	    return false;
	}
	this.issueInstant = issueInstant;
	return true;
    }

    /**
     * Gets the recipient of the Response.
     *
     * @return The Recipient.
     */
    public String getRecipient() {
	return recipient;
    }

    /**
     * Set the Recipient attribute of the Response.
     *
     * @param recipient A String representing the Recipient attribute of the
     *	      Response.
     * @return true if the operation is successful;
     */
    public boolean setRecipient(String recipient) {
	if (signed) {
	    return false;
	}
	if ((recipient == null) || (recipient.length() == 0)) {
	     return false;
	}
	this.recipient = recipient;
	return true;
    }
}
