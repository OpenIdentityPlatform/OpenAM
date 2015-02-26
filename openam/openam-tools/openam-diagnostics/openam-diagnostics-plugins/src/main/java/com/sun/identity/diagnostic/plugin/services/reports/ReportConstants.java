/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ReportConstants.java,v 1.1 2008/11/22 02:41:20 ak138937 Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.diagnostic.plugin.services.reports;

import com.sun.identity.shared.Constants;

/**
 * This interface contains the property names used by the 
 * report service.
 */
public interface ReportConstants extends Constants {
    /**
     * Resource file name used by Report service
     */
    String REPORT_RESOURCE_BUNDLE = "ServerConfigReport";
    String REPORT_SVR_BOOTFILE = "bootstrap";
    String AGENT_CFG_TYPE = 
        "com.sun.identity.agents.config.repository.location";

    final String OSE_MGR_PROP = "Sun OpenSSO Enterprise Properties";
    final String SYS_PROP = "System Properties";
    final String DEF_PROP = "Server Default Properties";
    final String AM_PROP_SUN_SUFFIX="com.sun";
    final String AM_PROP_SUFFIX="com.iplanet";
    final String AGENT_ORGANIZATION_NAME =
            "com.sun.identity.agents.config.organization.name";

    final String SMALL_LINE_SEP = "---------------------------------------";
    final String LINE_SEP = "-----------------------------------------------" + 
        "----------------------";
    final String PARA_SEP = "===============================================" +  
        "======================";
}
