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
 * $Id: RequestedAuthnContext.java,v 1.2 2008/06/25 05:47:57 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol;

import java.util.List;

import org.w3c.dom.Element;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * Java content class for RequestedAuthnContext element declaration.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <p>
 * <pre>
 * &lt;element name="RequestedAuthnContext" type="{urn:oasis:names:tc:SAML:2.0:protocol}RequestedAuthnContextType"/>
 * </pre>
 * 
 * @supported.all.api
 */
public interface RequestedAuthnContext {

    /**
     * Returns list of authentication context class references. References
     * in the list is String object.
     *
     * @return list of authentication context class references.
     */
    List getAuthnContextClassRef();
    
    /**
     * Sets authentication context class references.
     *
     * @param references List of authentication context class references where
     *        references in the list is String object.
     * @throws SAML2Exception if this object is immutable.
     */
    void setAuthnContextClassRef(List references)
        throws SAML2Exception;

    /**
     * Returns list of authentication context declaration class references. 
     * References in the list is String object.
     *
     * @return list of authentication context declaration class references.
     */
    List getAuthnContextDeclRef();

    /**
     * Sets authentication context declaration class references.
     *
     * @param references List of authentication context declaration class 
     *        references where references in the list is String object.
     * @throws SAML2Exception if this object is immutable.
     */
    void setAuthnContextDeclRef(List references) throws SAML2Exception;

    /**
     * Returns the value of <code>Comparison</code> property.
     * 
     * @return the value of <code>Comparison</code> property.
     */
    String getComparison();

    /**
     * Sets the value of the <code>Comparison</code> property.
     * 
     * @param value the value of the <code>Comparison</code> property.
     * @throws SAML2Exception if <code>Object</code> is immutable.
     */
    void setComparison(String value) throws SAML2Exception;

    /**
     * Returns a String representation of this Object.
     * 
     * @return a String representation of this Object.
     * @throws SAML2Exception if it failed to pasrse the object.
     */
    String toXMLString() throws SAML2Exception;
	
    /**
     * Returns a String representation of this Object.
     *
     * @param includeNSPrefix true to prepend the namespace qualifier
     *        to the Element.
     * @param declareNS true to declare the namespace within the Element.
     * @return the String representation of this Object.
     * @throws SAML2Exception if it failed to pasrse the object.
     */ 
    String toXMLString(boolean includeNSPrefix, boolean declareNS)
           throws SAML2Exception;

    /** 
    * Makes this object immutable by making this object unmodifiable.
    */
    void makeImmutable() ;
   
    /** 
    * Returns true if mutable, false otherwise.
    *
    * @return true if mutable, false otherwise.
    */
    boolean isMutable();
}
