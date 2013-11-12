/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.agents.jboss.install;

/**
 * Provides constants for the JBoss installer component.
 *
 * @author Peter Major
 */
public class InstallerConstants {

    public static final String BUNDLE_NAME = "jbossv7-tools";
    public static final String HOME_DIR = "HOME_DIR";
    public static final String INSTANCE_NAME = "INSTANCE_NAME";
    public static final String CONFIG_FILE = "CONFIG_FILE";
    public static final String GLOBAL_MODULE = "GLOBAL_MODULE";
    public static final String LOC_HOME_DIR_INVALID = "HOME_DIR_INVALID";
    public static final String LOC_HOME_DIR_VALID = "HOME_DIR_VALID";
    public static final String LOC_VERSION_INVALID = "VERSION_INVALID";
    public static final String LOC_VERSION_VALID = "VERSION_VALID";
    public static final String LOC_UPDATE_CONFIG_XML_EXECUTE = "UPDATE_CONFIG_XML_EXECUTE";
    public static final String LOC_UPDATE_CONFIG_XML_ROLLBACK = "UPDATE_CONFIG_XML_ROLLBACK";
    public static final String LOC_MODULE_MKDIR_FAIL = "MODULE_MKDIR_FAIL";
    public static final String LOC_ADD_AGENT_MODULE_EXECUTE = "ADD_AGENT_MODULE_EXECUTE";
    public static final String LOC_ADD_AGENT_MODULE_ROLLBACK = "ADD_AGENT_MODULE_ROLLBACK";
}
