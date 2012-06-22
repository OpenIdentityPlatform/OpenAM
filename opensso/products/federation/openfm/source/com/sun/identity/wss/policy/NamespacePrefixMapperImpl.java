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
 * $Id: NamespacePrefixMapperImpl.java,v 1.1 2009/09/17 05:49:29 mallas Exp $
 *
 */


package com.sun.identity.wss.policy;


import java.util.Map;
import java.util.HashMap;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;


/**
 * Stub implementation - need this for JAXB marshalling
 */
class NamespacePrefixMapperImpl extends NamespacePrefixMapper {
    
    private static Map nsPrefixMap = new HashMap();
    static {
        nsPrefixMap.put(
            "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "sp");
        nsPrefixMap.put(
                "http://schemas.xmlsoap.org/ws/2004/09/policy", "wsp");
    }
    
    public String getPreferredPrefix( String namespaceUri, String suggestion,
        boolean requirePrefix)
    {
        if(nsPrefixMap.containsKey(namespaceUri)) {
           return (String)nsPrefixMap.get(namespaceUri); 
        }
        
        return suggestion;
    }
}
