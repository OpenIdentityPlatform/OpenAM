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
 * $Id: FMActionMapper.java,v 1.3 2008/06/25 05:50:15 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.xacml.plugins;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLConstants;

import com.sun.identity.xacml.context.Action;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.spi.ActionMapper;

import java.net.URI;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * This class implements ActionMapper to map between XACML context 
 * action and FM native action.
 * This mapper would recognise only the following XACML 
 * defined <code>attributeId</code>
 * <pre>
 * urn:oasis:names:tc:xacml:1.0:action:action-id
 * </pre>
 * This attribute would be mapped to an action name in OpenAM Policy.
 * This mapper requires that the dataType of the attribute is
 * <pre>
 * http://www.w3.org/2001/XMLSchema#string
 * </pre>
 */
public class FMActionMapper implements ActionMapper {

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
            Map properties) throws XACMLException {
    }

    /**
     * Returns native action name
     * @param xacmlContextAction XACML  context Action
     * @param serviceName native service name the requested resource belongs to
     * @return native action name
     * @exception XACMLException if can not map to native action name
     */
    public String mapToNativeAction(Action xacmlContextAction, 
            String serviceName) throws XACMLException {
        String nativeAction = null;
        List attributes = xacmlContextAction.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
                Attribute attr = (Attribute) attributes.get(0);
                if (attr != null) {
                    URI tmpURI = attr.getAttributeId();
                    if (tmpURI.toString().equals(XACMLConstants.
                        ACTION_ID)) {
                        tmpURI = attr.getDataType();
                        if (tmpURI.toString().equals(XACMLConstants.XS_STRING)) {
                            Element element = (Element)attr.getAttributeValues().get(0);
                            nativeAction = XMLUtils.getElementValue(element);
                        }
                    }
                }
        }
        return nativeAction;
    }

    /**
     * Returns XACML  context Action
     * @param nativeActionName native action name
     * @param serviceName native service name the requested resource belongs to
     * @return XACML  context Action
     * @exception XACMLException if can not map to XACML  context Action
     */
    public Action mapToXACMLAction(String nativeActionName, 
            String serviceName) throws XACMLException {
        return null;
    }

    /**
     * Returns XACML  context decision effect
     * @param nativeActionEffect native action effect
     * @param serviceName native service name the requested resource belongs to
     * @exception XACMLException if can not map to XACML  context Action
     */
    public String mapToXACMLActionEffect(String nativeActionEffect,
            String serviceName) throws XACMLException {
        return null;
    }

}

