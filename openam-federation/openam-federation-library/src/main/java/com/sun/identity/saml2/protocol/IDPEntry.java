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
 * $Id: IDPEntry.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.IDPEntryImpl;

/** 
 * This interface defines methods to set/retrieve single identity provider
 * information trusted by the request issuer to authenticate the presenter.
 *
 * @supported.all.api
 */

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = IDPEntryImpl.class)
public interface IDPEntry {
    
    /** 
     * Returns the <code>ProviderID</code> attribute value.
     *
     * @return value of the <code>ProviderID</code> attribute.
     * @see #setProviderID(String)
     */
    public String getProviderID();
    
    /** 
     * Sets the <code>ProviderID</code> attribute value.
     *
     * @param uri new value of <code>ProviderID</code> attribute.
     * @throws SAML2Exception if the object is immutable. 
     * @see #getProviderID
     */
    public void setProviderID(String uri) throws SAML2Exception;
    
    /** 
     * Returns the value of <code>Name</code> attribute.
     *
     * @return value of the <code>Name</code> attribute.
     * @see #setName(String)
     */
    
    public String getName();
    
    /** 
     * Sets the value of <code>Name</code> attribute.
     *
     * @param name new value of <code>Name</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getName
     */
    public void setName(String name) throws SAML2Exception;
    
    /** 
     * Return the value of <code>Loc</code> attribute.
     *
     * @return value of <code>Loc</code> attribute.
     * @see #setLoc(String)
     */
    public String getLoc();
    
    /** 
     * Sets the value of <code>Loc</code> attribute.
     *
     * @param locationURI value of <code>Loc</code> attribute.
     * @throws SAML2Exception if the object is immutable. 
     * @see #getLoc
     */
    
    public void setLoc(String locationURI) throws SAML2Exception;
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return a String representation of this Object.
     * @throws SAML2Exception if cannot create String object.
     */
    public String toXMLString() throws SAML2Exception;
    
    /** 
     * Returns a String representation of this Object.
     *
     * @param includeNSPrefix determines whether or not the namespace
     *        qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *        within the Element.
     * @return the String representation of this Object.
     * @throws SAML2Exception if cannot create String object.
     **/
    
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
	   throws SAML2Exception;

    /** 
     * Makes this object immutable. 
     *
     */
    public void makeImmutable() ;
    
    /** 
     * Returns  true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable();
}
