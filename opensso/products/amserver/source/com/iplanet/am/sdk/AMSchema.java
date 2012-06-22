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
 * $Id: AMSchema.java,v 1.2 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.sun.identity.sm.SchemaType;

/**
 * The class <code>AMSchema</code> provides interfaces to get the schema
 * information for service configuration information
 * 
 * @deprecated This class has been deprecated. Please use
 *             <code>com.sun.identity.sm.ServiceSchema</code>.
 */
public class AMSchema {

    public static class Type extends Object {
        private SchemaType schemaType;

        /**
         * The <code>GLOBAL</code> schema type defines the service
         * configuration information that independent of organizations, users
         * and instances. Hence, the service configuration data defined using
         * this schema type will the same across organizations and users. An
         * example could be encryption algorithms used by the service for its
         * internal communication. Such configuration data can be changed only
         * by super administrator.
         */
        public static final Type GLOBAL = new Type(SchemaType.GLOBAL);

        /**
         * The <code>ORGANIZATION</code> schema type defines the service
         * configuration information that are organization dependent and could
         * be configured differently for organizations. Usually these
         * configuration data can be modified by organization administrators. An
         * example would be log level of a service.
         */
        public static final Type ORGANIZATION = new Type(
                SchemaType.ORGANIZATION);

        /**
         * The <code>User</code> schema type defines the service configuration
         * information that are user dependent. An example would user's mail
         * server or mail quota. Usually these configuration data can be
         * modified by users and/or administrators.
         */
        public static final Type USER = new Type(SchemaType.USER);

        /**
         * The <code>POLICY</code> schema type defines the service's privilege
         * information that are service dependent.
         */
        public static final Type POLICY = new Type(SchemaType.POLICY);

        /**
         * The <code>DYNAMIC</code> schema type defines
         */
        public static final Type DYNAMIC = new Type(SchemaType.DYNAMIC);

        private Type() {
            // do nothing
        }

        protected Type(SchemaType type) {
            schemaType = type;
        }

        /**
         * The method returns the string representation of the schema type.
         * 
         * @return String string representation of schema type
         */
        public String toString() {
            return schemaType.toString();
        }

        /**
         * Method to check if two schema types are equal.
         * 
         * @param obj
         *            the reference object with which to compare
         * 
         * @return <code>true</code> if the objects are same; <code>
         * false</code>
         *         otherwise
         */
        public boolean equals(Object obj) {
            if (obj instanceof Type) {
                Type inType = (Type) obj;
                SchemaType inSchemaType = inType.getInternalSchemaType();
                return schemaType.equals(inSchemaType);
            }
            return (false);
        }

        protected SchemaType getInternalSchemaType() {
            return schemaType;
        }

        /**
         * Returns the hash code of the object.
         * 
         * @return the hash code of the object.
         */
        public int hashCode() {
            return (schemaType.hashCode());
        }
    }

}
