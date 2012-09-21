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
 * $Id: SubjectConfirmation.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 */
package com.sun.identity.saml2.assertion;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 *  The <code>SubjectConfirmation</code> provides the means for a relying 
 *  party to verify the correspondence of the subject of the assertion
 *  with the party with whom the relying party is communicating.
 *
 *  @supported.all.api
 */
public interface SubjectConfirmation {

    /**
     * Returns the encrypted ID
     *
     * @return the encrypted ID 
     */
    public EncryptedID getEncryptedID();

    /**
     * Sets the encrypted ID
     *
     * @param value the encrypted ID 
     * @exception SAML2Exception if the object is immutable
     */
    public void setEncryptedID(EncryptedID value) throws SAML2Exception;

    /**
     * Returns the subject confirmation data
     *
     * @return the subject confirmation data 
     */
    public SubjectConfirmationData getSubjectConfirmationData();

    /**
     * Sets the subject confirmation data
     *
     * @param value the subject confirmation data 
     * @exception SAML2Exception if the object is immutable
     */
    public void setSubjectConfirmationData(SubjectConfirmationData value)
        throws SAML2Exception;

    /**
     * Returns the name identifier 
     *
     * @return the name identifier 
     */
    public NameID getNameID();

    /**
     * Sets the name identifier 
     *
     * @param value the name identifier 
     * @exception SAML2Exception if the object is immutable
     */
    public void setNameID(NameID value) throws SAML2Exception;

    /**
     * Returns the base ID 
     *
     * @return the base ID 
     */
    public BaseID getBaseID();

    /**
     * Sets the base ID 
     *
     * @param value the base ID 
     * @exception SAML2Exception if the object is immutable
     */
    public void setBaseID(BaseID value) throws SAML2Exception;

    /**
     * Returns the confirmation method 
     *
     * @return the confirmation method 
     */
    public String getMethod();

    /**
     * Sets the confirmation method 
     *
     * @param value the confirmation method 
     * @exception SAML2Exception if the object is immutable
     */
    public void setMethod(String value) throws SAML2Exception;

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
