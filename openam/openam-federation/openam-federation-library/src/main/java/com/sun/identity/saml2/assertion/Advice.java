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
 * $Id: Advice.java,v 1.2 2008/06/25 05:47:39 qcheng Exp $
 *
 */
package com.sun.identity.saml2.assertion;

import java.util.List;
import com.sun.identity.saml2.common.SAML2Exception;

/** 
 * The <code>Advice</code> contains any additional information that the 
 * SAML authority wishes to provide.  This information may be ignored 
 * by applications without affecting either the semantics or the 
 * validity of the assertion. An <code>Advice</code> contains a mixture
 * of zero or more <code>Assertion</code>, <code>EncryptedAssertion</code>,
 * <code>AssertionIDRef</code>, and <code>AssertionURIRef</code>.
 *
 * @supported.all.api
 */
public interface Advice {

    /** 
     *  Returns a list of <code>Assertion</code>
     * 
     *  @return a list of <code>Assertion</code>
     */
    public List getAssertions();

    /** 
     *  Sets a list of <code>Assertion</code>
     * 
     *  @param assertions a list of <code>Assertion</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAssertions(List assertions) throws SAML2Exception;

    /** 
     *  Returns a list of <code>AssertionIDRef</code>
     * 
     *  @return a list of <code>AssertionIDRef</code>
     */
    public List getAssertionIDRefs();

    /** 
     *  Sets a list of <code>AssertionIDRef</code>
     * 
     *  @param idRefs a list of <code>AssertionIDRef</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAssertionIDRefs(List idRefs) throws SAML2Exception;

    /** 
     *  Returns a list of <code>AssertionURIRef</code>  
     * 
     *  @return a list of <code>AssertionURIRef</code>
     */
    public List getAssertionURIRefs();

    /** 
     *  Sets a list of <code>AssertionURIRef</code>  
     * 
     *  @param uriRefs a list of <code>AssertionURIRef</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAssertionURIRefs(List uriRefs) throws SAML2Exception;

    /** 
     *  Returns a list of <code>EncryptedAssertion</code>
     * 
     *  @return a list of <code>EncryptedAssertion</code>
     */
    public List getEncryptedAssertions();

    /** 
     *  Sets a list of <code>EncryptedAssertion</code>
     * 
     *  @param encryptedAssertions a list of <code>EncryptedAssertion</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setEncryptedAssertions(List encryptedAssertions) 
        throws SAML2Exception;

    /** 
     *  Returns a list of additional information
     * 
     *  @return a list of additional information
     */
    public List getAdditionalInfo();

    /** 
     *  Sets a list of additional information
     * 
     *  @param info a list of additional information
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAdditionalInfo(List info) throws SAML2Exception;

   /**
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted.
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
