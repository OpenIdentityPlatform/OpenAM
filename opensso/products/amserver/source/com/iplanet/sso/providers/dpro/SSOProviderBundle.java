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
 * $Id: SSOProviderBundle.java,v 1.3 2008/06/25 05:41:43 qcheng Exp $
 *
 */

package com.iplanet.sso.providers.dpro;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class represents to get the locale-specific resource strings for the 
 * sso classes.
 * 
 */
  

public class SSOProviderBundle {

    /** The resource bundle */
    private static ResourceBundle ssoProviderBundle = null;

    /** the default resource bundle for AM provider amSSOProvider */
    public static String rbName = "amSSOProvider";

   /**
    * 
    * Gets a locale string for the given key string from the resource bundle
    */

    public static String getString(String str) {
        if (ssoProviderBundle == null) {
            synchronized (SSOProviderBundle.class) {
                if (ssoProviderBundle == null) {
                    ssoProviderBundle = com.sun.identity.shared.locale.Locale
                            .getInstallResourceBundle(rbName);
                }
            }
        }
        try {
            return ssoProviderBundle.getString(str);
        } catch (MissingResourceException mre) {
            SSOProviderImpl.debug.error("Missing resource: " + str, mre);
            return null;
        }
    }
}
