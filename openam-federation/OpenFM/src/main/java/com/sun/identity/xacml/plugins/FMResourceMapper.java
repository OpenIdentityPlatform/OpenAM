/*
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
 * $Id: FMResourceMapper.java,v 1.3 2008/06/25 05:50:16 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.xacml.plugins;

import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.Resource;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.spi.ResourceMapper;

import java.net.URI;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * This class implements ResourceMapper to map between XACML context 
 * Resource and FM native resource
 * This mapper would recognise only the following XACML 
 * defined <code>attributeId</code>
 * <pre>
 *   urn:oasis:names:tc:xacml:1.0:resource:resource-id
 * </pre>
 * The attribtue is required to have dataType
 * <pre>
 * http://www.w3.org/2001/XMLSchema#string
 * </pre>
 * Attribute resource-id is mapped to OpenAM Policy resource name.
 *
 * This mapper also recognises only additional attributeId
 * <pre>
 * urn:opensso:names:xacml:2.0:resource:target-service
 * </pre>
 * The attribtue is required to have dataType
 * <pre>
 * http://www.w3.org/2001/XMLSchema#string
 * </pre>
 * Attribute target-service is mapped to OpenAM Policy service type name
 *
 * If the attribute is not specified in the request a default value 
 * of <code>iPlanetAMWebAgentService</code> would be used. This is 
 * the service name for policies that protect URLs.
 */
public class FMResourceMapper implements ResourceMapper {

    /**
     * Initializes the mapper implementation. This would be called immediately 
     * after constructing an instance of the implementation.
     *
     * @param pdpEntityId EntityID of PDP
     * @param pepEntityId EntityID of PEP
     * @param properties configuration properties
     * @exception XACMLException if can not initialize
     */
    public void initialize(String pdpEntityId, String pepEntityId, 
            Map properties) 
            throws XACMLException {
    }

    /**
     * Returns native resource and service name
     * @param xacmlContextResource XACML  context Resource
     * @return native resource and service name. 
     *         Returned object is an array of String objects.
     *         First element would be resource name.
     *         Second element would be service name.
     * @exception XACMLException if can not map to native resource
     *                            and service name
     */
    public String[] mapToNativeResource(Resource xacmlContextResource) 
            throws XACMLException {
        String[] resourceService = new String[2];
        String resourceName = null;
        String serviceName = null;
        List attributes = xacmlContextResource.getAttributes();
        if (attributes != null) {
            for (int count = 0; count < attributes.size(); count++) {
                Attribute attr = (Attribute) attributes.get(count);
                if (attr != null) {
                    URI tmpURI = attr.getAttributeId();
                    if (tmpURI.toString().equals(XACMLConstants.
                        RESOURCE_ID)) {
                        tmpURI = attr.getDataType();
                        if (tmpURI.toString().equals(XACMLConstants.XS_STRING)) {
                            Element element = (Element)attr.getAttributeValues().get(0);
                            resourceName = XMLUtils.getElementValue(element);
                        }
                    } else if (tmpURI.toString().equals(XACMLConstants.TARGET_SERVICE)) {
                        tmpURI = attr.getDataType();
                        if (tmpURI.toString().equals(XACMLConstants.XS_STRING)) {
                            Element element = (Element)attr.getAttributeValues().get(0);
                            serviceName = XMLUtils.getElementValue(element);
                        }

                    }
                }
            }
        }
        resourceService[0] = resourceName;
        resourceService[1] = serviceName;
        return resourceService;
    }

    /**
     * Returns XACML  context Resource
     * @param resourceName native resource name
     * @param serviceName native service name the requested resource belongs to
     * @return XACML  context Resource
     * @exception XACMLException if can not map to XACML  context Resource
     */
    public Resource mapToXACMLResoure(String resourceName, 
            String serviceName) throws XACMLException {
        return null;
    }

}

