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
 * $Id: AuthzDecisionStatement.java,v 1.2 2008/06/25 05:47:40 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion;

import java.util.List;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>AuthzDecisionStatement</code> element describes a statement
 * by the SAML authority asserting that a request for access by the assertion
 * subject tot he specified resource has resulted in the specified authorization
 * decision on the basis of some optionally specified evidence. Its type is
 * <code>AuthzDecisionStatementType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="AuthzDecisionStatementType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}
 *     StatementAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Action"
 *         maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Evidence"
 *         minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Decision" use="required"
 *       type="{urn:oasis:names:tc:SAML:2.0:assertion}DecisionType" />
 *       &lt;attribute name="Resource" use="required"
 *       type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * @supported.all.api
 */
public interface AuthzDecisionStatement extends Statement {

    /**
     * Returns the <code>Action</code>(s) of the statement.
     *
     * @return List of <code>Action</code>(s) of the statement.
     * @see #setAction(List)
     */
    public List getAction();

    /**
     * Sets the <code>Action</code>(s) of the statement.
     *
     * @param value List of new <code>Action</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAction()
     */
    public void setAction(List value)
        throws SAML2Exception;

    /**
     * Returns the <code>Evidence</code> of the statement.
     *
     * @return <code>Evidence</code> of the statement.
     * @see #setEvidence(Evidence)
     */
    public Evidence getEvidence();

    /**
     * Sets the <code>Evidence</code> of the statement.
     *
     * @param value new value for <code>Evidence</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getEvidence()
     */
    public void setEvidence(Evidence value)
        throws SAML2Exception;

    /**
     * Returns the <code>Resource</code> of the statement.
     *
     * @return the <code>Resource</code> of the statement.
     * @see #setResource(String)
     */
    public String getResource();

    /**
     * Sets the <code>Resource</code> of the statement.
     *
     * @param value new <code>Resource</code> for the statement.
     * @throws SAML2Exception if the object is immutable.
     * @see #getResource()
     */
    public void setResource(String value)
        throws com.sun.identity.saml2.common.SAML2Exception;

    /**
     * Returns the <code>Decision</code> attribute of the statement.
     *
     * @return the <code>Decision</code> attribute of the statement.
     * @see #setDecision(String)
     */
    public String getDecision();

    /**
     * Sets the <code>Decision</code> attribute.
     *
     * @param value new <code>Decision</code> for the statement.
     * @throws SAML2Exception if the object is immutable.
     * @see #getDecision()
     */
    public void setDecision(String value)
        throws SAML2Exception;

}
