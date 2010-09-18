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
 * $Id: SubjectConfirmationData.java,v 1.4 2008/06/25 05:47:42 qcheng Exp $
 *
 */

package com.sun.identity.saml2.assertion;

import java.util.List;
import java.util.Date;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 *  The <code>SubjectConfirmationData</code> specifies additional data
 *  that allows the subject to be confirmed or constrains the circumstances
 *  under which the act of subject confirmation can take place. Subject
 *  confirmation takes place when a relying party seeks to verify the
 *  relationship between an entity presenting the assertion and the 
 *  subject of the assertion's claims.
 *
 *  @supported.all.api
 */
public interface SubjectConfirmationData {

    /**
     *  Returns the time instant at which the subject can no longer be
     *  confirmed
     *
     *  @return the time instant at which the subject can no longer be
     *  confirmed
     */
    public Date getNotOnOrAfter();

    /**
     *  Sets the time instant at which the subject can no longer be
     *  confirmed
     *
     *  @param value the time instant at which the subject can no longer be
     *  confirmed
     *  @exception SAML2Exception if the object is immutable
     */
    public void setNotOnOrAfter(Date value) throws SAML2Exception;

    /**
     *  Returns the ID of a SAML protocol message in response to which
     *  an attesting entity can present the assertion
     *
     *  @return the ID of a SAML protocol message in response to which
     *  an attesting entity can present the assertion
     */
    public String getInResponseTo();

    /**
     *  Sets the ID of a SAML protocol message in response to which
     *  an attesting entity can present the assertion
     *
     *  @param value the ID of a SAML protocol message in response to which
     *  an attesting entity can present the assertion
     *  @exception SAML2Exception if the object is immutable
     */
    public void setInResponseTo(String value) throws SAML2Exception;

    /**
     * Returns a list of arbitrary XML elements to be added to this 
     * <code>SubjectConfirmationData</code> object.
     *
     * @return a list of arbitrary XML elements to be added to this 
     * <code>SubjectConfirmationData</code> object.
     */
    public List getContent();

    /**
     * Sets a list of arbitrary XML elements to be added to this 
     * <code>SubjectConfirmationData</code> object.
     *
     * @param content a list of arbitrary XML elements to be added to this 
     * <code>SubjectConfirmationData</code> object.
     * @exception SAML2Exception if the object is immutable
     */
    public void setContent(List content) throws SAML2Exception;

    /**
     *  Returns the URI specifying the entity or location to which an
     *  attesting entity can present the assertion 
     *
     *  @return the URI specifying the entity or location to which an
     *  attesting entity can present the assertion 
     */
    public String getRecipient();

    /**
     *  Sets the URI specifying the entity or location to which an
     *  attesting entity can present the assertion 
     *
     *  @param value the URI specifying the entity or location to which an
     *  attesting entity can present the assertion 
     *  @exception SAML2Exception if the object is immutable
     */
    public void setRecipient(String value) throws SAML2Exception;

    /**
     *  Returns the time instant before which the subject cannot be confirmed
     *
     *  @return the time instant before which the subject cannot be confirmed
     */
    public Date getNotBefore();

    /**
     *  Sets the time instant before which the subject cannot be confirmed
     *
     *  @param value the time instant before which the subject cannot be
     *         confirmed
     *  @exception SAML2Exception if the object is immutable
     */
    public void setNotBefore(Date value) throws SAML2Exception;

    /**
     *  Returns the network address/location from which an attesting 
     *  entity can present the assertion 
     *
     *  @return the network address/location from which an attesting 
     *  entity can present the assertion 
     */
    public String getAddress();

    /**
     *  Sets the network address/location from which an attesting 
     *  entity can present the assertion 
     *
     *  @param value the network address/location from which an attesting 
     *  entity can present the assertion 
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAddress(String value) throws SAML2Exception;
    
    /**
     *  Returns the content type attribute     
     *
     *  @return the content type attribute     
     *  @see #setContentType(String)
     */
    public String getContentType();
    
    /**
     *  Sets the content type attribute     
     *
     *  @param attribute attribute type value for the content that will be 
     *         added
     *  @exception SAML2Exception if the object is immutable
     *  @see #getContentType()
     */
    public void setContentType(String attribute) throws SAML2Exception;

   /**
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace
    *        qualifier is
    *        prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
     throws SAML2Exception;

   /**
    * Returns a String representation
    *
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString() throws SAML2Exception;

   /**
    * Makes the object immutable
    */
    public void makeImmutable();

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable();

}
