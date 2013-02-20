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
package org.forgerock.identity.openam.xacml.model;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * XACML Request Digester
 *
 * Utility class to perform the UnMarshaling of a XACML XML Request
 * into a POJO for further processing the Original Request Graph.
 *
 * @author jeff.schenk@forgerock.com
 */
public class XACMLRequestDigester {


    public static com.sun.identity.entitlement.xacml3.core.Request digest(String xmlRequestString) {
        try {
            Digester digester = new Digester();
            // Create new instance of the Request Class
            digester.addObjectCreate( "request", com.sun.identity.entitlement.xacml3.core.Request.class );

            // Push the XML Source String object onto the stack
            com.sun.identity.entitlement.xacml3.core.Request request =
                    (com.sun.identity.entitlement.xacml3.core.Request)
                            digester.parse(new ByteArrayInputStream(xmlRequestString.getBytes()));
            return request;
        } catch(SAXException se) {
           // TODO -- Log Exception
        } catch(IOException ioe) {
           // TODO -- Log Exception
        }
        return null;
    }


}
