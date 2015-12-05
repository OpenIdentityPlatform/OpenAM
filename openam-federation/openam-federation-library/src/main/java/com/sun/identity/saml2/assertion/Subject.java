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
 * $Id: Subject.java,v 1.2 2008/06/25 05:47:41 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */


package com.sun.identity.saml2.assertion;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.identity.saml2.assertion.impl.SubjectImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import java.util.List;

/** 
 * The <code>Subject</code> specifies the principal that is the subject
 * of all of the statements in the assertion. It contains an identifier,
 * a series of one or more subject confirmations, or both.
 *
 * @supported.all.api
 */
@JsonDeserialize(as=SubjectImpl.class)
public interface Subject {

    /**
     *  Returns the encrypted identifier
     *
     *  @return the encrypted identifier
     */
    public EncryptedID getEncryptedID();

    /**
     *  Sets the encrypted identifier
     *
     *  @param value the encrypted identifier
     *  @exception SAML2Exception if the object is immutable
     */
    public void setEncryptedID(EncryptedID value) throws SAML2Exception;

    /**
     *  Returns the identifier in <code>NameID</code> format
     *
     *  @return the identifier in <code>NameID</code> format
     */
    public NameID getNameID();

    /**
     *  Sets the identifier in <code>NameID</code> format
     *
     *  @param value the identifier in <code>NameID</code> format
     *  @exception SAML2Exception if the object is immutable
     */
    public void setNameID(NameID value) throws SAML2Exception;

    /** 
     * Returns a list of subject confirmations
     *
     * @return a list of subject confirmations
     */
    public List getSubjectConfirmation();

    /** 
     * Sets a list of subject confirmations
     *
     * @param confirmations a list of subject confirmations
     *  @exception SAML2Exception if the object is immutable
     */
    public void setSubjectConfirmation(List confirmations)
        throws SAML2Exception;

    /**
     *  Returns the identifier in <code>BaseID</code> format
     *
     *  @return the identifier in <code>BaseID</code> format
     */
    public BaseID getBaseID();

    /**
     *  Sets the identifier in <code>BaseID</code> format
     *
     *  @param value the identifier in <code>BaseID</code> format
     *  @exception SAML2Exception if the object is immutable
     */
    public void setBaseID(BaseID value) throws SAML2Exception;

   /**
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted
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
