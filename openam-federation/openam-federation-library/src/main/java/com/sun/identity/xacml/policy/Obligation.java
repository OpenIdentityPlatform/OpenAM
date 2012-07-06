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
 * $Id: Obligation.java,v 1.2 2008/06/25 05:48:13 qcheng Exp $
 *
 */

package com.sun.identity.xacml.policy;

import com.sun.identity.xacml.common.XACMLException;

import java.net.URI;
import java.util.List;

/**
 * The <code>Obligation</code> element is a container of 
 * one or more <code>AttributeAssignment</code>s issuded by 
 * authorization authority.
 * @supported.all.api
 */
public interface Obligation {

    /* schema
	<xs:element name="Obligation" type="xacml:ObligationType"/>
	<xs:complexType name="ObligationType">
            <xs:sequence>
                <xs:element ref="xacml:AttributeAssignment" minOccurs="0" 
                        maxOccurs="unbounded"/>
            </xs:sequence>
            <xs:attribute name="ObligationId" type="xs:anyURI" 
                    use="required"/>
            <xs:attribute name="FulfillOn" type="xacml:EffectType" 
                    use="required"/>
	</xs:complexType>
    */

    /**
     * Returns the ObligationId of this <code>Obligation</code>
     * @return the <code>URI</code> representing ObligationId of this 
     * <code>Obligation</code>
     */
    public URI getObligationId();

    /**
     * Sets the ObligationId of the <code>Obligation</code>
     * @param obligationId <code>URI</code> representing the ObligationId.
     * @exception XACMLException if the object is immutable
     */
    public void setObligationId(URI obligationId) 
            throws XACMLException;

    /**
     * Returns the FullFillOn effect type of this obligation
     * @return the FullFillOn effect type of this obligation
     */
    public String getFulfillOn();

    /**
     * Sets the FullFillOn effect type of this obligation
     * @param fulfillOn FullFillOn effect type of this obligation
     */
    public void setFulfillOn(String fulfillOn) 
            throws XACMLException;



    /**
     * Returns XML elements corresponding to 
     * <code>AttributeAssignment</code> elements for  this obligation.
     *
     * @return the XML elements corresponding to 
     * <code>AttributeAssignment</code> elements for  this obligation.
     */
    public List getAttributeAssignments();

    /**
     * Sets XML elements corresponding to 
     * <code>AttributeAssignment</code> elements for  this obligation.
     *
     * @param attributeAssignments XML elements corresponding to 
     * <code>AttributeAssignment</code> elements for  this obligation.
     */
    public void setAttributeAssignments(List attributeAssignments) 
            throws XACMLException;

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
