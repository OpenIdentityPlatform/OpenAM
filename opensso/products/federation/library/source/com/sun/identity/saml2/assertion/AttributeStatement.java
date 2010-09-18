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
 * $Id: AttributeStatement.java,v 1.2 2008/06/25 05:47:40 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion;

import java.util.List;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>AttributeStatement</code> element describes a statement by
 * the SAML authority asserting that the assertion subject is associated with
 * the specified attributes. It is of type <code>AttributeStatementType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="AttributeStatementType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}
 *     StatementAbstractType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         EncryptedAttribute"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * @supported.all.api
 */
public interface AttributeStatement extends Statement {

    /**
     * Returns <code>Attribute</code>(s) of the statement. 
     *
     * @return List of <code>Attribute</code>(s) in the statement.
     * @see #setAttribute(List)
     */
    public List getAttribute();

    /**
     * Sets <code>Attribute</code>(s) of the statement.
     *
     * @param value List of new <code>Attribute</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttribute()
     */
    public void setAttribute(List value)
        throws SAML2Exception;

    /**
     * Returns <code>EncryptedAttribute</code>(s) of the statement. 
     *
     * @return List of <code>EncryptedAttribute</code>(s) in the statement.
     * @see #setEncryptedAttribute(List)
     */
    public List getEncryptedAttribute();

    /**
     * Sets <code>EncryptedAttribute</code>(s) of the statement.
     *
     * @param value List of new <code>EncryptedAttribute</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getEncryptedAttribute()
     */
    public void setEncryptedAttribute(List value)
        throws SAML2Exception;

}

