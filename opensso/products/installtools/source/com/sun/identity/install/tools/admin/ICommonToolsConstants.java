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
 * $Id: ICommonToolsConstants.java,v 1.2 2008/06/25 05:51:16 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.admin;

public interface ICommonToolsConstants {

    /** Field FILE_SEP **/
    // All paths must always use forward slashes only
    public static final String FILE_SEP = "/";

    /** Field LINE_SEP **/
    public static final String LINE_SEP = System.getProperty("line.separator");

    /** Field INSTANCE_CONFIG_DIR_NAME **/
    public static final String INSTANCE_CONFIG_DIR_NAME = "config";

    /** Field INSTANCE_LIB_DIR_NAME **/
    public static final String INSTANCE_LIB_DIR_NAME = "lib";

    /** Field INSTANCE_CONFIG_DIR_NAME **/
    public static final String INSTANCE_LOCALE_DIR_NAME = "locale";

    /** Field INSTANCE_LOGS_DIR_NAME **/
    public static final String INSTANCE_LOGS_DIR_NAME = "logs";

    /** Field INSTANCE_DEBUG_DIR_NAME **/
    public static final String INSTANCE_DEBUG_DIR_NAME = "debug";

    /** Field INSTANCE_AUDIT_DIR_NAME **/
    public static final String INSTANCE_AUDIT_DIR_NAME = "audit";

}
