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
 * $Id: Obligations.java,v 1.3 2008/11/10 22:57:05 veiming Exp $
 *
 */

package com.sun.identity.xacml.policy;

import com.sun.identity.xacml.common.XACMLException;

import java.net.URI;
import java.util.List;

/**
 * The <code>Obligations</code> element is a container of 
 * one or more <code>Obligation</code>s issuded by 
 * authorization authority.
 * @supported.all.api
 */
public interface Obligations {

    /* schema
	<xs:element name="Obligations" type="xacml:ObligationsType"/>
	<xs:complexType name="ObligationsType">
            <xs:sequence>
                <xs:element ref="xacml:Obligation" maxOccurs="unbounded"/>
            </xs:sequence>
	</xs:complexType>
    */

    public static final String OBLIGATIONS_ELEMENT = "Obligations";

    /**
     * Returns the <code>Obligation</code> objects set in this
     * <code>Obligations</code>
     * @return the <code>Obligation</code> objects set in this
     * <code>Obligations</code>
     */
    public List getObligations();

    /**
     * Sets the <code>Obligation</code> objects of this
     * <code>Obligations</code>
     * @param obligations the <code>Obligation</code> objects to set in this
     * <code>Obligations</code>
     */
    public void setObligations(List obligations) throws XACMLException;

    public void addObligation(Obligation obligation) throws XACMLException;

   /**
    * Returns a string representation of this object
    * @param includeNSPrefix Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
            throws XACMLException;

   /**
    * Returns a string representation of this object
    *
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException;

   /**
    * Makes this object immutable
    */
    public void makeImmutable();

   /**
    * Checks if this object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable();
    
}
