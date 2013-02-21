/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock US, Inc. All Rights Reserved
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
package org.forgerock.identity.openam.xacml.v3.commons;


import com.sun.identity.entitlement.xacml3.core.Response;

import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;

/**
 * XACML Response Object to XML Utility Class.
 * <p/>
 * Very simple Marshaller to place a XACML Response or any Object into a XML Object.
 * <p/>
 * Could have used JAXB, but we have several conflicts of using due to CLASSPATH
 * issues.
 * <p/>
 * Issue using JAXB in our Environment:
 * <p/>
 * java.lang.LinkageError: You are trying to run JAXB 2.0 runtime
 * (from jar:file:.../webservices-rt-2009-29-07.jar!/com/sun/xml/bind/v2/model/impl/ModelBuilder.class) but you have
 * old JAXB 1.0 runtime earlier in the classpath....
 *
 * @author Jeff.Schenk@forgerock.com
 */

public class POJOToXmlUtility implements XACML3Constants {

    /**
     * POJO to XML String Data Object.
     *
     * @param response - XACML Response Object to be Marshaled into a XML Representation.
     * @return
     * @throws java.io.IOException
     */
    public static String toXML(final Response response) {
        if (response == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        // **********************************************
        // Transform/Marshal/Serialize Request Object to
        // XML Object.
        stringBuilder.append(XML_HEADER);
        stringBuilder.append(response.toXML());
        // Return -- Marshaled Response Object to send over the wire.
        return stringBuilder.toString();
    }


}
