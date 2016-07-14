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
 * $Id: ArtifactResponse.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */



package com.sun.identity.saml2.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.ArtifactResponseImpl;

/**
 * The <code>ArtifactResopnse</code> message has the complex type
 * <code>ArtifactResponseType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ArtifactResponseType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}StatusResponseType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */

@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = ArtifactResponseImpl.class)
public interface ArtifactResponse extends StatusResponse {

    /**
     * Gets the <code>any</code> element of the response.
     *
     * @return <code>any</code> element in xml string format.
     * @see #setAny(String)
     */
    public String getAny();

    /**
     * Sets the <code>any</code> element of the response.
     * 
     * @param value new <code>any</code> element in xml string format.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAny()
     */
    public void setAny(String value)
	throws SAML2Exception;

}
