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
 * $Id: ServiceType.java,v 1.5 2008/06/25 05:43:45 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.util.*;

import org.w3c.dom.*;

import com.sun.identity.sm.*;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.policy.interfaces.*;


/**
 * The class <code>ServiceType</code> provides interfaces to
 * obtain information about a service, for which a policy
 * could be created. The information that can be obtained are
 * actions names (privileges) of service and its schema, whether the
 * service requires resource names and <code>ResourceName</code>
 * used to compare the resources
 */
public class ServiceType {

    private String serviceTypeName;
    private ServiceSchemaManager schemaManager;
    private ServiceSchema policySchema;
    private Node policySchemaNode;
    private ResourceName resourceNameUtil = null;
    private Set actionNames;
    private Map actionSchemas;


    private static final String resourceClass = 
        "com.sun.identity.policy.plugins.PrefixResourceName";
    private static final String resourceWildcard = "*";
    private static final String resourceOneLevelWildcard = "-*-";
    private static final String resourceDelimiter = "/";
    private static final String resourceCase = "false";

    /**
     * Constructs an instance of <code>ServiceType</code>
     */
    private ServiceType() {
        // cannot be instantiated
    }

    /**
     * Constructor used by <code>ServiceTypeManager</code>
     * to construct an instance of <code>ServiceType</code>,
     * @param serviceName name of the service for which to construct
     * <code>ServiceType</code>
     * @param ssm <code>ServiceTypeManager</code> to initialize the 
     * the <code>ServiceType</code> with
     */
    protected ServiceType(String serviceName, ServiceSchemaManager ssm,
        ServiceSchema pschema) {
        serviceTypeName = serviceName;
        schemaManager = ssm;
        policySchema = pschema;
        policySchemaNode = policySchema.getSchemaNode();
        actionNames = Collections.unmodifiableSet(getActionNamesInternal());
        actionSchemas = new HashMap();
        Iterator iter = actionNames.iterator();
        while ( iter.hasNext() ) {
            String action = (String) iter.next();
            actionSchemas.put(action, getActionSchemaInternal(action));
        }
        actionSchemas = Collections.unmodifiableMap(actionSchemas);
        String className = resourceClass;
        Map resourceMap = null;
        try {
            // get resource comparator configuration information
            resourceMap = 
                PolicyConfig.getResourceCompareConfig(serviceTypeName);
            if (resourceMap != null) {
                className = (String)
                resourceMap.get(PolicyConfig.RESOURCE_COMPARATOR_CLASS);
            } else {
                // we did not get resource comparator configuration.
                // use default configuration.
                resourceMap = new HashMap();
                resourceMap.put(PolicyConfig.RESOURCE_COMPARATOR_DELIMITER, 
                                resourceDelimiter);
                resourceMap.put(PolicyConfig.RESOURCE_COMPARATOR_WILDCARD, 
                                resourceWildcard);
                resourceMap.put(PolicyConfig.RESOURCE_COMPARATOR_CASE_SENSITIVE,
                                resourceCase);
                resourceMap.put(PolicyConfig.
                    RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD, 
                    resourceOneLevelWildcard);
            }
            if (className != null) {
                Class resourceClass = Class.forName(className);
                resourceNameUtil = (ResourceName) resourceClass.newInstance();
                // we pass all the resource compare parameters to Resourcename
                // implementation. Alterntively we could also pass these 
                // information to the compare method directly if number of
                // parameters are small. The advantage of the later approach is
                // that individual ResourceName implementations don't need 
                // to remember these parameters
                resourceNameUtil.initialize(resourceMap);
            }
        } catch (PolicyException e) {
            PolicyManager.debug.error("Failed to get resource " + 
                                "comparator For service: " + serviceName, e);
        } catch (ClassNotFoundException e) {
            PolicyManager.debug.error("ServiceType: Illegal exception ", e);
        } catch (IllegalAccessException e) {
            PolicyManager.debug.error("ServiceType: Illegal exception ", e);
        } catch (InstantiationException e) {
            PolicyManager.debug.error("ServiceType: InstantiationException " + 
                                                " exception ", e);
        }
        if (PolicyManager.debug.messageEnabled()) {
            PolicyManager.debug.message("class name is : " + className +
                                        " service name is: " + serviceName);
        }
    }

    /**
     * Returns <code>ResourceName</code> used by this object
     * @return <code>ResourceName</code> used by this object
     */
    public ResourceName getResourceNameComparator() {
        return resourceNameUtil;
    }
    /**
     * Returns the name of this object
     * @return the name of this object
     */
    public String getName() {
        return (serviceTypeName);
    }

    /**
     * Returns the I18N properties file name for the service.
     * If the I18N properties file is not defined by the service
     * XML (via SMS) the method returns <code>null</code>.
     * Used by GUI and CLI to display the service information
     *
     * @return name of the service type
     */
    public String getI18NPropertiesFileName() {
        return (schemaManager.getI18NFileName());
    }

    /**
     * Returns the I18N key for the service name.
     * If the I18N key is not defined by the service XML (via SMS),
     * or if it is not accessable the method returns
     * <code>null</code>.
     * Used by GUI and CLI to display the service name.
     *
     * @return name of the service type
     */
    public String getI18NKey() {
        try {
            return (schemaManager.getGlobalSchema().getI18NKey());
        } catch (Exception e) {
            // Ignore the exception and return null
            return (null);
        }
    }

    /**
     * Returns the actions supported by the service.
     * The returned <code>Set</code> has the actions names
     * sorted alphabetically in the ascending order.
     *
     * @return sorted set of all action names
     */
    public Set getActionNames() {
        return actionNames;
    }

    /**
     * Returns the actions supported by the service.
     * The returned <code>Set</code> has the actions names
     * sorted alphabetically in the ascending order.
     *
     * @return sorted set of all action names
     */
    private Set getActionNamesInternal() {
        if (policySchemaNode != null) {
            TreeSet answer = new TreeSet(StringComparator.getInstance());
            NodeList children = policySchemaNode.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeName().equalsIgnoreCase(
                    ActionSchema.ACTION_SCHEMA)) {
                    answer.add(XMLUtils.getNodeAttributeValue(node,
                        PolicyManager.NAME_ATTRIBUTE));
                }
            }
            return (answer);
        } else {
            return (Collections.EMPTY_SET);
        }
    }

    /**
     * Returns the schema associated with the action name. The schema
     * provides information such as its type (single, multiple, choice)
     * and its syntax (boolean, string, number, etc.). This information
     * will be useful for GUI/CLI to prompt users to input action values
     * and to validate the action values.
     *
     * @param actionName name of the action for which schema
     * will be returned; if action name is invalid an
     * <code>InvalidActioNameException</code> is thrown
     *
     * @return schema for the action values 
     *
     * @exception InvalidNameException if the action name is not a valid
     * action for the service
     */
    public ActionSchema getActionSchema(String actionName)
            throws InvalidNameException {
        ActionSchema actionSchema 
                = (ActionSchema) actionSchemas.get(actionName);
        if (actionSchema == null) {
            if (PolicyManager.debug.warningEnabled()) {
                PolicyManager.debug.warning("Action name: " + actionName +
                    " not valid for service: " + serviceTypeName);
            }
            String objs[] = { serviceTypeName, actionName };
            throw (new InvalidNameException(ResBundleUtils.rbName,
                    "invalid_action_name", objs, actionName,
                    PolicyException.SERVICE));
        }
        return actionSchema;
    }

    /**
     * Returns the schema associated with the action name. The schema
     * provides information such as its type (single, multiple, choice)
     * and its syntax (boolean, string, number, etc.). This information
     * will be useful for GUI/CLI to prompt users to input action values
     * and to validate the action values.
     *
     * @param actionName name of the action for which schema
     * will be returned; if action name is invalid an
     * <code>InvalidActioNameException</code> is thrown
     *
     * @return schema for the action values 
     *
     */
    private ActionSchema getActionSchemaInternal(String actionName) {
        ActionSchema actionSchema = null;
        Node actionSchemaNode = XMLUtils.getNamedChildNode(policySchemaNode,
                ActionSchema.ACTION_SCHEMA, PolicyManager.NAME_ATTRIBUTE, 
                actionName);
        if (actionSchemaNode != null) {
            actionSchema = new ActionSchema(actionSchemaNode);
        }
        return actionSchema;
    }

    /**
     * Returns the action schemas for all the actions supported by
     * the service. The returned <code>Map</code> has the action names sorted in
     * alphabetic ascending order.
     *
     * @return schemas for all the actions suppored by the service
     */
    public Map getActionSchemas() {
        return actionSchemas;
    }

    /**
     * Returns <code>true</code> if the action names and its values
     * in the given <code>Map</code> are valid and satisfy the schema
     * for the respective action names.
     *
     * @param actionValues a <code>Map</code> that contains action names
     * as its "key" and a <code>Set</code> of action values as its "value"
     * for the action name
     *
     * @return <code>true</code> if the action names and its values
     * satisfy the action schemas for the service
     *
     * @exception InvalidNameException if either the action names or action
     * values do not match the schema defined by the service
     */
    public boolean validateActionValues(Map actionValues)
        throws InvalidNameException {
        if ((actionValues == null) || (actionValues.isEmpty())) {
            // nothing really to validate
            return (true);
        }

        // For each action name in the map, check if it is valid
        Iterator actionNames = actionValues.keySet().iterator();
        while (actionNames.hasNext()) {
            try {
                String actionName = (String) actionNames.next();
                ActionSchema as = getActionSchema(actionName);

                // Make sure the values is a String or a Set
                Set values = null;
                Object o = actionValues.get(actionName);
                if (o instanceof java.lang.String) {
                    // It is a string, add it to the set
                    values = new HashSet();
                    values.add(o);
                } else {
                    values = (Set) o;
                }
                // Validate the values against the action schema
                ActionSchema.validate(as, values);
            } catch (ClassCastException cce) {
                // Invalid class in the Map
                PolicyManager.debug.error(
                    "In validate action name and values invalid class name: " +
                    cce.getMessage());
                throw (new InvalidNameException(ResBundleUtils.rbName,
                    "invalid_class_name", null, serviceTypeName,
                    PolicyException.POLICY));
            }
        }
        return (true);
    }

    /**
     * Returns <code>true</code> is the service has the specified action name.
     *
     * @return <code>true</code> if the service has the action name
     * <code>false</code> otherwise
     */
    public boolean containsActionName(String actionName) {
        return (getActionNames().contains(actionName));
    }

    /**
     * Compares two resources of this service type to determine their equality.
     * The method returns a <code>ResourceMatch</code> object which
     * specifies if the resources match exactly, do not match, or one
     * of them is a subordinate resource of the other. Wildcards in 
     * resource1 are escaped. Wildcards in resource2 are interpreted.
     *
     * @param resource1 resource name 1
     * @param resource2 resource name 2
     *
     * @return returns <code>ResourceMatch</code> that
     * specifies if the resources are exact match, or
     * otherwise. 
     * Return value describes resource2
     * For example, if the return value is 
     * ResourceMatch.SUPER_RESOURCE, resource2 
     * is super resource of resource1.
     */
    protected ResourceMatch compare(String resource1, String resource2) {
        if (resourceNameUtil == null) {
            return (ResourceMatch.NO_MATCH); // no resource comparator
        }
        return (resourceNameUtil.compare(resource1, resource2, true ));
    }

    /**
     * Compares two resources of this service type to determine their equality.
     * The method returns a <code>ResourceMatch</code> object which
     * specifies if the resources match exactly, do not match, or one
     * of them is a subordinate resource of the other. Wildcards in 
     * resource1 are escaped. Wildcards in resource2 are interpreted.
     *
     * @param resource1 resource name 1
     * @param resource2 resource name 2
     * @param interpretWildCard  if <code>true</code>, wild cards in resource2
     *                        are interpreted. Else, the wildcards are 
     *                        escaped.
     *
     * @return returns <code>ResourceMatch</code> that
     * specifies if the resources are exact match, or
     * otherwise. 
     * Return value describes resource2
     * For example, if the return value is 
     * ResourceMatch.SUPER_RESOURCE, resource2 is 
     * super resource of resource1.
     */
    protected ResourceMatch compare(String resource1, String resource2,
            boolean interpretWildCard ) {
        if (resourceNameUtil == null) {
            return (ResourceMatch.NO_MATCH); // no resource comparator
        }
        return (resourceNameUtil.compare(resource1, resource2, 
                interpretWildCard));
    }

    /**
     * Combine super-resource and sub-resource of this service type.
     *
     * @param superRes name of the resoruce which will be combined with
     * @param subRes name of the resource which will be combined
     *
     * @return returns a combined resource string
     */
    public String append(String superRes, String subRes) {
        if (resourceNameUtil == null) {
            PolicyManager.debug.error("Append: Don't have resource comparator");
            return(superRes);
        }
        return (resourceNameUtil.append(superRes, subRes));
    }

    /**
     * Method to get sub-resource from an original resource minus
     * a super resource. This is the complementary method of
     * append().
     *
     * @param res name of the original resource consisting of
     *        the second parameter superRes and the returned value
     * @param superRes name of the super-resource which the first
     *        parameter begins with.
     * @return the sub-resource which the first parameter
     * ends with. If the first parameter doesn't begin with the
     * the first parameter, then the return value is null.
     */
    public String getSubResource(String res, String superRes) {
        if (resourceNameUtil == null) {
            PolicyManager.debug.error("getSubRes: Don't have resource "
                +"comparator");
            return(res);
        }
        return (resourceNameUtil.getSubResource(res, superRes));
    }

    /**
     * Method to split a resource into the smallest possible
     * subresource units
     *
     * @param res name of the resource to be split
     *
     * @return returns the array of sub-resources, with the first
     * element being what the original resource begins with, and
     * the last one being what it ends with
     */
    public String[] split(String res){
        if (resourceNameUtil == null) {
            PolicyManager.debug.error("split: Don't have resource comparator");
            String[] list = new String[1];
            list[0] = res;
            return(list);
        }
        return (resourceNameUtil.split(res));
    }
    
    /**
     * Method to identify policies that have the specified resource.
     *
     * @return policy names that have the resource name
     */
    protected Set getPoliciesForExactResourceMatch(String resoruceName) {
        return (null);
    }

    /**
     * Method to identify polcies that has the specified resource
     * and its superior resources.
     */
    protected Set getPolicyForResource(String resourceName) {
        return (null);
    }

    /**
     * Returns <code>true</code> if the service has resource names.
     *
     * @return <code>true</code> if the service has resource names;
     * <code>false</code> otherwise
     */
    public boolean hasResourceNames() {
        boolean hasResources = false;
        Collection actionSchemas = getActionSchemas().values();
        Iterator iter = actionSchemas.iterator();
        while ( iter.hasNext()) {
            ActionSchema actionSchema = (ActionSchema) iter.next();
            if ( actionSchema.requiresResourceName() ) {
                hasResources = true;
                break;
            }
        }
        return hasResources;
    }

    /**
     * Canonicalizes the resource name.
     * @param resourceName resource name to canonicalize
     * @return canonicalized resource name
     */
    String canonicalize(String resourceName) throws PolicyException {
        if (resourceNameUtil == null) {
            return (resourceName); // no resource comparator
        }
        return (resourceNameUtil.canonicalize(resourceName));
    }

}

