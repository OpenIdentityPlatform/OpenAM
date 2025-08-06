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
 * $Id: MonitoringConfiguration.java,v 1.1 2009/06/19 02:29:39 bigfatrat Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.common;

import com.sun.identity.setup.AMSetupServlet;
import com.iplanet.services.naming.WebtopNaming;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;


/**
 * This class gathers the configuration information for the
 * monitoring service, which is initially started in WebtopNaming.java
 * Configuration information can be gathered after Session services
 * have started up.
 */

public class MonitoringConfiguration extends HttpServlet
{

    /**
     * Initializes the servlet.  This method does all the "work"
     * of gathering the configuration information for, and passing it
     * to the Monitoring service.
     * @param config servlet config
     * @throws ServletException if it fails to get servlet context.
     */

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (!AMSetupServlet.isCurrentConfigurationValid()) {  // skip if doing config
//            System.err.println ("MonitoringConfigure: server not configured");
            return;
        }
	if (WebtopNaming.configMonitoring() == 0) {
            ConfigMonitoring cm = new ConfigMonitoring();
            cm.configureMonitoring();
	}
    }
}
