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
 * $Id: AMEntityType.java,v 1.6 2008/06/25 05:41:20 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchemaManager;


/**
 * This class defines a supported managed object type by <code> AM SDK </code>
 * It defines the name, type, service name of the object. A set of the supported
 * types can be obtained by using the class <code>AMStoreConnection</code>:
 * <p>
 * 
 * <PRE>
 * 
 *        AMStoreConnection amsc = new AMStoreConnection(ssotoken); 
 *        Set supportedTypes = amsc.getSupportedTypes(); 
 *        Iterator it = supportedTypes.iterator(); 
 *        while (it.hasNext()) { 
 *            AMEntityType thisType = (AMEntityType) it.next(); 
 *            // Do stuff with AMEntityType 
 *        }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public final class AMEntityType {

    private String name;

    private int type;

    private String serviceName;

    private String searchTemplateName;

    private String creationTemplateName;

    private String containerRDN;

    private int containerType;

    private String stAttribute;

    private String namingAttribute;

    private String objectClass;

    private Debug debug = AMCommonUtils.debug;

    /**
     * 
     * @param name
     *            Name of entity
     * @param type
     *            The integer type of entity
     * @param serviceName
     *            Name of service to be used to display the entity profile
     * @param searchTemplate
     *            Name of search template to be used to search for this entity
     * @param creationTemplate
     *            Name of creation template to be used
     * @param containerDN
     *            Relative Distinguished Name of the container in which this
     *            entity shoould be created.
     * @param containerType
     *            The integer type of the container.
     * @param nAttr
     *            Naming attribute of this entity
     * @param stAttr
     *            Status attribute of this entity, if any. Not all entities have
     *            status attributes.
     * @param oc
     *            Objectclass used to identify this entry.
     */
    protected AMEntityType(String name, int type, String serviceName,
            String searchTemplate, String creationTemplate, String containerDN,
            int containerType, String nAttr, String stAttr, String oc) {
        this.name = name;
        this.type = type;
        this.serviceName = serviceName;
        this.containerRDN = containerDN;
        this.containerType = containerType;
        this.stAttribute = stAttr;
        this.namingAttribute = nAttr;
        this.objectClass = oc;
        this.searchTemplateName = searchTemplate;
        this.creationTemplateName = creationTemplate;
        if (debug.messageEnabled()) {
            debug.message("AMEntityType:Constructor-> created type "
                    + toString());
        }
    }

    /**
     * Returns a string representation of this Entity.
     * 
     * @return a string representation of this Entity.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Entity Name=\t").append(name).append("\n").append(
                "Entity type=\t").append(type).append("\n").append(
                "Object Class=\t").append(objectClass).append("\n").append(
                "Service Name=\t").append(serviceName).append("\n").append(
                "Creation Template=\t").append(creationTemplateName).append(
                "\n").append("Search Template=\t").append(searchTemplateName)
                .append("\n").append("Naming Attribute=\t").append(
                        namingAttribute).append("\n").append(
                        "Status Attribute=\t").append(stAttribute).append("\n")
                .append("Container RDN=\t").append(containerRDN).append("\n")
                .append("Container Type=\t").append(containerType).append("\n");
        return sb.toString();
    }

    /**
     * Returns the name of the entity
     * 
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the integer type of the entity
     * 
     * @return type
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the service name to be used to display entity profile
     * 
     * @return service Name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the schema manager for the service defined to display this
     * profile in the console. If the service is not defined then an exception
     * is thrown.
     * 
     * @param token
     *            Single sign on token of the user
     * @return com.sun.identity.sm.ServiceSchemaManager
     * @throws AMException
     *             If unable to obtain the service schema, or if schema is not
     *             defined.
     * @throws SSOException
     *             if the single sign on token of user is invalid.
     */
    public ServiceSchemaManager getServiceSchemaManager(SSOToken token)
            throws AMException, SSOException {
        if (serviceName == null || serviceName.length() == 0) {
            Object args[] = { name };
            throw new AMException(AMSDKBundle.getString("978", args), "978",
                    args);
        } else {
            try {
                return new ServiceSchemaManager(serviceName, token);
            } catch (SMSException smse) {
                debug.error("AMEntityType.getServiceSchemaManager: "
                        + "SM Exception", smse);
                Object args[] = { name };
                throw new AMException(AMSDKBundle.getString("978", args),
                        "978", args);
            }
        }
    }

    /**
     * Returns the naming attribute
     * 
     * @return value of naming attribute.
     */
    protected String getNamingAttribute() {
        return namingAttribute;
    }

    /**
     * Returns the objectclass
     * 
     * @return objectclass used to identify this entry.
     */
    protected String getObjectClass() {
        return objectClass;
    }

    /**
     * Returns the creation template name
     * 
     * @return name of creation template used
     */
    protected String getCreationTemplate() {
        return creationTemplateName;
    }

    /**
     * Returns the search template name
     * 
     * @return returns the name of the search template for this entity type
     */
    public String getSearchTemplate() {
        return searchTemplateName;
    }

    /**
     * Returns the parent container RDN
     * 
     * @return relative distinguished name of the container in which this
     * entity shoould be created.

     */
    protected String getContainerRDN() {
        return containerRDN;
    }

    /**
     * Returns the parent container type
     * 
     * @return the integer type of the container.
     */
    protected int getContainerType() {
        return containerType;
    }
}
