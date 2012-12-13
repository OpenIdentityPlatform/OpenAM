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
 * $Id: Attribute.java,v 1.5 2008/06/25 05:48:11 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context;

import com.sun.identity.xacml.common.XACMLException;
import java.net.URI;
import java.util.List;

/**
 * The <code>Attribute</code> element specifies information about the
 * action/subject/resource requested in the <code>Request</code> context by 
 * listing a sequence of <code>Attribute</code> elements associated with 
 * the action.
 * <p>
 * <pre>
 * &lt;xs:element name="Attribute" type="xacml-context:AttributeType"/>
 * &lt;xs:complexType name="AttributeType">
 *    &lt;xs:sequence>
 *       &lt;xs:element ref="xacml-context:AttributeValue" 
 *        maxOccurs="unbounded"/>
 *    &lt;xs:sequence>
 *    &lt;xs:attribute name="AttributeId" type="xs:anyURI" use="required"/>
 *    &lt;xs:attribute name="DataType" type="xs:anyURI" use="required"/>
 *    &lt;xs:attribute name="Issuer" type="xs:string" use="optional"/>
 * &lt;xs:complexType>
 * </pre>
 *@supported.all.api
 */
public interface Attribute {

    /**
     * Returns the AttributeId of the <code>Attribute</code>
     * which the attribute identifier.
     * @return the <code>URI</code> representing the data type.
     */
    public URI getAttributeId() ;

    /**
     * Sets the attributeId of the <code>Attribute</code>.
     * @param attributeID <code>URI</code> representing the attribite id.
     * @exception XACMLException if the object is immutable
     */
    public void setAttributeId(URI attributeID) throws XACMLException;

    /**
     * Returns the issuer of the <code>Attribute</code>.
     * @return <code>String</code> representing the issuer. It MAY be an 
     * x500Name that binds to a public key or some other identification 
     * exchanged out-of-band by participating entities.
     */
    public String getIssuer();

    /**
     * Sets the issuer of the <code>Attribute</code>.
     * @param issuer <code>String</code> representing the issuer. 
     * It MAY be an x500Name that binds to a public key or some other 
     * identification  exchanged out-of-band by participating entities. 
     * This is optional so return value could be null or an empty 
     * <code>String</code>.
     * @exception XACMLException if the object is immutable
     */
    public void setIssuer(String issuer) throws XACMLException;

    /**
     * Returns the datatype of the contents of the <code>AttributeValue</code>
     * elements. This will be either a primitive datatype defined by XACML 2.0 
     * specification or a type ( primitive or structured) defined in a  
     * namespace declared in the <xacml-context> element.
     * @return the <code>URI</code> representing the data type.
     */
    public URI getDataType();

    /**
     * Sets the data type of the contents of the <code>AttributeValue</code>
     * elements.
     * @param dataType <code>URI</code> representing the data type.
     * @exception XACMLException if the object is immutable
     */
    public void setDataType(URI dataType) throws XACMLException;
      

    /**
     * Returns one to many <code>AttributeValue</code> elements for this object
     * each attribite value MAY have empty contents, occur once or occur 
     * multiple times.
     *
     * @return the List <code>AttributeValue</code> elements of this object
     */
    public List getAttributeValues();

    /**
     * Sets the <code>AttributeValue</code> elements of this object
     *
     * @param attrValues List containing <code>AttributeValue</code> elements 
     * of this object.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setAttributeValues(List attrValues) throws XACMLException;

    /**
     * Sets the attribute values for this object
     *
     * @param attrValues <code>List</code> containing <code>String<code> values
     * of this object.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setAttributeStringValues(List attrValues) throws XACMLException;

   /**
    * Returns a <code>String</code> representation of this object
    * @param includeNSPrefix Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return a string representation of this object
    * @exception XACMLException if conversion fails for any reason
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
            throws XACMLException;

   /**
    * Returns a string representation of this object.
    *
    * @return a string representation of this object.
    * @exception XACMLException if conversion fails for any reason.
    */
    public String toXMLString() throws XACMLException;

   /**
    * Makes the object immutable
    */
    public void makeImmutable();

   /**
    * Returns <code>true</code> if the object is mutable.
    *
    * @return <code>true</code> if the object is mutable.
    */
    public boolean isMutable();
    
}
