/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: WSSPolicyUtils.java,v 1.1 2009/09/17 05:49:29 mallas Exp $
 *
 */
package com.sun.identity.wss.policy;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

public class WSSPolicyUtils {
    
      // dsig elements are buried in 'any' elements. Grrr...
    private static final String JAXB_PACKAGES =
        "com.sun.identity.wsfederation.jaxb.xmlsig:" +
        "com.sun.identity.wsfederation.jaxb.wsu:" +
        "com.sun.identity.wsfederation.jaxb.wsse:" +
        "com.sun.identity.wsfederation.jaxb.wsaddr:" +
        "com.sun.identity.wsfederation.jaxb.wspolicy:" +
        "com.sun.identity.wsfederation.jaxb.wsspolicy";
    
    private static JAXBContext jaxbContext = null;
    private static final String PROP_JAXB_FORMATTED_OUTPUT =
        "jaxb.formatted.output";
    private static final String PROP_NAMESPACE_PREFIX_MAPPER =
        "com.sun.xml.bind.namespacePrefixMapper";

    private static NamespacePrefixMapperImpl nsPrefixMapper =
        new NamespacePrefixMapperImpl();

    static {
        try {
            jaxbContext = JAXBContext.newInstance(JAXB_PACKAGES);
        } catch (JAXBException jaxbe) {
           
        }
    }
    
    public static String convertJAXBToString(Object jaxbObj)
        throws JAXBException {

        StringWriter sw = new StringWriter();
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(PROP_JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PROP_NAMESPACE_PREFIX_MAPPER, nsPrefixMapper);
        marshaller.marshal(jaxbObj, sw);
        return sw.toString();
    }
     
     
}