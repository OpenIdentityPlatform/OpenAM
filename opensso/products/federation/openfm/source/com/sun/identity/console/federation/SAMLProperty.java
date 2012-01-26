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
 * $Id: SAMLProperty.java,v 1.3 2008/06/25 05:49:36 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.sun.identity.saml.common.SAMLConstants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SAMLProperty implements Serializable {
    public static final String COMMON_SETTINGS = "common";
    public static final String ROLE_DESTINATION = "destination";
    public static final String ROLE_SOURCE = "source";
    
    public static final String METHOD_ARTIFACT = "artifact";
    public static final String METHOD_POST = "post";
    public static final String METHOD_SOAP = "soap";
    
    private static Set mandatoryAttributeNames = new HashSet ();
    
    static {
        mandatoryAttributeNames.add (SAMLConstants.SOURCEID);
        mandatoryAttributeNames.add (SAMLConstants.TARGET);
        mandatoryAttributeNames.add (SAMLConstants.SAMLURL);
        mandatoryAttributeNames.add (SAMLConstants.HOST_LIST);
        mandatoryAttributeNames.add (SAMLConstants.POSTURL);
        mandatoryAttributeNames.add (SAMLConstants.SOAPUrl);
        mandatoryAttributeNames.add (SAMLConstants.ISSUER);
    }
    
    private String name;
    private String role;
    private String bindMethod;
    private String[] attributeNames;
    private Set mandatory = new HashSet ();
    
    SAMLProperty (
        String name,
        String role,
        String bindMethod,
        String[] attributeNames,
        String[] mandNames
    ) {
        this.name = name;
        this.role = role;
        this.bindMethod = bindMethod;
        this.attributeNames = attributeNames;
        
        int sz = mandNames.length;
        for (int i = 0; i < sz; i++) {
            mandatory.add (mandNames[i]);
        }
    }
    
    List getAttributeNames() {
        int sz = attributeNames.length;
        List attributes = new ArrayList (sz);
        for (int i = 0; i < sz; i++) {
            attributes.add (attributeNames[i]);
        }
        return attributes;
    }
    
    Set getMandatoryAttributeNames() {
        return mandatory;
    }

    /**
     * getRole.
     *
     * @return role
     */
    public String getRole() {
        return role;
    }
    
    /**
     * getBindMethod.
     *
     * @return bindMethod
     */
    public String getBindMethod() {
        return bindMethod;
    }

    /**
     * check if attribute name is in manadatory
     *
     * @param name attribute name
     * @return true if attribute name is in mandatory.
     *         false if attribute name is not in mandatory.
     */
    public boolean isMandatoryAttribute(String name) {
        return mandatory.contains(name);
    }
}
