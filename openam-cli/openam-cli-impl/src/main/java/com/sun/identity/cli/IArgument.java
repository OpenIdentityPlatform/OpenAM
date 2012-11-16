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
 * $Id: IArgument.java,v 1.12 2009/12/18 07:13:25 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.cli;


/**
 * Arguments constants.
 */
public interface IArgument {
    /**
     * Resource bundle argument.
     */
    String RESOURCE_BUNDLE_NAME = "bundlename";

    /**
     * Resource bundle locale argument.
     */
    String RESOURCE_BUNDLE_LOCALE = "bundlelocale";

    /**
     * Service name argument.
     */
    String SERVICE_NAME = "servicename";

    /**
     * Sub Configuration name argument.
     */
    String SUB_CONFIGURATION_NAME = "subconfigname";

    /**
     * Sub Schema Name.
     */
    String SUBSCHEMA_NAME = "subschemaname";

    /**
     * Schema Type.
     */
    String SCHEMA_TYPE = "schematype";

    /**
     * Choice values.
     */
    String CHOICE_VALUES = "choicevalues";

    /**
     * Attribute names.
     */
    String ATTRIBUTE_NAMES = "attributenames";

    /**
     * Attribute schema name.
     */
    String ATTRIBUTE_SCHEMA = "attributeschema";

    /**
     * Attribute name.
     */
    String ATTRIBUTE_NAME = "attributename";

    /**
     * Property names.
     */
    String PROPERTY_NAMES = "propertynames";

    /**
     * Default values.
     */
    String DEFAULT_VALUES = "defaultvalues";

    /**
     * Type of sub configuration.
     */
    String SUB_CONFIGURATION_ID = "subconfigid";

    /**
     * Realm name argument.
     */
    String REALM_NAME = "realm";

    /**
     * Data file argument.
     */
    String DATA_FILE = "datafile";

    /**
     * Mandatory argument.
     */
    String MANDATORY = "mandatory";

    /**
     * Recursive argument.
     */
    String RECURSIVE = "recursive";

    /**
     * Filter pattern argument.
     */
    String FILTER = "filter";

    /**
     * Continue argument.
     */
    String CONTINUE = "continue";

    /**
     * XML file argument.
     */
    String XML_FILE = "xmlfile";

    /**
     * Privileges argument.
     */
    String PRIVILEGES = "privileges";

    /**
     * Attribute value pair argument.
     */
    String ATTRIBUTE_VALUES = "attributevalues";

    /**
     * Output file name.
     */
    String OUTPUT_FILE = "outfile";

    /**
     * Secret key for encrypting and decrypting password.
     */
    String ENCRYPT_SECRET = "encryptsecret";

    /**
     * Agent name.
     */
    String AGENT_NAME = "agentname";

    /**
     * Agent group  name.
     */
    String AGENT_GROUP_NAME = "agentgroupname";

    /**
     * Agent names.
     */
    String AGENT_NAMES = "agentnames";

    /**
     * Agent group names.
     */
    String AGENT_GROUP_NAMES = "agentgroupnames";

    /**
     * Agent type.
     */
    String AGENT_TYPE = "agenttype";

    /**
     * Set attribute values falg.
     */
    String AGENT_SET_ATTR_VALUE = "set";

    /**
     * Server name.
     */
    String SERVER_NAME = "servername";

    /**
     * Server names.
     */
    String SERVER_NAMES = "servernames";

    /**
     * Site name.
     */
    String SITE_NAME = "sitename";
    
    /**
     * Site URL.
     */
    String SITE_URL = "siteurl";

    /**
     * Site ID.
     */
    String SITE_ID = "siteid";
    
    /**
     * Site secondary URLs.
     */
    String SECONDARY_URLS = "secondaryurls";

    /**
     * Embedded store port.
     */
    String EMBEDDED_PORT = "port";

    /**
     * Embedded store password
     */
    String EMBEDDED_PASSWORD = "password";

    /**
     * Server URL option
     */
    String SERVER_URL = "serverurl";

    /**
     * File option name.
     */
    String FILE = "file";

    /**
     * Names Only option
     */
    String NAMES_ONLY = "namesonly";
}
