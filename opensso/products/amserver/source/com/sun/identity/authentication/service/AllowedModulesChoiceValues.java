/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AllowedModulesChoiceValues.java,v 1.4 2008/06/25 05:42:04 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.service;

import java.security.AccessController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * The class determines the allowed choices values for authentication modules.
 * It is dynamically computed from defaults values of the attribute
 * <code>iplanet-am-auth-authenticators</code>.
 */
public class AllowedModulesChoiceValues extends ChoiceValues {
    /**
     * Creates <code>AllowedModulesChoiceValues</code> object
     * Default constructor that will be used by the SMS
     * to create an instance of this class
     */
    public AllowedModulesChoiceValues() {
        // do nothing
    }
    
    /**
     * Returns the choice values and their corresponding localization keys.
     *
     * @return Choice values and their corresponding localization keys.
     */
    public Map getChoiceValues() {
        Map answer = new HashMap();
        // Get the AttributeSchema node and walk the tree
        Node attributeSchemaNode = getAttributeSchemaNode();
        // Walk the tree as follows
        // i) Obtain the Organization node, ie., parent node
        Node orgNode = attributeSchemaNode.getParentNode();
        // ii) Organization Node to parent --> Schema node
        Node schemaNode = orgNode.getParentNode();
        // iii) Get "Global" Child Node
        Node globalNode = XMLUtils.getChildNode(schemaNode, GLOBAL);
        // iv) Get AttributeSchemaNode corresponding to
        //     iplanet-am-auth-authenticators
        Node attrNode = XMLUtils.getNamedChildNode(globalNode,
            SCHEMA_ATTRIBUTE, NAME, AUTHENTICATORS);
        // v) Obtain the default values
        Node defaultsNode = XMLUtils.getChildNode(attrNode,
            ATTRIBUTE_DEFAULT_ELEMENT);
        NodeList children = defaultsNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeName().equals(ATTRIBUTE_VALUE)) {
                String defaultValue = XMLUtils.getValueOfValueNode(n);
                // Get the last substring after '.'
                String choiceValue = defaultValue.substring(
                    defaultValue.lastIndexOf('.') + 1);
                if (!choiceValue.equals(APPLICATION_MODULE)) {
                    answer.put(choiceValue, choiceValue);
                }
            }
        }
        // Cache the choice values
        choiceValues = answer;
        return (choiceValues);
    }

    /**
     * Returns choice values from  environment parameters
     * @param envParams map of environment parameters
     * @return choice values from  environment parameters
     */
    public Map getChoiceValues(Map envParams) {
        // Get default choice values
        getChoiceValues();
        
        Set serviceNames = null;
        String orgDN = null;
        Map registeredServices = new HashMap();
        if (envParams != null) {
            orgDN = (String)envParams.get(Constants.ORGANIZATION_NAME);
        }
        if (orgDN == null || orgDN.length() == 0) {
            orgDN = SMSEntry.getRootSuffix();
        }
        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
        AdminTokenAction.getInstance());
        try {
            OrganizationConfigManager orgConfig =
            getOrgConfigManager(orgDN, adminToken);
            serviceNames = orgConfig.getAssignedServices();
        } catch (Exception e) {
            // this Exception should have been (or will be) caught by the
            // caller of of this plugin(console). it does not worth to
            // duplicate log/debug here.
        }
        
        if (serviceNames != null) {
            for (Iterator ite=choiceValues.keySet().iterator(); ite.hasNext();){
                String value = (String) ite.next();
                if (serviceRegistered(value, serviceNames)) {
                    registeredServices.put(value, value);
                } else {
                    String serviceName = AuthUtils.getModuleServiceName(value);
                    try {
                        new ServiceConfigManager(serviceName, adminToken);
                    } catch (SMSException e) {
                        // services don't have template.
                        registeredServices.put(value, value);
                    } catch (Exception e) {
                        // SSO, do nothing
                    }
                }
            }
        }
        return registeredServices;
    }
    
    private boolean serviceRegistered(String name, Set serviceSet) {
        for (Iterator ite = serviceSet.iterator(); ite.hasNext(); ) {
            String serviceName = (String)ite.next();
            if (serviceName.indexOf(name) != -1) {
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * Returns the OrganizationConfigManager Object for an organization.
     * @param orgDN name of the org
     * @param adminToken administrator Single Sign On Token.
     * @return OrganizationConfigManager object
     */
    private OrganizationConfigManager getOrgConfigManager(
        String orgDN,
        SSOToken adminToken) {
        OrganizationConfigManager orgConfigMgr = null;
        try {
            if ((orgMap != null) && (!orgMap.isEmpty())) {
                orgConfigMgr = (OrganizationConfigManager) orgMap.get(orgDN);
            }
            synchronized (orgMap) {
                if (orgConfigMgr == null) {
                    orgConfigMgr = new OrganizationConfigManager(
                        adminToken,orgDN);
                    orgMap.put(orgDN,orgConfigMgr);
                }
            }
        } catch (Exception id) {
            // do nothing
        }
        return orgConfigMgr;
    }
    
    // Cache of choice values
    Map choiceValues = null;
    
    // Constants
    private static final String NAME = "name";
    private static final String GLOBAL = "Global";
    private static final String SCHEMA_ATTRIBUTE = "AttributeSchema";
    private static final String ATTRIBUTE_DEFAULT_ELEMENT = "DefaultValues";
    private static final String ATTRIBUTE_VALUE = "Value";
    private static final String AUTHENTICATORS =
        "iplanet-am-auth-authenticators";
    private static final String APPLICATION_MODULE = "Application";
    private static HashMap orgMap = new HashMap();
}
