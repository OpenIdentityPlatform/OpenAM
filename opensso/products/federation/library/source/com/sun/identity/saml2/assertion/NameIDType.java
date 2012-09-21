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
 * $Id: NameIDType.java,v 1.2 2008/06/25 05:47:41 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 *  The <code>NameIDType</code> is used when an element serves to represent 
 *  an entity by a string-valued name. In addition to the string content
 *  containing the actual identifier, it provides the following optional
 *  attributes:
 *    <code>NameQualifier</code>
 *    <code>SPNameQualifier</code>
 *    <code>Format</code>
 *    <code>SPProvidedID</code>
 *  @supported.all.api 
 */
public interface NameIDType {

    /**
     *  Returns the string-valued identifier
     *
     *  @return the string-valued identifier
     */
    public String getValue();

    /**
     *  Sets the string-valued identifier
     *
     *  @param value the string-valued identifier
     *  @exception SAML2Exception if the object is immutable
     */
    public void setValue(String value) throws SAML2Exception;

    /**
     *  Returns the name qualifier
     *
     *  @return the name qualifier 
     */
    public String getNameQualifier();

    /**
     *  Sets the name qualifier
     *
     *  @param value the name qualifier 
     *  @exception SAML2Exception if the object is immutable
     */
    public void setNameQualifier(String value) throws SAML2Exception;

    /**
     *  Returns the <code>SP</code> provided ID
     *
     *  @return the <code>SP</code> provided ID
     */
    public String getSPProvidedID();

    /**
     *  Sets the <code>SP</code> provided ID
     *
     *  @param value the <code>SP</code> provided ID
     *  @exception SAML2Exception if the object is immutable
     */
    public void setSPProvidedID(String value) throws SAML2Exception;

    /**
     *  Returns the <code>SP</code> name qualifier
     *
     *  @return the <code>SP</code> name qualifier 
     */
    public String getSPNameQualifier();

    /**
     *  Sets the <code>SP</code> name qualifier
     *
     *  @param value the <code>SP</code> name qualifier 
     *  @exception SAML2Exception if the object is immutable
     */
    public void setSPNameQualifier(String value) throws SAML2Exception;

    /**
     *  Returns the format 
     *
     *  @return the format
     */
    public String getFormat();

    /**
     *  Sets the format 
     *
     *  @param value the format
     *  @exception SAML2Exception if the object is immutable
     */
    public void setFormat(String value) throws SAML2Exception;

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
