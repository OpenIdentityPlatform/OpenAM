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
 * $Id: MissingAttributeDetail.java,v 1.2 2008/06/25 05:48:11 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context;

import com.sun.identity.xacml.common.XACMLException;

import java.util.List;

/**
 * The <code>StatusCode</code> element is a container of 
 * one or more <code>Status</code>s issuded by authorization authority.
 * @supported.all.api
 */
public interface MissingAttributeDetail {

    /* schema
        <xs:element name="MissingAttributeDetail" 
                type="xacml-context:MissingAttributeDetailType"/>
        <xs:complexType name="MissingAttributeDetailType">
            <xs:sequence>
                <xs:element ref="xacml-context:AttributeValue" minOccurs="0" 
                        maxOccurs="unbounded"/>
            </xs:sequence>
            <xs:attribute name="AttributeId" type="xs:anyURI" use="required"/>
            <xs:attribute name="DataType" type="xs:anyURI" use="required"/>
            <xs:attribute name="Issuer" type="xs:string" use="optional"/>
        </xs:complexType>

	<xs:element name="AttributeValue" 
                type="xacml-context:AttributeValueType"/>
	<xs:complexType name="AttributeValueType" mixed="true">
            <xs:sequence>
                <xs:any namespace="##any" processContents="lax" minOccurs="0" 
                        maxOccurs="unbounded"/>
            </xs:sequence>
            <xs:anyAttribute namespace="##any" processContents="lax"/>
	</xs:complexType>
    */

    /**
     * Returns the <code>AttributeValue</code>s of this object
     *
     * @return the <code>AttributeValue</code>s of this object
     */
    public List getAttributeValues();

    /**
     * Sets the <code>AttributeValue</code>s of this object
     *
     * @param values the <code>AttributeValue</code>s of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setAttributeValues(List values) throws XACMLException;

    /**
     * Returns the <code>AttributeId</code>s of this object
     *
     * @return the <code>AttributeId</code>s of this object
     */
    public String getAttributeId();

    /**
     * Sets the <code>AttributeId</code>s of this object
     *
     * @param attributeId the <code>AttributeId</code>s of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setAttributeId(String attributeId) throws XACMLException;

    /**
     * Returns the <code>DataType</code>s of this object
     *
     * @return the <code>DataType</code>s of this object
     */
    public String getDataType();

    /**
     * Sets the <code>DataType</code>s of this object
     *
     * @param dataType the <code>DataType</code>s of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setDataType(String dataType) throws XACMLException;

    /**
     * Returns the <code>Issuer</code>s of this object
     *
     * @return the <code>Issuer</code>s of this object
     */
    public String getIssuer();

    /**
     * Sets the <code>Issuer</code>s of this object
     *
     * @param issuer the <code>Issuer</code>s of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setIssuer(String issuer) throws XACMLException;

   /**
    * Returns a string representation
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
    * Returns a string representation
    *
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException;

   /**
    * Makes the object immutable
    */
    public void makeImmutable();

   /**
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable();
    
}
