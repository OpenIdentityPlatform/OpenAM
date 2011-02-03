/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ECPRelayState.java,v 1.2 2008/06/25 05:47:46 qcheng Exp $
 *
 */

package com.sun.identity.saml2.ecp;

import com.sun.identity.saml2.common.SAML2Exception;

/** 
 *  The <code>ECPRelayState</code> interface defines methods for properties
 *  required by an ECP RelayState.
 *
 *  @supported.all.api
 */
public interface ECPRelayState {
    
    /** 
     * Returns the value of the <code>RelayState</code>.
     *
     * @return value of the <code>RelayState</code>.
     * @see #setValue(String)
     */
    public String getValue();
    
    /** 
     * Sets the value of the <code>RelayState</code>.
     *
     * @param value new value of the <code>RelayState</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue
     */
    public void setValue(String value) throws SAML2Exception;

    /** 
     * Returns value of <code>mustUnderstand</code> attribute.
     *
     * @return value of <code>mustUnderstand</code> attribute.
     */
    public Boolean isMustUnderstand();
    
    /** 
     * Sets the value of the <code>mustUnderstand</code> attribute.
     *
     * @param mustUnderstand the value of <code>mustUnderstand</code>
     *     attribute.
     * @throws SAML2Exception if the object is immutable.
     */
    public void setMustUnderstand(Boolean mustUnderstand) throws SAML2Exception;

    /**
     * Returns value of <code>actor</code> attribute.
     *
     * @return value of <code>actor</code> attribute
     */
    public String getActor();

    /**
     * Sets the value of <code>actor</code> attribute.
     *
     * @param actor the value of <code>actor</code> attribute
     * @throws SAML2Exception if the object is immutable.
     */
    public void setActor(String actor) throws SAML2Exception;

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
