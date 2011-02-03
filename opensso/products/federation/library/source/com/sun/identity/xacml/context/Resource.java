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
 * $Id: Resource.java,v 1.3 2008/06/25 05:48:11 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context;

import com.sun.identity.xacml.common.XACMLException;

import java.util.List;
import org.w3c.dom.Element;

/**
 * The <code>Resource</code> element specifies information about the
 * resource to which access is requested by listing a 
 * sequence of <code>Attribute</code> elements associated with the
 * resource. it may include <code>ResourceContent</code>
 * <p>
 * <pre>
 * &lt;xs:element name="Resource" type="xacml-context:ResourceType"/>
 *   &lt;xs:complexType name="ResourceType">
 *     &lt;xs:sequence>
 *       &lt;xs:element ref="xacml-context:ResourceContent" minOccurs="0"/>
 *       &lt;xs:element ref="xacml-context:Attribute" minOccurs="0" 
 *          maxOccurs="unbounded"/>
 *    &lt;xs:sequence>
 *  &lt;xs:complexType>
 * </pre>
 *@supported.all.api
 */
public interface Resource {

    /**
     * Returns the resource content of the <code>Resource</code>
     *
     * @return <code>String</code> representing the contents
     * of the <code>Resource</code>.
     */
    public Element getResourceContent();

    /**
     * Sets the resource content of this object
     *
     * @param resourceContent  Resource Content of this resource
     * resource content  is optional so could be null.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setResourceContent(Element resourceContent) 
        throws XACMLException;

    /**
     * Returns zero to many <code>Attribute</code> elements of this object
     * If no attributes and present, empty <code>List</code> will be returned.
     * Typically a <code>Resource</code> element will contain an <code>
     * Attribute</code> with an <code>AttributeId</code> of
     * "urn:oasis:names:tc:xacml:1.0:resource:resource-id". Each such
     * <code>Attribute</code> SHALL be an absolute abd fully resolved 
     * representation of the identity of the single resource to which
     * access is requested.
     *
     * @return <code>List</code> containing the <code>Attribute</code> 
     * elements of this object
     */
    public List getAttributes();

    /**
     * Sets the <code>Attribute</code> elements of this object
     *
     * @param attributes <code>Attribute</code> elements of this object
     * attributes could be an empty <code>List</code>, if no attributes
     * are present.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setAttributes(List attributes) throws XACMLException;

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
    * Returns a string representation of this object
    *
    * @return a string representation of this object
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
