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
 * $Id: StatusMessage.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This class represents the <code>StatusMessage</code> element in
 * SAML protocol schema.
 * The <code>StatusMessage</code> element specifies a message that MAY be
 * returned to an operator.
 *
 * <pre>
 * &lt;element name="StatusMessage" type="{http://www.w3.org/2001/XMLSchema}string"/>
 * </pre>
 *
 * @supported.all.api
 */

public interface StatusMessage {
    
    /**
     * Returns the <code>StatusMessage</code> value.
     *
     * @return A String value of the <code>StatusMessage</code>
     *
     */
    public java.lang.String getValue();
    
    /**
     * Returns the <code>StatusMessage</code> in an XML document String format
     * based on the <code>StatusMessage</code> schema described above.
     *
     * @return An XML String representing the <code>StatusMessage</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString() throws SAML2Exception;
    
    /**
     * Returns the <code>StatusMessage</code> in an XML document String format
     * based on the <code>StatusMessage</code> schema described above.
     *
     * @param includeNSPrefix Determines whether or not the namespace qualifier 
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>StatusMessage</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
    throws SAML2Exception;
}
