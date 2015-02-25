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
 * $Id: AMTemplate.java,v 1.4 2008/06/25 05:41:23 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * Represents a service template associated with a AMObject. Sun Java System
 * Access Manager distinguishes between virtual and entry attibutes. Per iDS
 * terminology, virtual attribute is an attribute that is not physically stored
 * in an LDAP entry but still returned with the LDAP entry as a result of a LDAP
 * search operation. Virtual attributes are analagous to the "inherited"
 * attributes. Entry attributes are non-inherited attributes. For each AMObject,
 * virtual attributes can be grouped in a Template on a per-service basis.
 * Hence, there may be one service Template per service for any given AMObject.
 * A given object may have more than one Template, in total, one each for each
 * of the services for that object. Such templates determine the service
 * attributes inherited by all the other objects within the scope of this
 * object.
 * <p>
 * When any object inherits more than one template for the same service (by
 * virtue of being in the scope of two or more objects with service templates),
 * conflicts between such templates are resolved by the use of template
 * priorities. In this priority scheme, zero is the highest possible priority
 * with the lower priorities extending towards finity. Templates with higher
 * priorities will be favored over and to the exclusion of templates with lower
 * priorities. Templates which do not have an explicitly assigned priority are
 * considered to have the lowest priority possible, or no priority. In the case
 * where two or more templates are being considered for inheritance of an
 * attribute value, and they have the same (or no) priority, the result is
 * undefined, but does not exclude the possibility that a value will be
 * returned, however arbitrarily chosen.
 * <p>
 * The two types of templates supported in Identity Management are: Organization
 * templates and Dynamic templates. Organizatin templates manage service
 * attributes of services that are registered to an organization, while dynamic
 * templates manage service attributes for both organizations and roles.
 * <p>
 * Note: Policy templates are no longer supported by AM SDK. Use
 * <code>com.sun.identity.policy</code> package to manage policy attributes.
 * 
 * <PRE>
 * 
 * Code sample on how to obtain service templates from AMOrganization object:
 * 
 * AMTemplate orgTemplate; if (org.orgTemplateExists("iPlanetAMAuthService") {
 * orgTemplate = org.getTemplate("iPlanetAMAuthService",
 * AMTemplate.ORGANIZATION_TEMPLATE); Map attributes =
 * orgTemplate.getAttributes(); - more code here - }
 * 
 * 
 * AMTemplate dynTemplate = org.getTemplate("iPlanetAMSessionService",
 * AMTemplates.DYNAMIC_TEMPLATE); if (dynTemplate.isExists()) { Map attributes =
 * dynTemplate.getAttributes(); - more code here - }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMTemplate extends AMObject {

    // template type
    /** Represents a Policy template type */
    public static final int POLICY_TEMPLATE = 300;

    /** Represents the Dynamic template type */
    public static final int DYNAMIC_TEMPLATE = 301;

    /** Represents an Organization template type */
    public static final int ORGANIZATION_TEMPLATE = 302;

    /**
     * Represents both dynamic and policy template for a service
     */
    public static final int ALL_TEMPLATES = 303;

    /**
     * Represents the priority of a template whose priority is not explicitly
     * set.
     */
    public static final int UNDEFINED_PRIORITY = -1;

    /**
     * Gets the name of the service to which this template belongs. This method
     * can be used in conjunction with SMS APIs to get the
     * <code>AttributeSchema/ServiceSchema</code> for the service.
     * 
     * @return service name.
     */
    public String getServiceName();

    /**
     * Returns a set of Attribute Schemas that defines the schema (metadata) of
     * this template.
     * 
     * @return Set of <code>com.sun.identity.sm.AttributeSchema</code> for
     *         this template.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getAttributeSchemas() throws AMException, SSOException;

    /**
     * Returns the priority of this template in the DIT.
     * 
     * @return priority.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public int getPriority() throws SSOException;

    /**
     * Sets the priority of this template in the DIT
     * 
     * @param priority
     *            priority
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setPriority(int priority) throws AMException, SSOException;

    /**
     * Gets the type of the template.
     * 
     * @return Returns one of the following possible values:
     *         <ul>
     *         <li> DYNAMIC_TEMPLATE
     *         <li> ORGANIZATION_TEMPLATE
     */
    public int getType();

    /**
     * Returns a set of policy distinguished names if this
     * <code>AMTemplate</code> is a named policy template, otherwise returns
     * null.
     * 
     * @return set of policy distinguished names.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getPolicyNames() throws AMException, SSOException;
}
