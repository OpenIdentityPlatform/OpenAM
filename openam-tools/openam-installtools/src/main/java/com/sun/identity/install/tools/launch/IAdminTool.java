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
 * $Id: IAdminTool.java,v 1.2 2008/06/25 05:51:27 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.launch;

import java.util.List;

public interface IAdminTool {

    public void run(List args);

    public static final String STR_DEBUG_OPTION = "--debug";

    public static final String PROP_PREFIX = "com.sun.identity.product.";

    public static final String PROP_PRODUCT_HOME = PROP_PREFIX + 
        "install.home";

    public static final String PROP_BIN_DIR = PROP_PREFIX + "bin.dir";

    public static final String PROP_JCE_DIR = PROP_PREFIX + "jce.dir";

    public static final String PROP_JSSE_DIR = PROP_PREFIX + "jsse.dir";

    public static final String PROP_CONFIG_DIR = PROP_PREFIX + "config.dir";

    public static final String PROP_LOCALE_DIR = PROP_PREFIX + "locale.dir";

    public static final String PROP_LIB_DIR = PROP_PREFIX + "lib.dir";

    public static final String PROP_LOGS_DIR = PROP_PREFIX + "logs.dir";

    public static final String PROP_DATA_DIR = PROP_PREFIX + "data.dir";

    public static final String PROP_ETC_DIR = PROP_PREFIX + "etc.dir";

    public static final String PROP_DEBUG_ENABLED = PROP_PREFIX
            + "debug.enable";

    public static final String PROP_REGISTER_JCE_PROVIDER = PROP_PREFIX
            + "register.jce.provider";

    public static final String PROP_REGISTER_JSSE_PROVIDER = PROP_PREFIX
            + "register.jsse.provider";
}
