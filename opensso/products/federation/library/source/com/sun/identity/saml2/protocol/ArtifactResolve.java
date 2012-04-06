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
 * $Id: ArtifactResolve.java,v 1.2 2008/06/25 05:47:56 qcheng Exp $
 *
 */



package com.sun.identity.saml2.protocol;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>ArtifactResolve</code> message is used to request that a SAML
 * protocol message be returned in an <code>ArtifactResponse</code> message
 * by specifying an artifact that represents the SAML protocol message.
 * It has the complex type <code>ArtifactResolveType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="ArtifactResolveType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Artifact"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @supported.all.api
 */

public interface ArtifactResolve extends RequestAbstract {

    /**
     * Gets the <code>Artifact</code> of the request.
     *
     * @return <code>Artifact</code> of the request.
     * @see #setArtifact(Artifact)
     */
    public Artifact getArtifact();

    /**
     * Sets the <code>Artifact</code> of the request.
     * 
     * @param value new <code>Artifact</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getArtifact()
     */
    public void setArtifact(Artifact value)
	throws SAML2Exception;

}
