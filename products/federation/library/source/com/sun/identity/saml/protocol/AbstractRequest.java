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
 * $Id: AbstractRequest.java,v 1.2 2008/06/25 05:47:36 qcheng Exp $
 *
 */


package com.sun.identity.saml.protocol;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;

/**
 * This <code>AbstractRequest</code> class is an abstract base class for all
 * SAML Request in <code>samlp</code> namespace. It corresponds to
 * <code>RequestAbstractType</code> in SAML protocol schema.
 *
 * @supported.all.api
 */
public abstract class AbstractRequest {

    /*
     * data members
     */

    protected List	respondWiths	= Collections.EMPTY_LIST;
    protected Element	signature	= null;
    protected String	requestID	= null;
    protected int	majorVersion	= SAMLConstants.PROTOCOL_MAJOR_VERSION;
    protected int	minorVersion	= SAMLConstants.PROTOCOL_MINOR_VERSION;
    protected Date	issueInstant	= null;
    protected boolean	signed		= false;
    protected boolean	valid		= true;

    /*
     * Default constructor.
     */
    protected AbstractRequest() {
    }

    /**
     * Return whether the object is signed or not.
     *
     * @return true if the object is signed; false otherwise.
     */
    public boolean isSigned() {
	return signed;
    }

    /**
     * Return whether the signature on the object is valid or not.
     * @return true if the signature is valid; false otherwise.
     */
    public boolean isSignatureValid() {
	return valid;
    }

    /**
     *  An abstract method to sign the object.
     *
     *  @throws SAMLException If could not sign the object.
     */
    public abstract void signXML() throws SAMLException;

    /**
     * Gets 0 or more of <code>RespondWith</code> in the Request.
     *
     * @return A List of Strings.
     */
    public List getRespondWith() {
	return respondWiths;
    }

    /**
     * Adds a <code>RespondWith</code> to the Request.
     *
     * @param respondWith A String that needs to be added to the Request.
     * @return true if the operation is successful.
     */
    public boolean addRespondWith(String respondWith) {
	if (signed) {
	    return false;
	}
	if ((respondWith == null) || (respondWith.length() == 0)) {
	    return false;
	} else {
	    if ((respondWiths == null) ||
		(respondWiths == Collections.EMPTY_LIST)) {
		respondWiths = new ArrayList();
	    }
	    respondWiths.add(respondWith);
	    return true;
	}
    }

    /**
     * Gets 0 or 1 of Signature in the Request.
     * @return The signature Element the Request contains. It returns null if
     * 		the Request has no signature.
     */
    public Element getSignature() {
	return signature;
    }

    /**
     * Set the signature for the Request 
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
     * Gets the <code>RequestID</code> of the Request.
     * @return the <code>RequestID</code> of the Request.
     */
    public String getRequestID() {
	return requestID;
    }

    /**
     * Set the <code>RequestID</code> of the Request.
     * @param requestID A String that is the <code>RequestID</code> attribute of
     *        the Request.
     * @return true if the operation is successful.
     */
    public boolean setRequestID(String requestID) {
	if (signed) {
	    return false;
	}
	if ((requestID == null) || (requestID.length() == 0)) {
	    return false;
	}
	this.requestID = requestID;
	return true;
    }

    /**
     * Returns the <code>MajorVersion</code> of the Request.
     *
     * @return The <code>MajorVersion</code> of the Request.
     */
    public int getMajorVersion() {
	return majorVersion;
    }

    /**
     * Returns the <code>MinorVersion</code> of the Request.
     *
     * @return The <code>MinorVersion</code> of the request.
     */
    public int getMinorVersion() {
	return minorVersion;
    }
    
    /**
     * Sets the <code>MajorVersion</code> of the Request.
     *
     * @param majorVersion the intended major version for SAML Request
     */
    public void setMajorVersion(int majorVersion) {
	this.majorVersion = majorVersion;
    }

    /**
     * Sets the <code>MinorVersion</code> of the Request.
     *
     * @param minorVersion the intended minor version for SAML Request
     */
    public void setMinorVersion(int minorVersion) {
	this.minorVersion = minorVersion;
    }

    /**
     * Returns the <code>IssueInstant</code> of the Request.
     *
     * @return the <code>IssueInstant</code> of the Request.
     */
    public Date getIssueInstant() {
	return issueInstant;
    }

    /**
     * Set the <code>IssueInstant</code> of the Request.
     *
     * @param issueInstant a Date object representing the time when the Request
     *	      is issued.
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
}
