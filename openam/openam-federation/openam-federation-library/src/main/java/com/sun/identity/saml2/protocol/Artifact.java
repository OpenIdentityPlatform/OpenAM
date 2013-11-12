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
 * $Id: Artifact.java,v 1.2 2008/06/25 05:47:55 qcheng Exp $
 *
 */



package com.sun.identity.saml2.protocol;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This class represents the <code>Artifact</code> element in
 * SAMLv2 protocol schema.
 * <p>
 * <pre>
 * &lt;element name="Artifact" type="{http://www.w3.org/2001/XMLSchema}string"/>
 * </pre>
 *
 * @supported.all.api
 */
public interface Artifact {

    /**
     * Returns the artifact.
     *
     * @return the value of the artifact. It's <code>Base64</code>
     *		encoded.
     */
    public String getArtifactValue();

    /**
     * Returns the <code>SourceID</code> of the artifact.
     *
     * @return The <code>SourceID</code> of the artifact.
     */
    public String getSourceID();

    /**
     * Returns the <code>MessageHandle</code> of the artifact.
     *		The result will be decoded.
     *
     * @return The <code>MessageHandle</code> of the artifact.
     */
    public String getMessageHandle();

    /**
     * Returns the <code>TypeCode</code> of the artifact.
     * @return The byte array of the <code>TypeCode</code> for the artifact.
     */
    public byte[] getTypeCode();

    /**
     * Returns the <code>EndpointIndex</code> of the artifact.
     * @return value of the <code>EndpointIndex</code> for the
     *		artifact.
     */
    public int getEndpointIndex();

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *		By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString()
	throws SAML2Exception;

    /**
     * Returns a String representation of the element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *		prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *		within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
	throws SAML2Exception;

}
