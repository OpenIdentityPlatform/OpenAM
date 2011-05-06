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
 * $Id: NameIDPolicy.java,v 1.2 2008/06/25 05:47:57 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol;

import com.sun.identity.saml2.common.SAML2Exception;
import org.w3c.dom.Element;

/** 
 * This interface defines methods to retrieve name identifier related 
 * properties.
 *
 * @supported.all.api
 */
public interface NameIDPolicy {
    
    /** 
     * Returns the value of <code>Format</code> attribute.
     *
     * @return the value of <code>Format</code> attribute.
     * @see #setFormat(String)
     */
    public String getFormat();
    
    /** 
     * Sets the value of the <code>Format</code> attribute.
     *
     * @param uri the new value of <code>Format</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getFormat
     */
    public void setFormat(String uri) throws SAML2Exception;
    
    /** 
     * Returns the value of the <code>SPNameQualifier</code> attribute.
     *
     * @return the value of <code>SPNameQualifier</code> attribute.
     * @see #setSPNameQualifier(String)
     */
    public String getSPNameQualifier();
    
    /** 
     * Sets the value of <code>SPNameQualifier</code> attribute.
     *
     * @param spNameQualifier new value of <code>SPNameQualifier</code> 
     *        attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSPNameQualifier
     */
    public void setSPNameQualifier(String spNameQualifier) throws SAML2Exception ;
    
    /** 
     * Sets the value of <code>AllowCreate</code> attribute.
     *
     * @param allowCreate the new value of <code>AllowCreate</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setAllowCreate(boolean allowCreate) throws SAML2Exception ;
    
    /** 
     * Returns true if the identity provider is allowed to create a
     * new identifier to represent the principal.
     *
     * @return value of <code>AllowCreate</code> attribute.
     */
    public boolean isAllowCreate();
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return String representation of this Object.
     * @throws SAML2Exception if cannot create String object.
     */
    public String toXMLString() throws SAML2Exception ;
    
    /** 
     * Returns a String representation of this object.
     *
     * @param includeNSPrefix determines whether or not the namespace
     *	      qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *	      within the Element.
     * @return String representation of this Object.
     * @throws SAML2Exception if cannot create String object.
     */
    
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
    throws SAML2Exception ;
        
    /** 
     * Makes this object immutable. 
     */
    public void makeImmutable() ;
    
    /** 
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable();
}
