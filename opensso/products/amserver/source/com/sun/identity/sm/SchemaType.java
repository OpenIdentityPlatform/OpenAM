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
 * $Id: SchemaType.java,v 1.3 2008/06/25 05:44:05 qcheng Exp $
 *
 */

package com.sun.identity.sm;

/**
 * The class <code>SchemaType</code> defines the types of schema objects, and
 * provides static constants for these schema objects. Currently defined schema
 * objects are <code>SchemaType.GLOBAL</code>, <code>
 * SchemaType.ORGANIZATION</code>,
 * <code>
 * SchemaType.USER</code>, <code> SchemaType.POLICY
 * </code> and
 * <code> SchemaType.DYNAMIC </code>. The usage of the respective schema types
 * are defined along with their declaration.
 *
 * @supported.all.api
 */
public class SchemaType extends Object {

    private String schemaType;

    private String lSchemaType;

    /**
     * The <code>GLOBAL</code> schema type defines the service configuration
     * information that independent of organizations, users and instances.
     * Hence, the service configuration data defined using this schema type will
     * the same across organizations and users. An example could be encryption
     * algorithms used by the service for its internal communication. Such
     * configuration data can be changed only by super administrator.
     */
    public static final SchemaType GLOBAL = new SchemaType("Global");

    /**
     * The <code>ORGANIZATION</code> schema type defines the service
     * configuration information that are organization dependent and could be
     * configured differently for organizations. Usually these configuration
     * data can be modified by organization administrators. An example would be
     * log level of a service.
     */
    public static final SchemaType ORGANIZATION = 
        new SchemaType("Organization");

    /**
     * The <code>User</code> schema type defines the service configuration
     * information that are user dependent. An example would user's mail server
     * or mail quota. Usually these configuration data can be modified by users
     * and/or administrators.
     */
    public static final SchemaType USER = new SchemaType("User");

    /**
     * The <code>POLICY</code> schema type defines the service's privilege
     * information that are service dependent.
     */
    public static final SchemaType POLICY = new SchemaType("Policy");

    /**
     * The <code>DYNAMIC</code> schema type defines
     */
    public static final SchemaType DYNAMIC = new SchemaType("Dynamic");

    /**
     * The <code>GROUP</code> schema type defines attributes for a group
     */
    public static final SchemaType GROUP = new SchemaType("Group");

    /**
     * The <code>DOMAIN</code> schema type defines attributes for a domain
     */
    public static final SchemaType DOMAIN = new SchemaType("Domain");

    private SchemaType() {
        // do nothing
    }

    public SchemaType(String type) {
        schemaType = type;
        lSchemaType = type.toLowerCase();
    }

    /**
     * The method returns the string representation of the schema type.
     * 
     * @return String string representation of schema type
     */
    public String toString() {
        return ("SchemaType: " + schemaType);
    }

    /**
     * Method to check if two schema types are equal.
     * 
     * @param schemaType
     *            the reference object with which to compare
     * 
     * @return <code>true</code> if the objects are same; <code>
     * false</code>
     *         otherwise
     */
    public boolean equals(Object schemaType) {
        if (schemaType instanceof SchemaType) {
            SchemaType s = (SchemaType) schemaType;
            return (s.lSchemaType.equalsIgnoreCase(this.lSchemaType));
        }
        return (false);
    }

    /**
     * Returns the hash code of the object.
     * 
     * @return the hash code of the object.
     */
    public int hashCode() {
        return (lSchemaType.hashCode());
    }

    public String getType() {
        return (schemaType);
    }
}
