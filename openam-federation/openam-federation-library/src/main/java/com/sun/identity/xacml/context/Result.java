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
 * $Id: Result.java,v 1.3 2008/06/25 05:48:12 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.policy.Obligations;
import java.util.List;

/**
 * The <code>Result</code> element is a container of 
 * one or more <code>Result</code>s issuded by authorization authority.
 * @supported.all.api
 * <p/>
 * <pre>
 * schema:
 *      &lt;xs:complexType name="ResultType">
 *          &lt;xs:sequence>
 *              &lt;xs:element ref="xacml-context:Decision"/>
 *              &lt;xs:element ref="xacml-context:Status" minOccurs="0"/>
 *              &lt;xs:element ref="xacml:Obligations" minOccurs="0"/>
 *          &lt;xs:sequence>
 *          &lt;xs:attribute name="ResourceId" type="xs:string" use="optional"/>
 *      &lt;xs:complexType>
 * </pre>
 */
public interface Result {

    /**
     * Returns the <code>Resourceid</code>s of this object
     *
     * @return the <code>Resourceid</code>s of this object
     */
    public String getResourceId();

    /**
     * Sets the <code>Resourceid</code>s of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setResourceId(String resourceId) throws XACMLException;

    /**
     * Returns the <code>Decision</code> of this object
     *
     * @return the <code>Decision</code> of this object
     */
    public Decision getDecision();

    /**
     * Sets the <code>Decision</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setDecision(Decision decision) throws XACMLException;

    /**
     * Returns the <code>Status</code> of this object
     *
     * @return the <code>Status</code> of this object
     */
    public Status getStatus();

    /**
     * Sets the <code>Status</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setStatus(Status status) throws XACMLException;

    /**
     * Returns the <code>Obligations</code> of this object
     *
     * @return the <code>Obligations</code> of this object
     */
    public Obligations getObligations();

    /**
     * Sets the <code>Obligations</code> of this object
     * @param obligations <code>Obligations</code> to set
     *
     * @exception XACMLException if the object is immutable
     */
    public void setObligations(Obligations obligations) throws XACMLException;


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
