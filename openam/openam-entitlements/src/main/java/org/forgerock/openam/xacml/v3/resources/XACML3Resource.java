/**
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.xacml.v3.resources;

/**
 * XACML Resource
 * <p/>
 * Provides Top Level Object for XACML v3 Resources
 * which represent end components for the overall XACML Framework.
 *
 * @author Jeff.Schenk@forgerock.com
 */
public abstract class XACML3Resource {

    /**
     * Current Standards Schema Resource Name.
     */
    public static final String xacmlCoreSchemaResourceName =
            "xsd/xacml-core-v3-schema-wd-17.xsd";
    /**
     * XML Core Schema Resource Name.
     */
    public static final String xmlCoreSchemaResourceName =
            "xsd/xml.xsd";
    /**
     *  XACML 3 Default Namespace.
     */
    public static final String XACML3_NAMESPACE = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17";
    /**
     * Common Key Definitions
     */
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    public static final String PDP_ENDPOINT = "/xacml/pdp";

}
