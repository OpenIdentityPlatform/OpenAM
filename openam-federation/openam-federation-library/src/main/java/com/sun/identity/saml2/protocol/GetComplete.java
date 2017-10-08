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
 * $Id: GetComplete.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.GetCompleteImpl;

/** 
 * This interface contains methods for the <code>GetComplete</code> 
 * Element in the SAMLv2 Protocol Schema. 
 * <code>GetComplete</code> Element specifies a URI which resolves to 
 * the complete IDPList.
 *
 * @supported.all.api
 */

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = GetCompleteImpl.class)
public interface GetComplete {
    /** 
     * Returns the value of the <code>GetComplete</code> URI.
     *
     * @return value of the <code>GetComplete</code> URI.
     * @see #setValue(String)
     */
    public String getValue();
    
    /** 
     * Sets the value of the <code>GetComplete</code> URI.
     *
     * @param value new value of the <code>GetComplete</code> URI.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue
     */
    public void setValue(String value) throws SAML2Exception;
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return a String representation of this Object.
     * @throws SAML2Exception if cannot convert to String.
     */
    public String toXMLString() throws SAML2Exception;
    
    /** 
     * Returns a String representation of this Object.
     *
     * @param  includeNSPrefix determines whether or not the namespace
     *         qualifier is prepended to the Element when converted.
     * @param  declareNS determines whether or not the namespace is declared.
     *         within the Element.
     * @throws SAML2Exception if cannot convert to String.
     * @return String representation of this object.
     **/
    
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
    throws SAML2Exception;
        
    /** 
     * Makes this object immutable. 
     *
     */
    public void makeImmutable() ;
    
    /** 
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable();
}
