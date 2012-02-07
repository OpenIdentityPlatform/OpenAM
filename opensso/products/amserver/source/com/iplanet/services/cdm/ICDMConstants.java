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
 * $Id: ICDMConstants.java,v 1.4 2008/06/25 05:41:33 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.iplanet.services.cdm;

import com.iplanet.am.util.SystemProperties;

interface ICDMConstants {
    public static final String G11N_SETTINGS_SERVICE_NAME = 
        "iPlanetG11NSettings";

    public static final String LOCALE_CHARSET_ATTR = 
        "sun-identity-g11n-settings-locale-charset-mapping";

    public static final String CHARSET_ALIAS_ATTR = 
        "sun-identity-g11n-settings-charset-alias-mapping";

    public static final String JAVA_CHARSET_NAME = "javaname";

    public static final String CDM_ACCEPT_CHARSET = "CcppAccept-Charset";

    public static final String DEFAULT_CHARSET_PROPERTY ="openam.cdm.default.charset";

    public static final String CDM_DEFAULT_CHARSET = 
            SystemProperties.get(DEFAULT_CHARSET_PROPERTY,
            "UTF-8");

    public static final String CDM_DEFAULT_CLIENT_TYPE ="genericHTML";

    /**
     * Default content type for unidentified client type.
     */
    public static final String CDM_DEFAULT_CONTENT_TYPE ="text/html";
}
