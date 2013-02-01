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
 * $Id: Request.java,v 1.2 2008/06/25 05:48:11 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context;

import com.sun.identity.xacml.common.XACMLException;

import java.util.List;

/**
 * The <code>Request</code> element is the top-level element in the XACML
 * context scehema. Its an abstraction layer used by the policy language.
 * It contains <code>Subject</code>, <code>Resource</code>, <code>Action
 * </code> and <code>Environment<code> elements.
 * <p>
 * <pre>
 * &lt;xs:complexType name="RequestType">
 *   &lt;xs:sequence>
 *     &lt;xs:element ref="xacml-context:Subject" maxOccurs="unbounded"/>
 *     &lt;xs:element ref="xacml-context:Resource" maxOccurs="unbounded"/>
 *     &lt;xs:element ref="xacml-context:Action"/>
 *     &lt;xs:element ref="xacml-context:Environment"/>
 *   &lt;xs:sequence>
 * &lt;xs:complexType>
 * </pre>
 *@supported.all.api
 */
public interface Request {

    /**
     * Returns the one to many <code>Subject</code> elements of this object
     *
     * @return the <code>Subject</code> elements of this object
     */
    public List getSubjects();

    /**
     * Sets the one to many <code>Subject</code> elements of this object
     *
     * @param subjects the one to many <code>Subject</code> elements of this 
     * object
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setSubjects(List subjects) throws XACMLException;

    /**
     * Returns the one to many <code>Resource</code> elements of this object
     *
     * @return the <code>Resource</code> elements of this object
     */
    public List getResources();

    /**
     * Sets the one to many <code>Resource</code> elements of this object
     *
     * @param resources the one to many <code>Resource</code> elements of this 
     * object
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setResources(List resources) throws XACMLException;

    /**
     *
     * Returns the instance of <code>Action</code>
     *
     * @return instance of <code>Action</code> 
     */
    public Action getAction();

    /**
     * Sets the instance of <code>Action</code>
     *
     * @param action instance of <code>Action</code> 
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setAction(Action action) throws XACMLException;

    /**
     * Returns the instance of <code>Environment</code>
     *
     * @return the instance of <code>Environment</code>
     */
    public Environment getEnvironment();

    /**
     * Sets the instance of <code>Environment</code>
     *
     * @param env instance of <code>Environment</code>
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setEnvironment(Environment env) throws XACMLException;

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
