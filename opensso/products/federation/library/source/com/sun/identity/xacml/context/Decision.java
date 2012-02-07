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
 * $Id: Decision.java,v 1.2 2008/06/25 05:48:11 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context;

import com.sun.identity.xacml.common.XACMLException;

/**
 * The <code>Decision</code> element is a container of 
 * one or more <code>Decision</code>s issued by policy decision point
 * @supported.all.api
 * <p/>
 * Schema:
 * <pre>
 * <xs:simpleType name="DecisionType">
 *     <xs:restriction base="xs:string">
 *         <xs:enumeration value="Permit"/>
 *         <xs:enumeration value="Deny"/>
 *         <xs:enumeration value="Indeterminate"/>
 *         <xs:enumeration value="NotApplicable"/>
 *     </xs:restriction>
 * </xs:simpleType>
 * </pre>
 * 
 */
public interface Decision {

    /**
     * Returns the <code>value</code>s of this object
     *
     * @return the <code>value</code>s of this object
     */
    public String getValue();

    /**
     * Sets the <code>value</code>s of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setValue(String value) throws XACMLException;


   /**
    * Returns a string representation
    *
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException;

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
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable();

   /**
    * Makes the object immutable
    */
    public void makeImmutable();

    
}
