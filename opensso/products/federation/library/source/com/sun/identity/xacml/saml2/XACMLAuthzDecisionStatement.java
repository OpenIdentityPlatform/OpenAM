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
 * $Id: XACMLAuthzDecisionStatement.java,v 1.4 2008/06/25 05:48:15 qcheng Exp $
 *
 */

package com.sun.identity.xacml.saml2;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.saml2.assertion.Statement;

/**
 * <code>XACMLAuthzDecisionStatement<code> is an extension of
 * <code>samlp:StatementAbstractType</code> that is carried in a 
 * SAML Assertion to convey <code>xacml-context:Response</code>
 *
 * Schema:
 * <p>
 * <pre>
 * &lt;xs:element name="XACMLAuthzDecisionStatement"
 *          type="xacml-saml:XACMLAuthzDecisionStatementType"/>
 * &lt;xs:complexType name="XACMLAuthzDecisionStatementType">
 *   &lt;xs:complexContent>
 *     &lt;xs:extension base="saml:StatementAbstractType">
 *      &lt;xs:sequence>
 *        &lt;xs:element ref="xacml-context:Response"/>
 *        &lt;xs:element ref="xacml-context:Request"  minOccurs="0"/>
 *      &lt;xs:sequence>
 *    &lt;xs:extension>
 *  &lt;xs:complexContent>
 * &lt;xs:complexType>
 * <pre>
 * </p>
 *
 * Schema for Base:
 * Schema for the base type is
 * <p>
 * <pre>
 * &lt;complexType name="StatementAbstractType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * </p>
 *
 *@supported.all.api
 *
 **/

public interface XACMLAuthzDecisionStatement extends Statement {

    /**
     * Returns <code>Response</code> element of this object
     *
     * @return the <code>Response</code> element of this object
     */
   public Response getResponse();

    /**
     * Sets <code>Response</code> element of this object
     * @param response XACML context <code>Response</code> element to be 
     * set in this object
     *
     * @exception XACMLException if the object is immutable
     */
   public void setResponse(Response response) throws XACMLException;

    /**
     * Returns <code>Request</code> element of this object
     *
     * @return the <code>Request</code> element of this object
     */
   public Request getRequest() throws XACMLException;

    /**
     * Sets <code>Request</code> element of this object
     * @param request XACML context <code>Request</code> element to be 
     * set in this object
     *
     * @exception XACMLException if the object is immutable
     */
   public void setRequest(Request request) throws XACMLException;

    /**
     * Makes the object immutable.
     */
    public void makeImmutable();

    /**
     * Returns the mutability of the object.
     *
     * @return true if the object is mutable; false otherwise.
     */
    public boolean isMutable();

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *         By default name space name is prepended to the element name.
     * @throws XACMLException if the object does not conform to the schema.
     */
    public String toXMLString() throws XACMLException;

    /**
     * Returns a String representation of the element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *                prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *                within the Element.
     * @return A string containing the valid XML for this element
     * @throws XACMLException if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws XACMLException;
 
}
