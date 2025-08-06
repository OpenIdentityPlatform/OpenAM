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
 * $Id: ConfiguratorPlugin.java,v 1.4 2008/06/25 05:44:02 qcheng Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.setup;

import com.iplanet.sso.SSOToken;
import jakarta.servlet.ServletContext;

public interface ConfiguratorPlugin {
    /**
     * Re-initialize configuration file.
     *
     * @param baseDir Base directory of the configuration data store.
     */
    void reinitConfiguratioFile(String baseDir);

    /**
     * Does post configuration task.
     *
     * @param servletCtx Servlet Context.
     * @param adminSSOToken Super Administrator Single Sign On Token
     */
    void doPostConfiguration(ServletContext servletCtx, SSOToken adminSSOToken);
}
