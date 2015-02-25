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
 * $Id: ConfigUtil.java,v 1.2 2008/06/25 05:51:28 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

import com.sun.identity.install.tools.launch.IAdminTool;

public class ConfigUtil {

    public static boolean isDebugEnabled() {
        return Boolean.getBoolean(IAdminTool.PROP_DEBUG_ENABLED);
    }

    public static String getHomePath() {
        return System.getProperty(IAdminTool.PROP_PRODUCT_HOME);
    }

    public static String getBinDirPath() {
        return System.getProperty(IAdminTool.PROP_BIN_DIR);
    }

    public static String getConfigDirPath() {
        return System.getProperty(IAdminTool.PROP_CONFIG_DIR);
    }

    public static String getDataDirPath() {
        return System.getProperty(IAdminTool.PROP_DATA_DIR);
    }

    public static String getEtcDirPath() {
        return System.getProperty(IAdminTool.PROP_ETC_DIR);
    }

    public static String getJCEDirPath() {
        return System.getProperty(IAdminTool.PROP_JCE_DIR);
    }

    public static String getJSSEDirPath() {
        return System.getProperty(IAdminTool.PROP_JSSE_DIR);
    }

    public static String getLibPath() {
        return System.getProperty(IAdminTool.PROP_LIB_DIR);
    }

    public static String getLocaleDirPath() {
        return System.getProperty(IAdminTool.PROP_LOCALE_DIR);
    }

    public static String getLogsDirPath() {
        return System.getProperty(IAdminTool.PROP_LOGS_DIR);
    }

}
