/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: IConstants.java,v 1.1 2009/06/12 22:03:03 huacui Exp $
 *
 */

package com.sun.identity.agents.install.appserver.v91;

public interface IConstants {
        
    /** Field STR_AS_GROUP */
    public static String STR_AS_GROUP = "as91Tools";
   
    /** Field STR_FORWARD_SLASH */
    public static final String STR_FORWARD_SLASH = "/";

    /** key to lookup webservices-rt.jar */
    public static final String STR_WS_RT_JAR_FILE = "webservices-rt.jar";

    /** key to lookup webservices-tools.jar */
    public static final String STR_WS_TOOLS_JAR_FILE = "webservices-tools.jar";

    /** key to lookup webservices-tools.jar */
    public static final String STR_WS_API_JAR_FILE = "webservices-api.jar";

    /** key to lookup OS type */
    public static final String STR_OS_NAME_PROPERTY = "os.name";

    /** OS type of Windows */
    public static final String STR_WINDOWS = "windows";

    /** key to lookup AS lib directory */
    public static final String STR_KEY_AS_LIB_DIR = "AS_LIB_DIR";

    public static final String STR_AS_ENDORSED_DIR = "endorsed";

    public static final String STR_AS_LIB_DIR = "lib";

    public static final String STR_ADDONS_DIR = "addons";

    public static final String STR_OPENSSO = "opensso";

    public static final String STR_AMCONFIG_FILE = "AMConfig.properties";
}


